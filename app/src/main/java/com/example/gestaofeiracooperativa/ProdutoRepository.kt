package com.example.gestaofeiracooperativa

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class ProdutoRepository(private val produtoDao: ProdutoDao) {

    // Obtém a instância do Cloud Firestore
    private val firestore = Firebase.firestore
    // Define um nome para a nossa coleção de produtos na nuvem
    private val produtosCollection = firestore.collection("produtos")

    init {
        // Inicia um ouvinte em tempo real para a coleção de produtos
        // Isso manterá o banco de dados local (Room) sempre sincronizado com a nuvem
        ouvirAtualizacoesDeProdutos()
    }

    // --- FUNÇÃO PRINCIPAL DE LEITURA ---
    // A UI observará esta função. Ela lê do banco de dados local (Room),
    // que é atualizado em tempo real pelo ouvinte do Firestore.
    fun getAllProducts(): Flow<List<Produto>> = produtoDao.getAllProducts()


    // --- FUNÇÕES DE ESCRITA (Agora salvam na NUVEM) ---

    /**
     * Insere um novo produto.
     * Primeiro salva no Firestore, depois atualiza o cache local no Room.
     * Retorna true se teve sucesso, false se falhou.
     */
    suspend fun insert(produto: Produto): Boolean {
        return try {
            // Usa o 'numero' do produto como ID do documento no Firestore
            produtosCollection.document(produto.numero).set(produto).await()
            Log.d("ProdutoRepository", "Produto salvo no Firestore com sucesso: ${produto.numero}")
            // A atualização no Room será feita automaticamente pelo ouvinte
            true
        } catch (e: Exception) {
            Log.e("ProdutoRepository", "Erro ao salvar produto no Firestore", e)
            false
        }
    }

    /**
     * Atualiza um produto existente.
     * Funciona da mesma forma que o insert, sobrescrevendo o documento no Firestore.
     */
    suspend fun update(produto: Produto): Boolean {
        return insert(produto) // A função 'set' do Firestore serve tanto para criar quanto para sobrescrever/atualizar
    }

    /**
     * Deleta um produto.
     * Primeiro deleta do Firestore, depois a mudança será refletida no Room pelo ouvinte.
     */
    suspend fun delete(produto: Produto): Boolean {
        return try {
            produtosCollection.document(produto.numero).delete().await()
            Log.d("ProdutoRepository", "Produto deletado do Firestore com sucesso: ${produto.numero}")
            true
        } catch (e: Exception) {
            Log.e("ProdutoRepository", "Erro ao deletar produto do Firestore", e)
            false
        }
    }


    // --- LÓGICA DE SINCRONIZAÇÃO ---

    private fun ouvirAtualizacoesDeProdutos() {
        produtosCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("ProdutoRepository", "Ouvinte do Firestore falhou.", e)
                return@addSnapshotListener
            }

            if (snapshots != null) {
                val produtosDaNuvem = snapshots.toObjects(Produto::class.java)
                Log.d("ProdutoRepository", "Recebidos ${produtosDaNuvem.size} produtos da nuvem. Sincronizando com o banco local.")
                // Usa uma coroutine para não bloquear a thread principal
                CoroutineScope(Dispatchers.IO).launch {
                    sincronizarBancoLocal(produtosDaNuvem)
                }
            }
        }
    }

    private suspend fun sincronizarBancoLocal(produtosDaNuvem: List<Produto>) {
        try {
            // Estratégia simples: apaga tudo e insere os dados novos da nuvem.
            produtoDao.deleteAll()
            produtoDao.insertAll(produtosDaNuvem)
            Log.d("ProdutoRepository", "Banco local sincronizado com sucesso.")
        } catch (e: Exception) {
            Log.e("ProdutoRepository", "Erro ao sincronizar banco de dados local.", e)
        }
    }

    // Funções antigas de busca e get by number continuam lendo do cache local
    fun searchProducts(query: String): Flow<List<Produto>> = produtoDao.searchProducts(query)
    suspend fun getProductByNumber(numero: String): Produto? = produtoDao.getProductByNumber(numero)
}