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

    suspend fun calcularDistribuicao(
        listaSobrasUi: List<SobraUiItem>,
        entradasAtuaisDestino: Map<String, List<EntradaItemAgricultor>>
    ): Map<String, List<EntradaItemAgricultor>> {
        val idFeiraOrigem = _feiraAnteriorId.value ?: return entradasAtuaisDestino

        val mapaEntradasAtualizado = mutableMapOf<String, MutableList<EntradaItemAgricultor>>()
        entradasAtuaisDestino.forEach { (agricultorId, entradas) ->
            mapaEntradasAtualizado[agricultorId] = entradas.toMutableList()
        }

        listaSobrasUi.forEach { itemUi ->
            val sobraReal = itemUi.sobraRealInput.replace(',', '.').toDoubleOrNull() ?: 0.0
            if (sobraReal > 0) {
                val produto = itemUi.produto // O produto da sobra
                val produtoNumero = produto.numero
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

                        val agricultorId = entradaOrigem.agricultorId
                        val listaEntradasDestino = mapaEntradasAtualizado.getOrPut(agricultorId) { mutableListOf() }

                        // <<< CORREÇÃO AQUI: Usa acesso seguro '?.' >>>
                        val entradaExistenteIndex = listaEntradasDestino.indexOfFirst { it.produto?.numero == produtoNumero }

                        if (entradaExistenteIndex != -1) {
                            // Se já existe uma entrada para este produto, atualiza a sobra
                            val entradaAntiga = listaEntradasDestino[entradaExistenteIndex]
                            listaEntradasDestino[entradaExistenteIndex] = entradaAntiga.copy(quantidadeSobraDaSemanaAnterior = sobraDistribuida)
                        } else {
                            // Se não existe, cria uma nova entrada
                            listaEntradasDestino.add(
                                EntradaItemAgricultor(
                                    produto = produto,
                                    quantidadeSobraDaSemanaAnterior = sobraDistribuida,
                                    quantidadesPorDia = emptyMap()
                                )
                            )
                        }
                    }
                }
            }
        }
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