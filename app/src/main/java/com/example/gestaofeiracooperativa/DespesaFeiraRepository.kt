package com.example.gestaofeiracooperativa // Certifique-se que é o seu package correto

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await


// Importe DespesaFeiraEntity e DespesaFeiraDao
// (Certifique-se que os paths estão corretos, provavelmente do seu DespesaModels.kt e do arquivo DAO)
// import com.example.gestaofeiracooperativa.DespesaFeiraEntity
// import com.example.gestaofeiracooperativa.DespesaFeiraDao

class DespesaFeiraRepository(private val despesaFeiraDao: DespesaFeiraDao) {
    private val feirasCollection = Firebase.firestore.collection("feiras")

    fun getDespesasByFeiraId(feiraId: String): Flow<List<DespesaFeiraEntity>> {
        return despesaFeiraDao.getDespesasByFeiraId(feiraId)
    }

    // <<< FUNÇÃO ATUALIZADA para retornar Boolean não-nulável >>>
    suspend fun insertOrUpdateDespesa(despesa: DespesaFeiraEntity): Boolean {
        return try {
            val despesaDocRef = feirasCollection
                .document(despesa.feiraId)
                .collection("despesas")
                .document(despesa.itemDespesaId.toString())

            despesaDocRef.set(despesa).await()
            despesaFeiraDao.insertOrUpdate(despesa)
            true // Sucesso
        } catch (e: Exception) {
            Log.e("DespesaFeiraRepo", "Erro ao salvar despesa no Firestore", e)
            false // Falha
        }
    }

    // <<< FUNÇÃO ATUALIZADA para retornar Boolean não-nulável >>>
    suspend fun deleteDespesaByFeiraAndItem(feiraId: String, itemDespesaId: Long): Boolean {
        return try {
            feirasCollection.document(feiraId).collection("despesas")
                .document(itemDespesaId.toString()).delete().await()

            despesaFeiraDao.deleteByFeiraAndItemDespesaId(feiraId, itemDespesaId)
            true // Sucesso
        } catch (e: Exception) {
            Log.e("DespesaFeiraRepo", "Erro ao deletar despesa do Firestore", e)
            false // Falha
        }
    }



}