package com.example.gestaofeiracooperativa

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// Cores e tamanhos de fonte básicos (pode refinar depois)
private val TITLE_PAINT = Paint().apply {
    textSize = 24f
    isFakeBoldText = true
    textAlign = Paint.Align.CENTER
}
private val SUBTITLE_PAINT = Paint().apply {
    textSize = 18f
    isFakeBoldText = true
}
private val NORMAL_TEXT_PAINT = Paint().apply {
    textSize = 12f
}
private val BOLD_TEXT_PAINT = Paint().apply {
    textSize = 12f
    isFakeBoldText = true
}
private val HEADER_PAINT = Paint().apply {
    textSize = 14f
    isFakeBoldText = true
    textAlign = Paint.Align.LEFT
}

object PdfGenerator {

    // Função auxiliar para obter o diretório de arquivos internos onde podemos salvar PDFs
    private fun getPdfOutputDir(context: Context): File {
        val outputDir = File(context.filesDir, "pdfs")
        if (!outputDir.exists()) {
            outputDir.mkdirs() // Cria o diretório se não existir
        }
        return outputDir
    }

    // Função para gerar o PDF Individual do Agricultor
    fun generateAgricultorPdf(
        context: Context,
        resultadoAgricultor: ResultadoAgricultorFeira,
        feiraDetails: FairDetails
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size (approx 595x842 pts)
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var yPos = 50f
        val xCenter = pageInfo.pageWidth / 2f
        val margin = 40f
        val textWidth = pageInfo.pageWidth - 2 * margin

        // Título do Documento
        canvas.drawText("Relatório do Agricultor", xCenter, yPos, TITLE_PAINT)
        yPos += 30f
        canvas.drawText("Feira Nº ${feiraDetails.feiraId} (${feiraDetails.startDate} a ${feiraDetails.endDate})", xCenter, yPos, SUBTITLE_PAINT)
        yPos += 30f
        canvas.drawText("Agricultor Nº: ${resultadoAgricultor.agricultorId}", xCenter, yPos, SUBTITLE_PAINT)
        yPos += 40f

        // Cabeçalhos da Tabela (simplificado)
        val col1X = margin
        val col2X = margin + textWidth * 0.2f
        val col3X = margin + textWidth * 0.4f
        val col4X = margin + textWidth * 0.6f
        val col5X = margin + textWidth * 0.8f

        canvas.drawText("Produto", col1X, yPos, HEADER_PAINT)
        canvas.drawText("Entregue", col2X, yPos, HEADER_PAINT)
        canvas.drawText("Perda", col3X, yPos, HEADER_PAINT)
        canvas.drawText("Vendido", col4X, yPos, HEADER_PAINT)
        canvas.drawText("Valor", col5X, yPos, HEADER_PAINT)
        yPos += 15f
        canvas.drawLine(margin, yPos, pageInfo.pageWidth - margin, yPos, HEADER_PAINT) // Linha divisória
        yPos += 20f

        // Itens Processados
        resultadoAgricultor.itensProcessados.forEach { item ->
            canvas.drawText(item.produto.item, col1X, yPos, NORMAL_TEXT_PAINT)
            canvas.drawText("${"%.2f".format(item.quantidadeEntregueTotalSemana)} ${item.produto.unidade}", col2X, yPos, NORMAL_TEXT_PAINT)
            canvas.drawText("${"%.2f".format(item.quantidadePerdaAlocada)} ${item.produto.unidade}", col3X, yPos, NORMAL_TEXT_PAINT)
            canvas.drawText("${"%.2f".format(item.quantidadeVendida)} ${item.produto.unidade}", col4X, yPos, NORMAL_TEXT_PAINT)
            canvas.drawText("R$ ${"%.2f".format(item.valorTotalVendido)}", col5X, yPos, NORMAL_TEXT_PAINT)
            yPos += 20f
            if (yPos > pageInfo.pageHeight - 50f) { // Nova página se estiver no fim
                document.finishPage(page)
                // page = document.startPage(pageInfo) // Recomeça a página
                // canvas = page.canvas // Recomeça o canvas
                // yPos = 50f // Reset yPos
                // ... logic to handle new page ...
                // Para simplicidade, assumimos que uma página é suficiente por agricultor.
                // Em um app real, você criaria mais páginas.
            }
        }
        yPos += 20f
        canvas.drawLine(margin, yPos, pageInfo.pageWidth - margin, yPos, HEADER_PAINT)
        yPos += 20f

        // Resumo final do Agricultor
        canvas.drawText("Total Bruto Agricultor:", col1X, yPos, BOLD_TEXT_PAINT)
        canvas.drawText("R$ ${"%.2f".format(resultadoAgricultor.totalVendidoBrutoAgricultor)}", col5X, yPos, BOLD_TEXT_PAINT)
        yPos += 20f
        canvas.drawText("Valor Cooperativa (30%):", col1X, yPos, BOLD_TEXT_PAINT)
        canvas.drawText("R$ ${"%.2f".format(resultadoAgricultor.valorCooperativa)}", col5X, yPos, BOLD_TEXT_PAINT)
        yPos += 20f
        canvas.drawText("Valor Líquido Agricultor (70%):", col1X, yPos, BOLD_TEXT_PAINT)
        canvas.drawText("R$ ${"%.2f".format(resultadoAgricultor.valorLiquidoAgricultor)}", col5X, yPos, BOLD_TEXT_PAINT)
        yPos += 20f

        document.finishPage(page)

        val file = File(getPdfOutputDir(context), "agricultor_${resultadoAgricultor.agricultorId}_feira_${feiraDetails.feiraId}.pdf")
        try {
            FileOutputStream(file).use { fos ->
                document.writeTo(fos)
            }
            println("LOG_PDF: PDF do Agricultor ${resultadoAgricultor.agricultorId} gerado em ${file.absolutePath}")
            return file
        } catch (e: IOException) {
            println("LOG_PDF_ERROR: Erro ao salvar PDF do Agricultor: ${e.message}")
            e.printStackTrace()
            throw e // Relança para ser tratado pelo chamador
        } finally {
            document.close()
        }
    }

    // Função para gerar o PDF Resumo Geral da Feira
    fun generateSummaryPdf(
        context: Context,
        resultadoGeral: ResultadoGeralFeira
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        var yPos = 50f
        val xCenter = pageInfo.pageWidth / 2f
        val margin = 40f
        val lineHeight = 20f
        val colSpacing = (pageInfo.pageWidth - 2 * margin) / 4f // Para 4 colunas de dados

        // Título
        canvas.drawText("Resumo Geral da Feira", xCenter, yPos, TITLE_PAINT)
        yPos += 30f
        canvas.drawText("Feira Nº ${resultadoGeral.fairDetails.feiraId} (${resultadoGeral.fairDetails.startDate} a ${resultadoGeral.fairDetails.endDate})", xCenter, yPos, SUBTITLE_PAINT)
        yPos += 50f

        // Cabeçalhos da Tabela Resumo Geral
        canvas.drawText("Agricultor", margin, yPos, HEADER_PAINT)
        canvas.drawText("Vendido Bruto", margin + colSpacing, yPos, HEADER_PAINT)
        canvas.drawText("Cooperativa (30%)", margin + 2 * colSpacing, yPos, HEADER_PAINT)
        canvas.drawText("Líquido Agr. (70%)", margin + 3 * colSpacing, yPos, HEADER_PAINT)
        yPos += lineHeight
        canvas.drawLine(margin, yPos, pageInfo.pageWidth - margin, yPos, HEADER_PAINT)
        yPos += lineHeight

        // Detalhes por Agricultor no resumo
        resultadoGeral.resultadosPorAgricultor.forEach { agricultorRes ->
            canvas.drawText(agricultorRes.agricultorId, margin, yPos, NORMAL_TEXT_PAINT)
            canvas.drawText("R$ ${"%.2f".format(agricultorRes.totalVendidoBrutoAgricultor)}", margin + colSpacing, yPos, NORMAL_TEXT_PAINT)
            canvas.drawText("R$ ${"%.2f".format(agricultorRes.valorCooperativa)}", margin + 2 * colSpacing, yPos, NORMAL_TEXT_PAINT)
            canvas.drawText("R$ ${"%.2f".format(agricultorRes.valorLiquidoAgricultor)}", margin + 3 * colSpacing, yPos, NORMAL_TEXT_PAINT)
            yPos += lineHeight
            if (yPos > pageInfo.pageHeight - 50f) { // Nova página se necessário
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPos = 50f
                // Redesenha cabeçalhos na nova página, se desejar
                canvas.drawText("Resumo Geral da Feira (Cont.)", xCenter, yPos, TITLE_PAINT)
                yPos += 30f
                canvas.drawText("Agricultor", margin, yPos, HEADER_PAINT)
                canvas.drawText("Vendido Bruto", margin + colSpacing, yPos, HEADER_PAINT)
                canvas.drawText("Cooperativa (30%)", margin + 2 * colSpacing, yPos, HEADER_PAINT)
                canvas.drawText("Líquido Agr. (70%)", margin + 3 * colSpacing, yPos, HEADER_PAINT)
                yPos += lineHeight
                canvas.drawLine(margin, yPos, pageInfo.pageWidth - margin, yPos, HEADER_PAINT)
                yPos += lineHeight
            }
        }
        yPos += lineHeight
        canvas.drawLine(margin, yPos, pageInfo.pageWidth - margin, yPos, HEADER_PAINT)
        yPos += lineHeight

        // Totais Gerais
        canvas.drawText("TOTAL GERAL VENDIDO:", margin, yPos, BOLD_TEXT_PAINT)
        canvas.drawText("R$ ${"%.2f".format(resultadoGeral.totalGeralVendido)}", margin + colSpacing, yPos, BOLD_TEXT_PAINT)
        yPos += lineHeight
        canvas.drawText("TOTAL GERAL COOPERATIVA:", margin, yPos, BOLD_TEXT_PAINT)
        canvas.drawText("R$ ${"%.2f".format(resultadoGeral.totalGeralCooperativa)}", margin + 2 * colSpacing, yPos, BOLD_TEXT_PAINT)
        yPos += lineHeight
        canvas.drawText("TOTAL GERAL AGRICULTORES:", margin, yPos, BOLD_TEXT_PAINT)
        canvas.drawText("R$ ${"%.2f".format(resultadoGeral.totalGeralAgricultores)}", margin + 3 * colSpacing, yPos, BOLD_TEXT_PAINT)

        document.finishPage(page)

        val file = File(getPdfOutputDir(context), "resumo_feira_${resultadoGeral.fairDetails.feiraId}.pdf")
        try {
            FileOutputStream(file).use { fos ->
                document.writeTo(fos)
            }
            println("LOG_PDF: PDF Resumo Geral gerado em ${file.absolutePath}")
            return file
        } catch (e: IOException) {
            println("LOG_PDF_ERROR: Erro ao salvar PDF Resumo Geral: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            document.close()
        }
    }
}