package com.example.gestaofeiracooperativa

import android.content.Context
import android.graphics.Canvas // Importe Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale // Importe Locale
import android.graphics.Typeface

// Data classes (certifique-se que estão acessíveis/importadas)
import com.example.gestaofeiracooperativa.ResultadoGeralFeira
import com.example.gestaofeiracooperativa.ResultadoAgricultorFeira
import com.example.gestaofeiracooperativa.FairDetails
import com.example.gestaofeiracooperativa.Agricultor
import com.example.gestaofeiracooperativa.ItemProcessadoAgricultor
import com.example.gestaofeiracooperativa.Produto

object PdfGenerator {

    // Constantes de Layout
    private const val PAGE_WIDTH_A4 = 595
    private const val PAGE_HEIGHT_A4 = 842
    private const val MARGIN = 40f
    private const val LINE_HEIGHT_SMALL = 15f
    private const val LINE_HEIGHT_NORMAL = 18f
    private const val LINE_HEIGHT_MEDIUM = 22f
    private const val LINE_HEIGHT_LARGE = 28f
    private const val LINE_HEIGHT_TITLE = 35f
    private const val LINE_HEIGHT_HEADER = 20f

    // Paints (Estilos de Texto)
    private val TITLE_PAINT = Paint().apply {
        textSize = 22f // Um pouco menor para caber mais
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        color = android.graphics.Color.BLACK // Definindo cor explicitamente
    }
    private val SUBTITLE_PAINT = Paint().apply {
        textSize = 16f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        color = android.graphics.Color.DKGRAY
    }
    private val SECTION_HEADER_PAINT = Paint().apply {
        textSize = 14f
        isFakeBoldText = true
        color = android.graphics.Color.BLACK
    }
    private val TABLE_HEADER_PAINT = Paint().apply {
        textSize = 10f
        isFakeBoldText = true
        color = android.graphics.Color.BLACK
    }
    private val NORMAL_TEXT_PAINT = Paint().apply {
        textSize = 10f
        color = android.graphics.Color.BLACK
    }
    private val BOLD_TEXT_PAINT = Paint().apply {
        textSize = 11f
        isFakeBoldText = true
        color = android.graphics.Color.BLACK
    }
    private val ITALIC_TEXT_PAINT = Paint().apply {
        textSize = 10f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        color = android.graphics.Color.GRAY
    }
    private val LINE_PAINT = Paint().apply {
        strokeWidth = 1f
        color = android.graphics.Color.GRAY
    }


    // Classe auxiliar para gerenciar o estado da página atual
    private class PageManager(
        val document: PdfDocument,
        val pageInfo: PdfDocument.PageInfo,
        val initialYPos: Float = MARGIN
    ) {
        var currentPage: PdfDocument.Page = document.startPage(pageInfo)
            private set
        var currentCanvas: Canvas = currentPage.canvas
            private set
        var currentY: Float = initialYPos
            private set

        private var newPageCallback: ((Canvas, PageManager) -> Unit)? = null

        fun setNewPageCallback(callback: (Canvas, PageManager) -> Unit) {
            this.newPageCallback = callback
        }

        private fun startNewPage() {
            document.finishPage(currentPage)
            currentPage = document.startPage(pageInfo)
            currentCanvas = currentPage.canvas
            currentY = initialYPos
            newPageCallback?.invoke(currentCanvas, this)
        }

        fun checkAndAddNewPageIfNeeded(spaceNeeded: Float) {
            if (currentY + spaceNeeded > PAGE_HEIGHT_A4 - MARGIN) {
                startNewPage()
            }
        }

        fun advanceY(space: Float) {
            currentY += space
        }

        fun drawText(text: String, x: Float, yOffset: Float, paint: Paint) {
            checkAndAddNewPageIfNeeded(yOffset + paint.textSize) // Verifica espaço antes de desenhar
            currentCanvas.drawText(text, x, currentY + yOffset, paint)
        }

        fun drawTextLine(text: String, x: Float, paint: Paint, lineHeight: Float = LINE_HEIGHT_NORMAL) {
            checkAndAddNewPageIfNeeded(lineHeight)
            // Ajuste para alinhar melhor o texto verticalmente na "linha imaginária" do currentY
            val textY = currentY + (paint.textSize - paint.descent())
            currentCanvas.drawText(text, x, textY, paint)
            currentY += lineHeight
        }

        fun drawTextLineCentered(text: String, paint: Paint, lineHeight: Float = LINE_HEIGHT_NORMAL) {
            paint.textAlign = Paint.Align.CENTER
            drawTextLine(text, pageInfo.pageWidth / 2f, paint, lineHeight)
            paint.textAlign = Paint.Align.LEFT // Reset para o padrão
        }

        fun drawLineSeparator(lineHeight: Float = LINE_HEIGHT_SMALL) {
            checkAndAddNewPageIfNeeded(lineHeight)
            currentCanvas.drawLine(MARGIN, currentY, pageInfo.pageWidth - MARGIN, currentY, LINE_PAINT)
            currentY += lineHeight
        }

        fun finishDocument() {
            document.finishPage(currentPage)
        }
    }

    private fun getPdfOutputDir(context: Context): File {
        val outputDir = File(context.filesDir, "pdfs")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        return outputDir
    }

    // Formata um Double para String com vírgula decimal
    private fun formatCurrency(value: Double): String {
        return "R$ ${String.format(Locale.getDefault(), "%.2f", value).replace('.', ',')}"
    }

    private fun formatQuantity(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.getDefault(), "%.2f", value).replace('.', ',')
    }

    // Função para gerar o PDF Individual do Agricultor
    fun generateAgricultorPdf(
        context: Context,
        resultadoAgricultor: ResultadoAgricultorFeira,
        feiraDetails: FairDetails,
        agricultorInfo: Agricultor? // Agora esta informação é usada
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_A4, PAGE_HEIGHT_A4, 1).create()
        val pm = PageManager(document, pageInfo)

        val nomeAgricultorDisplay = agricultorInfo?.nome ?: "ID: ${resultadoAgricultor.agricultorId}"

        // Callback para redesenhar cabeçalho em nova página
        pm.setNewPageCallback { canvas, pageManager ->
            pageManager.drawTextLineCentered("Relatório do Agricultor (Cont.)", TITLE_PAINT, LINE_HEIGHT_TITLE)
            pageManager.advanceY(5f)
            pageManager.drawTextLineCentered("Feira Nº ${feiraDetails.feiraId} | Agricultor: $nomeAgricultorDisplay", SUBTITLE_PAINT, LINE_HEIGHT_MEDIUM)
            pageManager.advanceY(LINE_HEIGHT_MEDIUM)
            drawAgricultorItensTableHeader(canvas, pageManager)
        }


        // Título do Documento
        pm.drawTextLineCentered("Relatório do Agricultor", TITLE_PAINT, LINE_HEIGHT_TITLE)
        pm.advanceY(5f)
        pm.drawTextLineCentered("Feira Nº ${feiraDetails.feiraId} (${feiraDetails.startDate} a ${feiraDetails.endDate})", SUBTITLE_PAINT, LINE_HEIGHT_MEDIUM)
        pm.advanceY(5f)
        pm.drawTextLineCentered("Agricultor: $nomeAgricultorDisplay", SUBTITLE_PAINT, LINE_HEIGHT_MEDIUM)
        pm.advanceY(LINE_HEIGHT_LARGE)

        // Itens Processados
        pm.drawTextLine("Itens Processados:", MARGIN, SECTION_HEADER_PAINT, LINE_HEIGHT_MEDIUM)
        pm.advanceY(LINE_HEIGHT_SMALL / 2)
        drawAgricultorItensTableHeader(pm.currentCanvas, pm) // Desenha cabeçalho da tabela

        if (resultadoAgricultor.itensProcessados.isEmpty()){
            pm.drawTextLine("Nenhum item processado.", MARGIN + 5f, ITALIC_TEXT_PAINT, LINE_HEIGHT_NORMAL)
        } else {
            resultadoAgricultor.itensProcessados.forEach { item ->
                drawAgricultorItemRow(pm, item)
            }
        }
        pm.drawLineSeparator(LINE_HEIGHT_NORMAL)


        // Resumo final do Agricultor
        pm.advanceY(LINE_HEIGHT_SMALL)
        pm.drawTextLine("Resumo Financeiro:", MARGIN, SECTION_HEADER_PAINT, LINE_HEIGHT_MEDIUM)
        pm.advanceY(LINE_HEIGHT_SMALL / 2)

        val col1ResumoX = MARGIN + 150f // Para alinhar os valores à direita dos rótulos
        val col2ResumoX = PAGE_WIDTH_A4 - MARGIN // Para alinhar à margem direita

        BOLD_TEXT_PAINT.textAlign = Paint.Align.LEFT
        pm.drawTextLine("Total Bruto Agricultor:", MARGIN, BOLD_TEXT_PAINT, LINE_HEIGHT_NORMAL)
        BOLD_TEXT_PAINT.textAlign = Paint.Align.RIGHT
        pm.drawTextLine(formatCurrency(resultadoAgricultor.totalVendidoBrutoAgricultor), col2ResumoX, BOLD_TEXT_PAINT, LINE_HEIGHT_NORMAL)
        BOLD_TEXT_PAINT.textAlign = Paint.Align.LEFT // Reset

        pm.drawTextLine("Valor Cooperativa (30%):", MARGIN, BOLD_TEXT_PAINT, LINE_HEIGHT_NORMAL)
        BOLD_TEXT_PAINT.textAlign = Paint.Align.RIGHT
        pm.drawTextLine(formatCurrency(resultadoAgricultor.valorCooperativa), col2ResumoX, BOLD_TEXT_PAINT, LINE_HEIGHT_NORMAL)
        BOLD_TEXT_PAINT.textAlign = Paint.Align.LEFT

        pm.drawTextLine("Valor Líquido Agricultor (70%):", MARGIN, BOLD_TEXT_PAINT, LINE_HEIGHT_NORMAL)
        BOLD_TEXT_PAINT.textAlign = Paint.Align.RIGHT
        pm.drawTextLine(formatCurrency(resultadoAgricultor.valorLiquidoAgricultor), col2ResumoX, BOLD_TEXT_PAINT, LINE_HEIGHT_NORMAL)
        BOLD_TEXT_PAINT.textAlign = Paint.Align.LEFT

        pm.finishDocument()

        val file = File(getPdfOutputDir(context), "agricultor_${agricultorInfo?.id ?: resultadoAgricultor.agricultorId}_feira_${feiraDetails.feiraId}.pdf")
        try {
            FileOutputStream(file).use { fos -> document.writeTo(fos) }
            println("LOG_PDF: PDF do Agricultor $nomeAgricultorDisplay gerado em ${file.absolutePath}")
            return file
        } catch (e: IOException) {
            println("LOG_PDF_ERROR: Erro ao salvar PDF do Agricultor: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            document.close()
        }
    }

    private fun drawAgricultorItensTableHeader(canvas: Canvas, pm: PageManager) {
        val xProd = MARGIN
        val xEnt = MARGIN + 150f // Ajuste estas larguras conforme necessário
        val xPer = xEnt + 90f
        val xVen = xPer + 90f
        val xVal = xVen + 90f

        TABLE_HEADER_PAINT.textAlign = Paint.Align.LEFT
        pm.drawText("Produto", xProd, 0f, TABLE_HEADER_PAINT) // yOffset é 0 pois pm.drawTextLine cuida do currentY
        TABLE_HEADER_PAINT.textAlign = Paint.Align.RIGHT
        pm.drawText("Entregue", xEnt + 70f, 0f, TABLE_HEADER_PAINT)
        pm.drawText("Perda Aloc.", xPer + 70f, 0f, TABLE_HEADER_PAINT)
        pm.drawText("Vendido", xVen + 70f, 0f, TABLE_HEADER_PAINT)
        pm.drawText("Valor Total", xVal + 70f, 0f, TABLE_HEADER_PAINT) // Era "Valor"
        pm.advanceY(LINE_HEIGHT_HEADER) // Avança Y após desenhar todos os cabeçalhos
        pm.drawLineSeparator(LINE_HEIGHT_SMALL / 2)
        TABLE_HEADER_PAINT.textAlign = Paint.Align.LEFT // Reset
    }

    private fun drawAgricultorItemRow(pm: PageManager, item: ItemProcessadoAgricultor) {
        val xProd = MARGIN
        val xEnt = MARGIN + 150f
        val xPer = xEnt + 90f
        val xVen = xPer + 90f
        val xVal = xVen + 90f

        // Verifica espaço para pelo menos uma linha de item
        pm.checkAndAddNewPageIfNeeded(LINE_HEIGHT_NORMAL)

        // Nome do produto pode quebrar linha se for muito longo
        val productNameLines = splitTextIntoLines(item.produto.item + " (#${item.produto.numero})", xEnt - xProd - 5f, NORMAL_TEXT_PAINT)
        var maxLinesForItem = productNameLines.size

        // Calcula a altura necessária para esta linha inteira, considerando quebra de linha do nome do produto
        val spaceNeededForRow = LINE_HEIGHT_NORMAL * maxLinesForItem
        pm.checkAndAddNewPageIfNeeded(spaceNeededForRow) // Verifica de novo com a altura calculada

        var tempY = pm.currentY
        productNameLines.forEachIndexed { index, line ->
            pm.currentCanvas.drawText(line, xProd, tempY + (index * LINE_HEIGHT_SMALL) + (NORMAL_TEXT_PAINT.textSize - NORMAL_TEXT_PAINT.descent()), NORMAL_TEXT_PAINT)
        }

        NORMAL_TEXT_PAINT.textAlign = Paint.Align.RIGHT
        pm.currentCanvas.drawText("${formatQuantity(item.quantidadeEntregueTotalSemana)} ${item.produto.unidade}", xEnt + 70f, tempY + (NORMAL_TEXT_PAINT.textSize - NORMAL_TEXT_PAINT.descent()), NORMAL_TEXT_PAINT)
        pm.currentCanvas.drawText("${formatQuantity(item.quantidadePerdaAlocada)} ${item.produto.unidade}", xPer + 70f, tempY + (NORMAL_TEXT_PAINT.textSize - NORMAL_TEXT_PAINT.descent()), NORMAL_TEXT_PAINT)
        pm.currentCanvas.drawText("${formatQuantity(item.quantidadeVendida)} ${item.produto.unidade}", xVen + 70f, tempY + (NORMAL_TEXT_PAINT.textSize - NORMAL_TEXT_PAINT.descent()), NORMAL_TEXT_PAINT)
        pm.currentCanvas.drawText(formatCurrency(item.valorTotalVendido), xVal + 70f, tempY + (NORMAL_TEXT_PAINT.textSize - NORMAL_TEXT_PAINT.descent()), NORMAL_TEXT_PAINT)
        NORMAL_TEXT_PAINT.textAlign = Paint.Align.LEFT // Reset

        pm.advanceY(spaceNeededForRow) // Avança o Y pela altura total usada pela linha
    }


    // Função para gerar o PDF Resumo Geral da Feira
    fun generateSummaryPdf(
        context: Context,
        resultadoGeral: ResultadoGeralFeira,
        listaDeAgricultores: List<Agricultor>
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_A4, PAGE_HEIGHT_A4, 1).create()
        val pm = PageManager(document, pageInfo)

        val feiraDetails = resultadoGeral.fairDetails

        // Callback para redesenhar cabeçalho da tabela de resumo em nova página
        val drawSummaryTableHeader = { canvas: Canvas, pageManager: PageManager ->
            val colWidthSummary = (PAGE_WIDTH_A4 - 2 * MARGIN) / 4f
            val sxCol1 = MARGIN
            val sxCol2 = sxCol1 + colWidthSummary
            val sxCol3 = sxCol2 + colWidthSummary
            val sxCol4 = sxCol3 + colWidthSummary

            TABLE_HEADER_PAINT.textAlign = Paint.Align.LEFT
            pageManager.drawText("Agricultor", sxCol1,0f, TABLE_HEADER_PAINT)
            TABLE_HEADER_PAINT.textAlign = Paint.Align.RIGHT
            pageManager.drawText("Vendido Bruto", sxCol2 + colWidthSummary - 10f,0f, TABLE_HEADER_PAINT)
            pageManager.drawText("Cooperativa", sxCol3 + colWidthSummary - 10f,0f, TABLE_HEADER_PAINT)
            pageManager.drawText("Líquido Agr.", sxCol4 + colWidthSummary - 10f,0f, TABLE_HEADER_PAINT)
            pageManager.advanceY(LINE_HEIGHT_HEADER) // Avança Y após desenhar todos os cabeçalhos
            pageManager.drawLineSeparator(LINE_HEIGHT_SMALL / 2)
            TABLE_HEADER_PAINT.textAlign = Paint.Align.LEFT // Reset
        }

        pm.setNewPageCallback { canvas, pageManager ->
            pageManager.drawTextLineCentered("Resumo Geral da Feira (Cont.)", TITLE_PAINT, LINE_HEIGHT_TITLE)
            pageManager.advanceY(5f)
            pageManager.drawTextLineCentered("Feira Nº ${feiraDetails.feiraId}", SUBTITLE_PAINT, LINE_HEIGHT_MEDIUM)
            pageManager.advanceY(LINE_HEIGHT_MEDIUM)
            drawSummaryTableHeader(canvas, pageManager)
        }

        // Título
        pm.drawTextLineCentered("Resumo Geral da Feira", TITLE_PAINT, LINE_HEIGHT_TITLE)
        pm.advanceY(5f)
        pm.drawTextLineCentered("Feira Nº ${feiraDetails.feiraId} (${feiraDetails.startDate} a ${feiraDetails.endDate})", SUBTITLE_PAINT, LINE_HEIGHT_MEDIUM)
        pm.advanceY(LINE_HEIGHT_LARGE)

        // Tabela Resumo por Agricultor
        pm.drawTextLine("Detalhes por Agricultor:", MARGIN, SECTION_HEADER_PAINT, LINE_HEIGHT_MEDIUM)
        pm.advanceY(LINE_HEIGHT_SMALL/2)
        drawSummaryTableHeader(pm.currentCanvas, pm)

        if(resultadoGeral.resultadosPorAgricultor.isEmpty()){
            pm.drawTextLine("Nenhum agricultor com resultados para exibir.", MARGIN + 5f, ITALIC_TEXT_PAINT, LINE_HEIGHT_NORMAL)
        } else {
            val colWidthSummary = (PAGE_WIDTH_A4 - 2 * MARGIN) / 4f
            val sxCol1 = MARGIN
            val sxCol2 = sxCol1 + colWidthSummary
            val sxCol3 = sxCol2 + colWidthSummary
            val sxCol4 = sxCol3 + colWidthSummary

            resultadoGeral.resultadosPorAgricultor.forEach { agricultorRes ->
                val agricultorInfo = listaDeAgricultores.find { it.id == agricultorRes.agricultorId }
                val nomeDisplay = agricultorInfo?.nome ?: "ID: ${agricultorRes.agricultorId}"

                // Nome do agricultor pode quebrar linha
                val nomeLines = splitTextIntoLines(nomeDisplay, colWidthSummary - 5f, NORMAL_TEXT_PAINT)
                var maxLinesForAgrRow = nomeLines.size

                pm.checkAndAddNewPageIfNeeded(LINE_HEIGHT_NORMAL * maxLinesForAgrRow)
                var tempYAgr = pm.currentY

                nomeLines.forEachIndexed {idx, line ->
                    pm.currentCanvas.drawText(line, sxCol1, tempYAgr + (idx * LINE_HEIGHT_SMALL) + (NORMAL_TEXT_PAINT.textSize - NORMAL_TEXT_PAINT.descent()), NORMAL_TEXT_PAINT)
                }

                NORMAL_TEXT_PAINT.textAlign = Paint.Align.RIGHT
                pm.currentCanvas.drawText(formatCurrency(agricultorRes.totalVendidoBrutoAgricultor), sxCol2 + colWidthSummary - 10f, tempYAgr + (NORMAL_TEXT_PAINT.textSize - NORMAL_TEXT_PAINT.descent()), NORMAL_TEXT_PAINT)
                pm.currentCanvas.drawText(formatCurrency(agricultorRes.valorCooperativa), sxCol3 + colWidthSummary - 10f, tempYAgr + (NORMAL_TEXT_PAINT.textSize - NORMAL_TEXT_PAINT.descent()), NORMAL_TEXT_PAINT)
                pm.currentCanvas.drawText(formatCurrency(agricultorRes.valorLiquidoAgricultor), sxCol4 + colWidthSummary - 10f, tempYAgr + (NORMAL_TEXT_PAINT.textSize - NORMAL_TEXT_PAINT.descent()), NORMAL_TEXT_PAINT)
                NORMAL_TEXT_PAINT.textAlign = Paint.Align.LEFT // Reset

                pm.advanceY(LINE_HEIGHT_NORMAL * maxLinesForAgrRow)
            }
        }
        pm.drawLineSeparator(LINE_HEIGHT_NORMAL)

        // Totais Gerais
        pm.advanceY(LINE_HEIGHT_SMALL)
        pm.drawTextLine("Totais Gerais da Feira:", MARGIN, SECTION_HEADER_PAINT, LINE_HEIGHT_MEDIUM)
        pm.advanceY(LINE_HEIGHT_SMALL / 2)

        val col1TotalX = MARGIN + 250f // Para alinhar
        val col2TotalX = PAGE_WIDTH_A4 - MARGIN

        BOLD_TEXT_PAINT.textAlign = Paint.Align.LEFT
        pm.drawTextLine("Total Geral Vendido:", MARGIN, BOLD_TEXT_PAINT, LINE_HEIGHT_NORMAL)
        BOLD_TEXT_PAINT.textAlign = Paint.Align.RIGHT
        pm.drawTextLine(formatCurrency(resultadoGeral.totalGeralVendido), col2TotalX, BOLD_TEXT_PAINT, LINE_HEIGHT_NORMAL)
        BOLD_TEXT_PAINT.textAlign = Paint.Align.LEFT

        pm.drawTextLine("Total para Cooperativa (30%):", MARGIN, BOLD_TEXT_PAINT, LINE_HEIGHT_NORMAL)
        BOLD_TEXT_PAINT.textAlign = Paint.Align.RIGHT
        pm.drawTextLine(formatCurrency(resultadoGeral.totalGeralCooperativa), col2TotalX, BOLD_TEXT_PAINT, LINE_HEIGHT_NORMAL)
        BOLD_TEXT_PAINT.textAlign = Paint.Align.LEFT

        pm.drawTextLine("Total para Agricultores (70%):", MARGIN, BOLD_TEXT_PAINT, LINE_HEIGHT_NORMAL)
        BOLD_TEXT_PAINT.textAlign = Paint.Align.RIGHT
        pm.drawTextLine(formatCurrency(resultadoGeral.totalGeralAgricultores), col2TotalX, BOLD_TEXT_PAINT, LINE_HEIGHT_NORMAL)
        BOLD_TEXT_PAINT.textAlign = Paint.Align.LEFT

        pm.finishDocument()

        val file = File(getPdfOutputDir(context), "resumo_feira_${resultadoGeral.fairDetails.feiraId}.pdf")
        try {
            FileOutputStream(file).use { fos -> document.writeTo(fos) }
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

    // Função auxiliar para quebrar texto em múltiplas linhas se necessário
    private fun splitTextIntoLines(text: String, maxWidth: Float, paint: Paint): List<String> {
        val lines = mutableListOf<String>()
        var start = 0
        var count: Int
        while (start < text.length) {
            count = paint.breakText(text, start, text.length, true, maxWidth, null)
            if (count == 0) { // Não conseguiu encaixar nem um caractere, força a quebra (pode acontecer com palavras muito longas sem espaço)
                if (start < text.length) { // Evita loop infinito se maxWidth for muito pequeno
                    // Tenta encontrar um espaço para quebrar ou quebra a palavra
                    var breakPoint = text.indexOf(' ', start + 1)
                    if (breakPoint == -1 || breakPoint > start + countWhenNoFitChar) { // countWhenNoFitChar é um valor arbitrário, ex: 10
                        breakPoint = start + countWhenNoFitChar.coerceAtMost(text.length - start)
                    }
                    if (breakPoint <= start) breakPoint = start +1 // garante progresso mínimo
                    lines.add(text.substring(start, breakPoint.coerceAtMost(text.length)))
                    start = breakPoint
                    while (start < text.length && text[start] == ' ') start++ // Pula espaços no início da nova linha
                } else {
                    break // Segurança
                }

            } else {
                lines.add(text.substring(start, start + count))
                start += count
            }
        }
        if (lines.isEmpty() && text.isNotEmpty()) lines.add(text) // Garante que algo seja adicionado se o texto original não for vazio
        return lines
    }
    private const val countWhenNoFitChar = 15 // Para splitTextIntoLines, caracteres a tentar quebrar se breakText retornar 0
}