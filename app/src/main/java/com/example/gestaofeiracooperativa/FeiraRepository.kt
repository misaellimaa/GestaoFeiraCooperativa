package com.example.gestaofeiracooperativa // Seu package

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import androidx.room.withTransaction // Garanta que este import está presente

// Importe suas entidades, DAOs e a classe AppDatabase
import com.example.gestaofeiracooperativa.AppDatabase // Import da sua classe AppDatabase
import com.example.gestaofeiracooperativa.FeiraEntity
import com.example.gestaofeiracooperativa.EntradaEntity
import com.example.gestaofeiracooperativa.PerdaEntity
import com.example.gestaofeiracooperativa.DespesaFeiraEntity
import com.example.gestaofeiracooperativa.FeiraDao
import com.example.gestaofeiracooperativa.EntradaDao
import com.example.gestaofeiracooperativa.PerdaDao
import com.example.gestaofeiracooperativa.ProdutoDao
import com.example.gestaofeiracooperativa.AgricultorDao
import com.example.gestaofeiracooperativa.DespesaFeiraDao
import com.example.gestaofeiracooperativa.ItemDespesaDao

// Importe suas classes de modelo
import com.example.gestaofeiracooperativa.DadosCompletosFeira
import com.example.gestaofeiracooperativa.FairDetails
import com.example.gestaofeiracooperativa.EntradaItemAgricultor
import com.example.gestaofeiracooperativa.PerdaItemFeira
import com.example.gestaofeiracooperativa.DespesaFeiraUiItem
import com.example.gestaofeiracooperativa.Produto
import com.example.gestaofeiracooperativa.ItemDespesaEntity
import com.example.gestaofeiracooperativa.diasDaSemanaFeira // Import da constante global
import java.util.Locale

class FeiraRepository(
    private val appDatabase: AppDatabase, // <<< CORRIGIDO AQUI para AppDatabase
    private val feiraDao: FeiraDao,
    private val entradaDao: EntradaDao,
    private val perdaDao: PerdaDao,
    private val produtoDao: ProdutoDao,
    private val agricultorDao: AgricultorDao,
    private val itemDespesaDao: ItemDespesaDao,
    private val despesaFeiraDao: DespesaFeiraDao,
    private val applicationContext: Context
) {


    private val jsonFormat = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private fun Double?.formatParaUi(): String {
        if (this == null) return ""
        return if (this % 1.0 == 0.0) this.toInt().toString() else String.format(Locale.getDefault(), "%.2f", this).replace('.', ',')
    }

    suspend fun getFeiraAnterior(feiraIdAtual: String): FeiraEntity? {
        return feiraDao.getFeiraAnterior(feiraIdAtual)
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
    suspend fun getFairDetailsFromFeiraId(id: String): FairDetails? {
        return feiraDao.getFeiraById(id)?.let { entity ->
            FairDetails(entity.feiraId, entity.startDate, entity.endDate)
        }
    }

    fun getFeirasByMesAno(ano: Int, mes: Int): Flow<List<FairDetails>> {
        val mesFormatado = String.format(Locale.ROOT, "%02d", mes)
        val anoFormatado = ano.toString()
        return feiraDao.getFeirasByMesAno(mesFormatado, anoFormatado).map { entities ->
            entities.map { FairDetails(it.feiraId, it.startDate, it.endDate) }
        }
    }
    suspend fun executarDistribuicaoDeSobras(
        perdasParaAtualizar: List<PerdaEntity>,
        entradasParaAtualizarOuInserir: List<EntradaEntity>
    ) {
        appDatabase.withTransaction {
            // Atualiza as perdas com a quantidade de sobra real na feira de origem
            perdasParaAtualizar.forEach { perdaDao.update(it) }

            // Insere ou atualiza as entradas com as sobras na feira de destino
            // Usar insertOrUpdate para cada um garante que se a entrada já existe, ela é atualizada.
            // Se o seu EntradaDao tiver um insertOrUpdate com @Insert(onConflict=REPLACE), use-o.
            // Se não, esta lógica de buscar->atualizar/inserir é mais segura.
            entradasParaAtualizarOuInserir.forEach { entrada ->
                val existente = entradaDao.getEntradaEspecifica(entrada.feiraId, entrada.agricultorId, entrada.produtoNumero)
                if (existente != null) {
                    // Atualiza preservando os dados diários existentes, apenas mudando a sobra
                    entradaDao.update(existente.copy(quantidadeSobra = entrada.quantidadeSobra))
                } else {
                    entradaDao.insertEntrada(entrada)
                }
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
                    val entradaItem = EntradaItemAgricultor(
                        produto = produto,
                        quantidadeSobraDaSemanaAnterior = entradaEntity.quantidadeSobra,
                        quantidadesPorDia = quantidadesMap)
                    entradasAgricultoresMap.getOrPut(entradaEntity.agricultorId) { mutableListOf() }.add(entradaItem)
                } catch (e: Exception) {
                    Log.e("FeiraRepository", "Erro ao desserializar quantidadesPorDiaJson para feira $feiraId, agricultor ${entradaEntity.agricultorId}, produto ${entradaEntity.produtoNumero}: ${e.message}")
                }
            } else { Log.w("FeiraRepository", "Produto ${entradaEntity.produtoNumero} não encontrado para entrada da feira $feiraId.") }
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
                } catch (e: Exception) { Log.e("FeiraRepository", "Erro desserializar perdasPorDiaJson (Perda) feira $feiraId, prod ${perdaEntity.produtoNumero}: ${e.message}") }
            } else { Log.w("FeiraRepository", "Produto ${perdaEntity.produtoNumero} não encontrado para perda da feira $feiraId.") }
        }

        val todosOsItensDeDespesaCadastrados = itemDespesaDao.getAllItensDespesa().firstOrNull() ?: emptyList()
        val despesasLancadasDoBanco = despesaFeiraDao.getDespesasByFeiraId(feiraId).firstOrNull() ?: emptyList()

        Log.d("FeiraRepo_LoadDesp", "Feira $feiraId: Encontrados ${todosOsItensDeDespesaCadastrados.size} tipos de despesa. Encontrados ${despesasLancadasDoBanco.size} lançamentos de despesa para esta feira.")

        val despesasUiList = todosOsItensDeDespesaCadastrados.map { itemDeDespesa ->
            val lancamentoExistente = despesasLancadasDoBanco.find { it.itemDespesaId == itemDeDespesa.id }
            val valoresPorDiaInputMap = mutableMapOf<String, String>()

            diasDaSemanaFeira.forEach { dia ->
                val mapaDeValoresNumericos = lancamentoExistente?.valoresPorDiaJson?.let { jsonString ->
                    if (jsonString.isNotBlank()) {
                        try {
                            jsonFormat.decodeFromString<Map<String, Double>>(jsonString)
                        } catch (e: Exception) {
                            Log.e("FeiraRepository", "Erro desserializando valoresPorDiaJson (DESPESA) para item ${itemDeDespesa.id}, feira $feiraId: $jsonString", e)
                            emptyMap()
                        }
                    } else { emptyMap() }
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
        Log.d("FeiraRepo_LoadDesp", "Feira $feiraId: Mapeado para ${despesasUiList.size} DespesaFeiraUiItem.")

        return DadosCompletosFeira(
            fairDetails = FairDetails(feiraId = feiraEntity.feiraId, startDate = feiraEntity.startDate, endDate = feiraEntity.endDate),
            entradasTodosAgricultores = entradasAgricultoresMap,
            perdasTotaisDaFeira = perdasFeiraList,
            despesasDaFeira = despesasUiList,
            resultadoGeralCalculado = null
        )
    }

    suspend fun feiraExiste(feiraId: String): Boolean {
        return feiraDao.feiraExists(feiraId)
    }


    suspend fun salvarDadosCompletosFeira(dadosFeira: DadosCompletosFeira): Boolean {
        return try {
            appDatabase.withTransaction { // A chamada a withTransaction está correta SE appDatabase for do tipo AppDatabase (que herda de RoomDatabase)
                Log.d("FeiraRepository_Save", "Iniciando transação para salvar Feira ID: ${dadosFeira.fairDetails.feiraId}")

                val feiraEntity = FeiraEntity(
                    feiraId = dadosFeira.fairDetails.feiraId,
                    startDate = dadosFeira.fairDetails.startDate,
                    endDate = dadosFeira.fairDetails.endDate
                )
                feiraDao.insertFeira(feiraEntity)
                Log.d("FeiraRepository_Save", "FeiraEntity salva para Feira ID: ${feiraEntity.feiraId}")

                // Entradas
                entradaDao.deleteAllEntradasForFeira(dadosFeira.fairDetails.feiraId)
                val novasEntradasEntities = dadosFeira.entradasTodosAgricultores.flatMap { (agricultorId, listaEntradasItem) ->
                    listaEntradasItem.map { entradaItem ->
                        EntradaEntity(
                            feiraId = dadosFeira.fairDetails.feiraId,
                            agricultorId = agricultorId,
                            produtoNumero = entradaItem.produto.numero,
                            quantidadesPorDiaJson = jsonFormat.encodeToString(serializer<Map<String,Double>>(), entradaItem.quantidadesPorDia)
                        )
                    }
                }
                if (novasEntradasEntities.isNotEmpty()) {
                    entradaDao.insertAllEntradas(novasEntradasEntities)
                }
                Log.d("FeiraRepository_Save", "${novasEntradasEntities.size} EntradaEntity salvas para Feira ID: ${feiraEntity.feiraId}")

                // Perdas
                perdaDao.deleteAllPerdasForFeira(dadosFeira.fairDetails.feiraId)
                val novasPerdasEntities = dadosFeira.perdasTotaisDaFeira.map { perdaItem ->
                    PerdaEntity(
                        feiraId = dadosFeira.fairDetails.feiraId,
                        produtoNumero = perdaItem.produto.numero,
                        perdasPorDiaJson = jsonFormat.encodeToString(serializer<Map<String,Double>>(),perdaItem.perdasPorDia)
                    )
                }
                if (novasPerdasEntities.isNotEmpty()) {
                    perdaDao.insertAllPerdas(novasPerdasEntities)
                }
                Log.d("FeiraRepository_Save", "${novasPerdasEntities.size} PerdaEntity salvas para Feira ID: ${feiraEntity.feiraId}")

                // Despesas
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

                    if (temAlgumValor) {
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
                if (novasDespesasEntities.isNotEmpty()) {
                    novasDespesasEntities.forEach { despesaFeiraDao.insertOrUpdate(it) }
                }
                Log.d("FeiraRepository_Save", "${novasDespesasEntities.size} DespesaFeiraEntity salvas para Feira ID: ${feiraEntity.feiraId}")
                Log.d("FeiraRepository_Save", "Transação concluída com sucesso para Feira ID: ${dadosFeira.fairDetails.feiraId}")
            }
            true
        } catch (e: Exception) {
            Log.e("FeiraRepository_Save", "EXCEÇÃO GERAL ao salvar feira ${dadosFeira.fairDetails.feiraId}: ${e.message}", e)
            false
        }
    }

    suspend fun deletarFeira(feiraId: String): Boolean {
        return try {
            appDatabase.withTransaction { // Boa prática usar transação aqui também
                feiraDao.deleteFeiraById(feiraId)
            }
            Log.d("FeiraRepository", "Feira $feiraId deletada do banco (e dados associados via cascade).")
            true
        } catch (e: Exception) {
            Log.e("FeiraRepository", "Erro ao deletar feira $feiraId: ${e.message}", e)
            false
        }
    }
}