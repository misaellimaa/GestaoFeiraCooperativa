package com.example.gestaofeiracooperativa

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer
import java.text.DecimalFormat
import java.util.Locale

// Seus imports...

class RegistrarSobrasViewModel(
    private val feiraIdAtual: String, // ID da feira de DESTINO
    private val feiraRepository: FeiraRepository,
    private val produtoRepository: ProdutoRepository,
    private val perdaRepository: PerdaRepository,
    private val entradaRepository: EntradaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    private val _feiraAnteriorId = MutableStateFlow<String?>(null)
    val feiraAnteriorId: StateFlow<String?> = _feiraAnteriorId.asStateFlow()

    private val _sobraUiItems = MutableStateFlow<List<SobraUiItem>>(emptyList())
    val sobraUiItems: StateFlow<List<SobraUiItem>> = _sobraUiItems.asStateFlow()

    init {
        carregarDadosDaFeiraAnterior()
    }

    private fun carregarDadosDaFeiraAnterior() {
        viewModelScope.launch {
            try {
                val feiraAnterior = feiraRepository.getFeiraAnterior(feiraIdAtual)
                _feiraAnteriorId.value = feiraAnterior?.feiraId

                if (feiraAnterior != null) {
                    perdaRepository.getPerdasForFeira(feiraAnterior.feiraId).firstOrNull()?.let { perdas ->
                        val uiItems = perdas.mapNotNull { perdaEntity ->
                            produtoRepository.getProductByNumber(perdaEntity.produtoNumero)?.let { produto ->
                                val perdasMap = try { Json.decodeFromString<Map<String, Double>>(perdaEntity.perdasPorDiaJson) } catch (e: Exception) { emptyMap() }
                                SobraUiItem(
                                    produto = produto,
                                    perdaTotalCalculada = perdasMap.values.sum(),
                                    sobraRealInput = if (perdaEntity.quantidadeSobra > 0.0) formatDouble(perdaEntity.quantidadeSobra) else "",
                                    perdaEntityOriginal = perdaEntity
                                )
                            }
                        }
                        _sobraUiItems.value = uiItems
                    }
                }
            } catch (e: Exception) { Log.e("RegistrarSobrasVM", "Erro ao carregar dados iniciais.", e) }
        }
    }

    // <<< FUNÇÃO PRINCIPAL REATORADA E CORRIGIDA >>>
    fun distribuirSobras(listaSobrasUi: List<SobraUiItem>) {
        val idFeiraOrigem = _feiraAnteriorId.value
        if (idFeiraOrigem == null) {
            viewModelScope.launch { _uiState.value = UiState.Error("Nenhuma feira anterior encontrada para buscar sobras.") }
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // Prepara todas as alterações necessárias em memória primeiro

                // 1. Lista de PerdaEntity da feira de ORIGEM a serem atualizadas com a sobra real
                val perdasParaAtualizar = listaSobrasUi.map { itemUi ->
                    val sobraReal = itemUi.sobraRealInput.replace(',', '.').toDoubleOrNull() ?: 0.0
                    itemUi.perdaEntityOriginal.copy(quantidadeSobra = sobraReal)
                }

                // 2. Mapa de EntradaEntity da feira de DESTINO a serem inseridas/atualizadas
                val entradasParaAtualizarOuInserir = mutableMapOf<String, EntradaEntity>()

                // Busca todas as entradas da feira de destino UMA VEZ para evitar múltiplas chamadas ao DB
                val entradasDestinoExistentes = entradaRepository.getEntradasForFeira(feiraIdAtual).firstOrNull() ?: emptyList()

                // Primeiro, popula o mapa com as entradas existentes, resetando a sobra
                entradasDestinoExistentes.forEach { entrada ->
                    val chave = "${entrada.agricultorId}-${entrada.produtoNumero}"
                    entradasParaAtualizarOuInserir[chave] = entrada.copy(quantidadeSobra = 0.0)
                }

                // Agora, calcula a distribuição e atualiza o mapa
                listaSobrasUi.forEach { itemUi ->
                    val sobraReal = itemUi.sobraRealInput.replace(',', '.').toDoubleOrNull() ?: 0.0
                    if (sobraReal > 0) {
                        val produtoNumero = itemUi.produto.numero
                        val entradasOrigem = entradaRepository.getEntradasForFeira(idFeiraOrigem).firstOrNull()
                            ?.filter { it.produtoNumero == produtoNumero } ?: emptyList()

                        var totalEntregueOrigem = 0.0
                        entradasOrigem.forEach { entrada ->
                            val quantidadesMap = try { Json.decodeFromString<Map<String, Double>>(entrada.quantidadesPorDiaJson) } catch (e: Exception) { emptyMap() }
                            totalEntregueOrigem += quantidadesMap.values.sum()
                        }

                        if (totalEntregueOrigem > 0) {
                            entradasOrigem.forEach { entradaOrigem ->
                                val quantidadesMap = try { Json.decodeFromString<Map<String, Double>>(entradaOrigem.quantidadesPorDiaJson) } catch (e: Exception) { emptyMap() }
                                val entreguePeloAgricultor = quantidadesMap.values.sum()
                                val proporcao = entreguePeloAgricultor / totalEntregueOrigem
                                val sobraDistribuida = sobraReal * proporcao

                                // Atualiza ou cria a entrada no mapa em memória
                                val chave = "${entradaOrigem.agricultorId}-${produtoNumero}"
                                val entradaNoDestino = entradasParaAtualizarOuInserir[chave]
                                if (entradaNoDestino != null) {
                                    entradasParaAtualizarOuInserir[chave] = entradaNoDestino.copy(quantidadeSobra = sobraDistribuida)
                                } else {
                                    entradasParaAtualizarOuInserir[chave] = EntradaEntity(
                                        feiraId = feiraIdAtual,
                                        agricultorId = entradaOrigem.agricultorId,
                                        produtoNumero = produtoNumero,
                                        quantidadeSobra = sobraDistribuida,
                                        quantidadesPorDiaJson = "{}"
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Executa todas as operações de banco de dados em uma única transação no repositório
                feiraRepository.executarDistribuicaoDeSobras(perdasParaAtualizar, entradasParaAtualizarOuInserir.values.toList())

                _uiState.value = UiState.Success(Unit)

            } catch (e: Exception) {
                Log.e("RegistrarSobrasVM", "Erro ao distribuir sobras.", e)
                _uiState.value = UiState.Error("Erro ao distribuir sobras: ${e.message}")
            }
        }
    }

    fun resetState() { _uiState.value = UiState.Idle }
    private fun formatDouble(value: Double): String {
        return DecimalFormat("#,##0.00").format(value)
    }
}


class RegistrarSobrasViewModelFactory(
    private val feiraIdAtual: String,
    private val feiraRepository: FeiraRepository,
    private val produtoRepository: ProdutoRepository,
    private val perdaRepository: PerdaRepository,
    private val entradaRepository: EntradaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegistrarSobrasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegistrarSobrasViewModel(feiraIdAtual, feiraRepository, produtoRepository, perdaRepository, entradaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}