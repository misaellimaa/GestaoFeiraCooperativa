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

    @Query("SELECT * FROM produtos ORDER BY item COLLATE NOCASE ASC")
    fun getAllProducts(): Flow<List<Produto>>

    @Query("SELECT * FROM produtos WHERE numero LIKE :query || '%' OR item LIKE '%' || :query || '%' ORDER BY item COLLATE NOCASE ASC")
    fun searchProducts(query: String): Flow<List<Produto>>

    @Query("SELECT * FROM produtos WHERE numero = :numero LIMIT 1")
    suspend fun getProductByNumber(numero: String): Produto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(produto: Produto)

    @Update
    suspend fun update(produto: Produto)

    @Delete
    suspend fun delete(produto: Produto)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(produtos: List<Produto>)

    @Query("DELETE FROM produtos")
    suspend fun deleteAllProducts()
}
