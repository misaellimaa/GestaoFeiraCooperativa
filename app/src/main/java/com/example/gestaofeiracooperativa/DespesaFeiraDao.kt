package com.example.gestaofeiracooperativa // Certifique-se que é o seu package correto

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update // Embora @Insert com REPLACE possa cobrir updates simples
import kotlinx.coroutines.flow.Flow

// Importe DespesaFeiraEntity (do seu DespesaModels.kt ou DataModels.kt)
// import com.example.gestaofeiracooperativa.DespesaFeiraEntity

@Dao
interface DespesaFeiraDao {

    /**
     * Insere um novo lançamento de despesa para uma feira ou atualiza um existente.
     * Como a chave primária é (feiraId, itemDespesaId), OnConflictStrategy.REPLACE
     * garante que, se você inserir uma despesa para um item que já tem
     * um lançamento naquela feira, o antigo será substituído.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(despesa: DespesaFeiraEntity)

    /**
     * Busca todos os lançamentos de despesa para uma feira específica.
     * Retorna um Flow para observação.
     * Ordenado pelo ID do item de despesa para consistência na exibição.
     */
    @Query("SELECT * FROM despesas_feira WHERE feiraId = :feiraId ORDER BY itemDespesaId ASC")
    fun getDespesasByFeiraId(feiraId: String): Flow<List<DespesaFeiraEntity>>

    /**
     * Busca um lançamento de despesa específico para um item de despesa dentro de uma feira.
     * Útil para carregar um item específico para edição, se necessário.
     */
    @Query("SELECT * FROM despesas_feira WHERE feiraId = :feiraId AND itemDespesaId = :itemDespesaId LIMIT 1")
    suspend fun getDespesaByFeiraAndItemDespesaId(feiraId: String, itemDespesaId: Long): DespesaFeiraEntity?

    /**
     * Deleta um lançamento de despesa específico.
     */
    @Delete
    suspend fun delete(despesa: DespesaFeiraEntity)

    /**
     * Deleta todos os lançamentos de despesa para uma feira específica.
     * Pode ser útil se o usuário quiser "limpar" todas as despesas de uma feira
     * sem deletar a feira em si.
     */
    @Query("DELETE FROM despesas_feira WHERE feiraId = :feiraId")
    suspend fun deleteDespesasByFeiraId(feiraId: String)

    @Query("DELETE FROM despesas_feira WHERE feiraId = :feiraId AND itemDespesaId = :itemDespesaId")
    suspend fun deleteByFeiraAndItemDespesaId(feiraId: String, itemDespesaId: Long)
}