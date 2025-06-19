package com.example.gestaofeiracooperativa

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PerdasTotaisScreen(
    feiraId: String,
    catalogoProdutos: List<Produto>,
    perdasIniciais: List<PerdaItemFeira>,
    onFinalizarPerdas: (List<PerdaItemFeira>) -> Unit,
    onVoltar: () -> Unit
) {
    val context = LocalContext.current
    val perdasRegistradas = remember { mutableStateListOf<PerdaItemFeira>() }

    // Sincroniza a lista local com os dados iniciais
    LaunchedEffect(perdasIniciais) {
        if (perdasRegistradas.toList() != perdasIniciais) {
            perdasRegistradas.clear()
            perdasRegistradas.addAll(perdasIniciais)
        }
    }

    var produtoSelecionado by remember { mutableStateOf<Produto?>(null) }
    var textoBuscaProduto by remember(produtoSelecionado) {
        mutableStateOf(if (produtoSelecionado != null) "${produtoSelecionado?.numero} - ${produtoSelecionado?.item}" else "")
    }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var itemIndexToDelete by remember { mutableStateOf<Int?>(null) }
    val perdasDiariasInput = remember { mutableStateMapOf<String, String>() }
    var editIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(produtoSelecionado, editIndex) {
        if (editIndex != null) {
            perdasRegistradas.getOrNull(editIndex!!)?.let { itemParaEditar ->
                if (produtoSelecionado?.numero == itemParaEditar.produto?.numero) {
                    diasDaSemanaFeira.forEach { dia ->
                        val valor = itemParaEditar.perdasPorDia[dia]
                        perdasDiariasInput[dia] = valor?.let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format(Locale.getDefault(), "%.2f", it).replace('.', ',') } ?: ""
                    }
                }
            }
        } else {
            diasDaSemanaFeira.forEach { dia -> perdasDiariasInput[dia] = "" }
        }
    }

    val produtosFiltrados = remember(textoBuscaProduto, catalogoProdutos) {
        if (textoBuscaProduto.isBlank()) { catalogoProdutos }
        else { catalogoProdutos.filter { it.item.contains(textoBuscaProduto, ignoreCase = true) || it.numero.contains(textoBuscaProduto, ignoreCase = true) }.sortedBy { it.item } }
    }

    fun preencherCamposParaEdicao(index: Int) {
        perdasRegistradas.getOrNull(index)?.let {
            editIndex = index; produtoSelecionado = it.produto; textoBuscaProduto = "${it.produto?.numero} - ${it.produto?.item}"
        }
    }

    fun limparCamposDeEntradaESairDaEdicao() {
        produtoSelecionado = null; textoBuscaProduto = ""; editIndex = null
    }

    if (showConfirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false; itemIndexToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Aviso")},
            title = { Text("Confirmar Remoção") },
            text = { Text("Tem certeza que deseja remover as perdas para '${perdasRegistradas.getOrNull(itemIndexToDelete ?: -1)?.produto?.item}'?") },
            confirmButton = { TextButton(onClick = { itemIndexToDelete?.let { if (it in perdasRegistradas.indices) perdasRegistradas.removeAt(it) }; showConfirmDeleteDialog = false; itemIndexToDelete = null }) { Text("Remover") } },
            dismissButton = { TextButton(onClick = { showConfirmDeleteDialog = false; itemIndexToDelete = null }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(title = "Perdas Totais - Feira $feiraId", canNavigateBack = true, onNavigateBack = onVoltar)
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp).verticalScroll(rememberScrollState())) {
                Text("Lançar ou Editar Perdas Totais", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))

                Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (editIndex != null) "Editando Perda: ${produtoSelecionado?.item ?: ""}" else "Registrar Nova Perda", style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp))

                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded && editIndex == null,
                            onExpandedChange = { if (editIndex == null) { dropdownExpanded = it } }
                        ) {
                            OutlinedTextField(
                                value = textoBuscaProduto,
                                onValueChange = { if (editIndex == null) { textoBuscaProduto = it; produtoSelecionado = null; dropdownExpanded = true } },
                                label = { Text("Buscar Produto (Nome ou Nº)") },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                readOnly = editIndex != null,
                                singleLine = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded && editIndex == null) }
                            )
                            if (produtosFiltrados.isNotEmpty()) {
                                ExposedDropdownMenu(expanded = dropdownExpanded && editIndex == null, onDismissRequest = { dropdownExpanded = false }) {
                                    produtosFiltrados.forEach { prod ->
                                        DropdownMenuItem(
                                            text = { Text("${prod.numero} - ${prod.item}") },
                                            onClick = { produtoSelecionado = prod; textoBuscaProduto = "${prod.numero} - ${prod.item}"; dropdownExpanded = false }
                                        )
                                    }
                                }
                            }
                        }

                        if (produtoSelecionado != null) {
                            Text("Quantidades Perdidas por Dia (${produtoSelecionado?.unidade ?: "unid."}):", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3) {
                                diasDaSemanaFeira.forEach { dia ->
                                    OutlinedTextField(
                                        value = perdasDiariasInput[dia] ?: "",
                                        onValueChange = { novoValor ->
                                            val valorLimpo = novoValor.filter { it.isDigit() || it == '.' || it == ',' }; if (valorLimpo.count { it == '.' || it == ',' } <= 1) { perdasDiariasInput[dia] = valorLimpo }
                                        },
                                        label = { Text(dia) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.defaultMinSize(minWidth = 100.dp).padding(bottom = 8.dp),
                                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
                                    )
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            if (editIndex != null) { Button(onClick = { limparCamposDeEntradaESairDaEdicao() }, colors = ButtonDefaults.outlinedButtonColors()) { Text("Cancelar Edição") }; Spacer(Modifier.width(8.dp)) }
                            Button(
                                onClick = {
                                    produtoSelecionado?.let { produto ->
                                        val perdasValidasPorDia = mutableMapOf<String, Double>()
                                        diasDaSemanaFeira.forEach { dia -> perdasDiariasInput[dia]?.replace(',', '.')?.toDoubleOrNull()?.let { valor -> perdasValidasPorDia[dia] = valor } }
                                        val novaPerda = PerdaItemFeira(produto, HashMap(perdasValidasPorDia))
                                        if (editIndex != null) { perdasRegistradas[editIndex!!] = novaPerda }
                                        else {
                                            if (perdasRegistradas.any { it.produto?.numero == produto.numero }) { Toast.makeText(context, "Perda para este produto já foi adicionada.", Toast.LENGTH_SHORT).show() }
                                            else { perdasRegistradas.add(novaPerda) }
                                        }
                                        limparCamposDeEntradaESairDaEdicao()
                                    } ?: Toast.makeText(context, "Selecione um produto.", Toast.LENGTH_SHORT).show()
                                },
                                enabled = produtoSelecionado != null
                            ) { Text(if (editIndex != null) "Atualizar Item" else "Adicionar Item") }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Perdas Totais Registradas:", style = MaterialTheme.typography.titleMedium)

                if (perdasRegistradas.isEmpty()) { Text("Nenhuma perda adicionada ainda.", modifier = Modifier.padding(vertical = 8.dp)) }
                else {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        perdasRegistradas.forEachIndexed { index, perdaItem ->
                            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically){
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${perdaItem.produto?.item ?: "Produto Inválido"} (#${perdaItem.produto?.numero})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                            Text("Unidade: ${perdaItem.produto?.unidade ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { preencherCamposParaEdicao(index) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Edit, contentDescription = "Editar Perda") }
                                            IconButton(onClick = { itemIndexToDelete = index; showConfirmDeleteDialog = true }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Delete, contentDescription = "Remover Perda", tint = MaterialTheme.colorScheme.error) }
                                        }
                                    }

                                    val totalPerdasDiarias = perdaItem.getTotalPerdidoNaSemana()
                                    if (totalPerdasDiarias > 0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Perdas da Semana:", style = MaterialTheme.typography.labelLarge)
                                        Column(modifier = Modifier.padding(start = 16.dp, top = 2.dp)) {
                                            perdaItem.perdasPorDia.forEach { (dia, qtd) ->
                                                if (qtd > 0.0) {
                                                    Row(Modifier.fillMaxWidth()) {
                                                        Text("${dia.uppercase()}:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                                        Text(formatQuantity(qtd), style = MaterialTheme.typography.bodySmall)
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp)); Divider(); Spacer(modifier = Modifier.height(4.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            Text(
                                                "Total Perdido: ${formatQuantity(totalPerdasDiarias)} ${perdaItem.produto?.unidade ?: ""}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { onFinalizarPerdas(perdasRegistradas.toList()) },
                modifier = Modifier.fillMaxWidth().height(50.dp).padding(top = 16.dp),
                enabled = true
            ) {
                Text("Finalizar Lançamento de Perdas", fontSize = 16.sp)
            }
        }
    }
}