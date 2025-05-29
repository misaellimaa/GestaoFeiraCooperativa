package com.example.gestaofeiracooperativa // <<--- MUDE PARA O SEU PACKAGE REAL

import kotlinx.coroutines.flow.Flow // Import Flow

class AgricultorRepository(private val agricultorDao: AgricultorDao) {

    fun getAllAgricultores(): Flow<List<Agricultor>> = agricultorDao.getAllAgricultores()

    fun searchAgricultores(query: String): Flow<List<Agricultor>> = agricultorDao.searchAgricultores(query)

    suspend fun getAgricultorById(id: String): Agricultor? = agricultorDao.getAgricultorById(id)

    suspend fun insert(agricultor: Agricultor) {
        agricultorDao.insert(agricultor)
    }

    suspend fun update(agricultor: Agricultor) {
        agricultorDao.update(agricultor)
    }

    suspend fun delete(agricultor: Agricultor) {
        agricultorDao.delete(agricultor)
    }

    suspend fun deleteAllAgricultores() { // Adicionado para limpeza, se necessário
        agricultorDao.deleteAllAgricultores()
    }
}
