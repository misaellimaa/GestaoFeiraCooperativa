package com.example.gestaofeiracooperativa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf // Ícone de PDF
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // Para Toast e contexto de arquivo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent // Para compartilhar PDF
import android.net.Uri // Para compartilhar PDF
import androidx.core.content.FileProvider // Para compartilhar PDF
import android.widget.Toast // Para mensagens
import java.io.File // Para manipulação de arquivos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultadosFeiraScreen(
    resultadoGeralFeira: ResultadoGeralFeira,
    onVoltar: () -> Unit,
    onSalvarFeira: () -> Unit // Já existe
) {
    val context = LocalContext.current

    // Função para compartilhar um arquivo PDF
    fun compartilharPdf(pdfFile: File, feiraId: String, titulo: String) {
        if (pdfFile.exists() && pdfFile.canRead()) {
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    context.applicationContext.packageName + ".provider", // Definir FileProvider no Manifest
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
            TopAppBar(
                title = { Text("Resultados Feira Nº ${resultadoGeralFeira.fairDetails.feiraId}") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    // Botão Salvar Feira
                    IconButton(onClick = onSalvarFeira) {
                        Icon(Icons.Filled.Save, contentDescription = "Salvar Feira")
                    }
                    // Botão Exportar PDF Geral
                    IconButton(onClick = {
                        val pdfFile = PdfGenerator.generateSummaryPdf(context, resultadoGeralFeira)
                        compartilharPdf(pdfFile, resultadoGeralFeira.fairDetails.feiraId, "Resumo da Feira ${resultadoGeralFeira.fairDetails.feiraId}")
                    }) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "Exportar Resumo Geral PDF")
                    }
                }
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
                        Text("Resumo Geral da Feira", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text("Período: ${resultadoGeralFeira.fairDetails.startDate} a ${resultadoGeralFeira.fairDetails.endDate}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoLinha(label = "Total Geral Vendido:", valor = "R$ ${"%.2f".format(resultadoGeralFeira.totalGeralVendido)}")
                        InfoLinha(label = "Total para Cooperativa (30%):", valor = "R$ ${"%.2f".format(resultadoGeralFeira.totalGeralCooperativa)}")
                        InfoLinha(label = "Total para Agricultores (70%):", valor = "R$ ${"%.2f".format(resultadoGeralFeira.totalGeralAgricultores)}")
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
            items(resultadoGeralFeira.resultadosPorAgricultor) { resultadoAgricultor ->
                Card(elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Agricultor Nº: ${resultadoAgricultor.agricultorId}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            // Botão Exportar PDF Individual
                            IconButton(onClick = {
                                val pdfFile = PdfGenerator.generateAgricultorPdf(context, resultadoAgricultor, resultadoGeralFeira.fairDetails)
                                compartilharPdf(pdfFile, resultadoGeralFeira.fairDetails.feiraId, "Relatório Agricultor ${resultadoAgricultor.agricultorId} Feira ${resultadoGeralFeira.fairDetails.feiraId}")
                            }) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = "Exportar PDF Agricultor")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        resultadoAgricultor.itensProcessados.forEach { item ->
                            Text("${item.produto.item} (#${item.produto.numero})", fontWeight = FontWeight.SemiBold)
                            Text("  Entregue: ${item.quantidadeEntregueTotalSemana} ${item.produto.unidade}")
                            Text("  Perda Alocada: ${"%.2f".format(item.quantidadePerdaAlocada)} ${item.produto.unidade}")
                            Text("  Vendido: ${"%.2f".format(item.quantidadeVendida)} ${item.produto.unidade}")
                            Text("  Valor Vendido Item: R$ ${"%.2f".format(item.valorTotalVendido)}")
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        InfoLinha(label = "Total Bruto Agricultor:", valor = "R$ ${"%.2f".format(resultadoAgricultor.totalVendidoBrutoAgricultor)}")
                        InfoLinha(label = "Valor Cooperativa (30%):", valor = "R$ ${"%.2f".format(resultadoAgricultor.valorCooperativa)}")
                        InfoLinha(label = "Valor Líquido Agricultor (70%):", valor = "R$ ${"%.2f".format(resultadoAgricultor.valorLiquidoAgricultor)}", isTotal = true)
                    }
                }
            }
        }
    }
}

// InfoLinha composable como antes
@Composable
fun InfoLinha(label: String, valor: String, isTotal: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal)
        Text(valor, fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal, fontSize = if(isTotal) 18.sp else 16.sp)
    }
}