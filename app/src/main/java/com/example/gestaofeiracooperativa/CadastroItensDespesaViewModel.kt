package com.example.gestaofeiracooperativa

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Importe ItemDespesaEntity e ItemDespesaRepository
// import com.example.gestaofeiracooperativa.ItemDespesaEntity
// import com.example.gestaofeiracooperativa.ItemDespesaRepository

class CadastroItensDespesaViewModel(private val repository: ItemDespesaRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Lista de itens de despesa que reage à busca
    val itensDespesa: StateFlow<List<ItemDespesaEntity>> = _searchQuery
        .debounce(300L) // Opcional: pequeno delay para não buscar a cada tecla digitada
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllItensDespesa()
            } else {
                repository.searchItensDespesa(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L), // Mantém o Flow ativo por 5s após o último coletor sumir
            initialValue = emptyList()
        )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Busca um item de despesa pelo nome.
     * Usado para verificar se um item com o mesmo nome já existe antes de inserir.
     */
    suspend fun getItemDespesaByNome(nome: String): ItemDespesaEntity? {
        return repository.getItemDespesaByNome(nome)
    }

    /**
     * Insere um novo item de despesa.
     * Retorna true se a inserção for bem-sucedida, false caso contrário (ex: nome duplicado).
     */
    suspend fun insertItemDespesa(itemDespesa: ItemDespesaEntity): Boolean {
        return try {
            // Verifica novamente se o nome já existe (como uma dupla checagem,
            // embora o DAO com ABORT e índice único já previna no banco)
            if (repository.getItemDespesaByNome(itemDespesa.nome) != null) {
                Log.w("ViewModelDespesa", "Tentativa de inserir item de despesa com nome duplicado: ${itemDespesa.nome}")
                // Poderia lançar uma exceção customizada ou retornar um código de erro específico
                return false // Indica falha devido a nome duplicado (verificação extra)
            }
            repository.insertItemDespesa(itemDespesa)
            true // Sucesso
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            // Este catch é para o caso do DAO lançar a exceção devido ao OnConflictStrategy.ABORT
            // e ao índice único no nome da entidade ItemDespesaEntity.
            Log.e("ViewModelDespesa", "Erro de constraint ao inserir item de despesa (nome duplicado?): ${itemDespesa.nome}", e)
            false // Indica falha
        } catch (e: Exception) {
            Log.e("ViewModelDespesa", "Erro geral ao inserir item de despesa: ${itemDespesa.nome}", e)
            false // Indica falha
        }
    }

    fun updateItemDespesa(itemDespesa: ItemDespesaEntity) = viewModelScope.launch {
        try {
            repository.updateItemDespesa(itemDespesa)
        } catch (e: Exception) {
            Log.e("ViewModelDespesa", "Erro ao atualizar item de despesa: ${itemDespesa.id}", e)
            // TODO: Considerar como notificar a UI sobre falhas na atualização
        }
    }

    fun deleteItemDespesa(itemDespesa: ItemDespesaEntity) = viewModelScope.launch {
        try {
            repository.deleteItemDespesa(itemDespesa)
        } catch (e: Exception) {
            Log.e("ViewModelDespesa", "Erro ao deletar item de despesa: ${itemDespesa.id}", e)
            // TODO: Considerar como notificar a UI sobre falhas na deleção
        }
    }
}

// ViewModel Factory para injetar o ItemDespesaRepository
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