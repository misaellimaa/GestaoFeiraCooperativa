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

    // <<< ALTERAÇÃO: Ordenar por id numericamente >>>
    // Assume que 'id' é uma string que representa um número (ex: "1", "2", "28")
    @Query("SELECT * FROM agricultores ORDER BY CAST(id AS INTEGER) ASC")
    fun getAllAgricultores(): Flow<List<Agricultor>>

    // <<< ALTERAÇÃO: Ordenar busca por id numericamente, depois por nome >>>
    @Query("SELECT * FROM agricultores WHERE id LIKE :query || '%' OR nome LIKE '%' || :query || '%' ORDER BY CAST(id AS INTEGER) ASC, nome COLLATE NOCASE ASC")
    fun searchAgricultores(query: String): Flow<List<Agricultor>>

    @Query("SELECT * FROM agricultores WHERE id = :id LIMIT 1")
    suspend fun getAgricultorById(id: String): Agricultor?

    // <<< ALTERAÇÃO: Mudar estratégia para ABORT para insert individual >>>
    // Isso fará com que uma tentativa de inserir um 'id' duplicado lance uma SQLiteConstraintException.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(agricultor: Agricultor)

    @Update
    suspend fun update(agricultor: Agricultor)

    @Delete
    suspend fun delete(agricultor: Agricultor)

    // Manter REPLACE para insertAll é geralmente bom para a população inicial de dados,
    // onde a intenção pode ser sobrescrever.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(agricultores: List<Agricultor>) // Usado no AppDatabaseCallback

    @Query("DELETE FROM agricultores")
    suspend fun deleteAllAgricultores()
}