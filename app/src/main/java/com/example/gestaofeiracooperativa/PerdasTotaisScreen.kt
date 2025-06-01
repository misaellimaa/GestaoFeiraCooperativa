package com.example.gestaofeiracooperativa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState // Adicione este se não estiver
import androidx.compose.foundation.clickable // Adicione este se não estiver
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning // Para o ícone do diálogo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // Para tamanhos de fonte
import java.util.Locale

// diasDaSemanaFeira está em DataModels.kt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PerdasTotaisScreen(
    feiraId: String,
    catalogoProdutos: List<Produto>, // Recebe a lista de produtos do banco (via AppNavigation)
    perdasIniciais: List<PerdaItemFeira>,
    onFinalizarPerdas: (List<PerdaItemFeira>) -> Unit,
    onVoltar: () -> Unit
) {
    val perdasRegistradas = remember { mutableStateListOf<PerdaItemFeira>().apply { addAll(perdasIniciais) } }

    var produtoSelecionado by remember { mutableStateOf<Produto?>(null) }
    var textoBuscaProduto by remember(produtoSelecionado, perdasRegistradas.size) { // Resetar textoBusca se produtoSelecionado mudar ou após adicionar/atualizar item
        mutableStateOf(
            if (produtoSelecionado != null) "${produtoSelecionado!!.numero} - ${produtoSelecionado!!.item}" else ""
        )
    }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // <<< ALTERAÇÃO: Gerenciamento de perdasDiariasInput com LaunchedEffect >>>
    val perdasDiariasInput = remember { mutableStateMapOf<String, String>() }
    var editIndex by remember { mutableStateOf<Int?>(null) }

    // LaunchedEffect para popular/limpar os inputs diários quando produtoSelecionado ou editIndex muda
    LaunchedEffect(produtoSelecionado, editIndex) {
        if (editIndex != null) { // Modo de edição: pré-preenche com os dados do item
            val itemParaEditar = perdasRegistradas.getOrNull(editIndex!!)
            if (itemParaEditar != null && produtoSelecionado?.numero == itemParaEditar.produto.numero) {
                diasDaSemanaFeira.forEach { dia ->
                    perdasDiariasInput[dia] = itemParaEditar.perdasPorDia[dia]?.let { valor ->
                        if (valor % 1.0 == 0.0) valor.toInt().toString() else String.format(Locale.getDefault(), "%.2f", valor).replace('.', ',')
                    } ?: ""
                }
            } else {
                diasDaSemanaFeira.forEach { dia ->
                    perdasDiariasInput[dia] = ""
                }
            }
        } else { // Modo de adição
            diasDaSemanaFeira.forEach { dia ->
                perdasDiariasInput[dia] = ""
            }
        }
    }

    val produtosFiltrados = if (textoBuscaProduto.isBlank() && produtoSelecionado == null) {
        catalogoProdutos
    } else if (produtoSelecionado == null) {
        catalogoProdutos.filter {
            it.item.contains(textoBuscaProduto, ignoreCase = true) ||
                    it.numero.contains(textoBuscaProduto, ignoreCase = true)
        }.sortedBy { it.item }
    } else {
        emptyList()
    }

    // <<< NOVO: Estados para o diálogo de confirmação de exclusão >>>
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var itemIndexToDelete by remember { mutableStateOf<Int?>(null) }

    fun preencherCamposParaEdicao(index: Int) {
        val itemParaEditar = perdasRegistradas.getOrNull(index)
        if (itemParaEditar != null) {
            editIndex = index
            produtoSelecionado = itemParaEditar.produto
            textoBuscaProduto = "${itemParaEditar.produto.numero} - ${itemParaEditar.produto.item}"
            // Os inputs diários serão preenchidos pelo LaunchedEffect
        }
    }

    fun limparCamposDeEntradaESairDaEdicao() {
        produtoSelecionado = null // Dispara o LaunchedEffect para limpar inputs
        textoBuscaProduto = ""
        editIndex = null // Sai do modo de edição e também faz o LaunchedEffect limpar os inputs
    }

    // <<< NOVO: AlertDialog para confirmar a exclusão >>>
    if (showConfirmDeleteDialog) {
        val indexParaDeletar = itemIndexToDelete
        AlertDialog(
            onDismissRequest = {
                showConfirmDeleteDialog = false
                itemIndexToDelete = null
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Aviso de Exclusão") },
            title = { Text("Confirmar Exclusão") },
            text = { Text("Tem certeza que deseja remover este item da lista de perdas?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        indexParaDeletar?.let { index ->
                            if (index >= 0 && index < perdasRegistradas.size) {
                                perdasRegistradas.removeAt(index)
                            }
                        }
                        showConfirmDeleteDialog = false
                        itemIndexToDelete = null
                    }
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDeleteDialog = false
                        itemIndexToDelete = null
                    }
                ) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = "Perdas Totais - Feira $feiraId",
                canNavigateBack = true,
                onNavigateBack = { onVoltar() }

            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Lançar ou Editar Perdas Totais", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))

            Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (editIndex != null) "Editando Perda de Produto" else "Registrar Nova Perda",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded && produtoSelecionado == null && editIndex == null,
                        onExpandedChange = { if (editIndex == null) dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = textoBuscaProduto,
                            onValueChange = {
                                if (editIndex == null) {
                                    textoBuscaProduto = it
                                    produtoSelecionado = null
                                    dropdownExpanded = true
                                }
                            },
                            label = { Text("Buscar Produto (Nome ou Nº)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded && produtoSelecionado == null && editIndex == null) },
                            enabled = editIndex == null
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded && produtoSelecionado == null && editIndex == null && produtosFiltrados.isNotEmpty(),
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            produtosFiltrados.forEach { prod ->
                                DropdownMenuItem(
                                    text = { Text("${prod.numero} - ${prod.item} (R$ ${String.format(Locale.getDefault(), "%.2f", prod.valorUnidade).replace('.',',')})") },
                                    onClick = {
                                        produtoSelecionado = prod
                                        textoBuscaProduto = "${prod.numero} - ${prod.item}"
                                        dropdownExpanded = false
                                    }
                                )
                            }
                            if (produtosFiltrados.isEmpty() && textoBuscaProduto.isNotBlank() && produtoSelecionado == null) {
                                DropdownMenuItem(text = { Text("Nenhum produto encontrado") }, onClick = {}, enabled = false)
                            }
                        }
                    }

                    if (produtoSelecionado != null) {
                        Text(
                            "Quantidades Perdidas por Dia (${produtoSelecionado?.unidade ?: "unid."}):",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            maxItemsInEachRow = 3
                        ) {
                            diasDaSemanaFeira.forEach { dia ->
                                OutlinedTextField(
                                    value = perdasDiariasInput[dia] ?: "",
                                    onValueChange = { novoValor ->
                                        val valorLimpo = novoValor.filter { char -> char.isDigit() || char == '.' || char == ',' }
                                        if (valorLimpo.count { it == '.' || it == ',' } <= 1) {
                                            perdasDiariasInput[dia] = valorLimpo
                                        }
                                    },
                                    label = { Text(dia.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(bottom = 8.dp)
                                        .defaultMinSize(minWidth = 100.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (editIndex != null) {
                            Button(
                                onClick = { limparCamposDeEntradaESairDaEdicao() },
                                colors = ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text("Cancelar Edição")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Button(
                            onClick = {
                                val produto = produtoSelecionado
                                val perdasValidasPorDia = mutableMapOf<String, Double>()
                                var algumaPerdaPreenchida = false

                                diasDaSemanaFeira.forEach { dia ->
                                    val valorInput = perdasDiariasInput[dia]
                                        ?.replace(',', '.')
                                        ?.toDoubleOrNull()
                                    if (valorInput != null) {
                                        perdasValidasPorDia[dia] = valorInput
                                        if (valorInput > 0.0) algumaPerdaPreenchida = true
                                    }
                                }

                                if (produto != null) {
                                    if (algumaPerdaPreenchida || perdasValidasPorDia.any { it.value == 0.0 }) {
                                        val novaPerda = PerdaItemFeira(produto, HashMap(perdasValidasPorDia))
                                        if (editIndex != null) {
                                            perdasRegistradas[editIndex!!] = novaPerda
                                        } else {
                                            val perdaExistenteIndex = perdasRegistradas.indexOfFirst { it.produto.numero == produto.numero }
                                            if (perdaExistenteIndex != -1) {
                                                // TODO: Informar usuário ou sobrescrever
                                                println("Perda para este produto já adicionada. Edite o item existente.")
                                            } else {
                                                perdasRegistradas.add(novaPerda)
                                            }
                                        }
                                        limparCamposDeEntradaESairDaEdicao()
                                    } else {
                                        println("Erro: Informe ao menos uma quantidade de perda (pode ser 0) para algum dia.")
                                        // TODO: Mostrar Toast/Snackbar para o usuário
                                    }
                                } else {
                                    println("Erro: Selecione um produto.")
                                    // TODO: Mostrar Toast/Snackbar para o usuário
                                }
                            },
                            enabled = produtoSelecionado != null &&
                                    (editIndex != null || diasDaSemanaFeira.any { !(perdasDiariasInput[it].isNullOrBlank())}),
                        ) {
                            Text(if (editIndex != null) "Atualizar Perda" else "Adicionar Perda")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Perdas Totais Registradas (Semana):", style = MaterialTheme.typography.titleMedium)

            if (perdasRegistradas.isEmpty()) {
                Text("Nenhuma perda adicionada ainda.", modifier = Modifier.padding(vertical = 8.dp))
            } else {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    perdasRegistradas.forEachIndexed { index, perdaItem ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ){
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${perdaItem.produto.item} (#${perdaItem.produto.numero})",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            "Unidade: ${perdaItem.produto.unidade}", // Valor unitário não é tão relevante para perdas aqui
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { preencherCamposParaEdicao(index) }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar Perda")
                                        }
                                        // <<< ALTERAÇÃO: Botão de excluir agora mostra o diálogo >>>
                                        IconButton(
                                            onClick = {
                                                itemIndexToDelete = index
                                                showConfirmDeleteDialog = true
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remover Perda", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Total Perdido na Semana: ${perdaItem.getTotalPerdidoNaSemana().let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format(Locale.getDefault(), "%.2f", it).replace('.',',') }} ${perdaItem.produto.unidade}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error, // Cor de erro para perdas
                                    fontWeight = FontWeight.SemiBold
                                )
                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                    diasDaSemanaFeira.forEach { dia ->
                                        val qtdDia = perdaItem.perdasPorDia[dia]
                                        if (qtdDia != null && qtdDia > 0.0) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(
                                                    text = "${dia.uppercase()}:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.weight(0.4f)
                                                )
                                                Text(
                                                    text = qtdDia.let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format(Locale.getDefault(), "%.2f", it).replace('.',',') },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.weight(0.6f)
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
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onFinalizarPerdas(perdasRegistradas.toList()) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = true
            ) {
                Text("Finalizar", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}