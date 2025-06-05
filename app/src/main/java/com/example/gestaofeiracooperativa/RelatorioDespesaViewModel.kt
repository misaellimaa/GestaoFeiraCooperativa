package com.example.gestaofeiracooperativa

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Importe seus repositórios e entidades
import com.example.gestaofeiracooperativa.FeiraRepository
import com.example.gestaofeiracooperativa.DespesaFeiraRepository
import com.example.gestaofeiracooperativa.ItemDespesaRepository
import com.example.gestaofeiracooperativa.FairDetails
import com.example.gestaofeiracooperativa.DespesaFeiraEntity
import com.example.gestaofeiracooperativa.ItemDespesaEntity

class RelatorioDespesaViewModel(
    private val feiraRepository: FeiraRepository,
    private val despesaFeiraRepository: DespesaFeiraRepository,
    private val itemDespesaRepository: ItemDespesaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<File>>(UiState.Idle)
    val uiState: StateFlow<UiState<File>> = _uiState.asStateFlow()

    /**
     * Função principal que a UI chamará para iniciar a geração do PDF.
     */
    fun gerarRelatorioMensal(context: Context, ano: Int, mes: Int) {
        // Evita múltiplos cliques se já estiver gerando
        if (_uiState.value is UiState.Loading) return

        viewModelScope.launch {
            _uiState.value = UiState.Loading // Informa a UI que o processo começou
            try {
                // 1. Prepara os dados
                val dadosParaPdf = prepararDadosParaPdfMensal(ano, mes)

                if (dadosParaPdf == null || dadosParaPdf.feirasDasSemanasDoMes.isEmpty()) {
                    Log.w("RelatorioDespesaVM", "Não há feiras para gerar o relatório do mês $mes/$ano.")
                    _uiState.value = UiState.Error("Não há feiras registradas para o período selecionado.")
                    return@launch
                }

                // 2. Chama o PdfGenerator
                val pdfFile = PdfGenerator.generateDespesasMensaisPdf(
                    context = context,
                    ano = dadosParaPdf.ano,
                    mes = dadosParaPdf.mes,
                    feirasDasSemanasDoMes = dadosParaPdf.feirasDasSemanasDoMes,
                    despesasDeCadaFeiraDoMes = dadosParaPdf.despesasDeCadaFeiraDoMes,
                    todosOsItensDeDespesa = dadosParaPdf.todosOsItensDeDespesa
                )

                // 3. Emite o sucesso com o arquivo gerado
                _uiState.value = UiState.Success(pdfFile)

            } catch (e: Exception) {
                Log.e("RelatorioDespesaVM", "Falha ao gerar PDF de despesas mensais.", e)
                _uiState.value = UiState.Error("Falha ao gerar PDF: ${e.message}")
            }
        }
    }

    /**
     * Reseta o estado da UI para Idle, para que o usuário possa gerar um novo relatório.
     */
    fun resetUiState() {
        _uiState.value = UiState.Idle
    }

    /**
     * Função privada que busca e agrega todos os dados necessários.
     */
    private suspend fun prepararDadosParaPdfMensal(ano: Int, mes: Int): DadosPdfDespesasMensais? {
        // Busca todas as feiras do mês/ano.
        // Lembre-se que você precisa ter a função 'getFeirasByMesAno' no seu FeiraRepository e FeiraDao
        val todasAsFeirasDoMes = feiraRepository.getFeirasByMesAno(ano, mes).firstOrNull() ?: emptyList()
        if (todasAsFeirasDoMes.isEmpty()) {
            return null // Não há feiras, não há relatório
        }

        // Pega as 5 primeiras feiras do mês para as colunas do PDF.
        val feirasDasSemanasDoMes = todasAsFeirasDoMes.take(5)

        // Para cada uma dessas feiras, busca seus lançamentos de despesa.
        val despesasDeCadaFeiraDoMes = mutableMapOf<String, List<DespesaFeiraEntity>>()
        feirasDasSemanasDoMes.forEach { feira ->
            val despesasDaFeira = despesaFeiraRepository.getDespesasByFeiraId(feira.feiraId).firstOrNull() ?: emptyList()
            despesasDeCadaFeiraDoMes[feira.feiraId] = despesasDaFeira
        }

        // Busca todos os tipos de despesa cadastrados.
        val todosItens = itemDespesaRepository.getAllItensDespesa().firstOrNull() ?: emptyList()

        return DadosPdfDespesasMensais(
            ano = ano,
            mes = mes,
            feirasDasSemanasDoMes = feirasDasSemanasDoMes,
            despesasDeCadaFeiraDoMes = despesasDeCadaFeiraDoMes,
            todosOsItensDeDespesa = todosItens
        )
    }
}


class RelatorioDespesaViewModelFactory(
    private val feiraRepository: FeiraRepository,
    private val despesaFeiraRepository: DespesaFeiraRepository,
    private val itemDespesaRepository: ItemDespesaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RelatorioDespesaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RelatorioDespesaViewModel(feiraRepository, despesaFeiraRepository, itemDespesaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for RelatorioDespesa")
    }
}