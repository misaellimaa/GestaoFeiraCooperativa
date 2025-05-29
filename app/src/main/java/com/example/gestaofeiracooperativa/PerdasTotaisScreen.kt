package com.example.gestaofeiracooperativa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale

// diasDaSemanaFeira está em DataModels.kt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class) // ExperimentalLayoutApi para FlowRow
@Composable
fun PerdasTotaisScreen(
    feiraId: String,
    catalogoProdutos: List<Produto>,
    perdasIniciais: List<PerdaItemFeira>, // NOVO PARÂMETRO para pré-popular
    onFinalizarPerdas: (List<PerdaItemFeira>) -> Unit,
    onVoltar: () -> Unit
) {
    val perdasRegistradas = remember { mutableStateListOf<PerdaItemFeira>().apply { addAll(perdasIniciais) } }
    var produtoSelecionado by remember { mutableStateOf<Produto?>(null) }
    var textoBuscaProduto by remember(produtoSelecionado) {
        mutableStateOf(
            if (produtoSelecionado != null) "${produtoSelecionado!!.numero} - ${produtoSelecionado!!.item}" else ""
        )
    }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var dailyLossInputKey by remember { mutableStateOf(0L) }
    val perdasDiariasInput = remember(dailyLossInputKey, produtoSelecionado) {
        mutableStateMapOf<String, String>().apply {
            diasDaSemanaFeira.forEach { dia -> this[dia] = "" }
        }
    }

    var editIndex by remember { mutableStateOf<Int?>(null) } // Estado para controlar edição

    val produtosFiltrados = if (textoBuscaProduto.isBlank() && produtoSelecionado == null) {
        catalogoProdutos
    } else if (produtoSelecionado == null) {
        catalogoProdutos.filter {
            // CORREÇÃO: Busca por texto ou número usando textoBuscaProduto
            it.item.contains(textoBuscaProduto, ignoreCase = true) ||
                    it.numero.contains(textoBuscaProduto, ignoreCase = true) // <-- CORREÇÃO AQUI
        }.sortedBy { it.item } // Opcional: ordenar por nome do item
    } else {
        emptyList()
    }

    // Função para pré-popular campos para edição
    fun preencherCamposParaEdicao(index: Int) {
        val itemParaEditar = perdasRegistradas[index]
        produtoSelecionado = itemParaEditar.produto
        textoBuscaProduto = "${itemParaEditar.produto.numero} - ${itemParaEditar.produto.item}"
        diasDaSemanaFeira.forEach { dia ->
            perdasDiariasInput[dia] = itemParaEditar.perdasPorDia[dia]?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            } ?: ""
        }
        editIndex = index
        dailyLossInputKey = System.currentTimeMillis()
    }

    // Função para limpar campos e sair do modo de edição
    fun limparCamposDeEntrada() {
        produtoSelecionado = null
        textoBuscaProduto = ""
        editIndex = null
        dailyLossInputKey = System.currentTimeMillis()
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perdas Totais - Feira $feiraId") }, // feiraId é usado aqui
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Lançar ou Editar Perdas Totais", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 16.dp))

            Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (editIndex != null) "Editando Perda de Produto" else "Registrar Nova Perda",
                        style = MaterialTheme.typography.titleSmall
                    )

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded && produtoSelecionado == null,
                        onExpandedChange = { if (produtoSelecionado == null) dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = textoBuscaProduto,
                            onValueChange = {
                                textoBuscaProduto = it // <-- CORREÇÃO: Não filtrar mais caracteres
                                produtoSelecionado = null
                                dropdownExpanded = true
                                dailyLossInputKey = System.currentTimeMillis()
                            },
                            label = { Text("Buscar Produto (Nome ou Nº)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), // <-- Opcional: Mudar teclado
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded && produtoSelecionado == null) },
                            enabled = editIndex == null
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded && produtoSelecionado == null && produtosFiltrados.isNotEmpty(),
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            // CORREÇÃO: Removido .take(10)
                            produtosFiltrados.forEach { prod -> // <-- CORREÇÃO AQUI
                                DropdownMenuItem(
                                    text = { Text("${prod.numero} - ${prod.item} (R$ ${"%.2f".format(prod.valorUnidade)})") },
                                    onClick = {
                                        produtoSelecionado = prod
                                        textoBuscaProduto = "${prod.numero} - ${prod.item}"
                                        dailyLossInputKey = System.currentTimeMillis()
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
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        diasDaSemanaFeira.chunked(2).forEach { diasNaLinha ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                diasNaLinha.forEach { dia ->
                                    OutlinedTextField(
                                        value = perdasDiariasInput[dia] ?: "",
                                        onValueChange = { novoValor ->
                                            val valorLimpo = novoValor.filter { char -> char.isDigit() || char == '.' || char == ',' }
                                            if (valorLimpo.count { it == '.' || it == ',' } <= 1) {
                                                perdasDiariasInput[dia] = valorLimpo
                                            }
                                        },
                                        label = { Text(dia.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (diasNaLinha.size == 1 && diasDaSemanaFeira.size % 2 != 0) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (editIndex != null) {
                            Button(
                                onClick = { limparCamposDeEntrada() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline)
                            ) {
                                Text("Cancelar Edição")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Button(
                            onClick = {
                                val produto = produtoSelecionado
                                val perdasValidasPorDia = mutableMapOf<String, Double>()
                                var algumaPerdaRegistrada = false
                                diasDaSemanaFeira.forEach { dia ->
                                    val valorInput = perdasDiariasInput[dia]?.replace(',', '.')?.toDoubleOrNull()
                                    if (valorInput != null) {
                                        perdasValidasPorDia[dia] = valorInput
                                        if (valorInput > 0) algumaPerdaRegistrada = true
                                    }
                                }

                                if (produto != null && (algumaPerdaRegistrada || perdasValidasPorDia.any { it.value == 0.0 })) {
                                    val novaPerda = PerdaItemFeira(produto, HashMap(perdasValidasPorDia))
                                    if (editIndex != null) {
                                        perdasRegistradas[editIndex!!] = novaPerda
                                    } else {
                                        perdasRegistradas.add(novaPerda)
                                    }
                                    limparCamposDeEntrada()
                                } else if (produto == null) {
                                    println("Erro: Selecione um produto.")
                                } else {
                                    println("Erro: Informe ao menos uma quantidade de perda para algum dia.")
                                }
                            },
                            enabled = produtoSelecionado != null && diasDaSemanaFeira.any { !(perdasDiariasInput[it].isNullOrBlank()) && (perdasDiariasInput[it]?.toDoubleOrNull() ?: 0.0) >= 0.0 },
                        ) {
                            Text(if (editIndex != null) "Atualizar Perda" else "Adicionar Perda")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Perdas Totais Registradas (Semana):", style = MaterialTheme.typography.titleSmall)

            if (perdasRegistradas.isEmpty()) {
                Text("Nenhuma perda adicionada ainda.", modifier = Modifier.padding(vertical = 8.dp))
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    perdasRegistradas.forEachIndexed { index, perdaItem ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                            Column(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ){
                                    Text(
                                        "${perdaItem.produto.item} (#${perdaItem.produto.numero})",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row {
                                        IconButton(onClick = { preencherCamposParaEdicao(index) }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar Perda")
                                        }
                                        IconButton(onClick = { perdasRegistradas.removeAt(index) }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remover Perda")
                                        }
                                    }
                                }
                                Text("Total Perdido na Semana: ${perdaItem.getTotalPerdidoNaSemana()} ${perdaItem.produto.unidade}", color = MaterialTheme.colorScheme.error)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    diasDaSemanaFeira.forEach { dia ->
                                        val qtdDia = perdaItem.perdasPorDia[dia] ?: 0.0
                                        if (qtdDia > 0) {
                                            Text(
                                                "${dia.uppercase()}: $qtdDia",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
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
                modifier = Modifier.fillMaxWidth(),
                enabled = true
            ) {
                Text("Finalizar Lançamento de Perdas Totais da Feira")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
