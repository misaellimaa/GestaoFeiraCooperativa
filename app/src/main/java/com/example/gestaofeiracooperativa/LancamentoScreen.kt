package com.example.gestaofeiracooperativa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale

// diasDaSemanaFeira está em DataModels.kt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class) // ExperimentalLayoutApi para FlowRow
@Composable
fun LancamentoScreen(
    feiraId: String,
    agricultorId: String,
    catalogoProdutos: List<Produto>,
    entradasIniciais: List<EntradaItemAgricultor>,
    onFinalizar: (List<EntradaItemAgricultor>) -> Unit,
    onVoltar: () -> Unit
) {
    val entradasAgricultor = remember { mutableStateListOf<EntradaItemAgricultor>().apply { addAll(entradasIniciais) } }

    var produtoSelecionado by remember { mutableStateOf<Produto?>(null) }
    var textoBuscaProduto by remember(produtoSelecionado) {
        mutableStateOf(
            if (produtoSelecionado != null) "${produtoSelecionado!!.numero} - ${produtoSelecionado!!.item}" else ""
        )
    }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var dailyInputKey by remember { mutableStateOf(0L) }
    val quantidadesDiariasInput = remember(dailyInputKey, produtoSelecionado) {
        mutableStateMapOf<String, String>().apply {
            diasDaSemanaFeira.forEach { dia -> this[dia] = "" }
        }
    }

    var editIndex by remember { mutableStateOf<Int?>(null) }

    val produtosFiltrados = if (textoBuscaProduto.isBlank() && produtoSelecionado == null) {
        catalogoProdutos
    } else if (produtoSelecionado == null) {
        catalogoProdutos.filter {
            // CORREÇÃO: Busca por texto ou número usando textoBuscaProduto
            it.item.contains(textoBuscaProduto, ignoreCase = true) ||
                    it.numero.contains(textoBuscaProduto, ignoreCase = true) // <-- CORREÇÃO AQUI
        }.sortedBy { it.item } // Opcional: ordenar por nome do item para melhor UX
    } else {
        emptyList()
    }

    val valorTotalEntregueBruto = entradasAgricultor.sumOf { entrada ->
        (entrada.getTotalEntregueNaSemana() * entrada.produto.valorUnidade).toDouble()
    }

    fun preencherCamposParaEdicao(index: Int) {
        val itemParaEditar = entradasAgricultor[index]
        produtoSelecionado = itemParaEditar.produto
        textoBuscaProduto = "${itemParaEditar.produto.numero} - ${itemParaEditar.produto.item}"
        diasDaSemanaFeira.forEach { dia ->
            quantidadesDiariasInput[dia] = itemParaEditar.quantidadesPorDia[dia]?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            } ?: ""
        }
        editIndex = index
        dailyInputKey = System.currentTimeMillis()
    }

    fun limparCamposDeEntrada() {
        produtoSelecionado = null
        textoBuscaProduto = ""
        editIndex = null
        dailyInputKey = System.currentTimeMillis()
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entradas - Agr. $agricultorId (Feira $feiraId)") },
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
            Text("Lançar ou Editar Entradas", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 16.dp))

            Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (editIndex != null) "Editando Produto Entregue" else "Adicionar Novo Produto",
                        style = MaterialTheme.typography.titleSmall
                    )

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded && produtoSelecionado == null,
                        onExpandedChange = { if (produtoSelecionado == null) dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = textoBuscaProduto,
                            onValueChange = {
                                textoBuscaProduto = it // <-- CORREÇÃO: Não filtrar mais caracteres aqui
                                produtoSelecionado = null
                                dropdownExpanded = true
                                dailyInputKey = System.currentTimeMillis()
                            },
                            label = { Text("Buscar Produto (Nome ou Nº)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), // <-- Opcional: Mudar teclado para texto
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded && produtoSelecionado == null) },
                            enabled = editIndex == null
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded && produtoSelecionado == null && produtosFiltrados.isNotEmpty(),
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            // CORREÇÃO: Removido .take(10) para listar todas as opções filtradas
                            produtosFiltrados.forEach { prod -> // <-- CORREÇÃO AQUI
                                DropdownMenuItem(
                                    text = { Text("${prod.numero} - ${prod.item} (R$ ${"%.2f".format(prod.valorUnidade)})") },
                                    onClick = {
                                        produtoSelecionado = prod
                                        textoBuscaProduto = "${prod.numero} - ${prod.item}"
                                        dailyInputKey = System.currentTimeMillis()
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
                            "Quantidades Entregues por Dia (${produtoSelecionado?.unidade ?: "unid."}):",
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
                                        value = quantidadesDiariasInput[dia] ?: "",
                                        onValueChange = { novoValor ->
                                            val valorLimpo = novoValor.filter { char -> char.isDigit() || char == '.' || char == ',' }
                                            if (valorLimpo.count { it == '.' || it == ',' } <= 1) {
                                                quantidadesDiariasInput[dia] = valorLimpo
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
                                val quantidadesValidasPorDia = mutableMapOf<String, Double>()
                                var algumaQuantidadePreenchida = false
                                diasDaSemanaFeira.forEach { dia ->
                                    val valorInput = quantidadesDiariasInput[dia]?.replace(',', '.')?.toDoubleOrNull()
                                    if (valorInput != null) {
                                        quantidadesValidasPorDia[dia] = valorInput
                                        if (valorInput > 0) algumaQuantidadePreenchida = true
                                    }
                                }

                                if (produto != null && (algumaQuantidadePreenchida || quantidadesValidasPorDia.any { it.value == 0.0 })) {
                                    val novaEntrada = EntradaItemAgricultor(produto, HashMap(quantidadesValidasPorDia))
                                    if (editIndex != null) {
                                        entradasAgricultor[editIndex!!] = novaEntrada
                                    } else {
                                        entradasAgricultor.add(novaEntrada)
                                    }
                                    limparCamposDeEntrada()
                                } else if (produto == null) {
                                    println("Erro: Selecione um produto.")
                                } else {
                                    println("Erro: Informe ao menos uma quantidade para algum dia.")
                                }
                            },
                            enabled = produtoSelecionado != null && diasDaSemanaFeira.any { !(quantidadesDiariasInput[it].isNullOrBlank()) && (quantidadesDiariasInput[it]?.toDoubleOrNull() ?: 0.0) >= 0.0 },
                        ) {
                            Text(if (editIndex != null) "Atualizar Item" else "Adicionar Item")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Produtos Entregues (Semana):", style = MaterialTheme.typography.titleSmall)

            if (entradasAgricultor.isEmpty()) {
                Text("Nenhum produto adicionado ainda.", modifier = Modifier.padding(vertical = 8.dp))
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    entradasAgricultor.forEachIndexed { index, entrada ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                            Column(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ){
                                    Text(
                                        "${entrada.produto.item} (#${entrada.produto.numero})",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row {
                                        IconButton(onClick = { preencherCamposParaEdicao(index) }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar Item")
                                        }
                                        IconButton(onClick = { entradasAgricultor.removeAt(index) }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remover Item")
                                        }
                                    }
                                }
                                Text("Total Entregue na Semana: ${entrada.getTotalEntregueNaSemana()} ${entrada.produto.unidade}", color = MaterialTheme.colorScheme.primary)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    diasDaSemanaFeira.forEach { dia ->
                                        val qtdDia = entrada.quantidadesPorDia[dia] ?: 0.0
                                        if (qtdDia > 0) {
                                            Text(
                                                "${dia.uppercase()}: $qtdDia",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text("Valor Unit.: R$ ${"%.2f".format(entrada.produto.valorUnidade)}")
                                Text("Subtotal (Bruto da Semana): R$ ${"%.2f".format(entrada.getTotalEntregueNaSemana() * entrada.produto.valorUnidade)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Resumo Provisório das Entradas da Semana", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Entregue (Bruto):", style = MaterialTheme.typography.bodyLarge)
                        Text("R$ ${"%.2f".format(valorTotalEntregueBruto)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onFinalizar(entradasAgricultor.toList()) },
                modifier = Modifier.fillMaxWidth(),
                enabled = true
            ) {
                Text("Finalizar Entradas do Agricultor")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
