package com.example.gestaofeiracooperativa

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale // <<< NOVO IMPORT para Locale
import androidx.compose.ui.graphics.Color

// <<< NOVO IMPORT: Precisa da definição de Agricultor >>>

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultadosFeiraScreen(
    resultadoGeralFeira: ResultadoGeralFeira,
    listaDeAgricultores: List<Agricultor>, // <<< NOVO PARÂMETRO: Lista de todos os agricultores
    onVoltar: () -> Unit,
    onSalvarFeira: () -> Unit
) {
    val context = LocalContext.current

    fun compartilharPdf(pdfFile: File, feiraId: String, titulo: String) {
        // ... (função compartilharPdf permanece a mesma)
        if (pdfFile.exists() && pdfFile.canRead()) {
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    context.applicationContext.packageName + ".provider",
                    pdfFile
                )
                val shareIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "application/pdf"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, titulo))
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao compartilhar PDF: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        } else {
            Toast.makeText(context, "Arquivo PDF não encontrado ou não pode ser lido.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = "Resultados da Feira Nº ${resultadoGeralFeira.fairDetails.feiraId}",
                canNavigateBack = true,
                onNavigateBack = { onVoltar() },
                actions = { // Início do lambda para 'actions'

                    // Botão Exportar PDF Geral
                    IconButton(onClick = {
                        try {
                            // Certifique-se que 'listaDeAgricultores' está acessível aqui.
                            // Ela é um parâmetro de ResultadosFeiraScreen.
                            val pdfFile = PdfGenerator.generateSummaryPdf(
                                context,
                                resultadoGeralFeira,
                                listaDeAgricultores // Passando a lista de agricultores
                            )
                            // A função compartilharPdf já está definida dentro de ResultadosFeiraScreen
                            compartilharPdf(
                                pdfFile,
                                resultadoGeralFeira.fairDetails.feiraId,
                                "Resumo da Feira ${resultadoGeralFeira.fairDetails.feiraId}"
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro ao gerar PDF Geral: ${e.message}", Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = "Exportar Resumo Geral PDF"
                        )
                    }
                } // Fim do lambda para 'actions'

            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabeçalho Geral
            item {
                Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // ... (Text "Resumo Geral da Feira", Período, e os InfoLinha para totais gerais como antes) ...
                        Text("Resumo Geral da Feira", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text("Período: ${resultadoGeralFeira.fairDetails.startDate} a ${resultadoGeralFeira.fairDetails.endDate}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
                        )
                        // <<< INÍCIO DA SEÇÃO MODIFICADA PARA LISTA DE AGRICULTORES E SEUS VALORES >>>
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            // <<< ALTERAÇÃO NO TÍTULO PARA REFLETIR A NOVA ORDEM >>>
                            "Resumo por Agricultor (Cooperativa / Líquido):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        if (resultadoGeralFeira.resultadosPorAgricultor.isEmpty()) {
                            Text(
                                "Nenhum agricultor com movimentação para exibir neste resumo.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Cabeçalho da mini-lista (opcional, com ordem ajustada)
                                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                                    Text("Agricultor", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold)
                                    // <<< ALTERAÇÃO NA ORDEM DO CABEÇALHO >>>
                                    Text("Valor Coop. (30%)", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.25f), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                    Text("Valor Líquido (70%)", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.25f), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                }

                                resultadoGeralFeira.resultadosPorAgricultor.forEach { resAgricultor ->
                                    val agricultorInfo = listaDeAgricultores.find { it.id == resAgricultor.agricultorId }
                                    val nomeDisplay = agricultorInfo?.nome ?: "ID: ${resAgricultor.agricultorId}"

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = nomeDisplay,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(0.5f),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        // <<< ALTERAÇÃO: Valor Cooperativa (30%) primeiro e com cor padrão >>>
                                        Text(
                                            text = "R$ ${String.format(Locale.getDefault(), "%.2f", resAgricultor.valorCooperativa).replace('.', ',')}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(0.25f).padding(start = 4.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                            color = LocalContentColor.current // Cor padrão do texto
                                        )
                                        // <<< ALTERAÇÃO: Valor Líquido (70%) depois e com cor verde >>>
                                        Text(
                                            text = "R$ ${String.format(Locale.getDefault(), "%.2f", resAgricultor.valorLiquidoAgricultor).replace('.', ',')}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(0.25f).padding(start = 4.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                            color = Color(0xFF008000) // Verde (0xFF006400 para um verde mais escuro)
                                            // Ou use uma cor do seu tema se tiver: MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }
                        // <<< FIM DA SEÇÃO MODIFICADA >>>
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoLinha(label = "Total Geral Vendido:", valor = "R$ ${String.format(Locale.getDefault(), "%.2f", resultadoGeralFeira.totalGeralVendido).replace('.', ',')}")
                        InfoLinha(label = "Total para Cooperativa (30%):", valor = "R$ ${String.format(Locale.getDefault(), "%.2f", resultadoGeralFeira.totalGeralCooperativa).replace('.', ',')}")
                        InfoLinha(label = "Total para Agricultores (70%):", valor = "R$ ${String.format(Locale.getDefault(), "%.2f", resultadoGeralFeira.totalGeralAgricultores).replace('.', ',')}")// <<< FIM DA NOVA SEÇÃO >>>
                    }
                }
            }

            item {
                Text(
                    "Detalhes por Agricultor:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            // Detalhes por agricultor
            items(resultadoGeralFeira.resultadosPorAgricultor, key = { it.agricultorId }) { resultadoAgricultor ->
                // <<< NOVO: Buscar nome do agricultor >>>
                val agricultorInfo = listaDeAgricultores.find { it.id == resultadoAgricultor.agricultorId }
                val nomeDisplayAgricultor = agricultorInfo?.nome ?: "ID: ${resultadoAgricultor.agricultorId}"

                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) // Suaviza o card
                ) {
                    Column(modifier = Modifier.padding(12.dp)) { // Aumenta padding interno
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // <<< ALTERAÇÃO: Mostra o nome do agricultor >>>
                            Text(
                                text = nomeDisplayAgricultor,
                                style = MaterialTheme.typography.titleMedium, // Um pouco maior
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                try {
                                    // Passa agricultorInfo para ter o nome no PDF individual
                                    val pdfFile = PdfGenerator.generateAgricultorPdf(context, resultadoAgricultor, resultadoGeralFeira.fairDetails, agricultorInfo)
                                    compartilharPdf(pdfFile, resultadoGeralFeira.fairDetails.feiraId, "Relatório ${nomeDisplayAgricultor} Feira ${resultadoGeralFeira.fairDetails.feiraId}")
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erro ao gerar PDF do Agricultor: ${e.message}", Toast.LENGTH_LONG).show()
                                    e.printStackTrace()
                                }
                            }) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = "Exportar PDF Agricultor ${nomeDisplayAgricultor}")
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp)) // Aumenta o espaço

                        // <<< ALTERAÇÃO: Melhoria na exibição dos itens processados >>>
                        if (resultadoAgricultor.itensProcessados.isEmpty()) {
                            Text("Nenhum item processado para este agricultor.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            resultadoAgricultor.itensProcessados.forEach { item ->
                                Column(modifier = Modifier.padding(bottom = 8.dp)) { // Espaço entre produtos
                                    Text(
                                        "${item.produto.item} (#${item.produto.numero})",
                                        style = MaterialTheme.typography.titleSmall, // Destaque para o nome do produto
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    InfoLinha(label = "  Entregue:", valor = "${item.quantidadeEntregueTotalSemana.let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format(Locale.getDefault(), "%.2f", it).replace('.',',') }} ${item.produto.unidade}")
                                    InfoLinha(label = "  Perda Alocada:", valor = "${String.format(Locale.getDefault(), "%.2f", item.quantidadePerdaAlocada).replace('.',',')} ${item.produto.unidade}")
                                    InfoLinha(label = "  Vendido:", valor = "${String.format(Locale.getDefault(), "%.2f", item.quantidadeVendida).replace('.',',')} ${item.produto.unidade}")
                                    InfoLinha(label = "  Valor Vendido:", valor = "R$ ${String.format(Locale.getDefault(), "%.2f", item.valorTotalVendido).replace('.',',')}")
                                }
                                Divider(modifier = Modifier.padding(vertical = 4.dp)) // Divisor entre produtos
                            }
                            // Remove o último divisor se houver itens
                            if (resultadoAgricultor.itensProcessados.isNotEmpty()) {
                                // Esta é uma forma de remover o último divisor, mas pode ser mais complexo que o necessário.
                                // Uma alternativa é não adicionar o Divider no último item do loop.
                                // Por ora, o Divider após cada item pode ser suficiente.
                            }
                        }

                        // Totais do Agricultor (mantendo o Divider antes)
                        // O Divider anterior já serve para separar o último produto dos totais.
                        // Spacer(modifier = Modifier.height(4.dp)) // Ajuste se necessário
                        InfoLinha(label = "Total Bruto Agricultor:", valor = "R$ ${String.format(Locale.getDefault(), "%.2f", resultadoAgricultor.totalVendidoBrutoAgricultor).replace('.', ',')}")
                        InfoLinha(label = "Valor Cooperativa (30%):", valor = "R$ ${String.format(Locale.getDefault(), "%.2f", resultadoAgricultor.valorCooperativa).replace('.', ',')}")
                        InfoLinha(label = "Valor Líquido Agricultor (70%):", valor = "R$ ${String.format(Locale.getDefault(), "%.2f", resultadoAgricultor.valorLiquidoAgricultor).replace('.', ',')}", isTotal = true)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoLinha(label: String, valor: String, isTotal: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), // Pequeno padding vertical
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodyMedium)
        Text(valor, fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal, style = if(isTotal) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium)
    }
}