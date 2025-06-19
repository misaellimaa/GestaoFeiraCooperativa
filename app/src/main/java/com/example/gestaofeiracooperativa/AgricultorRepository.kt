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

class AgricultorRepository(private val agricultorDao: AgricultorDao) {

    private val firestore = Firebase.firestore
    private val agricultoresCollection = firestore.collection("agricultores")

    init {
        ouvirAtualizacoesDeAgricultores()
    }

    // --- LEITURA: Continua lendo do cache local (Room) ---
    fun getAllAgricultores(): Flow<List<Agricultor>> = agricultorDao.getAllAgricultores()

    // --- ESCRITA: Agora salva/atualiza/deleta na nuvem (Firestore) ---

    suspend fun insert(agricultor: Agricultor): Boolean {
        return try {
            // Usa o 'id' do agricultor como ID do documento no Firestore
            agricultoresCollection.document(agricultor.id).set(agricultor).await()
            Log.d("AgricultorRepository", "Agricultor salvo no Firestore: ${agricultor.id}")
            true
        } catch (e: Exception) {
            Log.e("AgricultorRepository", "Erro ao salvar agricultor no Firestore", e)
            false
        }
    }

    suspend fun update(agricultor: Agricultor): Boolean {
        // A função 'set' do Firestore serve tanto para criar quanto para sobrescrever/atualizar
        return insert(agricultor)
    }

    suspend fun delete(agricultor: Agricultor): Boolean {
        return try {
            agricultoresCollection.document(agricultor.id).delete().await()
            Log.d("AgricultorRepository", "Agricultor deletado do Firestore: ${agricultor.id}")
            true
        } catch (e: Exception) {
            Log.e("AgricultorRepository", "Erro ao deletar agricultor do Firestore", e)
            false
        }
    }

    // --- LÓGICA DE SINCRONIZAÇÃO ---

    private fun ouvirAtualizacoesDeAgricultores() {
        agricultoresCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("AgricultorRepository", "Ouvinte do Firestore falhou.", e)
                return@addSnapshotListener
            }

            if (snapshots != null) {
                val agricultoresDaNuvem = snapshots.toObjects(Agricultor::class.java)
                Log.d("AgricultorRepository", "Recebidos ${agricultoresDaNuvem.size} agricultores da nuvem.")
                // Usa uma coroutine para não bloquear a thread principal
                CoroutineScope(Dispatchers.IO).launch {
                    sincronizarBancoLocal(agricultoresDaNuvem)
                }
            }
        }
    }

    private suspend fun sincronizarBancoLocal(agricultoresDaNuvem: List<Agricultor>) {
        try {
            // Estratégia de cache: apaga tudo e insere os dados novos da nuvem.
            agricultorDao.deleteAll()
            agricultorDao.insertAll(agricultoresDaNuvem)
            Log.d("AgricultorRepository", "Banco local de agricultores sincronizado.")
        } catch (e: Exception) {
            Log.e("AgricultorRepository", "Erro ao sincronizar banco de agricultores.", e)
        }
    }

    // Funções de busca continuam lendo do cache local, que é rápido
    fun searchAgricultores(query: String): Flow<List<Agricultor>> = agricultorDao.searchAgricultores(query)
    suspend fun getAgricultorById(id: String): Agricultor? = agricultorDao.getAgricultorById(id)
}