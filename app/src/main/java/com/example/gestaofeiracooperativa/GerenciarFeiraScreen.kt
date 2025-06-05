package com.example.gestaofeiracooperativa

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Payment // Mantido para despesas
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationCard( // Mantendo private se estiver definida apenas aqui
    text: String,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        onClick = { if (enabled) onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 4.dp else 1.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
            contentColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(36.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenciarFeiraScreen(
    navController: NavHostController,
    feiraDetails: FairDetails,
    listaDeAgricultores: List<Agricultor>,
    isFeiraPersistida: Boolean,
    onNavigateToLancamentos: (agricultorId: String) -> Unit,
    onNavigateToDespesasFeira: (feiraId: String) -> Unit,
    onNavigateToPerdasTotais: () -> Unit,
    onNavigateToResultados: () -> Unit,
    onNavigateToDistribuirSobras: (feiraId: String) -> Unit,
    onSalvarFeira: () -> Unit
) {
    var agricultorIdSelecionado by remember { mutableStateOf("") }
    var expandedAgricultorDropdown by remember { mutableStateOf(false) }

    val nomeDisplayAgricultorSelecionado = remember(agricultorIdSelecionado, listaDeAgricultores) {
        if (agricultorIdSelecionado.isBlank()) {
            "Selecione o Agricultor"
        } else {
            val agricultorEncontrado = listaDeAgricultores.find { it.id == agricultorIdSelecionado }
            agricultorEncontrado?.let { "ID: ${it.id} - ${it.nome}" } ?: "ID: $agricultorIdSelecionado (não encontrado)"
        }
    }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = "Gerenciar Feira",
                canNavigateBack = true,
                onNavigateBack = { navController.navigateUp() },
                actions = {
                    IconButton(onClick = onSalvarFeira) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Salvar Feira")
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
            Text(
                "Feira Nº ${feiraDetails.feiraId}",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Período: ${feiraDetails.startDate} a ${feiraDetails.endDate}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Entradas de Produtos por Agricultor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandedAgricultorDropdown,
                        onExpandedChange = { expandedAgricultorDropdown = !expandedAgricultorDropdown },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            readOnly = true,
                            value = nomeDisplayAgricultorSelecionado,
                            onValueChange = {},
                            label = { Text("Selecionar Agricultor") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAgricultorDropdown) }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedAgricultorDropdown,
                            onDismissRequest = { expandedAgricultorDropdown = false }
                        ) {
                            if (listaDeAgricultores.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Nenhum agricultor cadastrado") },
                                    onClick = { expandedAgricultorDropdown = false },
                                    enabled = false
                                )
                            } else {
                                listaDeAgricultores.forEach { agricultor ->
                                    DropdownMenuItem(
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
                                Toast.makeText(context, "Por favor, selecione um agricultor.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = agricultorIdSelecionado.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.PostAdd, contentDescription = null)
                            Text("Lançar/Editar Entradas")
                        }
                    }
                }
            }

            Text(
                "Ações e Registros Gerais da Feira:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start).padding(top = 8.dp)
            )

            NavigationCard(
                text = "Registrar e Distribuir Sobras",
                icon = Icons.Filled.Recycling, // Ícone sugestivo para reaproveitamento
                contentDescription = "Registrar sobras da feira atual e distribuir para a próxima",
                enabled = isFeiraPersistida, // Só pode distribuir sobras de uma feira que já foi salva
                onClick = {
                    if (isFeiraPersistida) {
                        // O parâmetro 'feiraId' aqui é o da feira de DESTINO, como planejamos
                        // A tela DistribuirSobrasScreen irá então encontrar a feira ANTERIOR a esta.
                        // CORREÇÃO DE LÓGICA: Deve ser chamado para a feira que TERMINOU (origem), não a feira nova (destino).
                        // Vamos ajustar a lógica para que o botão apareça na feira certa.
                        // Por agora, o botão navegará para a nova tela, passando o ID da feira atual.
                        // A nova tela 'DistribuirSobrasScreen' receberá o ID da feira ATUAL como sendo a de DESTINO
                        // e encontrará a feira ANTERIOR automaticamente.
                        onNavigateToDistribuirSobras(feiraDetails.feiraId)
                    } else {
                        Toast.makeText(context, "Salve a feira antes de distribuir sobras.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            // <<< ALTERAÇÃO PRINCIPAL AQUI no onClick do NavigationCard de Despesas >>>
            NavigationCard(
                text = "Lançar/Ver Despesas da Feira", // Texto ajustado
                icon = Icons.Filled.Payment,
                contentDescription = "Lançar ou visualizar despesas para esta feira",
                enabled = isFeiraPersistida,
                onClick = {
                    if (isFeiraPersistida) {
                        // <<< ALTERAÇÃO AQUI: Usa o callback passado como parâmetro >>>
                        onNavigateToDespesasFeira(feiraDetails.feiraId)
                    } else {
                        Toast.makeText(context, "Salve a feira antes de lançar despesas.", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            NavigationCard(
                text = "Lançar/Editar Perdas Totais",
                icon = Icons.Filled.Inventory,
                contentDescription = "Lançar ou editar as perdas totais de produtos da feira",
                onClick = onNavigateToPerdasTotais
            )

            NavigationCard(
                text = "Processar e Ver Resultados",
                icon = Icons.Filled.Assessment,
                contentDescription = "Processar os dados e visualizar os resultados da feira",
                onClick = onNavigateToResultados
            )
        }
    }
}