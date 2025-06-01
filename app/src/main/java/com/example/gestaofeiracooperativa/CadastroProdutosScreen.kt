package com.example.gestaofeiracooperativa

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Certifique-se que este import está correto
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroProdutosScreen(
    navController: NavHostController,
    produtoViewModel: CadastroProdutosViewModel = viewModel(
        factory = CadastroProdutosViewModelFactory(
            (LocalContext.current.applicationContext as MyApplication).produtoRepository
            // Presumindo que você tem um MyApplication e ProdutoRepository configurados para DI
        )
    )
) {
    val context = LocalContext.current
    val produtos by produtoViewModel.produtos.collectAsState() // Já deve vir ordenado do DAO
    val searchQuery by produtoViewModel.searchQuery.collectAsState()

    var editandoProduto by remember { mutableStateOf<Produto?>(null) }
    var numeroInput by remember(editandoProduto) { mutableStateOf(editandoProduto?.numero ?: "") }
    var itemInput by remember(editandoProduto) { mutableStateOf(editandoProduto?.item ?: "") }
    var unidadeInput by remember(editandoProduto) { mutableStateOf(editandoProduto?.unidade ?: "") }
    var valorInput by remember(editandoProduto) { mutableStateOf(editandoProduto?.valorUnidade?.toString()?.replace('.', ',') ?: "") }

    val coroutineScope = rememberCoroutineScope()

    // <<< NOVO: Estados para o diálogo de confirmação de exclusão >>>
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var produtoParaDeletar by remember { mutableStateOf<Produto?>(null) }

    // <<< NOVO: AlertDialog para confirmar a exclusão >>>
    if (showConfirmDeleteDialog && produtoParaDeletar != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDeleteDialog = false
                produtoParaDeletar = null
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Aviso de Exclusão") },
            title = { Text("Confirmar Deleção") },
            text = { Text("Tem certeza que deseja deletar o produto '${produtoParaDeletar?.item ?: ""}' (Nº: ${produtoParaDeletar?.numero ?: ""})? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        produtoParaDeletar?.let { prod ->
                            coroutineScope.launch {
                                try {
                                    produtoViewModel.delete(prod)
                                    Toast.makeText(context, "Produto ${prod.item} deletado!", Toast.LENGTH_SHORT).show()
                                    if (editandoProduto?.numero == prod.numero) { // Se o produto deletado era o que estava em edição
                                        editandoProduto = null
                                        numeroInput = ""
                                        itemInput = ""
                                        unidadeInput = ""
                                        valorInput = ""
                                    }
                                } catch (e: Exception) {
                                    Log.e("CadastroProdutos", "Erro ao deletar produto ${prod.numero}", e)
                                    Toast.makeText(context, "Erro ao deletar produto.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        showConfirmDeleteDialog = false
                        produtoParaDeletar = null
                    }
                ) { Text("Deletar") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDeleteDialog = false
                        produtoParaDeletar = null
                    }
                ) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = { /* ... (TopAppBar como antes) ... */
            StandardTopAppBar(
                title = "Cadastro de Produtos",
                canNavigateBack = true,
                onNavigateBack = { navController.navigateUp() }
            )
        },
        floatingActionButton = { /* ... (FAB como antes) ... */
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
                    // ... (Text "Editar/Adicionar", OutlinedTextFields como antes) ...
                    Text(
                        if (editandoProduto != null) "Editar Produto Existente" else "Adicionar Novo Produto",
                        style = MaterialTheme.typography.titleSmall
                    )

                    OutlinedTextField(
                        value = numeroInput,
                        onValueChange = { numeroInput = it.filter { char -> char.isDigit() } }, // Permite apenas dígitos para número
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), // Mudado para Decimal
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )


                    Button(
                        onClick = {
                            val valorFormatado = valorInput.replace(',', '.').toDoubleOrNull()
                            if (numeroInput.isNotBlank() && itemInput.isNotBlank() && unidadeInput.isNotBlank() && valorFormatado != null) {
                                val produtoParaSalvar = Produto(numeroInput, itemInput, unidadeInput, valorFormatado)
                                coroutineScope.launch {
                                    if (editandoProduto != null) { // Modo de Edição
                                        try {
                                            produtoViewModel.update(produtoParaSalvar)
                                            Toast.makeText(context, "Produto ${produtoParaSalvar.item} atualizado!", Toast.LENGTH_SHORT).show()
                                            editandoProduto = null
                                            numeroInput = ""
                                            itemInput = ""
                                            unidadeInput = ""
                                            valorInput = ""
                                        } catch (e: Exception) {
                                            Log.e("CadastroProdutos", "Erro ao ATUALIZAR produto: ${produtoParaSalvar.numero}", e)
                                            Toast.makeText(context, "Erro ao atualizar produto.", Toast.LENGTH_LONG).show()
                                        }
                                    } else { // Modo de Adição
                                        val existingProduct = produtoViewModel.getProductByNumber(numeroInput)
                                        if (existingProduct != null) {
                                            Toast.makeText(context, "Produto com número ${produtoParaSalvar.numero} já existe. Não foi adicionado.", Toast.LENGTH_LONG).show()
                                        } else {
                                            try {
                                                produtoViewModel.insert(produtoParaSalvar)
                                                Toast.makeText(context, "Produto ${produtoParaSalvar.item} adicionado!", Toast.LENGTH_SHORT).show()
                                                numeroInput = ""
                                                itemInput = ""
                                                unidadeInput = ""
                                                valorInput = ""
                                            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                                                Log.e("CadastroProdutos", "Erro de CONSTRAIN ao INSERIR produto (numero duplicado?): ${produtoParaSalvar.numero}", e)
                                                Toast.makeText(context, "Erro: Número de produto '${produtoParaSalvar.numero}' já existe no banco.", Toast.LENGTH_LONG).show()
                                            } catch (e: Exception) {
                                                Log.e("CadastroProdutos", "Erro GERAL ao INSERIR produto: ${produtoParaSalvar.numero}", e)
                                                Toast.makeText(context, "Erro ao adicionar produto.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
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
                            Text("Cancelar Edição")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField( // Barra de busca como antes
                value = searchQuery,
                onValueChange = { produtoViewModel.updateSearchQuery(it) },
                label = { Text("Buscar Produtos por Nome ou Número") }, // Label ajustado
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Produtos Cadastrados:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))

            if (produtos.isEmpty() && searchQuery.isBlank()) {
                Text("Nenhum produto cadastrado ainda.")
            } else if (produtos.isEmpty() && searchQuery.isNotBlank()) {
                Text("Nenhum produto encontrado para \"$searchQuery\".")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(produtos, key = {it.numero} ) { produto -> // Adicionada key para performance
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
                                    Text("Unid: ${produto.unidade} | Valor: R$ ${String.format(Locale.getDefault(),"%.2f", produto.valorUnidade).replace('.',',')}") // Formatação com vírgula
                                }
                                Row {
                                    IconButton(onClick = {
                                        editandoProduto = produto
                                        numeroInput = produto.numero
                                        itemInput = produto.item
                                        unidadeInput = produto.unidade
                                        valorInput = produto.valorUnidade.toString().replace('.', ',') // Usa vírgula para edição
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar ${produto.item}")
                                    }
                                    // <<< ALTERAÇÃO: Botão de excluir agora mostra o diálogo >>>
                                    IconButton(onClick = {
                                        produtoParaDeletar = produto
                                        showConfirmDeleteDialog = true
                                    }) {
                                        // <<< ALTERAÇÃO: Cor do ícone de lixeira >>>
                                        Icon(Icons.Default.Delete, contentDescription = "Deletar ${produto.item}", tint = MaterialTheme.colorScheme.error)
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