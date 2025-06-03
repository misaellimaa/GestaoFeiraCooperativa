package com.example.gestaofeiracooperativa // Seu package

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlinx.serialization.encodeToString // Import para encodeToString
import kotlinx.serialization.decodeFromString // Import para decodeFromString
import androidx.room.withTransaction

// Importe suas entidades, DAOs e a classe AppDatabase
import com.example.gestaofeiracooperativa.AppDatabase
import com.example.gestaofeiracooperativa.FeiraEntity
import com.example.gestaofeiracooperativa.EntradaEntity
import com.example.gestaofeiracooperativa.PerdaEntity
import com.example.gestaofeiracooperativa.DespesaFeiraEntity // <<< NOVO IMPORT
import com.example.gestaofeiracooperativa.FeiraDao
import com.example.gestaofeiracooperativa.EntradaDao
import com.example.gestaofeiracooperativa.PerdaDao
import com.example.gestaofeiracooperativa.ProdutoDao
import com.example.gestaofeiracooperativa.AgricultorDao
import com.example.gestaofeiracooperativa.DespesaFeiraDao // <<< NOVO IMPORT (para despesaFeiraDao)
import com.example.gestaofeiracooperativa.ItemDespesaDao // <<< NOVO IMPORT (para itemDespesaDao)
// Repositórios para injetar, se não for passar os DAOs diretamente
import com.example.gestaofeiracooperativa.ItemDespesaRepository
import com.example.gestaofeiracooperativa.DespesaFeiraRepository


// Importe suas classes de modelo
import com.example.gestaofeiracooperativa.DadosCompletosFeira // Esta classe precisará ser atualizada
import com.example.gestaofeiracooperativa.FairDetails
import com.example.gestaofeiracooperativa.EntradaItemAgricultor
import com.example.gestaofeiracooperativa.PerdaItemFeira
import com.example.gestaofeiracooperativa.DespesaFeiraUiItem // <<< NOVO IMPORT
import com.example.gestaofeiracooperativa.Produto
import com.example.gestaofeiracooperativa.ItemDespesaEntity // <<< NOVO IMPORT
import java.util.Locale // <<< NOVO IMPORT se não estiver lá

class FeiraRepository(
    private val appDatabase: AppDatabase,
    private val feiraDao: FeiraDao,
    private val entradaDao: EntradaDao,
    private val perdaDao: PerdaDao,
    private val produtoDao: ProdutoDao,
    private val agricultorDao: AgricultorDao,
    // <<< ALTERAÇÃO: Injetar DAOs ou Repositórios de Despesa >>>
    private val itemDespesaDao: ItemDespesaDao, // Ou ItemDespesaRepository
    private val despesaFeiraDao: DespesaFeiraDao, // Ou DespesaFeiraRepository
    private val applicationContext: Context
) {

    private val jsonFormat = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true // Mantido, mas revise se é o comportamento desejado para todos os casos
    }

    // Função auxiliar interna para formatar Double para String de UI
    private fun Double?.formatParaUi(): String {
        if (this == null) return ""
        return if (this % 1.0 == 0.0) this.toInt().toString() else String.format(Locale.getDefault(), "%.2f", this).replace('.', ',')
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

        // Carregar Entradas
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
                    Log.e("FeiraRepository", "Erro ao desserializar quantidadesPorDiaJson para feira $feiraId, agricultor ${entradaEntity.agricultorId}, produto ${entradaEntity.produtoNumero}: ${e.message}")
                }
            } else { /* Log produto não encontrado */ }
        }

        // Carregar Perdas
        val perdasDoBanco = perdaDao.getPerdasForFeira(feiraId).firstOrNull() ?: emptyList()
        val perdasFeiraList = mutableListOf<PerdaItemFeira>()
        perdasDoBanco.forEach { perdaEntity ->
            val produto = produtoDao.getProductByNumber(perdaEntity.produtoNumero)
            if (produto != null) {
                try {
                    val perdasMap = jsonFormat.decodeFromString<Map<String, Double>>(perdaEntity.perdasPorDiaJson)
                    val perdaItem = PerdaItemFeira(produto = produto, perdasPorDia = perdasMap)
                    perdasFeiraList.add(perdaItem)
                } catch (e: Exception) { /* Log erro */ }
            } else { /* Log produto não encontrado */ }
        }

        // <<< NOVO: Carregar Despesas >>>
        val todosOsItensDeDespesaCadastrados = itemDespesaDao.getAllItensDespesa().firstOrNull() ?: emptyList()
        val despesasLancadasDoBanco = despesaFeiraDao.getDespesasByFeiraId(feiraId).firstOrNull() ?: emptyList()
        val despesasUiList = todosOsItensDeDespesaCadastrados.map { itemDeDespesa ->
            val lancamentoExistente = despesasLancadasDoBanco.find { it.itemDespesaId == itemDeDespesa.id }
            val valoresPorDiaInputMap = mutableMapOf<String, String>()

            diasDaSemanaFeira.forEach { dia -> // diasDaSemanaFeira é sua constante global
                val mapaDeValoresNumericos = lancamentoExistente?.valoresPorDiaJson?.let { jsonString ->
                    if (jsonString.isNotBlank()) {
                        try {
                            jsonFormat.decodeFromString<Map<String, Double>>(jsonString)
                        } catch (e: Exception) {
                            Log.e("FeiraRepository", "Erro desserializando valoresPorDiaJson (DESPESA) para item ${itemDeDespesa.id}, feira $feiraId: $jsonString", e)
                            emptyMap<String, Double>()
                        }
                    } else { emptyMap<String, Double>() }
                } ?: emptyMap<String, Double>()
                valoresPorDiaInputMap[dia] = mapaDeValoresNumericos[dia]?.formatParaUi() ?: ""
            }
            DespesaFeiraUiItem(
                itemDespesa = itemDeDespesa,
                valoresPorDiaInput = valoresPorDiaInputMap,
                observacaoInput = lancamentoExistente?.observacao ?: "",
                isExistingEntry = lancamentoExistente != null
            )
        }

        return DadosCompletosFeira(
            fairDetails = FairDetails(feiraId = feiraEntity.feiraId, startDate = feiraEntity.startDate, endDate = feiraEntity.endDate),
            entradasTodosAgricultores = entradasAgricultoresMap,
            perdasTotaisDaFeira = perdasFeiraList,
            despesasDaFeira = despesasUiList, // <<< NOVO CAMPO PREENCHIDO
            resultadoGeralCalculado = null // ou carregado se persistir
        )
    }

    suspend fun feiraExiste(feiraId: String): Boolean {
        return feiraDao.feiraExists(feiraId)
    }

    suspend fun salvarDadosCompletosFeira(dadosFeira: DadosCompletosFeira): Boolean {
        return try {
            appDatabase.withTransaction {
                Log.d("FeiraRepository_Save", "Iniciando transação para salvar Feira ID: ${dadosFeira.fairDetails.feiraId}")

                // 1. Salvar/Atualizar FeiraEntity
                val feiraEntity = FeiraEntity(
                    feiraId = dadosFeira.fairDetails.feiraId,
                    startDate = dadosFeira.fairDetails.startDate,
                    endDate = dadosFeira.fairDetails.endDate
                )
                feiraDao.insertFeira(feiraEntity) // OnConflictStrategy.REPLACE
                Log.d("FeiraRepository_Save", "FeiraEntity salva para Feira ID: ${feiraEntity.feiraId}")

                // 2. Salvar/Atualizar Entradas
                entradaDao.deleteAllEntradasForFeira(dadosFeira.fairDetails.feiraId)
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
                    entradaDao.insertAllEntradas(novasEntradasEntities)
                }
                Log.d("FeiraRepository_Save", "${novasEntradasEntities.size} EntradaEntity salvas para Feira ID: ${feiraEntity.feiraId}")

                // 3. Salvar/Atualizar Perdas
                perdaDao.deleteAllPerdasForFeira(dadosFeira.fairDetails.feiraId)
                val novasPerdasEntities = dadosFeira.perdasTotaisDaFeira.map { perdaItem ->
                    PerdaEntity(
                        feiraId = dadosFeira.fairDetails.feiraId,
                        produtoNumero = perdaItem.produto.numero,
                        perdasPorDiaJson = jsonFormat.encodeToString(serializer<Map<String, Double>>(), perdaItem.perdasPorDia)
                    )
                }
                if (novasPerdasEntities.isNotEmpty()) {
                    perdaDao.insertAllPerdas(novasPerdasEntities)
                }
                Log.d("FeiraRepository_Save", "${novasPerdasEntities.size} PerdaEntity salvas para Feira ID: ${feiraEntity.feiraId}")

                // <<< NOVO: Salvar/Atualizar Despesas >>>
                despesaFeiraDao.deleteDespesasByFeiraId(dadosFeira.fairDetails.feiraId)
                Log.d("FeiraRepository_Save", "Despesas antigas deletadas para Feira ID: ${feiraEntity.feiraId}")

                val novasDespesasEntities = mutableListOf<DespesaFeiraEntity>()
                dadosFeira.despesasDaFeira.forEach { despesaUiItem ->
                    val valoresConvertidos = mutableMapOf<String, Double>()
                    var temAlgumValor = false
                    diasDaSemanaFeira.forEach { dia ->
                        val valorStr = despesaUiItem.valoresPorDiaInput[dia]
                        if (!valorStr.isNullOrBlank()) {
                            val valorDouble = valorStr.replace(',', '.').toDoubleOrNull()
                            if (valorDouble != null) {
                                valoresConvertidos[dia] = valorDouble
                                if (valorDouble != 0.0) temAlgumValor = true
                            }
                        }
                    }

                    if (temAlgumValor || despesaUiItem.isExistingEntry) { // Salva se tem valores ou se existia (para permitir zerar e depois deletar se for o caso)
                        if (!temAlgumValor && despesaUiItem.isExistingEntry){
                            // Se existia e agora não tem valores, o deleteDespesasByFeiraId já cuidou.
                            // Ou se a lógica for item a item:
                            // despesaFeiraDao.deleteByFeiraAndItemDespesaId(feiraEntity.feiraId, despesaUiItem.itemDespesa.id)
                            // No entanto, como fizemos um deleteAllDespesasByFeiraId antes, só precisamos inserir os que têm valor.
                        }
                        if (temAlgumValor) { // Só insere se de fato há valores
                            novasDespesasEntities.add(
                                DespesaFeiraEntity(
                                    feiraId = feiraEntity.feiraId,
                                    itemDespesaId = despesaUiItem.itemDespesa.id,
                                    valoresPorDiaJson = jsonFormat.encodeToString(serializer<Map<String,Double>>(), valoresConvertidos),
                                    observacao = despesaUiItem.observacaoInput.trim().ifEmpty { null }
                                )
                            )
                        }
                    }
                }
                if (novasDespesasEntities.isNotEmpty()) {
                    // O DespesaFeiraDao.insertOrUpdate lida com insert ou replace baseado na PK composta
                    novasDespesasEntities.forEach { despesaFeiraDao.insertOrUpdate(it) }
                    Log.d("FeiraRepository_Save", "${novasDespesasEntities.size} DespesaFeiraEntity salvas para Feira ID: ${feiraEntity.feiraId}")
                } else {
                    Log.d("FeiraRepository_Save", "Nenhuma nova DespesaFeiraEntity para salvar para Feira ID: ${feiraEntity.feiraId}")
                }
                // <<< FIM NOVO SALVAR DESPESAS >>>

                Log.d("FeiraRepository_Save", "Transação concluída com sucesso para Feira ID: ${feiraEntity.feiraId}")
            }
            true
        } catch (e: Exception) {
            Log.e("FeiraRepository_Save", "EXCEÇÃO GERAL ao salvar feira ${dadosFeira.fairDetails.feiraId}: ${e.message}", e)
            false
        }
    }

    suspend fun deletarFeira(feiraId: String): Boolean {
        return try {
            // A deleção em cascata configurada nas ForeignKeys das entidades
            // (EntradaEntity, PerdaEntity, DespesaFeiraEntity)
            // garantirá que os dados associados sejam removidos quando a FeiraEntity é deletada.
            feiraDao.deleteFeiraById(feiraId)
            Log.d("FeiraRepository", "Feira $feiraId deletada do banco (e dados associados via cascade).")
            true
        } catch (e: Exception) {
            Log.e("FeiraRepository", "Erro ao deletar feira $feiraId: ${e.message}", e)
            false
        }
    }
}