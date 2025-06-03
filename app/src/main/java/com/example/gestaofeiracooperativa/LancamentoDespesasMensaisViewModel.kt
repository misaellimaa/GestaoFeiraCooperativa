package com.example.gestaofeiracooperativa // Seu package

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar // Para obter ano/mês atuais como padrão
import java.util.Locale

// Importe seus modelos, DAOs e Repositórios
// import com.example.gestaofeiracooperativa.ItemDespesaEntity
// import com.example.gestaofeiracooperativa.LancamentoMensalDespesaEntity
// import com.example.gestaofeiracooperativa.ItemDespesaRepository
// import com.example.gestaofeiracooperativa.LancamentoDespesaRepository
// import com.example.gestaofeiracooperativa.DespesaMensalParaUi // Se criou em arquivo separado

class LancamentoDespesasMensaisViewModel(
    private val itemDespesaRepository: ItemDespesaRepository,
    private val lancamentoDespesaRepository: LancamentoDespesaRepository
) : ViewModel() {

    private val _anoSelecionado = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val anoSelecionado: StateFlow<Int> = _anoSelecionado.asStateFlow()

    private val _mesSelecionado = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1) // Calendar.MONTH é 0-indexed
    val mesSelecionado: StateFlow<Int> = _mesSelecionado.asStateFlow()

    // Este StateFlow combinará os itens de despesa com seus lançamentos para o mês/ano selecionado
    private val _listaDespesasParaUi = MutableStateFlow<List<DespesaMensalParaUi>>(emptyList())
    val listaDespesasParaUi: StateFlow<List<DespesaMensalParaUi>> = _listaDespesasParaUi.asStateFlow()

    init {
        // Combina os flows de ano, mês e todos os itens de despesa para carregar os dados
        viewModelScope.launch {
            combine(
                anoSelecionado,
                mesSelecionado,
                itemDespesaRepository.getAllItensDespesa() // Pega todos os tipos de despesa
            ) { ano, mes, todosOsItensDespesa ->
                // Para cada tipo de despesa, busca o lançamento mensal existente
                val lancamentosDoMes = lancamentoDespesaRepository.getLancamentosByAnoMes(ano, mes).firstOrNull() ?: emptyList()

                todosOsItensDespesa.map { item ->
                    val lancamentoExistente = lancamentosDoMes.find { it.itemDespesaId == item.id }
                    DespesaMensalParaUi(
                        itemDespesa = item,
                        valorSemana1 = lancamentoExistente?.valorSemana1?.formatParaUi() ?: "",
                        valorSemana2 = lancamentoExistente?.valorSemana2?.formatParaUi() ?: "",
                        valorSemana3 = lancamentoExistente?.valorSemana3?.formatParaUi() ?: "",
                        valorSemana4 = lancamentoExistente?.valorSemana4?.formatParaUi() ?: "",
                        valorSemana5 = lancamentoExistente?.valorSemana5?.formatParaUi() ?: "",
                        idLancamentoExistente = lancamentoExistente?.id
                    )
                }
            }.catch { e ->
                Log.e("ViewModelDespesas", "Erro ao carregar lista de despesas UI: ${e.message}", e)
                _listaDespesasParaUi.value = emptyList() // Em caso de erro, lista vazia
            }.collect { itensMapeados ->
                _listaDespesasParaUi.value = itensMapeados
            }
        }
    }

    // Função auxiliar para formatar Double? para String (usando vírgula)
    private fun Double?.formatParaUi(): String {
        return this?.let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format(Locale.getDefault(), "%.2f", it).replace('.', ',') } ?: ""
    }

    fun selecionarAno(ano: Int) {
        _anoSelecionado.value = ano
    }

    fun selecionarMes(mes: Int) {
        _mesSelecionado.value = mes
    }

    // Este método será chamado pela UI quando um valor de semana for alterado
    // A UI precisará manter uma cópia mutável da listaDespesasParaUi ou notificar o ViewModel para recriá-la.
    // Uma abordagem mais simples para a UI é ter TextFields que atualizam um estado local na tela
    // e, ao salvar, a tela envia todos os dados para o ViewModel.
    // Vamos simplificar: a tela vai gerenciar os inputs e chamar `salvarTodasAsDespesasDoMes`
    // passando a lista de `DespesaMensalParaUi` com os valores atualizados.

    suspend fun salvarTodasAsDespesasDoMes(despesasUiAtualizadas: List<DespesaMensalParaUi>): Boolean {
        var sucessoGeral = true
        viewModelScope.launch {
            despesasUiAtualizadas.forEach { despesaUi ->
                // Converte de String para Double?, tratando vírgula e string vazia
                val s1 = despesaUi.valorSemana1.replace(',', '.').toDoubleOrNull()
                val s2 = despesaUi.valorSemana2.replace(',', '.').toDoubleOrNull()
                val s3 = despesaUi.valorSemana3.replace(',', '.').toDoubleOrNull()
                val s4 = despesaUi.valorSemana4.replace(',', '.').toDoubleOrNull()
                val s5 = despesaUi.valorSemana5.replace(',', '.').toDoubleOrNull()

                // Só salva se houver algum valor preenchido ou se já existia um lançamento (para permitir zerar valores)
                if (listOfNotNull(s1,s2,s3,s4,s5).any { it != 0.0 } || s1!=null || s2!=null || s3!=null || s4!=null || s5!=null || despesaUi.idLancamentoExistente != null) {
                    val lancamento = LancamentoMensalDespesaEntity(
                        id = despesaUi.idLancamentoExistente ?: 0L, // Se 0L, é novo (autogenerate)
                        itemDespesaId = despesaUi.itemDespesa.id,
                        ano = _anoSelecionado.value,
                        mes = _mesSelecionado.value,
                        valorSemana1 = s1,
                        valorSemana2 = s2,
                        valorSemana3 = s3,
                        valorSemana4 = s4,
                        valorSemana5 = s5,
                        // observacoes = // Você pode adicionar um campo para observações na DespesaMensalParaUi se necessário
                    )
                    try {
                        lancamentoDespesaRepository.insertOrUpdateLancamento(lancamento)
                    } catch (e: Exception) {
                        Log.e("ViewModelDespesas", "Erro ao salvar lançamento para item ${despesaUi.itemDespesa.nome}: ${e.message}", e)
                        sucessoGeral = false
                        // TODO: Coletar erros para mostrar um resumo ao usuário
                    }
                } else if (despesaUi.idLancamentoExistente != null) {
                    // Se todos os campos ficaram vazios mas existia um lançamento, deleta o lançamento antigo
                    try {
                        lancamentoDespesaRepository.deleteLancamento(
                            LancamentoMensalDespesaEntity(id=despesaUi.idLancamentoExistente, itemDespesaId = despesaUi.itemDespesa.id, ano = _anoSelecionado.value, mes = _mesSelecionado.value)
                        )
                    } catch (e: Exception) {
                        Log.e("ViewModelDespesas", "Erro ao deletar lançamento zerado para item ${despesaUi.itemDespesa.nome}: ${e.message}", e)
                    }
                }
            }
        }.join() // Espera todas as coroutines de salvamento terminarem se fossem lançadas individualmente.
        // Aqui, como o forEach é sequencial dentro de um launch, join não é estritamente necessário
        // a menos que cada save fosse um novo launch.
        return sucessoGeral
    }
    suspend fun getTodosOsItensDespesaParaPdf(): List<ItemDespesaEntity> {
        return itemDespesaRepository.getAllItensDespesa().firstOrNull() ?: emptyList()
    }

    /**
     * Retorna a lista de LancamentoMensalDespesaEntity para o ano e mês atualmente selecionados.
     * Usado para obter os valores das despesas para o PDF.
     */
    suspend fun getLancamentosDoMesSelecionadoParaPdf(): List<LancamentoMensalDespesaEntity> {
        val ano = _anoSelecionado.value // Usa o ano selecionado no ViewModel
        val mes = _mesSelecionado.value // Usa o mês selecionado no ViewModel
        return lancamentoDespesaRepository.getLancamentosByAnoMes(ano, mes).firstOrNull() ?: emptyList()
    }
}



class LancamentoDespesasMensaisViewModelFactory(
    private val itemDespesaRepository: ItemDespesaRepository,
    private val lancamentoDespesaRepository: LancamentoDespesaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LancamentoDespesasMensaisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LancamentoDespesasMensaisViewModel(itemDespesaRepository, lancamentoDespesaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for LancamentoDespesasMensais")
    }
}