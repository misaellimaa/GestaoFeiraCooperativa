package com.example.gestaofeiracooperativa

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

// Importe seus componentes e modelos
import com.example.gestaofeiracooperativa.StandardTopAppBar
import com.example.gestaofeiracooperativa.LancamentoDespesasFeiraViewModel
import com.example.gestaofeiracooperativa.LancamentoDespesasFeiraViewModelFactory
import com.example.gestaofeiracooperativa.DespesaFeiraUiItem
import com.example.gestaofeiracooperativa.diasDaSemanaFeira
import com.example.gestaofeiracooperativa.MyApplication
import com.example.gestaofeiracooperativa.PdfGenerator
import com.example.gestaofeiracooperativa.FairDetails
import com.example.gestaofeiracooperativa.compartilharPdf // Assumindo que esta função está em um arquivo de utilidades

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LancamentoDespesasFeiraScreen(
    navController: NavHostController,
    feiraId: String,
    fairDetails: FairDetails,
    viewModel: LancamentoDespesasFeiraViewModel,
    onDespesasSalvas: (List<DespesaFeiraUiItem>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val listaDespesasUiOriginal by viewModel.listaDespesasUi.collectAsState()
    val listaEditavelDespesas = remember { mutableStateListOf<DespesaFeiraUiItem>() }

    // Sincroniza a lista local editável com a lista do ViewModel
    LaunchedEffect(listaDespesasUiOriginal) {
        listaEditavelDespesas.clear() // <<< AGORA FUNCIONA
        listaEditavelDespesas.addAll( // <<< AGORA FUNCIONA
            listaDespesasUiOriginal.map { it.copy() } // Adiciona cópias para edição segura
        )
        Log.d("LancamentoDespesas", "Lista Editável sincronizada. Tamanho: ${listaEditavelDespesas.size}")
    }

    LaunchedEffect(Unit) {
        viewModel.saveStatus.collectLatest { sucesso ->
            if (sucesso) {
                Toast.makeText(context, "Despesas salvas!", Toast.LENGTH_SHORT).show()
                // Chama o callback com o estado atual da lista editável
                onDespesasSalvas(listaEditavelDespesas.toList())
                navController.popBackStack()
            } else {
                Toast.makeText(context, "Erro ao salvar despesas.", Toast.LENGTH_LONG).show()
            }
        }
    }


    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = "Despesas da Feira Nº $feiraId",
                canNavigateBack = true,
                onNavigateBack = { navController.popBackStack() },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            Log.d("LancamentoDespesas", "Botão PDF (Despesas da Feira) clicado para Feira ${fairDetails.feiraId}.")
                            // A lógica para gerar o PDF desta feira específica
                            if (listaEditavelDespesas.isNotEmpty()) {
                                try {
                                    val pdfFile = PdfGenerator.generateDespesasFeiraPdf(
                                        context = context,
                                        fairDetails = fairDetails,
                                        despesasDaFeira = listaEditavelDespesas.toList() // Passa os dados atuais da UI
                                    )

                                    val tituloChooser = "Despesas da Feira ${fairDetails.feiraId}"
                                    val authorityString = context.applicationContext.packageName + ".provider"
                                    compartilharPdf(context, pdfFile, authorityString, tituloChooser)

                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erro ao gerar PDF: ${e.message}", Toast.LENGTH_LONG).show()
                                    Log.e("LancamentoDespesas", "Erro ao gerar PDF de despesas da feira", e)
                                }
                            } else {
                                Toast.makeText(context, "Não há despesas lançadas para gerar o PDF.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "Gerar PDF de Despesas da Feira")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Text(
                "Período da Feira: ${fairDetails.startDate} a ${fairDetails.endDate}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp).align(Alignment.CenterHorizontally)
            )
            Divider(modifier = Modifier.padding(bottom = 12.dp))

            if (listaEditavelDespesas.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nenhum item de despesa cadastrado para lançamento.")
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { navController.navigate(AppRoutes.CADASTRO_ITENS_DESPESA) }) {
                            Text("Cadastrar Itens de Despesa")
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(listaEditavelDespesas, key = { _, item -> item.itemDespesa.id }) { index, despesaItemUi ->
                        // Card para cada item de despesa
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ){
                                    Text(
                                        despesaItemUi.itemDespesa.nome,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (!despesaItemUi.itemDespesa.descricao.isNullOrBlank()) {
                                    Text(
                                        despesaItemUi.itemDespesa.descricao!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))

                                // Inputs para as semanas
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    maxItemsInEachRow = 3
                                ) {
                                    diasDaSemanaFeira.forEach { dia ->
                                        OutlinedTextField(
                                            value = despesaItemUi.valoresPorDiaInput[dia] ?: "",
                                            onValueChange = { novoValor ->
                                                val valorLimpo = novoValor.filter { char -> char.isDigit() || char == '.' || char == ',' }
                                                if (valorLimpo.count { it == '.' || it == ',' } <= 1) {
                                                    // <<< CORREÇÃO AQUI: Substitui o item inteiro na lista >>>
                                                    val itemAntigo = listaEditavelDespesas[index]
                                                    val novoMapa = itemAntigo.valoresPorDiaInput.toMutableMap()
                                                    novoMapa[dia] = valorLimpo
                                                    // Cria uma nova cópia do item com o mapa atualizado
                                                    val itemAtualizado = itemAntigo.copy(valoresPorDiaInput = novoMapa)
                                                    // Substitui o item na lista. Esta operação dispara a recomposição.
                                                    listaEditavelDespesas[index] = itemAtualizado
                                                }
                                            },
                                            label = { Text(dia, fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier
                                                .defaultMinSize(minWidth = 80.dp)
                                                .padding(bottom = 6.dp, end = 4.dp),
                                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = despesaItemUi.observacaoInput,
                                    onValueChange = { novoValor ->
                                        val itemAntigo = listaEditavelDespesas[index]
                                        val itemAtualizado = itemAntigo.copy(observacaoInput = novoValor)
                                        listaEditavelDespesas[index] = itemAtualizado
                                    },
                                    label = { Text("Observação (Opcional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    viewModel.salvarDespesasDaFeira(listaEditavelDespesas.toList())
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = listaEditavelDespesas.isNotEmpty()
            ) {
                Text("Salvar e Voltar", fontSize = 16.sp)
            }
        }
    }
}