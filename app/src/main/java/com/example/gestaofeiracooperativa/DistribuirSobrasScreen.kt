package com.example.gestaofeiracooperativa

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@Composable
fun DistribuirSobrasScreen(
    navController: NavHostController,
    feiraIdAtual: String, // O ID da feira de DESTINO
    onDistribuicaoConcluida: () -> Unit,
    viewModel: RegistrarSobrasViewModel = viewModel(
        factory = RegistrarSobrasViewModelFactory(
            feiraIdAtual = feiraIdAtual,
            feiraRepository = (LocalContext.current.applicationContext as MyApplication).feiraRepository,
            produtoRepository = (LocalContext.current.applicationContext as MyApplication).produtoRepository,
            perdaRepository = (LocalContext.current.applicationContext as MyApplication).perdaRepository,
            entradaRepository = (LocalContext.current.applicationContext as MyApplication).entradaRepository
        )
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val feiraAnteriorId by viewModel.feiraAnteriorId.collectAsState()
    val sobraUiItems by viewModel.sobraUiItems.collectAsState()

    // Lista local mutável para os TextFields da UI
    var listaEditavelSobras by remember(sobraUiItems) {
        mutableStateOf(sobraUiItems.map { it.copy() })
    }

    // Lida com o resultado da distribuição (sucesso/erro)
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is UiState.Success -> {
                Toast.makeText(context, "Sobras distribuídas com sucesso!", Toast.LENGTH_SHORT).show()
                onDistribuicaoConcluida()
                navController.popBackStack() // Volta para GerenciarFeiraScreen
            }
            is UiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> { /* Idle ou Loading */ }
        }
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = "Distribuir Sobras",
                canNavigateBack = true,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (feiraAnteriorId == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma feira anterior encontrada para importar sobras.")
                }
            } else if (listaEditavelSobras.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma perda registrada na feira anterior (Feira Nº $feiraAnteriorId).")
                }
            } else {
                Text(
                    "Registrar sobras reaproveitáveis da Feira Nº $feiraAnteriorId.",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Divider(modifier = Modifier.padding(bottom = 12.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(listaEditavelSobras, key = { _, item -> item.produto.numero }) { index, itemSobra ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(itemSobra.produto.item, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "Perda Total Registrada: ${formatQuantity(itemSobra.perdaTotalCalculada)} ${itemSobra.produto.unidade}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = itemSobra.sobraRealInput,
                                    onValueChange = { novoValor ->
                                        val valorLimpo = novoValor.filter { char -> char.isDigit() || char == '.' || char == ',' }
                                        if (valorLimpo.count { it == '.' || it == ',' } <= 1) {
                                            val novaLista = listaEditavelSobras.toMutableList()
                                            novaLista[index] = itemSobra.copy(sobraRealInput = valorLimpo)
                                            listaEditavelSobras = novaLista
                                        }
                                    },
                                    label = { Text("Sobra Real Aproveitável") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.distribuirSobras(listaEditavelSobras) },
                    enabled = uiState !is UiState.Loading,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (uiState is UiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Text("Distribuindo...", modifier = Modifier.padding(start=8.dp))
                    } else {
                        Text("Distribuir Sobras para Feira Nº $feiraIdAtual", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}