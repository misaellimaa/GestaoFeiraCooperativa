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
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistribuirSobrasScreen(
    navController: NavHostController,
    feiraIdAtual: String,
    viewModel: RegistrarSobrasViewModel,
    entradasAtuaisDaFeira: Map<String, List<EntradaItemAgricultor>>,
    onDistribuicaoConcluida: (Map<String, List<EntradaItemAgricultor>>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val feiraAnteriorId by viewModel.feiraAnteriorId.collectAsState()
    val sobraUiItems by viewModel.sobraUiItems.collectAsState()

    val listaEditavelSobras = remember { mutableStateListOf<SobraUiItem>() }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(sobraUiItems, feiraAnteriorId) {
        if (listaEditavelSobras.toList() != sobraUiItems) {
            listaEditavelSobras.clear(); listaEditavelSobras.addAll(sobraUiItems.map { it.copy() })
        }
        isLoading = false // Para de carregar quando os dados chegam
    }

    Scaffold(
        topBar = { StandardTopAppBar(title = "Distribuir Sobras", canNavigateBack = true, onNavigateBack = { navController.popBackStack() }) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp)) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (feiraAnteriorId == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nenhuma feira anterior encontrada para buscar sobras.") }
            } else if (sobraUiItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nenhuma perda registrada na feira anterior (Nº $feiraAnteriorId). Não há sobras para distribuir.") }
            } else {
                Text("Registre as sobras reaproveitáveis da Feira Nº $feiraAnteriorId para distribuir na Feira Nº $feiraIdAtual.", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
                Divider(modifier = Modifier.padding(bottom = 12.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(listaEditavelSobras, key = { _, item -> item.produto.numero }) { index, itemSobra ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${itemSobra.produto.item} (#${itemSobra.produto.numero})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Perda Total Registrada: ${formatQuantity(itemSobra.perdaTotalCalculada)} ${itemSobra.produto.unidade}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = itemSobra.sobraRealInput,
                                    onValueChange = { novoValor ->
                                        val valorLimpo = novoValor.filter { it.isDigit() || it == '.' || it == ',' }
                                        if (valorLimpo.count { it == '.' || it == ',' } <= 1) {
                                            listaEditavelSobras[index] = itemSobra.copy(sobraRealInput = valorLimpo)
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
                    onClick = {
                        isSaving = true
                        coroutineScope.launch {
                            try {
                                val novasEntradas = viewModel.calcularDistribuicao(listaEditavelSobras.toList(), entradasAtuaisDaFeira)
                                onDistribuicaoConcluida(novasEntradas)
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Confirmar e Distribuir Sobras", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}