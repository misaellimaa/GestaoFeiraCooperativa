package com.example.gestaofeiracooperativa // Seu package

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

// Importe seus modelos, ViewModel, Factory, etc.
// import com.example.gestaofeiracooperativa.StandardTopAppBar
// import com.example.gestaofeiracooperativa.LancamentoDespesasMensaisViewModel
// import com.example.gestaofeiracooperativa.CadastroItensDespesaViewModelFactory // ERRO: Nome da Factory deve ser Lancamento...
// import com.example.gestaofeiracooperativa.LancamentoDespesasMensaisViewModelFactory // <<< CORRETO
// import com.example.gestaofeiracooperativa.DespesaMensalParaUi
// import com.example.gestaofeiracooperativa.MyApplication

val semanasDoMes = listOf("Sem 1", "Sem 2", "Sem 3", "Sem 4", "Sem 5") // Para os labels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LancamentoDespesasMensaisScreen(
    navController: NavHostController,
    // Passar ano/mês iniciais se vier de GerenciarFeiraScreen, ou usar padrão
    initialAno: Int = Calendar.getInstance().get(Calendar.YEAR),
    initialMes: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    viewModel: LancamentoDespesasMensaisViewModel = viewModel(
        factory = LancamentoDespesasMensaisViewModelFactory(
            (LocalContext.current.applicationContext as MyApplication).itemDespesaRepository,
            (LocalContext.current.applicationContext as MyApplication).lancamentoDespesaRepository
            // Presume que MyApplication expõe os repositórios
        )
    )
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Coleta estados do ViewModel
    val anoSelecionado by viewModel.anoSelecionado.collectAsState()
    val mesSelecionado by viewModel.mesSelecionado.collectAsState()
    val listaUiStateOriginal by viewModel.listaDespesasParaUi.collectAsState()

    // Estado local editável para os inputs, sincronizado com o ViewModel
    val listaEditavelDespesas = remember { mutableStateListOf<DespesaMensalParaUi>() }

    LaunchedEffect(listaUiStateOriginal) {
        listaEditavelDespesas.clear()
        listaEditavelDespesas.addAll(listaUiStateOriginal.map { it.copy() }) // Cria cópias para edição
    }

    // Forçar o ViewModel a carregar para o ano/mês inicial se eles mudarem
    LaunchedEffect(initialAno, initialMes) {
        viewModel.selecionarAno(initialAno)
        viewModel.selecionarMes(initialMes)
    }


    // --- UI para Seleção de Ano e Mês (Simplificado - pode ser melhorado com Dropdowns) ---
    var inputAno by remember(anoSelecionado) { mutableStateOf(anoSelecionado.toString()) }
    var inputMes by remember(mesSelecionado) { mutableStateOf(mesSelecionado.toString()) }

    fun atualizarPeriodoNoViewModel() {
        val ano = inputAno.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        val mes = inputMes.toIntOrNull()?.coerceIn(1, 12) ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)
        viewModel.selecionarAno(ano)
        viewModel.selecionarMes(mes)
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = "Lançar Despesas Mensais",
                canNavigateBack = true,
                onNavigateBack = { navController.popBackStack() },
                actions = { // <<< NOVO PARÂMETRO 'actions' ADICIONADO AQUI
                    IconButton(onClick = {
                        // A mesma lógica que estava no botão "Gerar PDF Mensal de Despesas"
                        coroutineScope.launch {
                            val todosItensDespesaFromVm = viewModel.getTodosOsItensDespesaParaPdf()
                            val lancamentosDoMesFromVm = viewModel.getLancamentosDoMesSelecionadoParaPdf()

                            // anoSelecionado e mesSelecionado já são states coletados do ViewModel

                            if (todosItensDespesaFromVm.isNotEmpty() || lancamentosDoMesFromVm.isNotEmpty()) {
                                try {
                                    val pdfFile = PdfGenerator.generateDespesasMensaisPdf(
                                        context,
                                        anoSelecionado,
                                        mesSelecionado,
                                        lancamentosDoMesFromVm,
                                        todosItensDespesaFromVm
                                    )

                                    val tituloChooser = "Compartilhar Desp. Mensais ${String.format("%02d", mesSelecionado)}/$anoSelecionado"
                                    val authorityString = context.applicationContext.packageName + ".provider"

                                    // Chame sua função compartilharPdf aqui
                                    compartilharPdf( // Certifique-se que esta função está acessível
                                        context = context,
                                        pdfFile = pdfFile,
                                        authority = authorityString,
                                        chooserTitle = tituloChooser
                                    )

                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erro ao gerar ou compartilhar PDF Mensal: ${e.message}", Toast.LENGTH_LONG).show()
                                    Log.e("LancamentoDespesas", "Erro ao gerar/compartilhar PDF Mensal", e)
                                }
                            } else {
                                Toast.makeText(context, "Não há dados de despesa para gerar o PDF para ${String.format("%02d", mesSelecionado)}/$anoSelecionado.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = "Gerar PDF Mensal de Despesas"
                        )
                    }
                    // Se você quiser adicionar um botão para o PDF Semanal aqui também,
                    // poderíamos discutir como o usuário selecionaria a semana.
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Seção de Seleção de Período
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputMes,
                    onValueChange = { if (it.length <= 2) inputMes = it.filter { c -> c.isDigit() } },
                    label = { Text("Mês (1-12)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = inputAno,
                    onValueChange = { if (it.length <= 4) inputAno = it.filter { c -> c.isDigit() } },
                    label = { Text("Ano (AAAA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { atualizarPeriodoNoViewModel() }, modifier = Modifier.weight(1f).padding(top=8.dp)) {
                    Text("Buscar")
                }
            }
            Text("Período Selecionado: ${String.format("%02d", mesSelecionado)}/$anoSelecionado", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
            Divider()

            // Lista de Itens de Despesa para Lançamento
            if (listaEditavelDespesas.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Nenhum item de despesa cadastrado ou período sem dados.")
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) { // weight(1f) para ocupar espaço restante
                    itemsIndexed(listaEditavelDespesas, key = { _, item -> item.itemDespesa.id }) { index, despesaUiItem ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(despesaUiItem.itemDespesa.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (!despesaUiItem.itemDespesa.descricao.isNullOrBlank()){
                                    Text(despesaUiItem.itemDespesa.descricao!!, style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // Inputs para as semanas
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val weeklyValues = listOf(
                                        despesaUiItem.valorSemana1 to { newVal: String -> listaEditavelDespesas[index] = despesaUiItem.copy(valorSemana1 = newVal) },
                                        despesaUiItem.valorSemana2 to { newVal: String -> listaEditavelDespesas[index] = despesaUiItem.copy(valorSemana2 = newVal) },
                                        despesaUiItem.valorSemana3 to { newVal: String -> listaEditavelDespesas[index] = despesaUiItem.copy(valorSemana3 = newVal) },
                                        despesaUiItem.valorSemana4 to { newVal: String -> listaEditavelDespesas[index] = despesaUiItem.copy(valorSemana4 = newVal) },
                                        despesaUiItem.valorSemana5 to { newVal: String -> listaEditavelDespesas[index] = despesaUiItem.copy(valorSemana5 = newVal) }
                                    )

                                    semanasDoMes.forEachIndexed { weekIndex, semanaLabel ->
                                        OutlinedTextField(
                                            value = weeklyValues[weekIndex].first,
                                            onValueChange = { newValue ->
                                                val filtered = newValue.filter { it.isDigit() || it == ',' || it == '.' }
                                                if (filtered.count { it == ',' || it == '.' } <= 1) {
                                                    weeklyValues[weekIndex].second(filtered)
                                                }
                                            },
                                            label = { Text(semanaLabel, fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        val sucesso = viewModel.salvarTodasAsDespesasDoMes(listaEditavelDespesas.toList())
                        if (sucesso) {
                            Toast.makeText(context, "Despesas de $mesSelecionado/$anoSelecionado salvas!", Toast.LENGTH_SHORT).show()
                            // Opcional: Forçar recarregamento ou confiar no Flow para atualizar listaDespesasParaUi
                            // atualizarPeriodoNoViewModel() // Para recarregar e ver os valores persistidos
                        } else {
                            Toast.makeText(context, "Erro ao salvar algumas despesas. Verifique os logs.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = listaEditavelDespesas.isNotEmpty()
            ) {
                Text("Salvar Despesas do Mês", fontSize = 16.sp)
            }
        }
    }
}