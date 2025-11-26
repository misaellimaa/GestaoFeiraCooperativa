package com.example.gestaofeiracooperativa

import kotlinx.coroutines.flow.Flow

class EntradaRepository(private val entradaDao: EntradaDao) {

    fun getEntradasForFeira(feiraId: String): Flow<List<EntradaEntity>> = entradaDao.getEntradasForFeira(feiraId)

    suspend fun getEntradaEspecifica(feiraId: String, agricultorId: String, produtoNumero: String): EntradaEntity? =
        entradaDao.getEntradaEspecifica(feiraId, agricultorId, produtoNumero)

    suspend fun update(entrada: EntradaEntity) = entradaDao.update(entrada)

    suspend fun insert(entrada: EntradaEntity) = entradaDao.insertEntrada(entrada)

    suspend fun resetarSobrasParaFeira(feiraId: String) {
        entradaDao.resetarSobrasParaFeira(feiraId)
    }

    // <<< CORREÇÃO DE BUG: Salva pesos sem zerar a sobra >>>
    suspend fun salvarEntradaPreservandoSobra(
        feiraId: String,
        agricultorId: String,
        produtoNumero: String,
        quantidadesPorDiaJson: String // JSON dos pesos da semana
    ) {
        // 1. Verifica o que já está no banco
        val entradaExistente = entradaDao.getEntradaEspecifica(feiraId, agricultorId, produtoNumero)

        if (entradaExistente != null) {
            // 2. Se já existe, atualiza SÓ os pesos da semana, mantendo a sobra antiga intacta
            val entradaAtualizada = entradaExistente.copy(
                quantidadesPorDiaJson = quantidadesPorDiaJson
                // Nota: Não mexemos no campo 'quantidadeSobra', então ele mantém o valor do banco
            )
            entradaDao.update(entradaAtualizada)
        } else {
            // 3. Se é novo, cria do zero (sobra é 0.0)
            val novaEntrada = EntradaEntity(
                feiraId = feiraId,
                agricultorId = agricultorId,
                produtoNumero = produtoNumero,
                quantidadesPorDiaJson = quantidadesPorDiaJson,
                quantidadeSobra = 0.0
            )
            entradaDao.insertEntrada(novaEntrada)
        }
    }
}