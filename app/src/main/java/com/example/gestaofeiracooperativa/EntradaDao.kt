package com.example.gestaofeiracooperativa // <<-- Seu package

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EntradaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntrada(entrada: EntradaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEntradas(entradas: List<EntradaEntity>)

    @Query("SELECT * FROM entradas_feira WHERE feiraId = :feiraId")
    fun getEntradasForFeira(feiraId: String): Flow<List<EntradaEntity>>

    @Query("SELECT * FROM entradas_feira WHERE feiraId = :feiraId AND agricultorId = :agricultorId")
    fun getEntradasForAgricultorInFeira(feiraId: String, agricultorId: String): Flow<List<EntradaEntity>>


    @Query("SELECT * FROM entradas_feira WHERE feiraId = :feiraId AND agricultorId = :agricultorId AND produtoNumero = :produtoNumero LIMIT 1")
    suspend fun getEntradaEspecifica(feiraId: String, agricultorId: String, produtoNumero: String): EntradaEntity?

    // Útil para quando você quiser substituir todas as entradas de um agricultor em uma feira
    @Query("DELETE FROM entradas_feira WHERE feiraId = :feiraId AND agricultorId = :agricultorId")
    suspend fun deleteEntradasByFeiraAndAgricultor(feiraId: String, agricultorId: String)

    @Query("DELETE FROM entradas_feira WHERE feiraId = :feiraId")
    suspend fun deleteAllEntradasForFeira(feiraId: String)
}