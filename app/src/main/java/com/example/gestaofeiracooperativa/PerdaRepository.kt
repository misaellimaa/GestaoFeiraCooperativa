package com.example.gestaofeiracooperativa

import kotlinx.coroutines.flow.Flow

class PerdaRepository(private val perdaDao: PerdaDao) {
    fun getPerdasForFeira(feiraId: String): Flow<List<PerdaEntity>> = perdaDao.getPerdasForFeira(feiraId)
    suspend fun update(perda: PerdaEntity) = perdaDao.update(perda)
}