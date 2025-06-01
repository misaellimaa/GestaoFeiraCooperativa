package com.example.gestaofeiracooperativa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions // <<< NOVO IMPORT
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search // <<< NOVO IMPORT para o ícone da busca (opcional)
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType // <<< NOVO IMPORT
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaFeirasSalvasScreen(
    navController: NavHostController,
    feirasSalvasDetails: List<FairDetails>,
    onAbrirFeira: (String) -> Unit,
    onDeletarFeira: (String) -> Unit
) {
    var showDialogConfirmarDelecao by remember { mutableStateOf(false) }
    var feiraParaDeletar by remember { mutableStateOf<String?>(null) }

    // <<< NOVO: Estado para o texto da barra de busca >>>
    var searchQuery by remember { mutableStateOf("") }

    // <<< NOVO: Lista filtrada baseada na searchQuery >>>
    val filteredFeiras = remember(searchQuery, feirasSalvasDetails) {
        if (searchQuery.isBlank()) {
            feirasSalvasDetails // Se a busca estiver vazia, mostra todas as feiras
        } else {
            feirasSalvasDetails.filter { fairDetail ->
                fairDetail.feiraId.contains(searchQuery, ignoreCase = true) // Filtra pelo ID da feira
            }
        }
    }

    fun handleDelecaoConfirmada(idFeira: String) {
        onDeletarFeira(idFeira)
    }

    if (showDialogConfirmarDelecao && feiraParaDeletar != null) {
        AlertDialog(
            onDismissRequest = { showDialogConfirmarDelecao = false; feiraParaDeletar = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Aviso") },
            title = { Text("Confirmar Deleção") },
            text = { Text("Tem certeza que deseja deletar a Feira Nº ${feiraParaDeletar}? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        feiraParaDeletar?.let { handleDelecaoConfirmada(it) }
                        showDialogConfirmarDelecao = false
                        feiraParaDeletar = null
                    }
                ) { Text("Deletar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialogConfirmarDelecao = false; feiraParaDeletar = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = "Feiras Salvas",
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
            // <<< NOVO: Barra de Busca >>>
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar por Nº da Feira") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp), // Espaçamento abaixo da barra de busca
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), // Ou KeyboardType.Number se o ID for sempre numérico
                leadingIcon = { // Opcional: ícone de busca
                    Icon(Icons.Default.Search, contentDescription = "Ícone de Busca")
                }
            )

            // <<< ALTERAÇÃO: Verifica se a lista FILTRADA está vazia >>>
            if (filteredFeiras.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (searchQuery.isNotBlank()) {
                        Text("Nenhuma feira encontrada para \"$searchQuery\".")
                    } else {
                        Text("Nenhuma feira salva ainda.")
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // <<< ALTERAÇÃO: Itera sobre a lista FILTRADA >>>
                    items(filteredFeiras, key = { it.feiraId }) { fairDetail ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAbrirFeira(fairDetail.feiraId) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Feira Nº ${fairDetail.feiraId}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Período: ${fairDetail.startDate} a ${fairDetail.endDate}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = { onAbrirFeira(fairDetail.feiraId) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Abrir/Editar Feira ${fairDetail.feiraId}")
                                    }
                                    IconButton(
                                        onClick = {
                                            feiraParaDeletar = fairDetail.feiraId
                                            showDialogConfirmarDelecao = true
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Deletar Feira ${fairDetail.feiraId}",
                                            tint = MaterialTheme.colorScheme.error // <<< COR VERMELHA APLICADA AQUI
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}