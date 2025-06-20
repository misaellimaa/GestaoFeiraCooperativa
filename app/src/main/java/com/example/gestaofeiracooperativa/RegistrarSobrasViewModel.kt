package com.example.gestaofeiracooperativa

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.DecimalFormat

class RegistrarSobrasViewModel(
    private val feiraIdAtual: String,
    private val feiraRepository: FeiraRepository,
    private val produtoRepository: ProdutoRepository,
    private val perdaRepository: PerdaRepository,
    private val entradaRepository: EntradaRepository
) : ViewModel() {

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
                Log.d("RegistrarSobrasVM", "Feira anterior encontrada: ${feiraAnterior?.feiraId}")

                if (feiraAnterior != null) {
                    perdaRepository.getPerdasForFeira(feiraAnterior.feiraId).firstOrNull()?.let { perdas ->
                        Log.d("RegistrarSobrasVM", "Encontradas ${perdas.size} perdas para a feira anterior.")
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
                        Log.d("RegistrarSobrasVM", "Itens de UI de sobra criados: ${uiItems.size}")
                    }
                }
            } catch (e: Exception) { Log.e("RegistrarSobrasVM", "Erro ao carregar dados iniciais.", e) }
        }
    }

    suspend fun calcularDistribuicao(
        listaSobrasUi: List<SobraUiItem>,
        entradasAtuaisDestino: Map<String, List<EntradaItemAgricultor>>
    ): Map<String, List<EntradaItemAgricultor>> {
        val idFeiraOrigem = _feiraAnteriorId.value ?: return entradasAtuaisDestino
        Log.d("CalcularDistribuicao", "Iniciando cálculo para feira de origem: $idFeiraOrigem")

        val mapaEntradasAtualizado = entradasAtuaisDestino.mapValues { it.value.toMutableList() }.toMutableMap()

        // Para cada item de sobra que o usuário preencheu...
        listaSobrasUi.forEach { itemUi ->
            val sobraReal = itemUi.sobraRealInput.replace(',', '.').toDoubleOrNull() ?: 0.0
            if (sobraReal > 0) {
                val produtoDaSobra = itemUi.produto
                val produtoNumero = produtoDaSobra.numero
                Log.d("CalcularDistribuicao", "Processando sobra de ${produtoDaSobra.item} (Nº$produtoNumero): $sobraReal")

                // 1. Encontrar todas as entradas do produto na feira de ORIGEM
                val entradasOrigem = entradaRepository.getEntradasForFeira(idFeiraOrigem).firstOrNull()
                    ?.filter { it.produtoNumero == produtoNumero } ?: emptyList()

                // 2. Calcular o total entregue deste produto na feira de ORIGEM
                var totalEntregueOrigem = 0.0
                entradasOrigem.forEach { entrada ->
                    val quantidadesMap = try { Json.decodeFromString<Map<String, Double>>(entrada.quantidadesPorDiaJson) } catch (e: Exception) { emptyMap() }
                    totalEntregueOrigem += quantidadesMap.values.sum() + entrada.quantidadeSobra
                }
                Log.d("CalcularDistribuicao", "Total entregue de ${produtoDaSobra.item} na origem: $totalEntregueOrigem")

                if (totalEntregueOrigem > 0) {
                    // 3. Para cada agricultor que entregou na ORIGEM, calcular sua proporção
                    entradasOrigem.forEach { entradaOrigem ->
                        val quantidadesMapOrigem = try { Json.decodeFromString<Map<String, Double>>(entradaOrigem.quantidadesPorDiaJson) } catch (e: Exception) { emptyMap() }
                        val entreguePeloAgricultorNaOrigem = quantidadesMapOrigem.values.sum() + entradaOrigem.quantidadeSobra
                        val proporcao = entreguePeloAgricultorNaOrigem / totalEntregueOrigem
                        val sobraDistribuida = sobraReal * proporcao
                        val agricultorId = entradaOrigem.agricultorId

                        Log.d("CalcularDistribuicao", "Agricultor ID $agricultorId: proporção $proporcao, sobra a adicionar $sobraDistribuida")

                        // 4. Adicionar a sobra ao agricultor na feira de DESTINO
                        val listaEntradasDestino = mapaEntradasAtualizado.getOrPut(agricultorId) { mutableListOf() }
                        val entradaExistenteIndex = listaEntradasDestino.indexOfFirst { it.produto?.numero == produtoNumero }

                        if (entradaExistenteIndex != -1) {
                            // Se o agricultor JÁ TEM uma entrada para este produto na feira de destino,
                            // atualizamos o campo da sobra.
                            val entradaAntiga = listaEntradasDestino[entradaExistenteIndex]
                            listaEntradasDestino[entradaExistenteIndex] = entradaAntiga.copy(quantidadeSobraDaSemanaAnterior = sobraDistribuida)
                            Log.d("CalcularDistribuicao", "  -> Atualizando entrada existente para Agricultor $agricultorId.")
                        } else {
                            // Se o agricultor NÃO TEM uma entrada para este produto,
                            // criamos uma nova, contendo apenas a sobra.
                            listaEntradasDestino.add(
                                EntradaItemAgricultor(
                                    produto = produtoDaSobra,
                                    quantidadeSobraDaSemanaAnterior = sobraDistribuida,
                                    quantidadesPorDia = emptyMap() // Sem novas entradas, só a sobra
                                )
                            )
                            Log.d("CalcularDistribuicao", "  -> Criando nova entrada para Agricultor $agricultorId.")
                        }
                    }
                }
            }
        }
        Log.d("CalcularDistribuicao", "Cálculo finalizado. Retornando mapa de entradas atualizado.")
        return mapaEntradasAtualizado
    }


    private fun formatDouble(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else DecimalFormat("#,##0.00").format(value)
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