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

class ItemDespesaRepository(private val itemDespesaDao: ItemDespesaDao) {

    private val firestore = Firebase.firestore
    private val itensDespesaCollection = firestore.collection("itens_despesa")

    init {
        ouvirAtualizacoesDeItensDespesa()
    }

    // A UI continua lendo do Room, que agora é um cache em tempo real
    fun getAllItensDespesa(): Flow<List<ItemDespesaEntity>> = itemDespesaDao.getAllItensDespesa()

    // --- Funções de Escrita agora operam primeiro no Room (para obter o ID) e depois no Firestore ---

    suspend fun insertItemDespesa(itemDespesa: ItemDespesaEntity): Boolean {
        return try {
            // 1. Insere no Room primeiro para obter o ID autogerado
            val novoId = itemDespesaDao.insertItemDespesa(itemDespesa)

            // 2. Cria o objeto final com o ID correto para salvar no Firestore
            val itemComId = itemDespesa.copy(id = novoId)

            // 3. Usa o ID gerado pelo Room como ID do documento no Firestore
            itensDespesaCollection.document(novoId.toString()).set(itemComId).await()
            Log.d("ItemDespesaRepo", "Item de despesa salvo com sucesso: ID=${novoId}, Nome=${itemComId.nome}")
            true
        } catch (e: Exception) {
            Log.e("ItemDespesaRepo", "Erro ao salvar item de despesa: ${itemDespesa.nome}", e)
            false
        }
    }

    suspend fun updateItemDespesa(itemDespesa: ItemDespesaEntity): Boolean {
        // Para atualizar, o objeto 'itemDespesa' já deve ter o ID correto do Room.
        if (itemDespesa.id == 0L) {
            Log.e("ItemDespesaRepo", "Tentativa de atualizar item de despesa sem um ID válido.")
            return false
        }
        return try {
            itensDespesaCollection.document(itemDespesa.id.toString()).set(itemDespesa).await()
            Log.d("ItemDespesaRepo", "Item de despesa atualizado com sucesso: ID=${itemDespesa.id}")
            true
        } catch (e: Exception) {
            Log.e("ItemDespesaRepo", "Erro ao atualizar item de despesa: ID=${itemDespesa.id}", e)
            false
        }
    }

    suspend fun deleteItemDespesa(itemDespesa: ItemDespesaEntity): Boolean {
        if (itemDespesa.id == 0L) {
            Log.e("ItemDespesaRepo", "Tentativa de deletar item de despesa sem um ID válido.")
            return false
        }
        return try {
            itensDespesaCollection.document(itemDespesa.id.toString()).delete().await()
            Log.d("ItemDespesaRepo", "Item de despesa deletado com sucesso: ID=${itemDespesa.id}")
            true
        } catch (e: Exception) {
            Log.e("ItemDespesaRepo", "Erro ao deletar item de despesa: ID=${itemDespesa.id}", e)
            false
        }
    }

    // --- Lógica de Sincronização ---
    private fun ouvirAtualizacoesDeItensDespesa() {
        itensDespesaCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("ItemDespesaRepo", "Ouvinte do Firestore para itens de despesa falhou.", e)
                return@addSnapshotListener
            }
            if (snapshots != null) {
                val itensDaNuvem = snapshots.toObjects(ItemDespesaEntity::class.java)
                Log.d("ItemDespesaRepo", "Recebidos ${itensDaNuvem.size} itens de despesa da nuvem.")
                CoroutineScope(Dispatchers.IO).launch {
                    sincronizarBancoLocal(itensDaNuvem)
                }
            }
        }
    }

    private suspend fun sincronizarBancoLocal(itensDaNuvem: List<ItemDespesaEntity>) {
        try {
            // A estratégia 'REPLACE' no insertAll cuidará de inserir novos e atualizar existentes.
            // Deletar tudo primeiro garante que itens removidos na nuvem também sejam removidos localmente.
            itemDespesaDao.deleteAll()
            itemDespesaDao.insertAll(itensDaNuvem)
            Log.d("ItemDespesaRepo", "Banco local de itens de despesa sincronizado.")
        } catch (e: Exception) {
            Log.e("ItemDespesaRepo", "Erro ao sincronizar banco de itens de despesa.", e)
        }
    }

    fun searchItensDespesa(query: String): Flow<List<ItemDespesaEntity>> = itemDespesaDao.searchItensDespesa(query)
    suspend fun getItemDespesaByNome(nome: String): ItemDespesaEntity? = itemDespesaDao.getItemDespesaByNome(nome)
}