package com.example.gestaofeiracooperativa

// Não precisa mais de 'android.content.Context' aqui para listarFeirasSalvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Importe items para LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

// <<< NOVO IMPORT: Precisa da definição de FairDetails >>>
import com.example.gestaofeiracooperativa.FairDetails // Certifique-se que este é o caminho correto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaFeirasSalvasScreen(
    navController: NavHostController,
    feirasSalvasDetails: List<FairDetails>, // <<< ALTERAÇÃO: Recebe List<FairDetails>
    onAbrirFeira: (String) -> Unit,
    onDeletarFeira: (String) -> Unit
) {


    var showDialogConfirmarDelecao by remember { mutableStateOf(false) }
    var feiraParaDeletar by remember { mutableStateOf<String?>(null) }

    // Função para lidar com a deleção após a confirmação do diálogo
    fun handleDelecaoConfirmada(idFeira: String) {
        onDeletarFeira(idFeira) // Notifica o AppNavigation para deletar a feira via Repository
    }

    if (showDialogConfirmarDelecao && feiraParaDeletar != null) {
        AlertDialog(
            onDismissRequest = { showDialogConfirmarDelecao = false; feiraParaDeletar = null }, // Limpa feiraParaDeletar também no dismiss
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
            TopAppBar(
                title = { Text("Feiras Salvas") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
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
            // <<< ALTERAÇÃO: Verifica se feirasSalvasDetails está vazia >>>
            if (feirasSalvasDetails.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma feira salva ainda.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // <<< ALTERAÇÃO: Itera sobre feirasSalvasDetails e usa fairDetail.feiraId como chave >>>
                    items(feirasSalvasDetails, key = { it.feiraId }) { fairDetail ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAbrirFeira(fairDetail.feiraId) } // Usa o ID do fairDetail
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) { // Para exibir ID e datas
                                    Text(
                                        text = "Feira Nº ${fairDetail.feiraId}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    // <<< NOVO: Exibe as datas da feira também >>>
                                    Text(
                                        text = "Período: ${fairDetail.startDate} a ${fairDetail.endDate}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = { onAbrirFeira(fairDetail.feiraId) }, // Usa o ID do fairDetail
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Abrir/Editar Feira ${fairDetail.feiraId}")
                                    }
                                    IconButton(
                                        onClick = {
                                            feiraParaDeletar = fairDetail.feiraId // Usa o ID do fairDetail
                                            showDialogConfirmarDelecao = true
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Deletar Feira ${fairDetail.feiraId}")
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