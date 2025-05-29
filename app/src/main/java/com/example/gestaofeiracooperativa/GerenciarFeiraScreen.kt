package com.example.gestaofeiracooperativa

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.*

// <<< NOVO IMPORT: Precisa da definição de Agricultor >>>
import com.example.gestaofeiracooperativa.Agricultor // Certifique-se que este é o caminho correto se estiver em DataModels.kt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenciarFeiraScreen(
    navController: NavHostController,
    feiraDetails: FairDetails,
    listaDeAgricultores: List<Agricultor>, // <<< NOVO PARÂMETRO: Lista de agricultores vinda do banco
    onNavigateToLancamentos: (agricultorId: String) -> Unit,
    onNavigateToPerdasTotais: () -> Unit,
    onNavigateToResultados: () -> Unit,
    onSalvarFeira: () -> Unit
) {
    var agricultorIdSelecionado by remember { mutableStateOf("") }
    // val agricultoresDisponiveis = (1..28).map { it.toString() } // <<< REMOVIDO: Usaremos listaDeAgricultores
    var expandedAgricultorDropdown by remember { mutableStateOf(false) }

    // <<< NOVO: Lógica para encontrar o nome do agricultor selecionado para exibição >>>
    val nomeDisplayAgricultorSelecionado = remember(agricultorIdSelecionado, listaDeAgricultores) {
        if (agricultorIdSelecionado.isBlank()) {
            "Selecione o Agricultor"
        } else {
            val agricultorEncontrado = listaDeAgricultores.find { it.id == agricultorIdSelecionado }
            if (agricultorEncontrado != null) {
                "ID: ${agricultorEncontrado.id} - ${agricultorEncontrado.nome}"
            } else {
                "Agricultor ID: $agricultorIdSelecionado (Nome não encontrado)" // Fallback se o ID não estiver na lista
            }
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Gerenciar Feira", style = MaterialTheme.typography.titleLarge)
            Text(
                "ID: ${feiraDetails.feiraId} | Período: ${feiraDetails.startDate} a ${feiraDetails.endDate}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

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
                            value = nomeDisplayAgricultorSelecionado, // <<< ALTERAÇÃO: Mostra ID e Nome (ou fallback)
                            onValueChange = {},
                            label = { Text("Agricultor para Lançar Entradas") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAgricultorDropdown) }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedAgricultorDropdown,
                            onDismissRequest = { expandedAgricultorDropdown = false }
                        ) {
                            // <<< ALTERAÇÃO: Itera sobre listaDeAgricultores real >>>
                            if (listaDeAgricultores.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Nenhum agricultor cadastrado") },
                                    onClick = { expandedAgricultorDropdown = false },
                                    enabled = false
                                )
                            } else {
                                listaDeAgricultores.forEach { agricultor ->
                                    DropdownMenuItem(
                                        // <<< ALTERAÇÃO: Mostra ID e Nome do agricultor >>>
                                        text = { Text("ID: ${agricultor.id} - ${agricultor.nome}") },
                                        onClick = {
                                            agricultorIdSelecionado = agricultor.id
                                            expandedAgricultorDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Button(
                        onClick = {
                            if (agricultorIdSelecionado.isNotBlank()) {
                                onNavigateToLancamentos(agricultorIdSelecionado)
                            } else {
                                // TODO: Mostrar Toast/Snackbar "Selecione um agricultor!"
                                // (Você pode adicionar um Toast aqui)
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
        }
    }
}