package com.example.gestaofeiracooperativa

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class ItemDespesaRepository(private val itemDespesaDao: ItemDespesaDao) {

    private val firestore = Firebase.firestore
    private val itensDespesaCollection = firestore.collection("itens_despesa")

    fun getAllItensDespesa(): Flow<List<ItemDespesaEntity>> = itemDespesaDao.getAllItensDespesa()

    // --- ESCRITA SEGURA ---
    suspend fun insertItemDespesa(itemDespesa: ItemDespesaEntity): Boolean {
        return try {
            // 1. Insere no Room para gerar o ID
            val novoId = itemDespesaDao.insertItemDespesa(itemDespesa)
            val itemComId = itemDespesa.copy(id = novoId)
            // 2. Envia para a nuvem com o ID correto
            itensDespesaCollection.document(novoId.toString()).set(itemComId).await()
            Log.d("ItemDespesaRepo", "Item salvo: ${itemComId.nome}")
            true
        } catch (e: Exception) {
            Log.e("ItemDespesaRepo", "Erro ao salvar item na nuvem.", e)
            // Como já salvou no Room na linha 1, retornamos true
            true
        }
    }

    suspend fun updateItemDespesa(itemDespesa: ItemDespesaEntity): Boolean {
        if (itemDespesa.id == 0L) return false
        return try {
            itemDespesaDao.updateItemDespesa(itemDespesa) // Update Local
            itensDespesaCollection.document(itemDespesa.id.toString()).set(itemDespesa).await()
            true
        } catch (e: Exception) {
            Log.e("ItemDespesaRepo", "Erro update nuvem.", e)
            true
        }
    }

    suspend fun deleteItemDespesa(itemDespesa: ItemDespesaEntity): Boolean {
        if (itemDespesa.id == 0L) return false
        return try {
            itensDespesaCollection.document(itemDespesa.id.toString()).delete().await()
            itemDespesaDao.deleteItemDespesa(itemDespesa)
            true
        } catch (e: Exception) {
            itemDespesaDao.deleteItemDespesa(itemDespesa)
            Log.e("ItemDespesaRepo", "Erro delete nuvem.", e)
            true
        }
    }

    // --- PUSH ---
    suspend fun sincronizarItensLocaisParaNuvem() {
        val locais = itemDespesaDao.getAllItensDespesa().firstOrNull() ?: emptyList()
        locais.forEach { item ->
            try {
                itensDespesaCollection.document(item.id.toString()).set(item).await()
            } catch (e: Exception) { Log.w("ItemRepo", "Erro upload item ${item.id}", e) }
        }
    }

    // --- PULL (Merge) ---
    suspend fun sincronizarItensDespesaDoFirestore() {
        try {
            val snapshots = itensDespesaCollection.get().await()
            if (snapshots != null && !snapshots.isEmpty) {
                val nuvem = snapshots.toObjects(ItemDespesaEntity::class.java)
                itemDespesaDao.insertAll(nuvem) // Merge seguro
            }
        } catch (e: Exception) { Log.e("ItemRepo", "Erro download itens.", e) }
    }

    fun searchItensDespesa(query: String): Flow<List<ItemDespesaEntity>> = itemDespesaDao.searchItensDespesa(query)
    suspend fun getItemDespesaByNome(nome: String): ItemDespesaEntity? = itemDespesaDao.getItemDespesaByNome(nome)
}