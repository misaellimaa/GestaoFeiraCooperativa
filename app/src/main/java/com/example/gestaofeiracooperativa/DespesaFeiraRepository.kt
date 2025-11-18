package com.example.gestaofeiracooperativa

import android.util.Log
import com.google.firebase.firestore.ktx.firestore // Use a importação ktx padrão se possível
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class DespesaFeiraRepository(private val despesaFeiraDao: DespesaFeiraDao) {

    private val feirasCollection = Firebase.firestore.collection("feiras")

    fun getDespesasByFeiraId(feiraId: String): Flow<List<DespesaFeiraEntity>> {
        return despesaFeiraDao.getDespesasByFeiraId(feiraId)
    }

    // <<< CORREÇÃO: PRIORIDADE LOCAL (Offline Safe) >>>
    suspend fun insertOrUpdateDespesa(despesa: DespesaFeiraEntity): Boolean {
        return try {
            // 1. Salva LOCALMENTE primeiro (Garante que o dado não se perde)
            despesaFeiraDao.insertOrUpdate(despesa)

            // 2. Tenta enviar para a NUVEM
            val despesaDocRef = feirasCollection
                .document(despesa.feiraId)
                .collection("despesas")
                .document(despesa.itemDespesaId.toString())

            despesaDocRef.set(despesa).await()

            true // Sucesso total
        } catch (e: Exception) {
            Log.e("DespesaFeiraRepo", "Salvo apenas localmente. Erro na nuvem.", e)
            // Retorna TRUE porque para o usuário o dado está salvo (no celular)
            true
        }
    }

    // <<< CORREÇÃO: PRIORIDADE LOCAL (Offline Safe) >>>
    suspend fun deleteDespesaByFeiraAndItem(feiraId: String, itemDespesaId: Long): Boolean {
        return try {
            // 1. Deleta LOCALMENTE primeiro (UI atualiza na hora)
            despesaFeiraDao.deleteByFeiraAndItemDespesaId(feiraId, itemDespesaId)

            // 2. Tenta deletar na NUVEM
            feirasCollection.document(feiraId)
                .collection("despesas")
                .document(itemDespesaId.toString())
                .delete()
                .await()

            true // Sucesso total
        } catch (e: Exception) {
            Log.e("DespesaFeiraRepo", "Deletado apenas localmente. Erro na nuvem.", e)
            // Retorna TRUE porque a ação foi concluída no celular
            true
        }
    }
}