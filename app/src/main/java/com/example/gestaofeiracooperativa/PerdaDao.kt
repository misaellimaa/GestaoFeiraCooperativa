package com.example.gestaofeiracooperativa // <<-- Seu package

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PerdaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerda(perda: PerdaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPerdas(perdas: List<PerdaEntity>)

    @Query("SELECT * FROM perdas_feira WHERE feiraId = :feiraId")
    fun getPerdasForFeira(feiraId: String): Flow<List<PerdaEntity>>

    @Query("SELECT * FROM perdas_feira WHERE feiraId = :feiraId AND produtoNumero = :produtoNumero LIMIT 1")
    suspend fun getPerdaEspecifica(feiraId: String, produtoNumero: String): PerdaEntity?

    // Útil para quando você quiser substituir todas as perdas de uma feira
    @Query("DELETE FROM perdas_feira WHERE feiraId = :feiraId")
    suspend fun deleteAllPerdasForFeira(feiraId: String)

    @Update
    suspend fun update(perda: PerdaEntity)
}