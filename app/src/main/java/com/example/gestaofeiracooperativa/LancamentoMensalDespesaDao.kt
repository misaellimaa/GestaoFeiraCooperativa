package com.example.gestaofeiracooperativa

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LancamentoMensalDespesaDao {

    // Insere um novo lançamento. Se já existir um para o mesmo item/mês/ano, substitui.
    // Isso é útil porque ao editar as despesas de um item para um mês, você pode simplesmente
    // chamar este método com os novos valores.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLancamento(lancamento: LancamentoMensalDespesaEntity): Long // Retorna o ID da linha inserida/atualizada

    @Update
    suspend fun updateLancamento(lancamento: LancamentoMensalDespesaEntity) // Pode ser útil para atualizações parciais se necessário

    @Delete
    suspend fun deleteLancamento(lancamento: LancamentoMensalDespesaEntity)

    // Busca todos os lançamentos de despesa para um determinado ano e mês.
    // Isso será usado para popular a tela de lançamento de despesas mensais.
    // Ordena pelo ID do item de despesa para consistência.
    @Query("SELECT * FROM lancamentos_despesas_mensais WHERE ano = :ano AND mes = :mes ORDER BY itemDespesaId ASC")
    fun getLancamentosByAnoMes(ano: Int, mes: Int): Flow<List<LancamentoMensalDespesaEntity>>

    // Busca um lançamento específico para um item de despesa, ano e mês.
    // Útil para carregar dados existentes ao editar as despesas de um item específico para um mês.
    @Query("SELECT * FROM lancamentos_despesas_mensais WHERE itemDespesaId = :itemDespesaId AND ano = :ano AND mes = :mes LIMIT 1")
    suspend fun getLancamentoByItemAnoMes(itemDespesaId: Long, ano: Int, mes: Int): LancamentoMensalDespesaEntity?

    // (Opcional) Se você precisar deletar todos os lançamentos de um item de despesa específico
    // (embora o onDelete = ForeignKey.CASCADE na entidade já deva cuidar disso se o ItemDespesa for deletado)
    // @Query("DELETE FROM lancamentos_despesas_mensais WHERE itemDespesaId = :itemDespesaId")
    // suspend fun deleteLancamentosForItemDespesa(itemDespesaId: Long)
}