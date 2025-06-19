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

    // <<< ALTERAÇÃO 1: Mude a estratégia para REPLACE >>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItemDespesa(itemDespesa: ItemDespesaEntity): Long

    @Update
    suspend fun updateItemDespesa(itemDespesa: ItemDespesaEntity)

    @Delete
    suspend fun deleteItemDespesa(itemDespesa: ItemDespesaEntity)

    @Query("SELECT * FROM itens_despesa WHERE id = :id")
    suspend fun getItemDespesaById(id: Long): ItemDespesaEntity?

    @Query("SELECT * FROM itens_despesa WHERE nome = :nome LIMIT 1")
    suspend fun getItemDespesaByNome(nome: String): ItemDespesaEntity?

    @Query("SELECT * FROM itens_despesa ORDER BY nome ASC")
    fun getAllItensDespesa(): Flow<List<ItemDespesaEntity>>

    @Query("SELECT * FROM itens_despesa WHERE nome LIKE '%' || :query || '%' ORDER BY nome ASC")
    fun searchItensDespesa(query: String): Flow<List<ItemDespesaEntity>>

    // <<< ALTERAÇÃO 2: Adicione um método para deletar tudo >>>
    @Query("DELETE FROM itens_despesa")
    suspend fun deleteAll()

    // <<< ALTERAÇÃO 3: Adicione um método para inserir uma lista >>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(itens: List<ItemDespesaEntity>)
}