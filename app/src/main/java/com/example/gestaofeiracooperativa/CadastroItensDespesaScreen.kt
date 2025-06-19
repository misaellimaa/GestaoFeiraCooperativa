package com.example.gestaofeiracooperativa

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Seus imports...
import com.example.gestaofeiracooperativa.ItemDespesaEntity
import com.example.gestaofeiracooperativa.CadastroItensDespesaViewModel
import com.example.gestaofeiracooperativa.CadastroItensDespesaViewModelFactory
import com.example.gestaofeiracooperativa.MyApplication
import com.example.gestaofeiracooperativa.UiState
import com.example.gestaofeiracooperativa.StandardTopAppBar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroItensDespesaScreen(
    navController: NavHostController,
    viewModel: CadastroItensDespesaViewModel = viewModel(
        factory = CadastroItensDespesaViewModelFactory(
            (LocalContext.current.applicationContext as MyApplication).itemDespesaRepository
        )
    )
) {
    val context = LocalContext.current
    val itensDespesa by viewModel.itensDespesa.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val operationStatus by viewModel.operationStatus.collectAsState()

    var editandoItem by remember { mutableStateOf<ItemDespesaEntity?>(null) }
    var nomeInput by remember(editandoItem) { mutableStateOf(editandoItem?.nome ?: "") }
    var descricaoInput by remember(editandoItem) { mutableStateOf(editandoItem?.descricao ?: "") }

    val coroutineScope = rememberCoroutineScope()
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var itemParaDeletar by remember { mutableStateOf<ItemDespesaEntity?>(null) }

    // Efeito para mostrar Toasts de feedback com base no estado da operação
    LaunchedEffect(operationStatus) {
        when (val status = operationStatus) {
            is UiState.Success -> {
                Toast.makeText(context, status.data, Toast.LENGTH_SHORT).show()
                // Limpa o formulário após adicionar ou atualizar com sucesso
                if (status.data.contains("adicionado") || status.data.contains("atualizado")) {
                    editandoItem = null
                    nomeInput = ""
                    descricaoInput = ""
                }
                viewModel.resetOperationStatus() // Reseta o estado para Idle
            }
            is UiState.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                viewModel.resetOperationStatus()
            }
            else -> { /* Não faz nada para Idle ou Loading */ }
        }
    }

    if (showConfirmDeleteDialog && itemParaDeletar != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false; itemParaDeletar = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Aviso") },
            title = { Text("Confirmar Deleção") },
            text = { Text("Tem certeza que deseja deletar o item '${itemParaDeletar?.nome}'? Esta ação não pode ser desfeita e pode afetar feiras que já usaram este item de despesa.") },
            confirmButton = {
                TextButton(onClick = {
                    itemParaDeletar?.let { viewModel.deleteItemDespesa(it) }
                    showConfirmDeleteDialog = false
                    itemParaDeletar = null
                }) { Text("Deletar") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteDialog = false; itemParaDeletar = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = "Itens de Despesa",
                canNavigateBack = true,
                onNavigateBack = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editandoItem = null; nomeInput = ""; descricaoInput = ""
            }) {
                Icon(Icons.Filled.Add, "Adicionar Novo Item de Despesa")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (editandoItem != null) "Editar Item de Despesa" else "Adicionar Novo Item", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = nomeInput,
                        onValueChange = { nomeInput = it },
                        label = { Text("Nome do Item*") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = descricaoInput,
                        onValueChange = { descricaoInput = it },
                        label = { Text("Descrição (Opcional)") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
                    )

                    Button(
                        onClick = {
                            if (nomeInput.isNotBlank()) {
                                val nomeTrimmed = nomeInput.trim()
                                coroutineScope.launch {
                                    if (editandoItem != null) { // Modo Edição
                                        val itemComMesmoNome = viewModel.getItemDespesaByNome(nomeTrimmed)
                                        if (itemComMesmoNome == null || itemComMesmoNome.id == editandoItem!!.id) {
                                            val itemAtualizado = editandoItem!!.copy(nome = nomeTrimmed, descricao = descricaoInput.trim().ifEmpty { null })
                                            viewModel.updateItemDespesa(itemAtualizado)
                                        } else {
                                            Toast.makeText(context, "Erro: Já existe um item com o nome '$nomeTrimmed'.", Toast.LENGTH_LONG).show()
                                        }
                                    } else { // Modo Adição
                                        val itemExistente = viewModel.getItemDespesaByNome(nomeTrimmed)
                                        if (itemExistente != null) {
                                            Toast.makeText(context, "Item com nome '$nomeTrimmed' já existe.", Toast.LENGTH_LONG).show()
                                        } else {
                                            val novoItem = ItemDespesaEntity(nome = nomeTrimmed, descricao = descricaoInput.trim().ifEmpty { null })
                                            viewModel.insertItemDespesa(novoItem)
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "O nome do item não pode estar vazio.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = nomeInput.isNotBlank() && operationStatus !is UiState.Loading
                    ) {
                        if (operationStatus is UiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text(if (editandoItem != null) "Atualizar Item" else "Adicionar Item")
                        }
                    }
                    if (editandoItem != null) {
                        Button(onClick = { editandoItem = null; nomeInput = ""; descricaoInput = "" }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors()) {
                            Text("Cancelar Edição")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Buscar Itens por Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Itens Cadastrados:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))

            if (itensDespesa.isEmpty() && searchQuery.isBlank()) {
                Text("Nenhum item de despesa cadastrado ainda.", modifier = Modifier.padding(top = 8.dp))
            } else if (itensDespesa.isEmpty() && searchQuery.isNotBlank()) {
                Text("Nenhum item encontrado para \"$searchQuery\".", modifier = Modifier.padding(top = 8.dp))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(itensDespesa, key = { it.id }) { item ->
                        Card(elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.nome, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    if (!item.descricao.isNullOrBlank()) {
                                        Text(item.descricao, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Row {
                                    IconButton(onClick = { editandoItem = item; nomeInput = item.nome; descricaoInput = item.descricao ?: "" }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar ${item.nome}")
                                    }
                                    IconButton(onClick = { itemParaDeletar = item; showConfirmDeleteDialog = true }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Deletar ${item.nome}", tint = MaterialTheme.colorScheme.error)
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