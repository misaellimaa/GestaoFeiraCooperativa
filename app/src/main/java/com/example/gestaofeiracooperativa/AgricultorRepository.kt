package com.example.gestaofeiracooperativa

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class AgricultorRepository(private val agricultorDao: AgricultorDao) {

    private val firestore = Firebase.firestore
    private val agricultoresCollection = firestore.collection("agricultores")

    fun getAllAgricultores(): Flow<List<Agricultor>> = agricultorDao.getAllAgricultores()

    // --- ESCRITA SEGURA (Local + Nuvem) ---
    suspend fun insert(agricultor: Agricultor): Boolean {
        return try {
            // 1. Salva localmente primeiro (para aparecer na tela na hora)
            agricultorDao.insert(agricultor)
            // 2. Tenta salvar na nuvem
            agricultoresCollection.document(agricultor.id).set(agricultor).await()
            Log.d("AgricultorRepository", "Agricultor salvo: ${agricultor.id}")
            true
        } catch (e: Exception) {
            Log.e("AgricultorRepository", "Salvo apenas localmente. Erro na nuvem.", e)
            true // Retorna true pois está salvo no celular (fonte principal)
        }
    }

    suspend fun update(agricultor: Agricultor): Boolean = insert(agricultor)

    suspend fun delete(agricultor: Agricultor): Boolean {
        return try {
            agricultoresCollection.document(agricultor.id).delete().await()
            agricultorDao.delete(agricultor)
            true
        } catch (e: Exception) {
            // Se falhar na nuvem, deleta localmente pelo menos
            agricultorDao.delete(agricultor)
            Log.e("AgricultorRepository", "Deletado localmente. Erro na nuvem.", e)
            true
        }
    }

    // --- PUSH: Envia dados locais para a nuvem ---
    suspend fun sincronizarAgricultoresLocaisParaNuvem() {
        Log.d("AgricultorRepo", "Enviando agricultores locais para a nuvem...")
        val locais = agricultorDao.getAllAgricultores().firstOrNull() ?: emptyList()
        locais.forEach { agricultor ->
            try {
                agricultoresCollection.document(agricultor.id).set(agricultor).await()
            } catch (e: Exception) {
                Log.w("AgricultorRepo", "Falha ao subir agricultor ${agricultor.id}", e)
            }
        }
    }

    // --- PULL: Baixa da nuvem sem apagar os locais (Merge) ---
    suspend fun sincronizarAgricultoresDoFirestore() {
        Log.d("AgricultorRepo", "Baixando agricultores da nuvem...")
        try {
            val snapshots = agricultoresCollection.get().await()
            if (snapshots != null && !snapshots.isEmpty) {
                val nuvem = snapshots.toObjects(Agricultor::class.java)
                // AGORA É SEGURO: Não usamos deleteAll(), apenas atualizamos/inserimos
                agricultorDao.insertAll(nuvem)
                Log.d("AgricultorRepo", "Agricultores sincronizados (Merge).")
            }
        } catch (e: Exception) {
            Log.e("AgricultorRepo", "Falha ao baixar agricultores.", e)
        }
    }

    fun searchAgricultores(query: String): Flow<List<Agricultor>> = agricultorDao.searchAgricultores(query)
    suspend fun getAgricultorById(id: String): Agricultor? = agricultorDao.getAgricultorById(id)
}