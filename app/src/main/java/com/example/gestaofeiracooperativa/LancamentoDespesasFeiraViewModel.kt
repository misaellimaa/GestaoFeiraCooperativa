package com.example.gestaofeiracooperativa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
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

    private val _operationStatus = MutableStateFlow<UiState<String>>(UiState.Idle)
    val operationStatus: StateFlow<UiState<String>> = _operationStatus.asStateFlow()

    fun resetOperationStatus() {
        _operationStatus.value = UiState.Idle
    }

    // Função para salvar ou atualizar uma despesa específica
    fun salvarDespesa(despesaUi: DespesaFeiraUiItem) {
        viewModelScope.launch {
            _operationStatus.value = UiState.Loading

            val valoresConvertidos = mutableMapOf<String, Double>()
            despesaUi.valoresPorDiaInput.forEach { (dia, valorStr) ->
                valorStr.replace(',', '.').toDoubleOrNull()?.let {
                    valoresConvertidos[dia] = it
                }
            }

            // Só salva se houver algum valor ou uma observação
            if (valoresConvertidos.isNotEmpty() || !despesaUi.observacaoInput.isBlank()) {
                val despesaEntity = DespesaFeiraEntity(
                    feiraId = feiraId, // O feiraId já é um membro da classe
                    itemDespesaId = despesaUi.itemDespesa.id,
                    valoresPorDiaJson = jsonFormat.encodeToString(valoresConvertidos),
                    observacao = despesaUi.observacaoInput.trim().ifEmpty { null }
                )
                val sucesso = despesaFeiraRepository.insertOrUpdateDespesa(despesaEntity)
                if (sucesso) {
                    _operationStatus.value = UiState.Success("Despesa '${despesaUi.itemDespesa.nome}' salva.")
                } else {
                    _operationStatus.value = UiState.Error("Falha ao salvar a despesa '${despesaUi.itemDespesa.nome}'.")
                }
            } else {
                // Se não há valores e era um registo existente, deleta-o
                if (despesaUi.isExistingEntry) {
                    deletarDespesa(despesaUi)
                } else {
                    _operationStatus.value = UiState.Idle // Nenhum dado para salvar
                }
            }
        }
    }

    // Função para deletar uma despesa
    fun deletarDespesa(despesaUi: DespesaFeiraUiItem) {
        viewModelScope.launch {
            _operationStatus.value = UiState.Loading
            val sucesso = despesaFeiraRepository.deleteDespesaByFeiraAndItem(feiraId, despesaUi.itemDespesa.id)
            if (sucesso) {
                _operationStatus.value = UiState.Success("Despesa '${despesaUi.itemDespesa.nome}' removida.")
            } else {
                _operationStatus.value = UiState.Error("Falha ao remover a despesa '${despesaUi.itemDespesa.nome}'.")
            }
        }
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