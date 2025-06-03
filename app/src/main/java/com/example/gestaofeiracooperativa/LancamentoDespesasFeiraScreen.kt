package com.example.gestaofeiracooperativa // Seu package

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import java.util.Locale

// Importe seus modelos, ViewModel, Factory, StandardTopAppBar etc.
// import com.example.gestaofeiracooperativa.StandardTopAppBar
// import com.example.gestaofeiracooperativa.LancamentoDespesasFeiraViewModel
// import com.example.gestaofeiracooperativa.LancamentoDespesasFeiraViewModelFactory
// import com.example.gestaofeiracooperativa.DespesaFeiraUiItem
// import com.example.gestaofeiracooperativa.diasDaSemanaFeira // Sua lista global de dias
// import com.example.gestaofeiracooperativa.MyApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LancamentoDespesasFeiraScreen(
    navController: NavHostController,
    feiraId: String,
    viewModel: LancamentoDespesasFeiraViewModel,
    onDespesasSalvas: (List<DespesaFeiraUiItem>) -> Unit, // <<< ADICIONE ESTE NOVO PARÂMETRO
    // O onVoltar da TopAppBar é feito com navController.popBackStack(), então não precisa de um onVoltar aqui
    // a menos que você tenha outra lógica específica de "voltar" além do botão da TopAppBar.
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() // Se precisar para alguma ação direta na UI
    val listaDespesasUiOriginal by viewModel.listaDespesasUi.collectAsState()
    var listaEditavelDespesas by remember { mutableStateOf<List<DespesaFeiraUiItem>>(emptyList()) }

    LaunchedEffect(listaDespesasUiOriginal) {
        listaEditavelDespesas = listaDespesasUiOriginal.map { it.copy(valoresPorDiaInput = it.valoresPorDiaInput.toMutableMap()) }
    }

    LaunchedEffect(Unit) {
        viewModel.saveStatus.collectLatest { sucesso ->
            if (sucesso) {
                Toast.makeText(context, "Despesas da Feira Nº $feiraId salvas com sucesso!", Toast.LENGTH_SHORT).show()
                onDespesasSalvas(listaEditavelDespesas.toList()) // <<< CHAME O CALLBACK AQUI
                navController.popBackStack()
            } else {
                Toast.makeText(context, "Erro ao salvar despesas da Feira Nº $feiraId. Tente novamente.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = "Lançar Despesas (Feira Nº $feiraId)",
                canNavigateBack = true,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp) // Padding geral
        ) {
            if (listaEditavelDespesas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Carregando itens de despesa...")
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { /* TODO: Navegar para CadastroItensDespesaScreen */ }) {
                            Text("Cadastrar Itens de Despesa")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f), // Para ocupar o espaço disponível
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(listaEditavelDespesas, key = { _, item -> item.itemDespesa.id }) { index, despesaItemUi ->
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    despesaItemUi.itemDespesa.nome,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!despesaItemUi.itemDespesa.descricao.isNullOrBlank()) {
                                    Text(
                                        despesaItemUi.itemDespesa.descricao!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }

                                // Inputs para as semanas
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    diasDaSemanaFeira.forEach { dia ->
                                        OutlinedTextField(
                                            value = despesaItemUi.valoresPorDiaInput[dia] ?: "",
                                            onValueChange = { novoValor ->
                                                val valorLimpo = novoValor.filter { char -> char.isDigit() || char == '.' || char == ',' }
                                                if (valorLimpo.count { it == '.' || it == ',' } <= 1) {
                                                    // Atualiza diretamente o mapa mutável dentro do item na lista editável
                                                    val updatedItem = listaEditavelDespesas[index].copy(
                                                        valoresPorDiaInput = listaEditavelDespesas[index].valoresPorDiaInput.toMutableMap().apply {
                                                            this[dia] = valorLimpo
                                                        }
                                                    )
                                                    listaEditavelDespesas = listaEditavelDespesas.toMutableList().apply{ set(index, updatedItem) }
                                                }
                                            },
                                            label = { Text(dia, fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = despesaItemUi.observacaoInput,
                                    onValueChange = { novoValor ->
                                        val updatedItem = listaEditavelDespesas[index].copy(observacaoInput = novoValor)
                                        listaEditavelDespesas = listaEditavelDespesas.toMutableList().apply{ set(index, updatedItem) }
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
                    // Passa a lista editável para o ViewModel salvar
                    viewModel.salvarDespesasDaFeira(listaEditavelDespesas.toList())
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = listaEditavelDespesas.isNotEmpty() // Habilita se houver itens para potencialmente salvar
            ) {
                Text("Salvar Despesas da Feira", fontSize = 16.sp)
            }
        }
    }
}