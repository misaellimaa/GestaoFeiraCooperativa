package com.example.gestaofeiracooperativa // <<--- MUDE PARA O SEU PACKAGE REAL

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull

class CadastroAgricultoresViewModel(private val repository: AgricultorRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val agricultores: StateFlow<List<Agricultor>> = searchQuery
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllAgricultores()
            } else {
                repository.searchAgricultores(query)
            }
        }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    suspend fun getAgricultorById(id: String): Agricultor? {
        return repository.getAgricultorById(id)
    }

    fun insert(agricultor: Agricultor) = viewModelScope.launch {
        repository.insert(agricultor)
    }

    fun update(agricultor: Agricultor) = viewModelScope.launch {
        repository.update(agricultor)
    }

    fun delete(agricultor: Agricultor) = viewModelScope.launch {
        repository.delete(agricultor)
    }

    fun deleteAllAgricultores() = viewModelScope.launch {
        repository.deleteAllAgricultores()
    }
}

class CadastroAgricultoresViewModelFactory(private val repository: AgricultorRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CadastroAgricultoresViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CadastroAgricultoresViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
