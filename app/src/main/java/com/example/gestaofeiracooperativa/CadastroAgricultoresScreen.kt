package com.example.gestaofeiracooperativa

import android.util.Log // Para Log.e
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Warning // <<< NOVO IMPORT para o ícone do diálogo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroAgricultoresScreen(
    navController: NavHostController,
    agricultorViewModel: CadastroAgricultoresViewModel = viewModel(
        factory = CadastroAgricultoresViewModelFactory(
            (LocalContext.current.applicationContext as MyApplication).agricultorRepository
        )
    )
) {
    val context = LocalContext.current
    val agricultores by agricultorViewModel.agricultores.collectAsState() // Esta lista já deve vir ordenada do DAO
    val searchQuery by agricultorViewModel.searchQuery.collectAsState()

    var editandoAgricultor by remember { mutableStateOf<Agricultor?>(null) }
    var idInput by remember(editandoAgricultor) { mutableStateOf(editandoAgricultor?.id ?: "") }
    var nomeInput by remember(editandoAgricultor) { mutableStateOf(editandoAgricultor?.nome ?: "") }

    val coroutineScope = rememberCoroutineScope()

    // <<< NOVO: Estados para o diálogo de confirmação de exclusão >>>
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var agricultorParaDeletar by remember { mutableStateOf<Agricultor?>(null) }


    // <<< NOVO: AlertDialog para confirmar a exclusão >>>
    if (showConfirmDeleteDialog && agricultorParaDeletar != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDeleteDialog = false
                agricultorParaDeletar = null
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Aviso de Exclusão") },
            title = { Text("Confirmar Deleção") },
            text = { Text("Tem certeza que deseja deletar o agricultor '${agricultorParaDeletar?.nome ?: ""}' (ID: ${agricultorParaDeletar?.id ?: ""})? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        agricultorParaDeletar?.let { agr ->
                            coroutineScope.launch {
                                try {
                                    agricultorViewModel.delete(agr)
                                    Toast.makeText(context, "Agricultor ${agr.nome} deletado!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Log.e("CadastroAgricultores", "Erro ao deletar agricultor ${agr.id}", e)
                                    Toast.makeText(context, "Erro ao deletar agricultor.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        showConfirmDeleteDialog = false
                        agricultorParaDeletar = null
                    }
                ) { Text("Deletar") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDeleteDialog = false
                        agricultorParaDeletar = null
                    }
                ) { Text("Cancelar") }
            }
        )
    }


    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = "Cadastro de Agricultores",
                canNavigateBack = true,
                onNavigateBack = { navController.navigateUp() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editandoAgricultor = null
                idInput = ""
                nomeInput = ""
            }) {
                Icon(Icons.Filled.Add, "Adicionar Novo Agricultor")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (editandoAgricultor != null) "Editar Agricultor Existente" else "Adicionar Novo Agricultor",
                        style = MaterialTheme.typography.titleSmall
                    )

                    OutlinedTextField(
                        value = idInput,
                        onValueChange = { idInput = it.filter { char -> char.isDigit() } },
                        label = { Text("ID do Agricultor (Ex: 1, 28)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        readOnly = editandoAgricultor != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nomeInput,
                        onValueChange = { nomeInput = it },
                        label = { Text("Nome do Agricultor") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (idInput.isNotBlank() && nomeInput.isNotBlank()) {
                                val agricultorParaSalvar = Agricultor(idInput, nomeInput)
                                coroutineScope.launch {
                                    if (editandoAgricultor != null) { // Modo de edição
                                        try {
                                            agricultorViewModel.update(agricultorParaSalvar)
                                            Toast.makeText(context, "Agricultor ${agricultorParaSalvar.nome} atualizado!", Toast.LENGTH_SHORT).show()
                                            editandoAgricultor = null // Limpa campos após sucesso
                                            idInput = ""
                                            nomeInput = ""
                                        } catch (e: Exception) {
                                            Log.e("CadastroAgricultores", "Erro ao ATUALIZAR agricultor: ${agricultorParaSalvar.id}", e)
                                            Toast.makeText(context, "Erro ao atualizar agricultor. Verifique os logs.", Toast.LENGTH_LONG).show()
                                        }
                                    } else { // Modo de adição
                                        val existingAgricultor = agricultorViewModel.getAgricultorById(idInput)
                                        if (existingAgricultor != null) {
                                            Toast.makeText(context, "Agricultor com ID ${agricultorParaSalvar.id} já existe. Não foi adicionado.", Toast.LENGTH_LONG).show()
                                        } else {
                                            // <<< ALTERAÇÃO: Adicionar try-catch para o insert >>>
                                            try {
                                                agricultorViewModel.insert(agricultorParaSalvar)
                                                Toast.makeText(context, "Agricultor ${agricultorParaSalvar.nome} adicionado!", Toast.LENGTH_SHORT).show()
                                                idInput = "" // Limpa campos após sucesso
                                                nomeInput = ""
                                                // editandoAgricultor já é null
                                            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                                                Log.e("CadastroAgricultores", "Erro de CONSTRAIN ao INSERIR agricultor (ID duplicado?): ${agricultorParaSalvar.id}", e)
                                                Toast.makeText(context, "Erro: ID de agricultor '${agricultorParaSalvar.id}' já existe no banco.", Toast.LENGTH_LONG).show()
                                            } catch (e: Exception) {
                                                Log.e("CadastroAgricultores", "Erro GERAL ao INSERIR agricultor: ${agricultorParaSalvar.id}", e)
                                                Toast.makeText(context, "Erro ao adicionar agricultor. Verifique os logs.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                                // A limpeza de campos foi movida para dentro dos blocos de sucesso/lógica do FAB
                            } else {
                                Toast.makeText(context, "Preencha todos os campos.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = idInput.isNotBlank() && nomeInput.isNotBlank()
                    ) {
                        Text(if (editandoAgricultor != null) "Atualizar Agricultor" else "Adicionar Agricultor")
                    }
                    if (editandoAgricultor != null) {
                        Button(onClick = { // Botão Cancelar Edição
                            editandoAgricultor = null
                            idInput = ""
                            nomeInput = ""
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Cancelar Edição")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { agricultorViewModel.updateSearchQuery(it) },
                label = { Text("Buscar Agricultores por ID ou Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Agricultores Cadastrados:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))

            if (agricultores.isEmpty() && searchQuery.isBlank()) {
                Text("Nenhum agricultor cadastrado ainda.")
            } else if (agricultores.isEmpty() && searchQuery.isNotBlank()) {
                Text("Nenhum agricultor encontrado para \"$searchQuery\".")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) { // Define uma altura máxima para a lista se necessário
                    items(agricultores, key = { it.id }) { agricultor -> // Adiciona key para melhor performance
                        Card(elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("ID: ${agricultor.id}", fontWeight = FontWeight.Bold)
                                    Text(agricultor.nome) // Mostra o nome diretamente
                                }
                                Row {
                                    IconButton(onClick = {
                                        editandoAgricultor = agricultor
                                        idInput = agricultor.id
                                        nomeInput = agricultor.nome
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar Agricultor ${agricultor.nome}")
                                    }
                                    // <<< ALTERAÇÃO: Botão de excluir agora mostra o diálogo >>>
                                    IconButton(onClick = {
                                        agricultorParaDeletar = agricultor
                                        showConfirmDeleteDialog = true
                                    }) {
                                        // <<< ALTERAÇÃO: Cor do ícone de lixeira >>>
                                        Icon(Icons.Default.Delete, contentDescription = "Deletar Agricultor ${agricultor.nome}", tint = MaterialTheme.colorScheme.error)
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