package com.example.gestaofeiracooperativa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.util.Locale

class LancamentoDespesasFeiraViewModel(
    private val itemDespesaRepository: ItemDespesaRepository,
    private val despesaFeiraRepository: DespesaFeiraRepository,
    private val feiraId: String
) : ViewModel() {

    private val _listaDespesasUi = MutableStateFlow<List<DespesaFeiraUiItem>>(emptyList())
    val listaDespesasUi: StateFlow<List<DespesaFeiraUiItem>> = _listaDespesasUi.asStateFlow()

    private val jsonFormat = Json { ignoreUnknownKeys = true; isLenient = true }

    init {
        carregarDespesasDaFeiraAtual()
    }

    private fun carregarDespesasDaFeiraAtual() {
        viewModelScope.launch {
            itemDespesaRepository.getAllItensDespesa()
                .combine(despesaFeiraRepository.getDespesasByFeiraId(feiraId)) { todosItens, despesasLancadas ->
                    todosItens.map { itemDeDespesa ->
                        val lancamentoExistente = despesasLancadas.find { it.itemDespesaId == itemDeDespesa.id }
                        val valoresPorDiaInputMap = mutableMapOf<String, String>()
                        val mapaDeValoresNumericos = lancamentoExistente?.valoresPorDiaJson?.let { json ->
                            try { jsonFormat.decodeFromString<Map<String, Double>>(json) } catch (e: Exception) { emptyMap() }
                        } ?: emptyMap()

                        diasDaSemanaFeira.forEach { dia ->
                            valoresPorDiaInputMap[dia] = mapaDeValoresNumericos[dia]?.formatParaUi() ?: ""
                        }
                        DespesaFeiraUiItem(
                            itemDespesa = itemDeDespesa,
                            valoresPorDiaInput = valoresPorDiaInputMap,
                            observacaoInput = lancamentoExistente?.observacao ?: "",
                            isExistingEntry = lancamentoExistente != null
                        )
                    }
                }
                .catch { /* Tratar erro se necessário */ }
                .collect { _listaDespesasUi.value = it }
        }
    }

    private fun Double?.formatParaUi(): String {
        if (this == null) return ""
        return if (this % 1.0 == 0.0) this.toInt().toString() else String.format(Locale.getDefault(), "%.2f", this).replace('.', ',')
    }
}


// A Factory foi simplificada, não precisa mais do FeiraRepository
class LancamentoDespesasFeiraViewModelFactory(
    private val itemDespesaRepository: ItemDespesaRepository,
    private val despesaFeiraRepository: DespesaFeiraRepository,
    private val feiraId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LancamentoDespesasFeiraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LancamentoDespesasFeiraViewModel(itemDespesaRepository, despesaFeiraRepository, feiraId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}