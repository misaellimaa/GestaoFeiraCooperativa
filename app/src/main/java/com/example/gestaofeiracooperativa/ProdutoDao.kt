package com.example.gestaofeiracooperativa // <<--- MUDE PARA O SEU PACKAGE REAL

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface ProdutoDao {

    // <<< ALTERAÇÃO: Ordenar por número (numericamente) >>>
    @Query("SELECT * FROM produtos ORDER BY CAST(numero AS INTEGER) ASC")
    fun getAllProducts(): Flow<List<Produto>>

    // <<< ALTERAÇÃO: Ordenar busca também por número, depois por item >>>
    @Query("SELECT * FROM produtos WHERE numero LIKE :query || '%' OR item LIKE '%' || :query || '%' ORDER BY CAST(numero AS INTEGER) ASC, item COLLATE NOCASE ASC")
    fun searchProducts(query: String): Flow<List<Produto>>

    @Query("SELECT * FROM produtos WHERE numero = :numero LIMIT 1")
    suspend fun getProductByNumber(numero: String): Produto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(produto: Produto)

    @Update
    suspend fun update(produto: Produto)

    @Delete
    suspend fun delete(produto: Produto)

    // Manter REPLACE para insertAll pode ser útil para a carga inicial do CSV,
    // onde a intenção pode ser sobrescrever.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(produtos: List<Produto>)

    @Query("DELETE FROM produtos")
    suspend fun deleteAll()
}
