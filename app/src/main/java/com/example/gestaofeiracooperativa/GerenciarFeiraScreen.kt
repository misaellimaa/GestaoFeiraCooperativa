package com.example.gestaofeiracooperativa

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController // Importe NavHostController
import androidx.compose.material.icons.Icons // Importe Icons
import androidx.compose.material.icons.filled.ArrowBack // Importe ArrowBack Icon
import androidx.compose.material.icons.filled.Save // Importe Save Icon
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenciarFeiraScreen(
    navController: NavHostController,
    feiraDetails: FairDetails, // Detalhes da feira (ID, datas)
    onNavigateToLancamentos: (agricultorId: String) -> Unit,
    onNavigateToPerdasTotais: () -> Unit,
    onNavigateToResultados: () -> Unit,
    onSalvarFeira: () -> Unit
) {
    // Estado local para a seleção do agricultor para lançamento
    var agricultorIdSelecionado by remember { mutableStateOf("") }
    val agricultoresDisponiveis = (1..28).map { it.toString() } // Exemplo, pode vir de outro lugar
    var expandedAgricultorDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feira Nº ${feiraDetails.feiraId} (${feiraDetails.startDate} a ${feiraDetails.endDate})") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack(AppRoutes.HOME_SCREEN, inclusive = false) }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar para Home")
                    }
                },
                actions = {
                    // Botão Salvar Feira na TopAppBar
                    IconButton(onClick = onSalvarFeira) {
                        Icon(Icons.Filled.Save, contentDescription = "Salvar Feira")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp) // Espaçamento entre os blocos
        ) {
            Text("Gerenciar Feira", style = MaterialTheme.typography.titleLarge)
            Text(
                "ID: ${feiraDetails.feiraId} | Período: ${feiraDetails.startDate} a ${feiraDetails.endDate}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // --- Seção de Lançamento de Entradas ---
            Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Entradas de Produtos", style = MaterialTheme.typography.titleSmall)

                    ExposedDropdownMenuBox(
                        expanded = expandedAgricultorDropdown,
                        onExpandedChange = { expandedAgricultorDropdown = !expandedAgricultorDropdown },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            readOnly = true,
                            value = if (agricultorIdSelecionado.isNotEmpty()) "Agricultor Nº $agricultorIdSelecionado" else "Selecione o Agricultor",
                            onValueChange = {},
                            label = { Text("Agricultor para Lançar Entradas") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAgricultorDropdown) }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedAgricultorDropdown,
                            onDismissRequest = { expandedAgricultorDropdown = false }
                        ) {
                            agricultoresDisponiveis.forEach { id ->
                                DropdownMenuItem(
                                    text = { Text("Agricultor Nº $id") },
                                    onClick = {
                                        agricultorIdSelecionado = id
                                        expandedAgricultorDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            if (agricultorIdSelecionado.isNotBlank()) {
                                onNavigateToLancamentos(agricultorIdSelecionado)
                            } else {
                                // TODO: Mostrar Toast/Snackbar "Selecione um agricultor!"
                            }
                        },
                        enabled = agricultorIdSelecionado.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lançar/Editar Entradas do Agricultor")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Seção de Lançamento de Perdas ---
            Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Perdas Totais da Feira", style = MaterialTheme.typography.titleSmall)
                    Button(
                        onClick = onNavigateToPerdasTotais,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lançar/Editar Perdas Totais por Produto")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Seção de Resultados ---
            Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resultados da Feira", style = MaterialTheme.typography.titleSmall)
                    Button(
                        onClick = onNavigateToResultados,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Processar e Ver Resultados da Feira")
                    }
                }
            }
            // TODO: Botão para Exportar PDF aqui no futuro
            // Spacer(modifier = Modifier.height(16.dp))
            // Button(onClick = { /* Exportar PDF */ }, modifier = Modifier.fillMaxWidth()) { Text("Exportar PDF") }

        }
    }
}