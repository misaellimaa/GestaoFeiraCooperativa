package com.example.gestaofeiracooperativa

import kotlinx.coroutines.flow.Flow

class PerdaRepository(private val perdaDao: PerdaDao) { // 'private val' está correto aqui

    fun getPerdasForFeira(feiraId: String): Flow<List<PerdaEntity>> = perdaDao.getPerdasForFeira(feiraId)

    suspend fun update(perda: PerdaEntity) = perdaDao.update(perda)

    // --- NOVAS FUNÇÕES PARA O VIEWMODEL ---

    suspend fun deleteAllPerdasForFeira(feiraId: String) {
        perdaDao.deleteAllPerdasForFeira(feiraId)
    }

    suspend fun insertAllPerdas(perdas: List<PerdaEntity>) {
        perdaDao.insertAllPerdas(perdas)
    }
}