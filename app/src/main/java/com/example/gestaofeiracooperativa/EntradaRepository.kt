package com.example.gestaofeiracooperativa

import kotlinx.coroutines.flow.Flow

class EntradaRepository(private val entradaDao: EntradaDao) {
    fun getEntradasForFeira(feiraId: String): Flow<List<EntradaEntity>> = entradaDao.getEntradasForFeira(feiraId)
    suspend fun getEntradaEspecifica(feiraId: String, agricultorId: String, produtoNumero: String): EntradaEntity? =
        entradaDao.getEntradaEspecifica(feiraId, agricultorId, produtoNumero)
    suspend fun update(entrada: EntradaEntity) = entradaDao.update(entrada)
    suspend fun insert(entrada: EntradaEntity) = entradaDao.insertEntrada(entrada)
    suspend fun resetarSobrasParaFeira(feiraId: String) {
        entradaDao.resetarSobrasParaFeira(feiraId)
    }
}