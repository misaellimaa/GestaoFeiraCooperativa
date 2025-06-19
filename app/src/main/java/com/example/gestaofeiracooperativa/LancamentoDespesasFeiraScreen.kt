package com.example.gestaofeiracooperativa

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.navigation.NavHostController

// Seus imports...
import com.example.gestaofeiracooperativa.StandardTopAppBar
import com.example.gestaofeiracooperativa.LancamentoDespesasFeiraViewModel
import com.example.gestaofeiracooperativa.DespesaFeiraUiItem
import com.example.gestaofeiracooperativa.diasDaSemanaFeira
import com.example.gestaofeiracooperativa.FairDetails
import com.example.gestaofeiracooperativa.PdfGenerator
import com.example.gestaofeiracooperativa.compartilharPdf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LancamentoDespesasFeiraScreen(
    navController: NavHostController,
    fairDetails: FairDetails,
    viewModel: LancamentoDespesasFeiraViewModel,
    onDespesasSalvas: (List<DespesaFeiraUiItem>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val listaDespesasUiOriginal by viewModel.listaDespesasUi.collectAsState()

    // <<< Usando mutableStateListOf para garantir que a UI reaja a mudanças nos itens >>>
    val listaEditavelDespesas = remember { mutableStateListOf<DespesaFeiraUiItem>() }

    // Sincroniza a lista local com a do ViewModel quando ela é carregada
    LaunchedEffect(listaDespesasUiOriginal) {
        if (listaEditavelDespesas.toList() != listaDespesasUiOriginal) {
            listaEditavelDespesas.clear()
            listaEditavelDespesas.addAll(listaDespesasUiOriginal.map { it.copy() })
        }
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = "Despesas - Feira ${fairDetails.feiraId}",
                canNavigateBack = true,
                onNavigateBack = { navController.popBackStack() },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            try {
                                val pdfFile = PdfGenerator.generateDespesasFeiraPdf(
                                    context = context,
                                    fairDetails = fairDetails,
                                    despesasDaFeira = listaEditavelDespesas.toList()
                                )
                                val authorityString = context.applicationContext.packageName + ".provider"
                                compartilharPdf(context, pdfFile, authorityString, "Despesas da Feira ${fairDetails.feiraId}")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro ao gerar PDF: ${e.message}", Toast.LENGTH_LONG).show()
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
                .padding(horizontal = 16.dp)
        ) {
            // Cabeçalho com o período da feira
            Text(
                "Período da Feira: ${fairDetails.startDate} a ${fairDetails.endDate}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 12.dp).align(Alignment.CenterHorizontally)
            )
            Divider()

            // Lista de despesas rolável que ocupa o espaço disponível
            LazyColumn(modifier = Modifier.weight(1f)) {
                if (listaEditavelDespesas.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Nenhum item de despesa cadastrado.")
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { navController.navigate(AppRoutes.CADASTRO_ITENS_DESPESA) }) {
                                    Text("Cadastrar Itens de Despesa")
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(listaEditavelDespesas, key = { _, item -> item.itemDespesa.id }) { index, despesaItemUi ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(despesaItemUi.itemDespesa.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (!despesaItemUi.itemDespesa.descricao.isNullOrBlank()) {
                                    Text(
                                        despesaItemUi.itemDespesa.descricao!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    maxItemsInEachRow = 3
                                ) {
                                    diasDaSemanaFeira.forEach { dia ->
                                        OutlinedTextField(
                                            value = despesaItemUi.valoresPorDiaInput[dia] ?: "",
                                            onValueChange = { novoValor ->
                                                val valorLimpo = novoValor.filter { it.isDigit() || it == '.' || it == ',' }
                                                if (valorLimpo.count { it == '.' || it == ',' } <= 1) {
                                                    // <<< CORREÇÃO: Atualiza o item na lista para forçar a recomposição >>>
                                                    val itemAntigo = listaEditavelDespesas[index]
                                                    val novoMapa = itemAntigo.valoresPorDiaInput.toMutableMap().apply { this[dia] = valorLimpo }
                                                    listaEditavelDespesas[index] = itemAntigo.copy(valoresPorDiaInput = novoMapa)
                                                }
                                            },
                                            label = { Text(dia, fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.defaultMinSize(minWidth = 80.dp).padding(bottom = 6.dp),
                                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = despesaItemUi.observacaoInput,
                                    onValueChange = { novoValor ->
                                        // <<< CORREÇÃO: Atualiza o item na lista para forçar a recomposição >>>
                                        listaEditavelDespesas[index] = despesaItemUi.copy(observacaoInput = novoValor)
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

            // Rodapé com o botão de salvar, que agora fica sempre visível
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    onDespesasSalvas(listaEditavelDespesas.toList())
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).padding(bottom = 8.dp),
                enabled = true // Botão sempre habilitado para salvar o estado atual
            ) {
                Text("Concluir e Salvar Alterações", fontSize = 16.sp)
            }
        }
    }
}