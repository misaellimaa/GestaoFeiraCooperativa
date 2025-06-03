package com.example.gestaofeiracooperativa // Certifique-se que é o seu package correto

import kotlinx.coroutines.flow.Flow

// Importe DespesaFeiraEntity e DespesaFeiraDao
// (Certifique-se que os paths estão corretos, provavelmente do seu DespesaModels.kt e do arquivo DAO)
// import com.example.gestaofeiracooperativa.DespesaFeiraEntity
// import com.example.gestaofeiracooperativa.DespesaFeiraDao

class DespesaFeiraRepository(private val despesaFeiraDao: DespesaFeiraDao) {

    /**
     * Busca todos os lançamentos de despesa para uma feira específica.
     * Retorna um Flow para observação pela UI.
     */
    fun getDespesasByFeiraId(feiraId: String): Flow<List<DespesaFeiraEntity>> {
        return despesaFeiraDao.getDespesasByFeiraId(feiraId)
    }

    /**
     * Busca um lançamento de despesa específico para um item de despesa dentro de uma feira.
     */
    suspend fun getDespesaByFeiraAndItemDespesaId(feiraId: String, itemDespesaId: Long): DespesaFeiraEntity? {
        return despesaFeiraDao.getDespesaByFeiraAndItemDespesaId(feiraId, itemDespesaId)
    }

    /**
     * Insere ou atualiza um lançamento de despesa para uma feira.
     * O DAO usa OnConflictStrategy.REPLACE.
     */
    suspend fun insertOrUpdateDespesa(despesa: DespesaFeiraEntity) {
        despesaFeiraDao.insertOrUpdate(despesa)
    }

    /**
     * Deleta um lançamento de despesa específico.
     */
    suspend fun deleteDespesa(despesa: DespesaFeiraEntity) {
        despesaFeiraDao.delete(despesa)
    }

    /**
     * Deleta todos os lançamentos de despesa para uma feira específica.
     */
    suspend fun deleteDespesasByFeiraId(feiraId: String) {
        despesaFeiraDao.deleteDespesasByFeiraId(feiraId)
    }

    suspend fun deleteDespesaByFeiraAndItem(feiraId: String, itemDespesaId: Long) {
        despesaFeiraDao.deleteByFeiraAndItemDespesaId(feiraId, itemDespesaId)
    }
}