package com.example.gestaofeiracooperativa

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDespesaDao {

    @Insert(onConflict = OnConflictStrategy.ABORT) // Aborta se tentar inserir um item com nome que já existe (devido ao índice único no nome)
    suspend fun insertItemDespesa(itemDespesa: ItemDespesaEntity): Long // Retorna o ID gerado

    @Update
    suspend fun updateItemDespesa(itemDespesa: ItemDespesaEntity)

    @Delete
    suspend fun deleteItemDespesa(itemDespesa: ItemDespesaEntity)

    @Query("SELECT * FROM itens_despesa WHERE id = :id")
    suspend fun getItemDespesaById(id: Long): ItemDespesaEntity?

    @Query("SELECT * FROM itens_despesa WHERE nome = :nome LIMIT 1")
    suspend fun getItemDespesaByNome(nome: String): ItemDespesaEntity? // Útil para verificar duplicidade de nome

    @Query("SELECT * FROM itens_despesa ORDER BY nome ASC")
    fun getAllItensDespesa(): Flow<List<ItemDespesaEntity>> // Lista observável ordenada por nome

    @Query("SELECT * FROM itens_despesa WHERE nome LIKE '%' || :query || '%' ORDER BY nome ASC")
    fun searchItensDespesa(query: String): Flow<List<ItemDespesaEntity>> // Para busca
}