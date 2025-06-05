package com.example.gestaofeiracooperativa

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// Seus imports...
import com.example.gestaofeiracooperativa.StandardTopAppBar
import com.example.gestaofeiracooperativa.Produto
import com.example.gestaofeiracooperativa.EntradaItemAgricultor
import com.example.gestaofeiracooperativa.diasDaSemanaFeira
import com.example.gestaofeiracooperativa.formatQuantity
import com.example.gestaofeiracooperativa.formatCurrency

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LancamentoScreen(
    nomeAgricultor: String,
    catalogoProdutos: List<Produto>,
    entradasIniciais: List<EntradaItemAgricultor>,
    onFinalizar: (List<EntradaItemAgricultor>) -> Unit,
    onVoltar: () -> Unit
) {
    val context = LocalContext.current
    val entradasAgricultor = remember { mutableStateListOf<EntradaItemAgricultor>() }

    LaunchedEffect(entradasIniciais) {
        if (entradasAgricultor.isEmpty() && entradasIniciais.isNotEmpty() || entradasAgricultor.toList() != entradasIniciais) {
            entradasAgricultor.clear()
            entradasAgricultor.addAll(entradasIniciais)
        }
    }

    var produtoSelecionado by remember { mutableStateOf<Produto?>(null) }
    var textoBuscaProduto by remember(produtoSelecionado) {
        mutableStateOf(if (produtoSelecionado != null) "${produtoSelecionado!!.numero} - ${produtoSelecionado!!.item}" else "")
    }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var itemIndexToDelete by remember { mutableStateOf<Int?>(null) }
    val quantidadesDiariasInput = remember { mutableStateMapOf<String, String>() }
    var editIndex by remember { mutableStateOf<Int?>(null) }

    val valorTotalEntregueBruto by remember(entradasAgricultor.size) {
        derivedStateOf {
            entradasAgricultor.sumOf { entrada ->
                (entrada.getContribuicaoTotalParaFeira() * entrada.produto.valorUnidade)
            }
        }
    }

    LaunchedEffect(produtoSelecionado, editIndex) {
        if (editIndex != null) {
            entradasAgricultor.getOrNull(editIndex!!)?.let { itemParaEditar ->
                if (produtoSelecionado?.numero == itemParaEditar.produto.numero) {
                    diasDaSemanaFeira.forEach { dia ->
                        val valor = itemParaEditar.quantidadesPorDia[dia]
                        quantidadesDiariasInput[dia] = valor?.let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format(Locale.getDefault(), "%.2f", it).replace('.', ',') } ?: ""
                    }
                }
            }
        } else {
            diasDaSemanaFeira.forEach { dia -> quantidadesDiariasInput[dia] = "" }
        }
    }

    val produtosFiltrados = remember(textoBuscaProduto, catalogoProdutos) {
        if (textoBuscaProduto.isBlank()) { catalogoProdutos }
        else { catalogoProdutos.filter { it.item.contains(textoBuscaProduto, ignoreCase = true) || it.numero.contains(textoBuscaProduto, ignoreCase = true) }.sortedBy { it.item } }
    }

    fun preencherCamposParaEdicao(index: Int) {
        entradasAgricultor.getOrNull(index)?.let {
            editIndex = index; produtoSelecionado = it.produto; textoBuscaProduto = "${it.produto.numero} - ${it.produto.item}"
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
            text = { Text("Tem certeza que deseja remover a entrada para '${entradasAgricultor.getOrNull(itemIndexToDelete ?: -1)?.produto?.item}'?") },
            confirmButton = { TextButton(onClick = { itemIndexToDelete?.let { if (it in entradasAgricultor.indices) entradasAgricultor.removeAt(it) }; showConfirmDeleteDialog = false; itemIndexToDelete = null }) { Text("Remover") } },
            dismissButton = { TextButton(onClick = { showConfirmDeleteDialog = false; itemIndexToDelete = null }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(title = "Entradas - $nomeAgricultor", canNavigateBack = true, onNavigateBack = onVoltar)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // --- PARTE SUPERIOR (NÃO ROLÁVEL ISOLADAMENTE) ---
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Lançar ou Editar Entradas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (editIndex != null) "Editando: ${produtoSelecionado?.item ?: ""}" else "Adicionar Novo Produto", style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp))
                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded && editIndex == null,
                            onExpandedChange = { if (editIndex == null) { dropdownExpanded = it } }
                        ) {
                            OutlinedTextField(
                                value = textoBuscaProduto,
                                onValueChange = {
                                    if (editIndex == null) {
                                        textoBuscaProduto = it; produtoSelecionado = null; dropdownExpanded = true
                                    }
                                },
                                label = { Text("Buscar Produto (Nome ou Nº)") },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                readOnly = editIndex != null,
                                singleLine = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded && editIndex == null) }
                            )
                            val produtosFiltrados = remember(textoBuscaProduto, catalogoProdutos) {
                                if (textoBuscaProduto.isBlank()) catalogoProdutos
                                else catalogoProdutos.filter { it.item.contains(textoBuscaProduto, true) || it.numero.contains(textoBuscaProduto, true) }
                            }
                            if (produtosFiltrados.isNotEmpty()) {
                                ExposedDropdownMenu(
                                    expanded = dropdownExpanded && editIndex == null,
                                    onDismissRequest = { dropdownExpanded = false }
                                ) {
                                    produtosFiltrados.forEach { prod ->
                                        DropdownMenuItem(
                                            text = { Text("${prod.numero} - ${prod.item} (${formatCurrency(prod.valorUnidade)})") },
                                            onClick = { produtoSelecionado = prod; textoBuscaProduto = "${prod.numero} - ${prod.item}"; dropdownExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                        if (produtoSelecionado != null) {
                            Text("Quantidades Entregues por Dia (${produtoSelecionado?.unidade ?: "unid."}):", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3) {
                                diasDaSemanaFeira.forEach { dia ->
                                    OutlinedTextField(
                                        value = quantidadesDiariasInput[dia] ?: "",
                                        onValueChange = { novoValor -> val valorLimpo = novoValor.filter { it.isDigit() || it == '.' || it == ',' }; if (valorLimpo.count { it == '.' || it == ',' } <= 1) { quantidadesDiariasInput[dia] = valorLimpo } },
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
                            if (editIndex != null) { Button(onClick = { limparCamposDeEntradaESairDaEdicao() }, colors = ButtonDefaults.outlinedButtonColors()) { Text("Cancelar Edição") }; Spacer(modifier = Modifier.width(8.dp)) }
                            Button(
                                onClick = {
                                    produtoSelecionado?.let { produto ->
                                        val quantidadesValidasPorDia = mutableMapOf<String, Double>()
                                        diasDaSemanaFeira.forEach { dia -> quantidadesDiariasInput[dia]?.replace(',', '.')?.toDoubleOrNull()?.let { valor -> quantidadesValidasPorDia[dia] = valor } }
                                        val sobraAnterior = if(editIndex != null) entradasAgricultor[editIndex!!].quantidadeSobraDaSemanaAnterior else 0.0
                                        val novaEntrada = EntradaItemAgricultor(produto, sobraAnterior, HashMap(quantidadesValidasPorDia))
                                        if (editIndex != null) { entradasAgricultor[editIndex!!] = novaEntrada }
                                        else {
                                            if (entradasAgricultor.any { it.produto.numero == produto.numero }) { Toast.makeText(context, "Produto já adicionado.", Toast.LENGTH_SHORT).show() }
                                            else { entradasAgricultor.add(novaEntrada) }
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
                Text("Produtos Entregues:", style = MaterialTheme.typography.titleMedium)
            }

            // <<< PARTE CENTRAL (ROLÁVEL) >>>
            LazyColumn(modifier = Modifier.weight(1f)) {
                if (entradasAgricultor.isEmpty()) {
                    item { Text("Nenhum produto adicionado ainda.", modifier = Modifier.padding(vertical = 16.dp)) }
                } else {
                    itemsIndexed(entradasAgricultor,
                        key = { _, entrada -> entrada.produto.numero }) { index, entrada ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${entrada.produto.item} (#${entrada.produto.numero})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text("Valor Unit.: ${formatCurrency(entrada.produto.valorUnidade)} / ${entrada.produto.unidade}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { preencherCamposParaEdicao(index) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Edit, contentDescription = "Editar Item") }
                                        IconButton(onClick = { itemIndexToDelete = index; showConfirmDeleteDialog = true }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Delete, contentDescription = "Remover Item", tint = MaterialTheme.colorScheme.error) }
                                    }
                                }

                                // <<< INÍCIO DAS ALTERAÇÕES DE LAYOUT E CORES >>>
                                Spacer(modifier = Modifier.height(10.dp))

                                // 1. Linha da Sobra Anterior em Azul
                                if (entrada.quantidadeSobraDaSemanaAnterior > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Sobra da Feira Anterior:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(
                                            "${formatQuantity(entrada.quantidadeSobraDaSemanaAnterior)} ${entrada.produto.unidade}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary // Azul do tema
                                        )
                                    }
                                }

                                // 2. Linha das Entradas da Semana em Verde
                                val totalEntradasDiarias = entrada.getTotalEntradasDaSemana()
                                if (totalEntradasDiarias > 0.0 || entrada.quantidadeSobraDaSemanaAnterior > 0.0) { // Mostra mesmo se for 0, mas tiver sobra
                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Entradas desta Semana:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(
                                            "${formatQuantity(totalEntradasDiarias)} ${entrada.produto.unidade}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF008000) // Verde
                                        )
                                    }
                                }

                                // 3. Detalhamento dos Dias com Alinhamento
                                // Só mostra se houver entradas diárias
                                if (totalEntradasDiarias > 0) {
                                    Column(modifier = Modifier.padding(start = 16.dp, top = 2.dp)) {
                                        entrada.quantidadesPorDia.forEach { (dia, qtd) ->
                                            if (qtd > 0.0) {
                                                Row(modifier = Modifier.fillMaxWidth()) {
                                                    Text(
                                                        text = "${dia.uppercase()}:",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        modifier = Modifier.weight(1f) // Ocupa o espaço à esquerda
                                                    )
                                                    Text( // Alinhado à direita implicitamente pelo Spacer
                                                        text = formatQuantity(qtd),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp)); Divider(); Spacer(modifier = Modifier.height(8.dp))

                                // 4. Subtotal (agora somando tudo)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Text(
                                        "Subtotal (Sobra + Semana): ${formatCurrency(entrada.getContribuicaoTotalParaFeira() * entrada.produto.valorUnidade)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                // <<< FIM DAS ALTERAÇÕES DE LAYOUT E CORES >>>
                            }
                        }
                    }
                }
            }

            // <<< PARTE INFERIOR (FIXA) >>>
            Column {
                Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Resumo do Agricultor", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically // Bom para alinhar verticalmente
                        ) {
                            Text(
                                text = "Total Bruto (Sobra + Semana):", // Texto ajustado para clareza
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.weight(1f)) // <<< O Spacer flexível que ocupa o espaço vazio
                            Text(
                                text = formatCurrency(valorTotalEntregueBruto),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Button(
                    onClick = { onFinalizar(entradasAgricultor.toList()) },
                    modifier = Modifier.fillMaxWidth().height(50.dp).padding(top = 16.dp),
                    enabled = true
                ) {
                    Text("Finalizar Entradas do Agricultor", fontSize = 16.sp)
                }
            }
        }
    }
}

// Pequeno Composable auxiliar para exibir as linhas de informação no card do produto
@Composable
private fun InfoLinhaEntrada(label: String, quantidade: Double, unidade: String, isTotalSemana: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isTotalSemana) FontWeight.Bold else FontWeight.Normal)
        Text(
            "${formatQuantity(quantidade)} $unidade",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotalSemana) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotalSemana) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
        )
    }
}