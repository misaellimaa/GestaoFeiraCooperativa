package com.example.gestaofeiracooperativa

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class ProdutoRepository(private val produtoDao: ProdutoDao) {

    private val firestore = Firebase.firestore
    private val produtosCollection = firestore.collection("produtos")

    fun getAllProducts(): Flow<List<Produto>> = produtoDao.getAllProducts()

    // --- ESCRITA SEGURA ---
    suspend fun insert(produto: Produto): Boolean {
        return try {
            produtoDao.insert(produto) // Salva Local
            produtosCollection.document(produto.numero).set(produto).await() // Salva Nuvem
            Log.d("ProdutoRepository", "Produto salvo: ${produto.numero}")
            true
        } catch (e: Exception) {
            Log.e("ProdutoRepository", "Salvo localmente. Erro na nuvem.", e)
            true
        }
    }

    suspend fun update(produto: Produto): Boolean = insert(produto)

    suspend fun delete(produto: Produto): Boolean {
        return try {
            produtosCollection.document(produto.numero).delete().await()
            produtoDao.delete(produto)
            true
        } catch (e: Exception) {
            produtoDao.delete(produto)
            Log.e("ProdutoRepository", "Deletado localmente. Erro na nuvem.", e)
            true
        }
    }

    // --- PUSH ---
    suspend fun sincronizarProdutosLocaisParaNuvem() {
        Log.d("ProdutoRepo", "Enviando produtos locais para a nuvem...")
        val locais = produtoDao.getAllProducts().firstOrNull() ?: emptyList()
        locais.forEach { produto ->
            try {
                produtosCollection.document(produto.numero).set(produto).await()
            } catch (e: Exception) {
                Log.w("ProdutoRepo", "Falha ao subir produto ${produto.numero}", e)
            }
        }
    }

    // --- PULL (Merge) ---
    suspend fun sincronizarProdutosDoFirestore() {
        Log.d("ProdutoRepo", "Baixando produtos da nuvem...")
        try {
            val snapshots = produtosCollection.get().await()
            if (snapshots != null && !snapshots.isEmpty) {
                val nuvem = snapshots.toObjects(Produto::class.java)
                produtoDao.insertAll(nuvem) // Merge seguro
                Log.d("ProdutoRepo", "Produtos sincronizados (Merge).")
            }
        } catch (e: Exception) {
            Log.e("ProdutoRepo", "Falha ao baixar produtos.", e)
        }
    }

    fun searchProducts(query: String): Flow<List<Produto>> = produtoDao.searchProducts(query)
    suspend fun getProductByNumber(numero: String): Produto? = produtoDao.getProductByNumber(numero)
}