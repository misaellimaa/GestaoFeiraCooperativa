package com.example.gestaofeiracooperativa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // Para tamanhos de fonte
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LancamentoScreen(
    feiraId: String,
    agricultorId: String,
    nomeAgricultor: String,
    catalogoProdutos: List<Produto>, // Recebe a lista de produtos do banco (via AppNavigation)
    entradasIniciais: List<EntradaItemAgricultor>,
    onFinalizar: (List<EntradaItemAgricultor>) -> Unit,
    onVoltar: () -> Unit
) {
    val entradasAgricultor = remember { mutableStateListOf<EntradaItemAgricultor>().apply { addAll(entradasIniciais) } }

    var produtoSelecionado by remember { mutableStateOf<Produto?>(null) }
    var textoBuscaProduto by remember(produtoSelecionado, entradasAgricultor.size) { // Resetar textoBusca se produtoSelecionado mudar ou após adicionar/atualizar item
        mutableStateOf(
            if (produtoSelecionado != null) "${produtoSelecionado!!.numero} - ${produtoSelecionado!!.item}" else ""
        )
    }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var itemIndexToDelete by remember { mutableStateOf<Int?>(null) }
    // <<< ALTERAÇÃO: Gerenciamento de quantidadesDiariasInput com LaunchedEffect >>>
    val quantidadesDiariasInput = remember { mutableStateMapOf<String, String>() }
    var editIndex by remember { mutableStateOf<Int?>(null) } // Controla se estamos editando um item existente


    // LaunchedEffect para popular/limpar os inputs diários quando produtoSelecionado ou editIndex muda
    LaunchedEffect(produtoSelecionado, editIndex) {
        if (editIndex != null) { // Modo de edição: pré-preenche com os dados do item
            val itemParaEditar = entradasAgricultor.getOrNull(editIndex!!) // Pega o item com segurança
            if (itemParaEditar != null && produtoSelecionado?.numero == itemParaEditar.produto.numero) {
                diasDaSemanaFeira.forEach { dia ->
                    quantidadesDiariasInput[dia] = itemParaEditar.quantidadesPorDia[dia]?.let { valor ->
                        // Formata para exibição, usando vírgula para decimal
                        if (valor % 1.0 == 0.0) valor.toInt().toString() else String.format(Locale.getDefault(), "%.2f", valor).replace('.', ',')
                    } ?: ""
                }
            } else if (itemParaEditar == null || produtoSelecionado?.numero != itemParaEditar.produto.numero) {
                // Se o item não existe mais ou o produto selecionado mudou do item em edição, limpa os campos.
                // Isso pode acontecer se o produto for deselecionado enquanto se edita, por exemplo.
                // Ou se o item foi removido da lista enquanto o formulário de edição estava aberto para ele.
                diasDaSemanaFeira.forEach { dia ->
                    quantidadesDiariasInput[dia] = ""
                }
                // Opcional: Sair do modo de edição se o item original não for mais válido para o produtoSelecionado
                // editIndex = null
            }
        } else { // Modo de adição (ou produtoSelecionado mudou fora do modo de edição)
            diasDaSemanaFeira.forEach { dia ->
                quantidadesDiariasInput[dia] = ""
            }
        }
    }


    val produtosFiltrados = if (textoBuscaProduto.isBlank() && produtoSelecionado == null) {
        catalogoProdutos // Mostra todos se a busca estiver vazia e nenhum produto selecionado
    } else if (produtoSelecionado == null) { // Filtra apenas se nenhum produto já estiver selecionado
        catalogoProdutos.filter {
            it.item.contains(textoBuscaProduto, ignoreCase = true) ||
                    it.numero.contains(textoBuscaProduto, ignoreCase = true)
        }.sortedBy { it.item }
    } else {
        emptyList() // Não mostra sugestões se um produto já está selecionado
    }

    val valorTotalEntregueBruto = entradasAgricultor.sumOf { entrada ->
        (entrada.getTotalEntregueNaSemana() * entrada.produto.valorUnidade)
    }

    fun preencherCamposParaEdicao(index: Int) {
        val itemParaEditar = entradasAgricultor.getOrNull(index)
        if (itemParaEditar != null) {
            editIndex = index // Define o índice de edição primeiro
            produtoSelecionado = itemParaEditar.produto // Depois o produto, para o LaunchedEffect ter o editIndex correto
            textoBuscaProduto = "${itemParaEditar.produto.numero} - ${itemParaEditar.produto.item}"
            // Os inputs diários serão preenchidos pelo LaunchedEffect
        }
    }

    fun limparCamposDeEntradaESairDaEdicao() {
        produtoSelecionado = null // Isso fará o LaunchedEffect limpar os inputs
        textoBuscaProduto = ""
        editIndex = null // Sai do modo de edição e também faz o LaunchedEffect limpar os inputs
    }


    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = "Entradas - $nomeAgricultor (Feira $feiraId)",
                canNavigateBack = true,
                onNavigateBack = onVoltar
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()) // Permite rolagem da tela inteira
        ) {
            Text("Lançar ou Editar Entradas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))

            Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { // Aumentado espaçamento
                    Text(
                        if (editIndex != null) "Editando Produto Entregue" else "Adicionar Novo Produto",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp) // Ajustado estilo
                    )

                    // Dropdown para selecionar produto
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded && produtoSelecionado == null && editIndex == null, // Só expande se não estiver editando
                        onExpandedChange = {
                            if (editIndex == null) { // Só permite mudar expansão se não estiver editando
                                dropdownExpanded = !dropdownExpanded
                            }
                        }
                    ) {
                        OutlinedTextField(
                            value = textoBuscaProduto,
                            onValueChange = {
                                if (editIndex == null) { // Só permite buscar/alterar se não estiver editando
                                    textoBuscaProduto = it
                                    produtoSelecionado = null // Limpa seleção ao digitar
                                    dropdownExpanded = true
                                }
                            },
                            label = { Text("Buscar Produto (Nome ou Nº)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded && produtoSelecionado == null && editIndex == null) },
                            enabled = editIndex == null // Desabilita o campo de busca durante a edição
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded && produtoSelecionado == null && editIndex == null && produtosFiltrados.isNotEmpty(),
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            produtosFiltrados.forEach { prod ->
                                DropdownMenuItem(
                                    text = { Text("${prod.numero} - ${prod.item} (R$ ${String.format(Locale.getDefault(), "%.2f", prod.valorUnidade).replace('.',',')})") },
                                    onClick = {
                                        produtoSelecionado = prod // Dispara o LaunchedEffect para limpar inputs
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
                    // <<< NOVO: AlertDialog para confirmar a exclusão >>>
                    if (showConfirmDeleteDialog) {
                        val indexParaDeletar = itemIndexToDelete // Copia para uma val para evitar problemas de concorrência em lambdas
                        AlertDialog(
                            onDismissRequest = {
                                showConfirmDeleteDialog = false
                                itemIndexToDelete = null // Limpa o índice ao fechar
                            },
                            title = { Text("Confirmar Exclusão") },
                            text = { Text("Tem certeza que deseja remover este item da lista de entradas?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        indexParaDeletar?.let { index ->
                                            if (index >= 0 && index < entradasAgricultor.size) { // Verificação de segurança
                                                entradasAgricultor.removeAt(index)
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
                    // Campos de quantidade diária
                    if (produtoSelecionado != null) {
                        Text(
                            "Quantidades Entregues por Dia (${produtoSelecionado?.unidade ?: "unid."}):",
                            style = MaterialTheme.typography.labelLarge, // Um pouco maior
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        // Usando FlowRow para melhor adaptação em telas menores, mas limitado a 3 por linha
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            maxItemsInEachRow = 3 // Exemplo: máximo de 3 por linha
                        ) {
                            diasDaSemanaFeira.forEach { dia ->
                                OutlinedTextField(
                                    value = quantidadesDiariasInput[dia] ?: "",
                                    onValueChange = { novoValor ->
                                        // Permite apenas dígitos e um separador decimal (ponto ou vírgula)
                                        val valorLimpo = novoValor.filter { char -> char.isDigit() || char == '.' || char == ',' }
                                        if (valorLimpo.count { it == '.' || it == ',' } <= 1) {
                                            quantidadesDiariasInput[dia] = valorLimpo
                                        }
                                    },
                                    label = { Text(dia.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f) // Para FlowRow, weight pode não funcionar como em Row.
                                        .padding(bottom = 8.dp) // Espaçamento inferior
                                        .defaultMinSize(minWidth = 100.dp) // Largura mínima
                                )
                            }
                        }
                    }

                    // Botões de Ação
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (editIndex != null) {
                            Button(
                                onClick = { limparCamposDeEntradaESairDaEdicao() },
                                colors = ButtonDefaults.outlinedButtonColors() // Estilo de outline
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
                                    // Converte usando vírgula como separador decimal, depois para ponto para Double
                                    val valorInput = quantidadesDiariasInput[dia]
                                        ?.replace(',', '.') // Garante ponto para toDoubleOrNull
                                        ?.toDoubleOrNull()
                                    if (valorInput != null) {
                                        quantidadesValidasPorDia[dia] = valorInput
                                        if (valorInput > 0.0) algumaQuantidadePreenchida = true
                                    }
                                }

                                if (produto != null) {
                                    if (algumaQuantidadePreenchida || quantidadesValidasPorDia.any { it.value == 0.0 }) { // Permite adicionar se tiver 0 ou >0
                                        val novaEntrada = EntradaItemAgricultor(produto, HashMap(quantidadesValidasPorDia)) // Cria um novo HashMap
                                        if (editIndex != null) {
                                            entradasAgricultor[editIndex!!] = novaEntrada
                                        } else {
                                            // Verifica se já existe uma entrada para este produto para evitar duplicatas na adição
                                            val entradaExistenteIndex = entradasAgricultor.indexOfFirst { it.produto.numero == produto.numero }
                                            if (entradaExistenteIndex != -1) {
                                                // TODO: Informar ao usuário que o produto já foi adicionado e ele deveria editá-lo.
                                                // Ou simplesmente sobrescrever:
                                                // entradasAgricultor[entradaExistenteIndex] = novaEntrada
                                                println("Produto já adicionado. Edite o item existente.")
                                            } else {
                                                entradasAgricultor.add(novaEntrada)
                                            }
                                        }
                                        limparCamposDeEntradaESairDaEdicao()
                                    } else {
                                        println("Erro: Informe ao menos uma quantidade (pode ser 0) para algum dia.")
                                        // TODO: Mostrar Toast/Snackbar para o usuário
                                    }
                                } else {
                                    println("Erro: Selecione um produto.")
                                    // TODO: Mostrar Toast/Snackbar para o usuário
                                }
                            },
                            // Habilita se um produto está selecionado e pelo menos um campo de quantidade foi tocado/preenchido
                            // (mesmo que seja para 0, desde que não esteja em branco se o produto estiver selecionado)
                            enabled = produtoSelecionado != null &&
                                    (editIndex != null || diasDaSemanaFeira.any { !(quantidadesDiariasInput[it].isNullOrBlank())}),
                        ) {
                            Text(if (editIndex != null) "Atualizar Item" else "Adicionar Item")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Produtos Entregues (Semana):", style = MaterialTheme.typography.titleMedium) // Um pouco maior

            if (entradasAgricultor.isEmpty()) {
                Text("Nenhum produto adicionado ainda.", modifier = Modifier.padding(vertical = 8.dp))
            } else {
                // <<< ALTERAÇÃO: Melhoria na exibição da lista de entradas >>>
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    entradasAgricultor.forEachIndexed { index, entrada ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) // Cor de fundo suave
                        ) {
                            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) { // Mais padding
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ){
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${entrada.produto.item} (#${entrada.produto.numero})",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium // Um pouco maior
                                        )
                                        Text(
                                            "Valor Unit.: R$ ${String.format(Locale.getDefault(), "%.2f", entrada.produto.valorUnidade).replace('.',',')} / ${entrada.produto.unidade}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { preencherCamposParaEdicao(index) }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar Item")
                                        }
                                        IconButton(
                                            onClick = {
                                                itemIndexToDelete = index
                                                showConfirmDeleteDialog = true
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remover Item", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Total Entregue na Semana: ${entrada.getTotalEntregueNaSemana().let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format(Locale.getDefault(), "%.2f", it).replace('.',',') }} ${entrada.produto.unidade}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF008000),
                                    fontWeight = FontWeight.SemiBold
                                )
                                // Exibição mais estruturada das quantidades diárias
                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                    diasDaSemanaFeira.forEach { dia ->
                                        val qtdDia = entrada.quantidadesPorDia[dia]
                                        if (qtdDia != null && qtdDia > 0.0) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(
                                                    text = "${dia.uppercase()}:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.weight(0.4f) // Ajuste peso conforme necessário
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
                                Spacer(modifier = Modifier.height(4.dp))
                                Divider() // Linha divisória
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Text(
                                        "Subtotal (Bruto da Semana): R$ ${String.format(Locale.getDefault(), "%.2f", entrada.getTotalEntregueNaSemana() * entrada.produto.valorUnidade).replace('.',',')}",
                                        style = MaterialTheme.typography.bodyLarge, // Maior
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
                        Text("R$ ${String.format(Locale.getDefault(), "%.2f", valorTotalEntregueBruto).replace('.',',')}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onFinalizar(entradasAgricultor.toList()) },
                modifier = Modifier.fillMaxWidth().height(50.dp), // Aumentado altura do botão
                enabled = true // Habilitado por padrão, pode adicionar lógica se necessário
            ) {
                Text("Finalizar Entradas do Agricultor", fontSize = 16.sp) // Aumentado tamanho da fonte
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}