package com.example.gestaofeiracooperativa // <<-- Seu package

import android.content.Context
import android.util.Log // <<< NOVO IMPORT para Log.d e Log.e
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.serializer
import androidx.room.withTransaction
// Importe suas entidades, DAOs e a classe AppDatabase
import com.example.gestaofeiracooperativa.AppDatabase // <<< NOVO IMPORT (ou o caminho correto)
import com.example.gestaofeiracooperativa.FeiraEntity
import com.example.gestaofeiracooperativa.EntradaEntity
import com.example.gestaofeiracooperativa.PerdaEntity
import com.example.gestaofeiracooperativa.FeiraDao
import com.example.gestaofeiracooperativa.EntradaDao
import com.example.gestaofeiracooperativa.PerdaDao
import com.example.gestaofeiracooperativa.ProdutoDao
import com.example.gestaofeiracooperativa.AgricultorDao
import com.example.gestaofeiracooperativa.DadosCompletosFeira
import com.example.gestaofeiracooperativa.FairDetails
import com.example.gestaofeiracooperativa.EntradaItemAgricultor
import com.example.gestaofeiracooperativa.PerdaItemFeira
import com.example.gestaofeiracooperativa.Produto
// import com.example.gestaofeiracooperativa.ResultadoGeralFeira // Se for usar

class FeiraRepository(
    private val appDatabase: AppDatabase, // <<< ALTERAÇÃO: Recebe AppDatabase para transações
    private val feiraDao: FeiraDao,
    private val entradaDao: EntradaDao,
    private val perdaDao: PerdaDao,
    private val produtoDao: ProdutoDao,
    private val agricultorDao: AgricultorDao,
    private val applicationContext: Context // Mantido para flexibilidade
) {

    private val jsonFormat = Json {
        ignoreUnknownKeys = true
        prettyPrint = false // Para armazenamento, não precisamos de prettyPrint
        isLenient = true
        encodeDefaults = true
    }

    fun getTodasAsFeirasInfo(): Flow<List<FairDetails>> {
        return feiraDao.getAllFeiras().map { listaDeFeiraEntities ->
            listaDeFeiraEntities.map { feiraEntity ->
                FairDetails(
                    feiraId = feiraEntity.feiraId,
                    startDate = feiraEntity.startDate,
                    endDate = feiraEntity.endDate
                )
            }
        }
    }

    suspend fun carregarDadosCompletosFeira(feiraId: String): DadosCompletosFeira? {
        val feiraEntity = feiraDao.getFeiraById(feiraId) ?: return null
        val entradasDoBanco = entradaDao.getEntradasForFeira(feiraId).firstOrNull() ?: emptyList()
        val entradasAgricultoresMap = mutableMapOf<String, MutableList<EntradaItemAgricultor>>()

        entradasDoBanco.forEach { entradaEntity ->
            val produto = produtoDao.getProductByNumber(entradaEntity.produtoNumero)
            if (produto != null) {
                try {
                    val quantidadesMap = jsonFormat.decodeFromString<Map<String, Double>>(entradaEntity.quantidadesPorDiaJson)
                    val entradaItem = EntradaItemAgricultor(produto = produto, quantidadesPorDia = quantidadesMap)
                    entradasAgricultoresMap.getOrPut(entradaEntity.agricultorId) { mutableListOf() }.add(entradaItem)
                } catch (e: Exception) {
                    Log.e("FeiraRepository", "Erro ao desserializar quantidadesPorDiaJson para feira ${feiraId}, agricultor ${entradaEntity.agricultorId}, produto ${entradaEntity.produtoNumero}: ${e.message}")
                }
            } else {
                Log.w("FeiraRepository", "Produto com número ${entradaEntity.produtoNumero} não encontrado no banco para entrada da feira $feiraId.")
            }
        }

        val perdasDoBanco = perdaDao.getPerdasForFeira(feiraId).firstOrNull() ?: emptyList()
        val perdasFeiraList = mutableListOf<PerdaItemFeira>()
        perdasDoBanco.forEach { perdaEntity ->
            val produto = produtoDao.getProductByNumber(perdaEntity.produtoNumero)
            if (produto != null) {
                try {
                    val perdasMap = jsonFormat.decodeFromString<Map<String, Double>>(perdaEntity.perdasPorDiaJson)
                    val perdaItem = PerdaItemFeira(produto = produto, perdasPorDia = perdasMap)
                    perdasFeiraList.add(perdaItem)
                } catch (e: Exception) {
                    Log.e("FeiraRepository", "Erro ao desserializar perdasPorDiaJson para feira ${feiraId}, produto ${perdaEntity.produtoNumero}: ${e.message}")
                }
            } else {
                Log.w("FeiraRepository", "Produto com número ${perdaEntity.produtoNumero} não encontrado no banco para perda da feira $feiraId.")
            }
        }

        // val resultadoGeral = feiraEntity.resultadoGeralJson?.let { jsonFormat.decodeFromString<ResultadoGeralFeira>(it) }

        return DadosCompletosFeira(
            fairDetails = FairDetails(feiraId = feiraEntity.feiraId, startDate = feiraEntity.startDate, endDate = feiraEntity.endDate),
            entradasTodosAgricultores = entradasAgricultoresMap,
            perdasTotaisDaFeira = perdasFeiraList,
            resultadoGeralCalculado = null // ou 'resultadoGeral'
        )
    }

    suspend fun feiraExiste(feiraId: String): Boolean {
        return feiraDao.feiraExists(feiraId)
    }

    suspend fun salvarDadosCompletosFeira(dadosFeira: DadosCompletosFeira): Boolean {
        return try {
            // <<< ALTERAÇÃO: Início da Transação >>>
            appDatabase.withTransaction {
                Log.d("FeiraRepository_Save", "Iniciando transação para salvar Feira ID: ${dadosFeira.fairDetails.feiraId}")

                val feiraEntity = FeiraEntity(
                    feiraId = dadosFeira.fairDetails.feiraId,
                    startDate = dadosFeira.fairDetails.startDate,
                    endDate = dadosFeira.fairDetails.endDate
                    // resultadoGeralJson = dadosFeira.resultadoGeralCalculado?.let { jsonFormat.encodeToString(serializer<ResultadoGeralFeira>(), it) }
                )
                feiraDao.insertFeira(feiraEntity)
                Log.d("FeiraRepository_Save", "FeiraEntity inserida/atualizada para Feira ID: ${feiraEntity.feiraId}")

                entradaDao.deleteAllEntradasForFeira(dadosFeira.fairDetails.feiraId)
                Log.d("FeiraRepository_Save", "Entradas antigas deletadas para Feira ID: ${dadosFeira.fairDetails.feiraId}")
                perdaDao.deleteAllPerdasForFeira(dadosFeira.fairDetails.feiraId)
                Log.d("FeiraRepository_Save", "Perdas antigas deletadas para Feira ID: ${dadosFeira.fairDetails.feiraId}")

                val novasEntradasEntities = mutableListOf<EntradaEntity>()
                dadosFeira.entradasTodosAgricultores.forEach { (agricultorId, listaEntradasItem) ->
                    listaEntradasItem.forEach { entradaItem ->
                        novasEntradasEntities.add(
                            EntradaEntity(
                                feiraId = dadosFeira.fairDetails.feiraId,
                                agricultorId = agricultorId,
                                produtoNumero = entradaItem.produto.numero,
                                quantidadesPorDiaJson = jsonFormat.encodeToString(serializer<Map<String, Double>>(), entradaItem.quantidadesPorDia)
                            )
                        )
                    }
                }

                if (novasEntradasEntities.isNotEmpty()) {
                    Log.d("FeiraRepository_Save", "Preparando para inserir ${novasEntradasEntities.size} EntradaEntity:")
                    novasEntradasEntities.forEachIndexed { index, entrada ->
                        // <<< NOVO LOG DETALHADO PARA DEBUG DA FOREIGN KEY >>>
                        Log.d("FeiraRepository_Save_Debug", "EntradaEntity[$index]: FeiraID=${entrada.feiraId}, AgricultorID=${entrada.agricultorId}, ProdutoNUM=${entrada.produtoNumero}")
                        val agricultorExiste = agricultorDao.getAgricultorById(entrada.agricultorId) != null
                        val produtoExiste = produtoDao.getProductByNumber(entrada.produtoNumero) != null
                        Log.d("FeiraRepository_Save_Debug", "--> Agricultor ${entrada.agricultorId} existe no DB? $agricultorExiste. Produto ${entrada.produtoNumero} existe no DB? $produtoExiste")
                        if (!agricultorExiste) {
                            Log.e("FeiraRepository_Save_Debug", "ERRO FK: Agricultor com ID ${entrada.agricultorId} NÃO EXISTE no banco de dados!")
                        }
                        if (!produtoExiste) {
                            Log.e("FeiraRepository_Save_Debug", "ERRO FK: Produto com Número ${entrada.produtoNumero} NÃO EXISTE no banco de dados!")
                        }
                    }
                    entradaDao.insertAllEntradas(novasEntradasEntities) // Onde o erro de FK estava acontecendo
                    Log.d("FeiraRepository_Save", "${novasEntradasEntities.size} EntradaEntity inseridas.")
                } else {
                    Log.d("FeiraRepository_Save", "Nenhuma nova EntradaEntity para inserir.")
                }

                val novasPerdasEntities = dadosFeira.perdasTotaisDaFeira.map { perdaItem ->
                    PerdaEntity(
                        feiraId = dadosFeira.fairDetails.feiraId,
                        produtoNumero = perdaItem.produto.numero,
                        perdasPorDiaJson = jsonFormat.encodeToString(serializer<Map<String, Double>>(), perdaItem.perdasPorDia)
                    )
                }
                if (novasPerdasEntities.isNotEmpty()) {
                    Log.d("FeiraRepository_Save", "Preparando para inserir ${novasPerdasEntities.size} PerdaEntity.")
                    novasPerdasEntities.forEachIndexed { index, perda ->
                        Log.d("FeiraRepository_Save_Debug", "PerdaEntity[$index]: FeiraID=${perda.feiraId}, ProdutoNUM=${perda.produtoNumero}")
                        val produtoExiste = produtoDao.getProductByNumber(perda.produtoNumero) != null
                        Log.d("FeiraRepository_Save_Debug", "--> Produto ${perda.produtoNumero} para Perda existe no DB? $produtoExiste")
                        if (!produtoExiste) {
                            Log.e("FeiraRepository_Save_Debug", "ERRO FK: Produto com Número ${perda.produtoNumero} para Perda NÃO EXISTE no banco de dados!")
                        }
                    }
                    perdaDao.insertAllPerdas(novasPerdasEntities)
                    Log.d("FeiraRepository_Save", "${novasPerdasEntities.size} PerdaEntity inseridas.")
                } else {
                    Log.d("FeiraRepository_Save", "Nenhuma nova PerdaEntity para inserir.")
                }
                Log.d("FeiraRepository_Save", "Transação concluída com sucesso para Feira ID: ${dadosFeira.fairDetails.feiraId}")
            } // <<< ALTERAÇÃO: Fim da Transação
            true // Retorna true se a transação for bem-sucedida
        } catch (e: Exception) {
            // O e.printStackTrace() já está no seu código original, o que é bom.
            // Adicionar um Log.e pode ajudar a centralizar os erros do repositório.
            Log.e("FeiraRepository_Save", "EXCEÇÃO GERAL ao salvar feira ${dadosFeira.fairDetails.feiraId}: ${e.message}", e)
            false
        }
    }

    suspend fun deletarFeira(feiraId: String): Boolean {
        return try {
            // A deleção em cascata já está configurada nas ForeignKeys
            feiraDao.deleteFeiraById(feiraId)
            Log.d("FeiraRepository", "Feira $feiraId deletada do banco (e dados associados via cascade).")
            true
        } catch (e: Exception) {
            Log.e("FeiraRepository", "Erro ao deletar feira $feiraId: ${e.message}", e)
            false
        }
    }
}