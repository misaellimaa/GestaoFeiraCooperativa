package com.example.gestaofeiracooperativa

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaFeirasSalvasScreen(
    navController: NavHostController,
    onAbrirFeira: (String) -> Unit, // Callback para carregar e "abrir" a feira selecionada
    onDeletarFeira: (String) -> Unit // Callback para solicitar deleção (AppNavigation fará a deleção real e a atualização global)
) {
    val context = LocalContext.current
    // Recarrega a lista de feiras salvas sempre que a tela é composta ou o contexto muda.
    // Isso garante que se uma feira for deletada (e o AppNavigation notificar), a lista é atualizada.
    var feirasSalvas by remember { mutableStateOf(listarFeirasSalvas(context)) }

    var showDialogConfirmarDelecao by remember { mutableStateOf(false) }
    var feiraParaDeletar by remember { mutableStateOf<String?>(null) }

    // Função para lidar com a deleção após a confirmação do diálogo
    fun handleDelecaoConfirmada(idFeira: String) {
        onDeletarFeira(idFeira) // Notifica o AppNavigation para deletar a feira
        feirasSalvas = listarFeirasSalvas(context) // Re-lê a lista de arquivos para atualizar a exibição local
    }

    if (showDialogConfirmarDelecao && feiraParaDeletar != null) {
        AlertDialog(
            onDismissRequest = { showDialogConfirmarDelecao = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Aviso") },
            title = { Text("Confirmar Deleção") },
            text = { Text("Tem certeza que deseja deletar a Feira Nº ${feiraParaDeletar}? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        feiraParaDeletar?.let { handleDelecaoConfirmada(it) } // Chama a função de deleção
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
            if (feirasSalvas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma feira salva ainda.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(feirasSalvas) { feiraId ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAbrirFeira(feiraId) } // Ação ao clicar no card para abrir
                                    .padding(horizontal = 16.dp, vertical = 8.dp), // Padding ajustado
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Feira Nº $feiraId",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Row {
                                    IconButton(
                                        onClick = { onAbrirFeira(feiraId) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Abrir/Editar Feira $feiraId")
                                    }
                                    IconButton(
                                        onClick = {
                                            feiraParaDeletar = feiraId
                                            showDialogConfirmarDelecao = true // Mostra o diálogo de confirmação
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Deletar Feira $feiraId")
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
