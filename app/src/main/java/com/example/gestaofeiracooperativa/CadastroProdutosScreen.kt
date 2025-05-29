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
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroProdutosScreen(
    navController: NavHostController,
    produtoViewModel: CadastroProdutosViewModel = viewModel(
        factory = CadastroProdutosViewModelFactory(
            (LocalContext.current.applicationContext as MyApplication).produtoRepository
        )
    )
) {
    val context = LocalContext.current
    val produtos by produtoViewModel.produtos.collectAsState()
    val searchQuery by produtoViewModel.searchQuery.collectAsState()

    var editandoProduto by remember { mutableStateOf<Produto?>(null) }
    var numeroInput by remember(editandoProduto) { mutableStateOf(editandoProduto?.numero ?: "") }
    var itemInput by remember(editandoProduto) { mutableStateOf(editandoProduto?.item ?: "") }
    var unidadeInput by remember(editandoProduto) { mutableStateOf(editandoProduto?.unidade ?: "") }
    var valorInput by remember(editandoProduto) { mutableStateOf(editandoProduto?.valorUnidade?.toString()?.replace('.', ',') ?: "") }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cadastro de Produtos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editandoProduto = null
                numeroInput = ""
                itemInput = ""
                unidadeInput = ""
                valorInput = ""
            }) {
                Icon(Icons.Filled.Add, "Adicionar Novo Produto")
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
                        if (editandoProduto != null) "Editar Produto Existente" else "Adicionar Novo Produto",
                        style = MaterialTheme.typography.titleSmall
                    )

                    OutlinedTextField(
                        value = numeroInput,
                        onValueChange = { numeroInput = it },
                        label = { Text("Número do Produto") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        readOnly = editandoProduto != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = itemInput,
                        onValueChange = { itemInput = it },
                        label = { Text("Nome do Item") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = unidadeInput,
                        onValueChange = { unidadeInput = it },
                        label = { Text("Unidade (Ex: KG, UN)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = valorInput,
                        onValueChange = { newValue ->
                            val filteredValue = newValue.filter { it.isDigit() || it == '.' || it == ',' }
                            if (filteredValue.count { it == '.' } <= 1 && filteredValue.count { it == ',' } <= 1) {
                                valorInput = filteredValue
                            }
                        },
                        label = { Text("Valor p/ Unidade (Ex: 10,50)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val valorFormatado = valorInput.replace(',', '.').toDoubleOrNull()
                            if (numeroInput.isNotBlank() && itemInput.isNotBlank() && unidadeInput.isNotBlank() && valorFormatado != null) {
                                val produto = Produto(numeroInput, itemInput, unidadeInput, valorFormatado)
                                coroutineScope.launch {
                                    val existingProduct = produtoViewModel.getProductByNumber(numeroInput)
                                    if (editandoProduto != null) {
                                        produtoViewModel.update(produto)
                                        Toast.makeText(context, "Produto ${produto.item} atualizado!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        if (existingProduct != null) {
                                            Toast.makeText(context, "Produto com número ${produto.numero} já existe.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            produtoViewModel.insert(produto)
                                            Toast.makeText(context, "Produto ${produto.item} adicionado!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                editandoProduto = null
                                numeroInput = ""
                                itemInput = ""
                                unidadeInput = ""
                                valorInput = ""
                            } else {
                                Toast.makeText(context, "Preencha todos os campos e use um valor válido.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = numeroInput.isNotBlank() && itemInput.isNotBlank() && unidadeInput.isNotBlank() && valorInput.isNotBlank()
                    ) {
                        Text(if (editandoProduto != null) "Atualizar Produto" else "Adicionar Produto")
                    }
                    if (editandoProduto != null) {
                        Button(onClick = {
                            editandoProduto = null
                            numeroInput = ""
                            itemInput = ""
                            unidadeInput = ""
                            valorInput = ""
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Cancelar")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { produtoViewModel.updateSearchQuery(it) },
                label = { Text("Buscar Produtos") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Produtos Cadastrados:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))

            if (produtos.isEmpty() && searchQuery.isBlank()) {
                Text("Nenhum produto cadastrado ainda.")
            } else if (produtos.isEmpty() && searchQuery.isNotBlank()) {
                Text("Nenhum produto encontrado para a busca.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(produtos) { produto ->
                        Card(elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${produto.item} (#${produto.numero})", fontWeight = FontWeight.Bold)
                                    Text("Unidade: ${produto.unidade} | Valor: R$ ${"%.2f".format(produto.valorUnidade)}")
                                }
                                Row {
                                    IconButton(onClick = {
                                        editandoProduto = produto
                                        numeroInput = produto.numero
                                        itemInput = produto.item
                                        unidadeInput = produto.unidade
                                        valorInput = produto.valorUnidade.toString().replace('.', ',')
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                                    }
                                    IconButton(onClick = {
                                        coroutineScope.launch {
                                            produtoViewModel.delete(produto)
                                            Toast.makeText(context, "Produto ${produto.item} deletado!", Toast.LENGTH_SHORT).show()
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
