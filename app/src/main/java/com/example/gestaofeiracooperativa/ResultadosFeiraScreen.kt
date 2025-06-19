package com.example.gestaofeiracooperativa

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

// Seus imports...
import com.example.gestaofeiracooperativa.StandardTopAppBar
import com.example.gestaofeiracooperativa.ResultadoGeralFeira
import com.example.gestaofeiracooperativa.Agricultor
import com.example.gestaofeiracooperativa.formatQuantity
import com.example.gestaofeiracooperativa.formatCurrency
import com.example.gestaofeiracooperativa.PdfGenerator
import com.example.gestaofeiracooperativa.compartilharPdf


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultadosFeiraScreen(
    resultadoGeralFeira: ResultadoGeralFeira,
    listaDeAgricultores: List<Agricultor>,
    onVoltar: () -> Unit,
    onSalvarFeira: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = "Resultados Feira Nº ${resultadoGeralFeira.fairDetails.feiraId}",
                canNavigateBack = true,
                onNavigateBack = onVoltar, // Usa o callback passado
                actions = {
                    // <<< Botão SALVAR adicionado de volta >>>
                    IconButton(onClick = onSalvarFeira) {
                        Icon(Icons.Filled.Save, contentDescription = "Salvar Feira e Resultados")
                    }
                    // Botão para gerar o PDF de Resumo Geral
                    IconButton(onClick = {
                        try {
                            val pdfFile = PdfGenerator.generateSummaryPdf(context, resultadoGeralFeira, listaDeAgricultores)
                            // <<< CHAMADA CORRIGIDA para a função compartilharPdf >>>
                            val authorityString = context.applicationContext.packageName + ".provider"
                            val chooserTitle = "Resumo da Feira ${resultadoGeralFeira.fairDetails.feiraId}"
                            compartilharPdf(context, pdfFile, authorityString, chooserTitle)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro ao gerar PDF Geral: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "Exportar Resumo Geral PDF")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card de Resumo Geral Financeiro
            item {
                Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Resumo Geral da Feira", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text("Período: ${resultadoGeralFeira.fairDetails.startDate} a ${resultadoGeralFeira.fairDetails.endDate}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp))

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // Tabela de Resumo por Agricultor
                        Text("Resumo por Agricultor (Líquido / Cooperativa):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        if (resultadoGeralFeira.resultadosPorAgricultor.isEmpty()) {
                            Text("Nenhum agricultor com resultados para exibir.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                                    Text("Agricultor", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold)
                                    Text("Valor Líquido (70%)", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.25f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                    Text("Valor Coop. (30%)", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.25f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                }
                                resultadoGeralFeira.resultadosPorAgricultor.forEach { resAgricultor ->
                                    val nomeDisplay = listaDeAgricultores.find { it.id == resAgricultor.agricultorId }?.nome ?: "ID: ${resAgricultor.agricultorId}"
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = nomeDisplay, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(text = formatCurrency(resAgricultor.valorLiquidoAgricultor), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.25f).padding(start = 4.dp), textAlign = TextAlign.End, color = Color(0xFF008000))
                                        Text(text = formatCurrency(resAgricultor.valorCooperativa), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.25f).padding(start = 4.dp), textAlign = TextAlign.End, color = LocalContentColor.current)
                                    }
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // Totais Gerais
                        InfoLinha(label = "Total Geral Vendido:", valor = formatCurrency(resultadoGeralFeira.totalGeralVendido), isTotal = true)
                        InfoLinha(label = "Total para Cooperativa (30%):", valor = formatCurrency(resultadoGeralFeira.totalGeralCooperativa), isTotal = true)
                        InfoLinha(label = "Total para Agricultores (70%):", valor = formatCurrency(resultadoGeralFeira.totalGeralAgricultores), isTotal = true)
                    }
                }
            }

            item {
                Text("Detalhes por Agricultor:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            }

            // Lista de Detalhes por Agricultor
            items(resultadoGeralFeira.resultadosPorAgricultor, key = { it.agricultorId }) { resultadoAgricultor ->
                val agricultorInfo = listaDeAgricultores.find { it.id == resultadoAgricultor.agricultorId }
                val nomeDisplayAgricultor = agricultorInfo?.nome ?: "ID: ${resultadoAgricultor.agricultorId}"

                Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = nomeDisplayAgricultor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                try {
                                    val pdfFile = PdfGenerator.generateAgricultorPdf(context, resultadoAgricultor, resultadoGeralFeira.fairDetails, agricultorInfo)
                                    val authorityString = context.applicationContext.packageName + ".provider"
                                    val chooserTitle = "Resumo da Feira ${resultadoGeralFeira.fairDetails.feiraId}"
                                    compartilharPdf(context, pdfFile, authorityString, chooserTitle)

                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erro ao gerar PDF do Agricultor: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }) { Icon(Icons.Filled.PictureAsPdf, contentDescription = "Exportar PDF Agricultor ${nomeDisplayAgricultor}") }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        if (resultadoAgricultor.itensProcessados.isEmpty()) {
                            Text("Nenhum item processado para este agricultor.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            resultadoAgricultor.itensProcessados.forEach { item ->
                                // <<< SEÇÃO DE ITENS PROCESSADOS CORRIGIDA E ATUALIZADA >>>
                                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                    // Acesso seguro ao produto
                                    Text("${item.produto?.item ?: "Produto Inválido"} (#${item.produto?.numero})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Mostra o total geral entregue
                                    InfoLinha(
                                        label = "  Total Entregue:",
                                        valor = "${formatQuantity(item.contribuicaoTotal)} ${item.produto?.unidade ?: ""}",
                                        isBold = true // Destaca a linha
                                    )

                                    // Mostra o detalhe da composição, se houver sobra
                                    if (item.quantidadeSobraAnterior > 0) {
                                        Text(
                                            text = "   (Sobra: ${formatQuantity(item.quantidadeSobraAnterior)} + Semana: ${formatQuantity(item.quantidadeEntradaSemana)})",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    InfoLinha(label = "  Perda Alocada:", valor = "${formatQuantity(item.quantidadePerdaAlocada)} ${item.produto?.unidade ?: ""}")
                                    InfoLinha(label = "  Vendido:", valor = "${formatQuantity(item.quantidadeVendida)} ${item.produto?.unidade ?: ""}")
                                    InfoLinha(label = "  Valor Vendido:", valor = formatCurrency(item.valorTotalVendido))
                                }
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }

                        // Totais financeiros do agricultor
                        InfoLinha(label = "Total Bruto Agricultor:", valor = formatCurrency(resultadoAgricultor.totalVendidoBrutoAgricultor), isTotal = true)
                        InfoLinha(label = "Valor Cooperativa (30%):", valor = formatCurrency(resultadoAgricultor.valorCooperativa), isTotal = true)
                        InfoLinha(label = "Valor Líquido Agricultor (70%):", valor = formatCurrency(resultadoAgricultor.valorLiquidoAgricultor), isTotal = true)
                    }
                }
            }
        }
    }
}

// <<< FUNÇÃO InfoLinha ATUALIZADA >>>
@Composable
fun InfoLinha(
    label: String,
    valor: String,
    isTotal: Boolean = false,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val fontWeight = if (isTotal || isBold) FontWeight.Bold else FontWeight.Normal
        val style = if(isTotal) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium
        Text(text = label, fontWeight = fontWeight, style = style)
        Text(text = valor, fontWeight = fontWeight, style = style)
    }
}