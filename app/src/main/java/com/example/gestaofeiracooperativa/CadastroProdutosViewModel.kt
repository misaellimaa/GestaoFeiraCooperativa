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

class CadastroProdutosViewModel(private val repository: ProdutoRepository) : ViewModel() {

    // Estado da barra de busca
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Lista de produtos observável (filtrada pela busca)
    val produtos: StateFlow<List<Produto>> = searchQuery
        .debounce(300L) // Espera 300ms após a digitação
        .distinctUntilChanged() // Emite apenas se o valor mudou
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllProducts() // Se busca vazia, mostra todos
            } else {
                repository.searchProducts(query) // Se tem busca, filtra
            }
        }
        .stateIn(
            viewModelScope, // Scope para a coroutine
            started = SharingStarted.WhileSubscribed(5000), // Inicia e para a coleta de Flow
            initialValue = emptyList() // Valor inicial antes da primeira emissão
        )

    // Métodos para operações CRUD
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    suspend fun getProductByNumber(numero: String): Produto? {
        return repository.getProductByNumber(numero)
    }

    fun insert(produto: Produto) = viewModelScope.launch {
        repository.insert(produto)
    }

    fun update(produto: Produto) = viewModelScope.launch {
        repository.update(produto)
    }

    fun delete(produto: Produto) = viewModelScope.launch {
        repository.delete(produto)
    }

    fun deleteAllProducts() = viewModelScope.launch {
        repository.deleteAllProducts()
    }
}

// Factory para criar a ViewModel com o repositório
class CadastroProdutosViewModelFactory(private val repository: ProdutoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CadastroProdutosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CadastroProdutosViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
