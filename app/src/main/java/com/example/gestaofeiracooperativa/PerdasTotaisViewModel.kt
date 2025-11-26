package com.example.gestaofeiracooperativa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PerdasTotaisViewModel(
    private val feiraId: String,
    private val perdaRepository: PerdaRepository,
    private val produtoRepository: ProdutoRepository
) : ViewModel() {

    private val _perdasRegistradas = MutableStateFlow<List<PerdaItemFeira>>(emptyList())
    val perdasRegistradas: StateFlow<List<PerdaItemFeira>> = _perdasRegistradas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        carregarPerdas()
    }

    private fun carregarPerdas() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                perdaRepository.getPerdasForFeira(feiraId).collect { listaEntidades ->
                    val listaConvertida = listaEntidades.mapNotNull { entidade ->
                        produtoRepository.getProductByNumber(entidade.produtoNumero)?.let { produto ->
                            val mapPerdas = try {
                                Json.decodeFromString<Map<String, Double>>(entidade.perdasPorDiaJson)
                            } catch (e: Exception) {
                                emptyMap()
                            }

                            PerdaItemFeira(
                                produto = produto,
                                perdasPorDia = HashMap(mapPerdas),
                                quantidadeSobra = entidade.quantidadeSobra
                            )
                        }
                    }
                    _perdasRegistradas.value = listaConvertida
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    fun adicionarOuAtualizarPerda(novaPerda: PerdaItemFeira) {
        val listaAtual = _perdasRegistradas.value.toMutableList()
        val index = listaAtual.indexOfFirst { it.produto?.numero == novaPerda.produto?.numero }

        if (index != -1) {
            listaAtual[index] = novaPerda
        } else {
            listaAtual.add(novaPerda)
        }
        _perdasRegistradas.value = listaAtual
    }

    fun removerPerda(index: Int) {
        if (index in _perdasRegistradas.value.indices) {
            val listaAtual = _perdasRegistradas.value.toMutableList()
            listaAtual.removeAt(index)
            _perdasRegistradas.value = listaAtual
        }
    }

    fun salvarPerdasFinais(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // <<< CORREÇÃO: Agora chamamos direto do repositório, sem '.perdaDao' >>>
                perdaRepository.deleteAllPerdasForFeira(feiraId)

                val entidades = _perdasRegistradas.value.map { item ->
                    PerdaEntity(
                        feiraId = feiraId,
                        produtoNumero = item.produto?.numero ?: "",
                        perdasPorDiaJson = Json.encodeToString(item.perdasPorDia),
                        quantidadeSobra = item.quantidadeSobra
                    )
                }

                if (entidades.isNotEmpty()) {
                    // <<< CORREÇÃO: Agora chamamos direto do repositório >>>
                    perdaRepository.insertAllPerdas(entidades)
                }

                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class PerdasTotaisViewModelFactory(
    private val feiraId: String,
    private val perdaRepository: PerdaRepository,
    private val produtoRepository: ProdutoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PerdasTotaisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PerdasTotaisViewModel(feiraId, perdaRepository, produtoRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}