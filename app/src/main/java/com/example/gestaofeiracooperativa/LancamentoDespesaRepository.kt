package com.example.gestaofeiracooperativa

import kotlinx.coroutines.flow.Flow

// Importe LancamentoMensalDespesaEntity e LancamentoMensalDespesaDao
// (Certifique-se que os paths estão corretos, provavelmente do seu DespesaModels.kt e do arquivo DAO)
// import com.example.gestaofeiracooperativa.LancamentoMensalDespesaEntity
// import com.example.gestaofeiracooperativa.LancamentoMensalDespesaDao

class LancamentoDespesaRepository(private val lancamentoMensalDespesaDao: LancamentoMensalDespesaDao) {

    /**
     * Busca todos os lançamentos de despesa para um determinado ano e mês.
     * Retorna um Flow para observação pela UI.
     */
    fun getLancamentosByAnoMes(ano: Int, mes: Int): Flow<List<LancamentoMensalDespesaEntity>> {
        return lancamentoMensalDespesaDao.getLancamentosByAnoMes(ano, mes)
    }

    /**
     * Busca um lançamento de despesa específico para um item, ano e mês.
     * Útil para carregar dados existentes ao editar.
     */
    suspend fun getLancamentoByItemAnoMes(itemDespesaId: Long, ano: Int, mes: Int): LancamentoMensalDespesaEntity? {
        return lancamentoMensalDespesaDao.getLancamentoByItemAnoMes(itemDespesaId, ano, mes)
    }

    /**
     * Insere ou atualiza um lançamento de despesa mensal.
     * O DAO usa OnConflictStrategy.REPLACE, então se um registro para o mesmo
     * item/ano/mês já existir, ele será substituído.
     * Retorna o ID da linha inserida/atualizada.
     */
    suspend fun insertOrUpdateLancamento(lancamento: LancamentoMensalDespesaEntity): Long {
        return lancamentoMensalDespesaDao.insertOrUpdateLancamento(lancamento)
    }

    /**
     * Deleta um lançamento de despesa específico.
     */
    suspend fun deleteLancamento(lancamento: LancamentoMensalDespesaEntity) {
        lancamentoMensalDespesaDao.deleteLancamento(lancamento)
    }

    // Você pode adicionar outros métodos aqui conforme necessário, por exemplo,
    // para buscar todos os lançamentos de um item específico ao longo de todos os meses, etc.
}