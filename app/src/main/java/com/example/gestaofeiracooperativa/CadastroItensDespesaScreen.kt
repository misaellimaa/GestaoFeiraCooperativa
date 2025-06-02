package com.example.gestaofeiracooperativa // Certifique-se que é o seu package correto

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

// Importe ItemDespesaEntity, ViewModel e Factory
import com.example.gestaofeiracooperativa.ItemDespesaEntity
import com.example.gestaofeiracooperativa.CadastroItensDespesaViewModel
import com.example.gestaofeiracooperativa.CadastroItensDespesaViewModelFactory
import com.example.gestaofeiracooperativa.MyApplication // Se você usa para DI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroItensDespesaScreen(
    navController: NavHostController,
    // Assume que você tem uma forma de obter o ItemDespesaRepository,
    // por exemplo, através de uma classe Application customizada como "MyApplication"
    // Se não, você precisará ajustar a instanciação do ViewModel.
    viewModel: CadastroItensDespesaViewModel = viewModel(
        factory = CadastroItensDespesaViewModelFactory(
            (LocalContext.current.applicationContext as MyApplication).itemDespesaRepository
        )
    )
) {
    val context = LocalContext.current
    val itensDespesa by viewModel.itensDespesa.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var editandoItem by remember { mutableStateOf<ItemDespesaEntity?>(null) }
    var nomeInput by remember(editandoItem) { mutableStateOf(editandoItem?.nome ?: "") }
    var descricaoInput by remember(editandoItem) { mutableStateOf(editandoItem?.descricao ?: "") }

    val coroutineScope = rememberCoroutineScope()

    // Estados para o diálogo de confirmação de exclusão
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var itemParaDeletar by remember { mutableStateOf<ItemDespesaEntity?>(null) }

    // AlertDialog para confirmar a exclusão
    if (showConfirmDeleteDialog && itemParaDeletar != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDeleteDialog = false
                itemParaDeletar = null
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Aviso de Exclusão") },
            title = { Text("Confirmar Deleção") },
            text = { Text("Tem certeza que deseja deletar o item de despesa '${itemParaDeletar?.nome ?: ""}'? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        itemParaDeletar?.let { item ->
                            viewModel.deleteItemDespesa(item) // Chama o ViewModel para deletar
                            Toast.makeText(context, "Item '${item.nome}' deletado!", Toast.LENGTH_SHORT).show()
                            if (editandoItem?.id == item.id) { // Limpa formulário se o item deletado estava em edição
                                editandoItem = null
                                nomeInput = ""
                                descricaoInput = ""
                            }
                        }
                        showConfirmDeleteDialog = false
                        itemParaDeletar = null
                    }
                ) { Text("Deletar") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDeleteDialog = false
                        itemParaDeletar = null
                    }
                ) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            StandardTopAppBar( // Usando o cabeçalho padrão
                title = "Cadastro de Itens de Despesa",
                canNavigateBack = true,
                onNavigateBack = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editandoItem = null // Limpa para modo de adição
                nomeInput = ""
                descricaoInput = ""
            }) {
                Icon(Icons.Filled.Add, "Adicionar Novo Item de Despesa")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // Permite rolagem se o conteúdo exceder
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (editandoItem != null) "Editar Item de Despesa" else "Adicionar Novo Item de Despesa",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = nomeInput,
                        onValueChange = { nomeInput = it },
                        label = { Text("Nome do Item de Despesa*") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = descricaoInput,
                        onValueChange = { descricaoInput = it },
                        label = { Text("Descrição (Opcional)") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp), // Para múltiplas linhas
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
                    )

                    Button(
                        onClick = {
                            if (nomeInput.isNotBlank()) {
                                val nomeTrimmed = nomeInput.trim()
                                coroutineScope.launch {
                                    if (editandoItem != null) { // Modo de Edição
                                        // Verifica se o nome mudou e se o novo nome já existe (exceto para o próprio item)
                                        if (editandoItem!!.nome.equals(nomeTrimmed, ignoreCase = true)) {
                                            val itemAtualizado = editandoItem!!.copy(nome = nomeTrimmed, descricao = descricaoInput.trim().ifEmpty { null })
                                            viewModel.updateItemDespesa(itemAtualizado)
                                            Toast.makeText(context, "Item '${itemAtualizado.nome}' atualizado!", Toast.LENGTH_SHORT).show()
                                            editandoItem = null // Limpa campos
                                            nomeInput = ""
                                            descricaoInput = ""
                                        } else {
                                            val existingItemWithNewName = viewModel.getItemDespesaByNome(nomeTrimmed)
                                            if (existingItemWithNewName != null) {
                                                Toast.makeText(context, "Erro: Já existe um item de despesa com o nome '$nomeTrimmed'.", Toast.LENGTH_LONG).show()
                                            } else {
                                                val itemAtualizado = editandoItem!!.copy(nome = nomeTrimmed, descricao = descricaoInput.trim().ifEmpty { null })
                                                viewModel.updateItemDespesa(itemAtualizado)
                                                Toast.makeText(context, "Item '${itemAtualizado.nome}' atualizado!", Toast.LENGTH_SHORT).show()
                                                editandoItem = null // Limpa campos
                                                nomeInput = ""
                                                descricaoInput = ""
                                            }
                                        }
                                    } else { // Modo de Adição
                                        val existingItem = viewModel.getItemDespesaByNome(nomeTrimmed)
                                        if (existingItem != null) {
                                            Toast.makeText(context, "Item de despesa com nome '$nomeTrimmed' já existe.", Toast.LENGTH_LONG).show()
                                        } else {
                                            val novoItem = ItemDespesaEntity(nome = nomeTrimmed, descricao = descricaoInput.trim().ifEmpty { null })
                                            if (viewModel.insertItemDespesa(novoItem)) { // ViewModel agora retorna Boolean
                                                Toast.makeText(context, "Item '${novoItem.nome}' adicionado!", Toast.LENGTH_SHORT).show()
                                                nomeInput = "" // Limpa campos
                                                descricaoInput = ""
                                            } else {
                                                // O ViewModel já logou o erro, aqui podemos mostrar um Toast genérico ou específico
                                                // se o ViewModel retornasse mais detalhes sobre a falha.
                                                Toast.makeText(context, "Falha ao adicionar. Nome '$nomeTrimmed' pode já existir (verifique o banco).", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "O nome do item de despesa não pode estar vazio.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = nomeInput.isNotBlank()
                    ) {
                        Text(if (editandoItem != null) "Atualizar Item" else "Adicionar Item")
                    }
                    if (editandoItem != null) {
                        Button(
                            onClick = {
                                editandoItem = null
                                nomeInput = ""
                                descricaoInput = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("Cancelar Edição")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Buscar Itens de Despesa por Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Itens de Despesa Cadastrados:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))

            if (itensDespesa.isEmpty() && searchQuery.isBlank()) {
                Text("Nenhum item de despesa cadastrado ainda.")
            } else if (itensDespesa.isEmpty() && searchQuery.isNotBlank()) {
                Text("Nenhum item de despesa encontrado para \"$searchQuery\".")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) { // Altura máxima para a lista
                    items(itensDespesa, key = { it.id }) { item ->
                        Card(elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.nome, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    if (!item.descricao.isNullOrBlank()) {
                                        Text(item.descricao, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Row {
                                    IconButton(onClick = {
                                        editandoItem = item
                                        nomeInput = item.nome
                                        descricaoInput = item.descricao ?: ""
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar ${item.nome}")
                                    }
                                    IconButton(onClick = {
                                        itemParaDeletar = item
                                        showConfirmDeleteDialog = true
                                    }) {
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