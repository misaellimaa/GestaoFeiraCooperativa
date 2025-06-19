package com.example.gestaofeiracooperativa

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

// Importe ItemDespesaEntity e ItemDespesaRepository

class CadastroItensDespesaViewModel(private val repository: ItemDespesaRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // A lista de itens de despesa continua sendo lida do Room, que é
    // atualizado em tempo real pelo ouvinte do Firestore no repositório.
    val itensDespesa: StateFlow<List<ItemDespesaEntity>> = _searchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllItensDespesa()
            } else {
                repository.searchItensDespesa(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // <<< NOVO: StateFlow para notificar a UI sobre o resultado das operações >>>
    private val _operationStatus = MutableStateFlow<UiState<String>>(UiState.Idle)
    val operationStatus: StateFlow<UiState<String>> = _operationStatus.asStateFlow()

    fun resetOperationStatus() {
        _operationStatus.value = UiState.Idle
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Busca um item de despesa pelo nome no cache local (Room).
     * Útil para uma verificação rápida na UI para evitar nomes duplicados.
     */
    suspend fun getItemDespesaByNome(nome: String): ItemDespesaEntity? {
        return repository.getItemDespesaByNome(nome)
    }

    /**
     * Tenta inserir um novo item de despesa.
     */
    fun insertItemDespesa(itemDespesa: ItemDespesaEntity) {
        viewModelScope.launch {
            _operationStatus.value = UiState.Loading
            val sucesso = repository.insertItemDespesa(itemDespesa)
            if (sucesso) {
                _operationStatus.value = UiState.Success("Item '${itemDespesa.nome}' adicionado com sucesso!")
            } else {
                _operationStatus.value = UiState.Error("Falha ao adicionar o item '${itemDespesa.nome}'.")
            }
        }
    }

    /**
     * Tenta atualizar um item de despesa.
     */
    fun updateItemDespesa(itemDespesa: ItemDespesaEntity) {
        viewModelScope.launch {
            _operationStatus.value = UiState.Loading
            val sucesso = repository.updateItemDespesa(itemDespesa)
            if (sucesso) {
                _operationStatus.value = UiState.Success("Item '${itemDespesa.nome}' atualizado com sucesso!")
            } else {
                _operationStatus.value = UiState.Error("Falha ao atualizar o item '${itemDespesa.nome}'.")
            }
        }
    }

    /**
     * Tenta deletar um item de despesa.
     */
    fun deleteItemDespesa(itemDespesa: ItemDespesaEntity) {
        viewModelScope.launch {
            _operationStatus.value = UiState.Loading
            val sucesso = repository.deleteItemDespesa(itemDespesa)
            if (sucesso) {
                _operationStatus.value = UiState.Success("Item '${itemDespesa.nome}' deletado com sucesso!")
            } else {
                _operationStatus.value = UiState.Error("Falha ao deletar o item '${itemDespesa.nome}'.")
            }
        }
    }
}

// A Factory para criar a ViewModel permanece a mesma
class CadastroItensDespesaViewModelFactory(
    private val repository: ItemDespesaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CadastroItensDespesaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CadastroItensDespesaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for CadastroItensDespesa")
    }
}