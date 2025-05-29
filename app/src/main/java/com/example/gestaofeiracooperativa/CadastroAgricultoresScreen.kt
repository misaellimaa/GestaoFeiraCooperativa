package com.example.gestaofeiracooperativa // <<--- MUDE PARA O SEU PACKAGE REAL

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController // Import para NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel // Import para viewModel()
import kotlinx.coroutines.launch // Import para coroutineScope.launch
import kotlinx.coroutines.flow.firstOrNull // Import para firstOrNull

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
    val agricultores by agricultorViewModel.agricultores.collectAsState()
    val searchQuery by agricultorViewModel.searchQuery.collectAsState()

    var editandoAgricultor by remember { mutableStateOf<Agricultor?>(null) }
    var idInput by remember(editandoAgricultor) { mutableStateOf(editandoAgricultor?.id ?: "") }
    var nomeInput by remember(editandoAgricultor) { mutableStateOf(editandoAgricultor?.nome ?: "") }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cadastro de Agricultores") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
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
                        onValueChange = { idInput = it.filter { char -> char.isDigit() } }, // ID é geralmente numérico
                        label = { Text("ID do Agricultor") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        readOnly = editandoAgricultor != null, // Não permite mudar o ID ao editar
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
                                val agricultor = Agricultor(idInput, nomeInput)
                                coroutineScope.launch {
                                    val existingAgricultor = agricultorViewModel.getAgricultorById(idInput)
                                    if (editandoAgricultor != null) { // Modo de edição
                                        agricultorViewModel.update(agricultor)
                                        Toast.makeText(context, "Agricultor ${agricultor.nome} atualizado!", Toast.LENGTH_SHORT).show()
                                    } else { // Modo de adição
                                        if (existingAgricultor != null) {
                                            Toast.makeText(context, "Agricultor com ID ${agricultor.id} já existe.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            agricultorViewModel.insert(agricultor)
                                            Toast.makeText(context, "Agricultor ${agricultor.nome} adicionado!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                editandoAgricultor = null
                                idInput = ""
                                nomeInput = ""
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
                        Button(onClick = {
                            editandoAgricultor = null
                            idInput = ""
                            nomeInput = ""
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Cancelar")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { agricultorViewModel.updateSearchQuery(it) },
                label = { Text("Buscar Agricultores") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Agricultores Cadastrados:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))

            if (agricultores.isEmpty() && searchQuery.isBlank()) {
                Text("Nenhum agricultor cadastrado ainda.")
            } else if (agricultores.isEmpty() && searchQuery.isNotBlank()) {
                Text("Nenhum agricultor encontrado para a busca.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(agricultores) { agricultor ->
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
                                    Text("Nome: ${agricultor.nome}")
                                }
                                Row {
                                    IconButton(onClick = {
                                        editandoAgricultor = agricultor
                                        idInput = agricultor.id
                                        nomeInput = agricultor.nome
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                                    }
                                    IconButton(onClick = {
                                        coroutineScope.launch {
                                            agricultorViewModel.delete(agricultor)
                                            Toast.makeText(context, "Agricultor ${agricultor.nome} deletado!", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Deletar")
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
