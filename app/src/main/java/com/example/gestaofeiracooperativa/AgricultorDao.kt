package com.example.gestaofeiracooperativa // <<--- MUDE PARA O SEU PACKAGE REAL

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface AgricultorDao {

    @Query("SELECT * FROM agricultores ORDER BY id COLLATE NOCASE ASC")
    fun getAllAgricultores(): Flow<List<Agricultor>>

    @Query("SELECT * FROM agricultores WHERE id LIKE :query || '%' OR nome LIKE '%' || :query || '%' ORDER BY id COLLATE NOCASE ASC")
    fun searchAgricultores(query: String): Flow<List<Agricultor>>

    @Query("SELECT * FROM agricultores WHERE id = :id LIMIT 1")
    suspend fun getAgricultorById(id: String): Agricultor?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(agricultor: Agricultor)

    @Update
    suspend fun update(agricultor: Agricultor)

    @Delete
    suspend fun delete(agricultor: Agricultor)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(agricultores: List<Agricultor>)

    @Query("DELETE FROM agricultores")
    suspend fun deleteAllAgricultores()
}