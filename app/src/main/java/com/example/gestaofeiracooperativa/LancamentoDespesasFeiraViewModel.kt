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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Importe seus modelos e Repositórios.
// Certifique-se que os caminhos estão corretos.
import com.example.gestaofeiracooperativa.ItemDespesaEntity
import com.example.gestaofeiracooperativa.DespesaFeiraEntity
import com.example.gestaofeiracooperativa.ItemDespesaRepository
import com.example.gestaofeiracooperativa.DespesaFeiraRepository
import com.example.gestaofeiracooperativa.FeiraRepository // Para buscar FairDetails e feiras do mês
import com.example.gestaofeiracooperativa.FairDetails // Para o tipo de retorno de getFairDetailsFromFeiraId
import com.example.gestaofeiracooperativa.DespesaFeiraUiItem
import com.example.gestaofeiracooperativa.diasDaSemanaFeira

class LancamentoDespesasFeiraViewModel(
    private val itemDespesaRepository: ItemDespesaRepository,
    private val despesaFeiraRepository: DespesaFeiraRepository,
    private val feiraRepository: FeiraRepository, // Adicionado para buscar detalhes da feira e feiras do mês
    private val feiraId: String // ID da feira atual que esta tela está gerenciando
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
        carregarDespesasDaFeiraAtual()
    }

    // Carrega as despesas para o feiraId específico deste ViewModel
    private fun carregarDespesasDaFeiraAtual() {
        viewModelScope.launch {
            Log.d("ViewModelDespFeira_Load", "Iniciando carregarDespesasDaFeiraAtual para Feira ID: $feiraId")
            try {
                itemDespesaRepository.getAllItensDespesa()
                    .combine(despesaFeiraRepository.getDespesasByFeiraId(feiraId)) { todosItensCadastrados, despesasLancadasParaEstaFeira ->
                        Log.d("ViewModelDespFeira_Load", "Feira ID $feiraId - Coletado ${todosItensCadastrados.size} itens de despesa cadastrados.")
                        Log.d("ViewModelDespFeira_Load", "Feira ID $feiraId - Coletado ${despesasLancadasParaEstaFeira.size} despesas lançadas no banco para esta feira.")
                        despesasLancadasParaEstaFeira.forEach { lanc ->
                            Log.d("ViewModelDespFeira_Load", "  -> Lançamento existente no DB: itemDespesaId=${lanc.itemDespesaId}, jsonValores='${lanc.valoresPorDiaJson}', obs='${lanc.observacao}'")
                        }

                        if (todosItensCadastrados.isEmpty()) {
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
                            val mapaDeValoresNumericos = lancamentoExistente?.valoresPorDiaJson?.let { jsonString ->
                                if (jsonString.isNotBlank()) {
                                    try {
                                        jsonFormat.decodeFromString<Map<String, Double>>(jsonString)
                                    } catch (e: Exception) {
                                        Log.e("ViewModelDespFeira_Load", "ERRO DESSERIALIZANDO para ${itemDeDespesa.nome}: '$jsonString'", e)
                                        emptyMap()
                                    }
                                } else {
                                    Log.w("ViewModelDespFeira_Load", "  JSON de valoresPorDia está em branco para ${itemDeDespesa.nome}.")
                                    emptyMap()
                                }
                            } ?: emptyMap<String, Double>()

                            if (lancamentoExistente != null && mapaDeValoresNumericos.isEmpty() && !lancamentoExistente.valoresPorDiaJson.isNullOrBlank()) {
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
                    .catch { e ->
                        Log.e("ViewModelDespFeira", "EXCEÇÃO no Flow de carregarDespesas: ${e.message}", e)
                        _listaDespesasUi.value = emptyList()
                    }
                    .collect { itensMapeados ->
                        Log.i("ViewModelDespFeira_Load", "COLETOU e atualizou _listaDespesasUi com ${itensMapeados.size} itens.")
                        _listaDespesasUi.value = itensMapeados
                    }
            } catch (e: Exception) {
                Log.e("ViewModelDespFeira", "EXCEÇÃO GERAL em carregarDespesas: ${e.message}", e)
                _listaDespesasUi.value = emptyList()
            }
        }
    }

    private fun Double?.formatParaUi(): String {
        if (this == null) return ""
        return if (this % 1.0 == 0.0) this.toInt().toString() else String.format(Locale.getDefault(), "%.2f", this).replace('.', ',')
    }

    fun salvarDespesasDaFeira(listaAtualizadaUi: List<DespesaFeiraUiItem>) {
        viewModelScope.launch {
            Log.d("ViewModelDespFeira_Save", "Iniciando salvarDespesasDaFeira para Feira ID: $feiraId. Itens na UI: ${listaAtualizadaUi.size}")
            try {
                listaAtualizadaUi.forEach { despesaUi ->
                    val valoresConvertidos = mutableMapOf<String, Double>()
                    var temAlgumValorValidoOuZero = false

                    diasDaSemanaFeira.forEach { dia ->
                        val valorStr = despesaUi.valoresPorDiaInput[dia]
                        if (!valorStr.isNullOrBlank()) {
                            val valorDouble = valorStr.replace(',', '.').toDoubleOrNull()
                            if (valorDouble != null) {
                                valoresConvertidos[dia] = valorDouble
                                temAlgumValorValidoOuZero = true
                            } else {
                                Log.w("ViewModelDespFeira_Save", "Valor inválido para ${despesaUi.itemDespesa.nome}, dia $dia: '$valorStr'. Será ignorado.")
                            }
                        }
                    }

                    Log.d("ViewModelDespFeira_Save", "Item: ${despesaUi.itemDespesa.nome}, Valores Convertidos: $valoresConvertidos, Era Existente: ${despesaUi.isExistingEntry}, Tem Valor Válido/Zero: $temAlgumValorValidoOuZero")

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
                        despesaFeiraRepository.deleteDespesaByFeiraAndItem(feiraId, despesaUi.itemDespesa.id)
                        Log.i("ViewModelDespFeira_Save", "Deletado DespesaFeiraEntity para item ${despesaUi.itemDespesa.nome} (zerado/esvaziado)")
                    }
                }
                _saveStatus.emit(true)
            } catch (e: Exception) {
                Log.e("ViewModelDespFeira_Save", "Erro EXCEPCIONAL ao salvar despesas da feira $feiraId: ${e.message}", e)
                _saveStatus.emit(false)
            }
        }
    }

    // <<< NOVA FUNÇÃO PARA PREPARAR DADOS DO PDF MENSAL >>>
    suspend fun prepararDadosParaPdfMensal(): DadosPdfDespesasMensais? {
        Log.d("ViewModelDespFeira_Pdf", "Preparando dados para PDF Mensal da feira: $feiraId")
        // 1. Obter FairDetails da feira atual para saber o mês/ano
        val fairDetailsAtual = feiraRepository.getFairDetailsFromFeiraId(feiraId)
        if (fairDetailsAtual == null) {
            Log.e("ViewModelDespFeira_Pdf", "Não foi possível obter FairDetails para a feira atual $feiraId")
            return null
        }

        val calendario = Calendar.getInstance()
        try {
            // Assumindo que fairDetailsAtual.startDate está no formato "dd/MM/yyyy"
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dataInicioDate = sdf.parse(fairDetailsAtual.startDate)
            if (dataInicioDate != null) {
                calendario.time = dataInicioDate
            } else {
                Log.e("ViewModelDespFeira_Pdf", "Data de início da feira atual é inválida ou nula: ${fairDetailsAtual.startDate}")
                return null
            }
        } catch (e: java.text.ParseException) {
            Log.e("ViewModelDespFeira_Pdf", "Erro ao parsear data de início da feira atual: ${fairDetailsAtual.startDate}", e)
            return null
        }

        val anoParaPdf = calendario.get(Calendar.YEAR)
        val mesParaPdf = calendario.get(Calendar.MONTH) + 1 // Calendar.MONTH é 0-indexed

        Log.d("ViewModelDespFeira_Pdf", "PDF para Ano: $anoParaPdf, Mês: $mesParaPdf")

        // 2. Buscar todas as FairDetails daquele mês/ano
        //    Esta função precisa existir no FeiraRepository e FeiraDao.
        //    Exemplo de query no FeiraDao:
        //    @Query("SELECT * FROM feiras WHERE SUBSTR(startDate, 7, 4) = :anoStr AND SUBSTR(startDate, 4, 2) = :mesStr ORDER BY startDate ASC")
        //    fun getFeirasByMesAno(anoStr: String, mesStr: String): Flow<List<FeiraEntity>> (e depois mapear para FairDetails)
        //    Por enquanto, vamos simular que o FeiraRepository tem um método que faz isso.
        val todasAsFeirasDoMesEntities = feiraRepository.getFeirasByMesAno(anoParaPdf, mesParaPdf).firstOrNull() ?: emptyList()
        val feirasDasSemanasDoMesNoPdf = todasAsFeirasDoMesEntities.take(5) // Pega as 5 primeiras do mês para as colunas do PDF

        Log.d("ViewModelDespFeira_Pdf", "Encontradas ${feirasDasSemanasDoMesNoPdf.size} feiras para as colunas do PDF no mês $mesParaPdf/$anoParaPdf.")


        // 3. Para cada feira do mês, buscar suas despesas
        val despesasDeCadaFeiraDoMes = mutableMapOf<String, List<DespesaFeiraEntity>>()
        feirasDasSemanasDoMesNoPdf.forEach { feira ->
            val despesasDaFeira = despesaFeiraRepository.getDespesasByFeiraId(feira.feiraId).firstOrNull() ?: emptyList()
            despesasDeCadaFeiraDoMes[feira.feiraId] = despesasDaFeira
            Log.d("ViewModelDespFeira_Pdf", "Para Feira ${feira.feiraId}, encontradas ${despesasDaFeira.size} despesas.")
        }

        // 4. Buscar todos os itens de despesa cadastrados
        val todosItens = itemDespesaRepository.getAllItensDespesa().firstOrNull() ?: emptyList()
        Log.d("ViewModelDespFeira_Pdf", "Total de ${todosItens.size} itens de despesa cadastrados.")


        if (feirasDasSemanasDoMesNoPdf.isEmpty() && todosItens.isEmpty()){
            Log.w("ViewModelDespFeira_Pdf", "Nenhuma feira ou item de despesa para gerar o PDF.")
            // Ainda pode gerar um PDF vazio se desejar, ou retornar null para a UI tratar.
            // Por ora, permite gerar com itens mas sem feiras, ou com feiras mas sem itens (mostrará zerado).
        }

        return DadosPdfDespesasMensais(
            ano = anoParaPdf,
            mes = mesParaPdf,
            feirasDasSemanasDoMes = feirasDasSemanasDoMesNoPdf,
            despesasDeCadaFeiraDoMes = despesasDeCadaFeiraDoMes,
            todosOsItensDeDespesa = todosItens
        )
    }
} // Fim da classe LancamentoDespesasFeiraViewModel


class LancamentoDespesasFeiraViewModelFactory(
    private val itemDespesaRepository: ItemDespesaRepository,
    private val despesaFeiraRepository: DespesaFeiraRepository,
    private val feiraRepository: FeiraRepository, // <<< ADICIONADO
    private val feiraId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LancamentoDespesasFeiraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LancamentoDespesasFeiraViewModel(
                itemDespesaRepository,
                despesaFeiraRepository,
                feiraRepository, // <<< PASSADO PARA O ViewModel
                feiraId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for LancamentoDespesasFeira")
    }
}