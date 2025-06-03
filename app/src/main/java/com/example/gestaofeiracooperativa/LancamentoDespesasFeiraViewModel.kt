package com.example.gestaofeiracooperativa // Seu package

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.serializer
import java.util.Locale

// Importe seus modelos, DAOs e Repositórios.
// Certifique-se que os caminhos estão corretos.
import com.example.gestaofeiracooperativa.ItemDespesaEntity
import com.example.gestaofeiracooperativa.DespesaFeiraEntity
import com.example.gestaofeiracooperativa.ItemDespesaRepository
import com.example.gestaofeiracooperativa.DespesaFeiraRepository
import com.example.gestaofeiracooperativa.DespesaFeiraUiItem
import com.example.gestaofeiracooperativa.diasDaSemanaFeira // Sua lista global de dias

class LancamentoDespesasFeiraViewModel(
    private val itemDespesaRepository: ItemDespesaRepository,
    private val despesaFeiraRepository: DespesaFeiraRepository,
    private val feiraId: String // Recebe o ID da feira atual
) : ViewModel() {

    private val _listaDespesasUi = MutableStateFlow<List<DespesaFeiraUiItem>>(emptyList())
    val listaDespesasUi: StateFlow<List<DespesaFeiraUiItem>> = _listaDespesasUi.asStateFlow()

    private val _saveStatus = MutableSharedFlow<Boolean>()
    val saveStatus: SharedFlow<Boolean> = _saveStatus.asSharedFlow()

    private val jsonFormat = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    init {
        Log.d("ViewModelDespFeira_Lifecycle", "ViewModel INIT para Feira ID: $feiraId")
        carregarDespesas()
    }

    private fun carregarDespesas() {
        viewModelScope.launch {
            Log.d("ViewModelDespFeira_Load", "Iniciando carregarDespesas para Feira ID: $feiraId")

            itemDespesaRepository.getAllItensDespesa()
                .combine(despesaFeiraRepository.getDespesasByFeiraId(feiraId)) { todosItensCadastrados, despesasLancadasParaEstaFeira ->

                    Log.d("ViewModelDespFeira_Load", "Feira ID $feiraId - Coletado ${todosItensCadastrados.size} itens de despesa cadastrados.")
                    Log.d("ViewModelDespFeira_Load", "Feira ID $feiraId - Coletado ${despesasLancadasParaEstaFeira.size} despesas lançadas no banco para esta feira.")
                    despesasLancadasParaEstaFeira.forEach { lanc ->
                        Log.d("ViewModelDespFeira_Load", "  -> Lançamento existente no DB: itemDespesaId=${lanc.itemDespesaId}, jsonValores='${lanc.valoresPorDiaJson}', obs='${lanc.observacao}'")
                    }

                    if (todosItensCadastrados.isEmpty()){
                        Log.w("ViewModelDespFeira_Load", "Nenhum item de despesa cadastrado no sistema. A lista de UI ficará vazia.")
                    }

                    todosItensCadastrados.map { itemDeDespesa ->
                        Log.d("ViewModelDespFeira_Load", "Mapeando UI para ItemDespesa: ${itemDeDespesa.nome} (ID: ${itemDeDespesa.id})")

                        val lancamentoExistente = despesasLancadasParaEstaFeira.find { it.itemDespesaId == itemDeDespesa.id }

                        if (lancamentoExistente != null) {
                            Log.i("ViewModelDespFeira_Load", "  Encontrado lançamento existente para ${itemDeDespesa.nome}. JSON: '${lancamentoExistente.valoresPorDiaJson}'")
                        } else {
                            Log.i("ViewModelDespFeira_Load", "  NÃO encontrado lançamento existente para ${itemDeDespesa.nome}.")
                        }

                        val valoresPorDiaInputMap = mutableMapOf<String, String>()
                        // val diasDaSemana = diasDaSemanaFeira // Já é uma constante global

                        val mapaDeValoresNumericos = lancamentoExistente?.valoresPorDiaJson?.let { jsonString ->
                            if (jsonString.isNotBlank()) {
                                try {
                                    jsonFormat.decodeFromString<Map<String, Double>>(jsonString)
                                } catch (e: Exception) {
                                    Log.e("ViewModelDespFeira_Load", "ERRO DESSERIALIZANDO para ${itemDeDespesa.nome}: '$jsonString'", e)
                                    emptyMap<String, Double>()
                                }
                            } else {
                                Log.w("ViewModelDespFeira_Load", "  JSON de valoresPorDia está em branco para ${itemDeDespesa.nome}.")
                                emptyMap<String, Double>()
                            }
                        } ?: emptyMap<String, Double>()

                        if(lancamentoExistente != null && mapaDeValoresNumericos.isEmpty() && !lancamentoExistente.valoresPorDiaJson.isNullOrBlank()){
                            Log.w("ViewModelDespFeira_Load", "  MAPA NUMÉRICO VAZIO para ${itemDeDespesa.nome} apesar do JSON não ser branco. Verifique o JSON: '${lancamentoExistente.valoresPorDiaJson}'")
                        }


                        diasDaSemanaFeira.forEach { dia ->
                            valoresPorDiaInputMap[dia] = mapaDeValoresNumericos[dia]?.formatParaUi() ?: ""
                        }

                        Log.d("ViewModelDespFeira_Load", "  ValoresPorDiaInputMap final para ${itemDeDespesa.nome}: $valoresPorDiaInputMap")

                        DespesaFeiraUiItem(
                            itemDespesa = itemDeDespesa,
                            valoresPorDiaInput = valoresPorDiaInputMap,
                            observacaoInput = lancamentoExistente?.observacao ?: "",
                            isExistingEntry = lancamentoExistente != null
                        )
                    }
                }
                .catch { e -> // Captura exceções do Flow ou do combine/map
                    Log.e("ViewModelDespFeira", "EXCEÇÃO no Flow de carregarDespesas: ${e.message}", e)
                    _listaDespesasUi.value = emptyList()
                }
                .collect { itensMapeados ->
                    Log.i("ViewModelDespFeira_Load", "COLETOU e atualizou _listaDespesasUi com ${itensMapeados.size} itens.")
                    _listaDespesasUi.value = itensMapeados
                }
        }
    }

    private fun Double?.formatParaUi(): String {
        if (this == null) return ""
        return if (this % 1.0 == 0.0) this.toInt().toString() else String.format(Locale.getDefault(), "%.2f", this).replace('.', ',')
    }

    fun salvarDespesasDaFeira(listaAtualizadaUi: List<DespesaFeiraUiItem>) {
        viewModelScope.launch {
            var operacaoBemSucedidaGeral = true // Renomeado para não conflitar
            Log.d("ViewModelDespFeira_Save", "Iniciando salvarDespesasDaFeira para Feira ID: $feiraId. Itens na UI: ${listaAtualizadaUi.size}")
            try {
                listaAtualizadaUi.forEach { despesaUi ->
                    val valoresConvertidos = mutableMapOf<String, Double>()
                    var temAlgumValorValidoOuZero = false // Para saber se o usuário interagiu com os campos

                    diasDaSemanaFeira.forEach { dia ->
                        val valorStr = despesaUi.valoresPorDiaInput[dia]
                        if (!valorStr.isNullOrBlank()) { // Se não for nulo nem branco
                            val valorDouble = valorStr.replace(',', '.').toDoubleOrNull()
                            if (valorDouble != null) { // Se for um número válido (incluindo 0.0)
                                valoresConvertidos[dia] = valorDouble
                                temAlgumValorValidoOuZero = true
                            } else {
                                Log.w("ViewModelDespFeira_Save", "Valor inválido para ${despesaUi.itemDespesa.nome}, dia $dia: '$valorStr'. Será ignorado.")
                                // Não adiciona ao mapa, efetivamente tratando como não preenchido
                            }
                        }
                        // Se for nulo ou branco, não é adicionado ao mapa, então o JSON não terá essa chave ou terá valor nulo se o map for serializado com nulos.
                        // kotlinx.serialization por padrão omite chaves com valores nulos (a menos que Json { encodeDefaults = true } e o tipo seja Double?)
                        // O importante é que o mapa 'valoresConvertidos' só terá entradas para dias com números válidos.
                    }

                    Log.d("ViewModelDespFeira_Save", "Item: ${despesaUi.itemDespesa.nome}, Valores Convertidos: $valoresConvertidos, Era Existente: ${despesaUi.isExistingEntry}, Tem Valor Válido: $temAlgumValorValidoOuZero")

                    if (temAlgumValorValidoOuZero) {
                        val jsonValoresPorDia = jsonFormat.encodeToString(serializer<Map<String, Double>>(), valoresConvertidos)
                        val despesaEntity = DespesaFeiraEntity(
                            feiraId = feiraId,
                            itemDespesaId = despesaUi.itemDespesa.id,
                            valoresPorDiaJson = jsonValoresPorDia,
                            observacao = despesaUi.observacaoInput.trim().ifEmpty { null }
                        )
                        despesaFeiraRepository.insertOrUpdateDespesa(despesaEntity)
                        Log.i("ViewModelDespFeira_Save", "Salvo/Atualizado DespesaFeiraEntity para item ${despesaUi.itemDespesa.nome}")
                    } else if (despesaUi.isExistingEntry && !temAlgumValorValidoOuZero) {
                        // Era existente, mas agora não tem nenhum valor válido (todos os campos foram apagados ou são inválidos)
                        despesaFeiraRepository.deleteDespesaByFeiraAndItem(feiraId, despesaUi.itemDespesa.id)
                        Log.i("ViewModelDespFeira_Save", "Deletado DespesaFeiraEntity para item ${despesaUi.itemDespesa.nome} (zerado/esvaziado)")
                    }
                    // Se não tem valor válido e não era existente, não faz nada.
                }
                _saveStatus.emit(true)
            } catch (e: Exception) {
                Log.e("ViewModelDespFeira_Save", "Erro EXCEPCIONAL ao salvar despesas da feira $feiraId: ${e.message}", e)
                _saveStatus.emit(false)
            }
        }
    }
}

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
        throw IllegalArgumentException("Unknown ViewModel class for LancamentoDespesasFeira")
    }
}