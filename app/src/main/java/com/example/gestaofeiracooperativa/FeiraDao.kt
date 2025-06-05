package com.example.gestaofeiracooperativa // <<-- Seu package

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FeiraDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeira(feira: FeiraEntity)

    @Update
    suspend fun updateFeira(feira: FeiraEntity)

    @Query("SELECT * FROM feiras WHERE feiraId = :id LIMIT 1")
    suspend fun getFeiraById(id: String): FeiraEntity?

    @Query("SELECT * FROM feiras ORDER BY CAST(feiraId AS INTEGER) ASC")
    fun getAllFeiras(): Flow<List<FeiraEntity>> // Observável, para listas que atualizam automaticamente

    @Query("DELETE FROM feiras WHERE feiraId = :id")
    suspend fun deleteFeiraById(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM feiras WHERE feiraId = :id LIMIT 1)")
    suspend fun feiraExists(id: String): Boolean

    @Query("SELECT * FROM feiras WHERE SUBSTR(startDate, 4, 2) = :mesStr AND SUBSTR(startDate, 7, 4) = :anoStr ORDER BY startDate ASC")
    fun getFeirasByMesAno(mesStr: String, anoStr: String): Flow<List<FeiraEntity>>

    @Query("""
        SELECT * FROM feiras 
        WHERE (SUBSTR(startDate, 7, 4) || '-' || SUBSTR(startDate, 4, 2) || '-' || SUBSTR(startDate, 1, 2)) 
              < (SELECT SUBSTR(startDate, 7, 4) || '-' || SUBSTR(startDate, 4, 2) || '-' || SUBSTR(startDate, 1, 2) FROM feiras WHERE feiraId = :feiraIdAtual)
        ORDER BY (SUBSTR(startDate, 7, 4) || '-' || SUBSTR(startDate, 4, 2) || '-' || SUBSTR(startDate, 1, 2)) DESC
        LIMIT 1
    """)
    suspend fun getFeiraAnterior(feiraIdAtual: String): FeiraEntity?
}