package com.example.gestaofeiracooperativa

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar
import java.util.Locale

// Certifique-se de que estas classes de dados estão acessíveis/importadas
// Se estiverem no mesmo pacote, não precisa de import para elas.
// Ex:
// import com.example.gestaofeiracooperativa.FairDetails
// import com.example.gestaofeiracooperativa.Produto
// import com.example.gestaofeiracooperativa.Agricultor
// import com.example.gestaofeiracooperativa.ResultadoGeralFeira
// import com.example.gestaofeiracooperativa.ResultadoAgricultorFeira
// import com.example.gestaofeiracooperativa.ItemProcessadoAgricultor
// import com.example.gestaofeiracooperativa.ItemDespesaEntity
// import com.example.gestaofeiracooperativa.DespesaFeiraEntity
// import com.example.gestaofeiracooperativa.DespesaFeiraUiItem
// import com.example.gestaofeiracooperativa.diasDaSemanaFeira


object PdfGenerator {

    private val jsonFormat = Json { ignoreUnknownKeys = true; isLenient = true }

    // Constantes de Layout
    private const val PAGE_WIDTH_A4 = 595
    private const val PAGE_HEIGHT_A4 = 842
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH_A4 - 2 * MARGIN
    private const val CELL_PADDING_DEFAULT = 5f

    private const val LINE_HEIGHT_EXTRA_SMALL = 12f
    private const val LINE_HEIGHT_SMALL = 15f
    private const val LINE_HEIGHT_NORMAL = 18f
    private const val LINE_HEIGHT_MEDIUM = 22f
    private const val LINE_HEIGHT_LARGE = 28f
    private const val LINE_HEIGHT_TITLE_MAIN = 32f
    private const val LINE_HEIGHT_TITLE_SECTION = 26f
    private const val LINE_HEIGHT_TABLE_HEADER = 20f

    // Paints (Estilos de Texto)
    private val TITLE_PAINT = Paint().apply {
        textSize = 18f; isFakeBoldText = true; textAlign = Paint.Align.CENTER; color = android.graphics.Color.BLACK
    }
    private val SUBTITLE_PAINT = Paint().apply {
        textSize = 14f; textAlign = Paint.Align.CENTER; color = android.graphics.Color.DKGRAY
    }
    private val SECTION_HEADER_PAINT = Paint().apply {
        textSize = 12f; isFakeBoldText = true; color = android.graphics.Color.BLACK
    }
    private val TABLE_HEADER_PAINT = Paint().apply {
        textSize = 9f; isFakeBoldText = true; color = android.graphics.Color.BLACK
    }
    private val NORMAL_TEXT_PAINT = Paint().apply {
        textSize = 9f; color = android.graphics.Color.BLACK
    }
    private val BOLD_TEXT_PAINT = Paint().apply {
        textSize = 10f; isFakeBoldText = true; color = android.graphics.Color.BLACK
    }
    private val ITALIC_TEXT_PAINT = Paint().apply {
        textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC); color = android.graphics.Color.GRAY
    }
    private val LINE_PAINT = Paint().apply {
        strokeWidth = 0.5f; color = android.graphics.Color.LTGRAY
    }

    private class PageManager(
        private val document: PdfDocument,
        private val pageInfo: PdfDocument.PageInfo,
        private val initialYPos: Float = MARGIN,
        private val headerPageHeight: Float = 0f,
        private val footerPageHeight: Float = 0f
    ) {
        lateinit var currentPage: PdfDocument.Page
        lateinit var currentCanvas: Canvas
        var currentY: Float = 0f; private set
        var pageNumber = 0; private set
        private var onNewPageCallback: ((canvas: Canvas, pageManager: PageManager, pageNum: Int) -> Unit)? = null

        init { startNewPage() }

        fun setNewPageCallback(callback: (Canvas, PageManager, Int) -> Unit) {
            this.onNewPageCallback = callback
            if (pageNumber == 1 && currentY == (initialYPos + headerPageHeight)) {
                this.onNewPageCallback?.invoke(currentCanvas, this, pageNumber)
            }
        }

        private fun startNewPage() {
            if (pageNumber > 0) { document.finishPage(currentPage) }
            pageNumber++; currentPage = document.startPage(pageInfo)
            currentCanvas = currentPage.canvas; currentY = initialYPos + headerPageHeight
            onNewPageCallback?.invoke(currentCanvas, this, pageNumber)
        }

        fun checkAndAddNewPageIfNeeded(spaceNeeded: Float = LINE_HEIGHT_NORMAL) {
            if (currentY + spaceNeeded > PAGE_HEIGHT_A4 - MARGIN - footerPageHeight) { startNewPage() }
        }

        fun advanceY(space: Float) { currentY += space }

        fun drawTextOnLine(text: String, x: Float, paint: Paint, yForTextBaseline: Float) {
            currentCanvas.drawText(text, x, yForTextBaseline, paint)
        }

        fun drawTextAndAdvance(text: String, x: Float, paint: Paint, lineHeight: Float = LINE_HEIGHT_NORMAL, yOffsetForBaseline: Float = 0f) {
            checkAndAddNewPageIfNeeded(lineHeight)
            val textBaselineY = currentY + yOffsetForBaseline + (paint.textSize - paint.descent())
            currentCanvas.drawText(text, x, textBaselineY, paint)
            currentY += lineHeight
        }

        fun drawTextCenteredAndAdvance(text: String, paint: Paint, lineHeight: Float = LINE_HEIGHT_NORMAL, yOffsetForBaseline: Float = 0f) {
            val originalAlign = paint.textAlign; paint.textAlign = Paint.Align.CENTER
            drawTextAndAdvance(text, pageInfo.pageWidth / 2f, paint, lineHeight, yOffsetForBaseline); paint.textAlign = originalAlign
        }

        fun drawLineSeparator(yOffset: Float = LINE_HEIGHT_SMALL / 2, advanceTotal: Float = LINE_HEIGHT_SMALL) {
            checkAndAddNewPageIfNeeded(advanceTotal + yOffset); val lineY = currentY + yOffset
            currentCanvas.drawLine(MARGIN, lineY, pageInfo.pageWidth - MARGIN, lineY, LINE_PAINT); currentY += advanceTotal
        }

        private fun drawPageFooter(context: Context, titleHint: String?) {
            val originalPaintSize = ITALIC_TEXT_PAINT.textSize
            val originalPaintColor = ITALIC_TEXT_PAINT.color
            ITALIC_TEXT_PAINT.textSize = 8f
            ITALIC_TEXT_PAINT.color = android.graphics.Color.DKGRAY
            ITALIC_TEXT_PAINT.textAlign = Paint.Align.CENTER
            // Tenta pegar o nome do app de R.string.app_name. Se não existir, usa um padrão.
            val appName = try { context.getString(R.string.app_name) } catch (e: Exception) { "Gestão Cooperativa" }
            val footerText = "$appName | ${titleHint ?: "Relatório"} | Página $pageNumber"
            currentCanvas.drawText(footerText, pageInfo.pageWidth / 2f, PAGE_HEIGHT_A4 - (MARGIN / 2) - 5f, ITALIC_TEXT_PAINT)
            ITALIC_TEXT_PAINT.textSize = originalPaintSize
            ITALIC_TEXT_PAINT.color = originalPaintColor
            ITALIC_TEXT_PAINT.textAlign = Paint.Align.LEFT
        }

        fun finishDocument(context: Context, titleHintForFooter: String?) {
            drawPageFooter(context, titleHintForFooter); document.finishPage(currentPage)
        }
    }

    private fun getPdfOutputDir(context: Context): File {
        val outputDir = File(context.filesDir, "pdfs"); if (!outputDir.exists()) { outputDir.mkdirs() }; return outputDir
    }

    private fun formatCurrency(value: Double, incluirSimbolo: Boolean = true): String {
        val prefix = if (incluirSimbolo) "R$ " else ""
        return prefix + String.format(Locale("pt", "BR"), "%.2f", value)
    }

    private fun formatQuantity(value: Double): String {
        return if (value == 0.0) "-" else if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale("pt", "BR"), "%.2f", value)
    }

    private fun splitTextIntoLines(text: String, maxWidth: Float, paint: Paint, maxCharsForceBreak: Int = 20): List<String> {
        val lines = mutableListOf<String>()
        var currentStart = 0
        if (text.isEmpty()) return listOf("")

        while (currentStart < text.length) {
            val count = paint.breakText(text, currentStart, text.length, true, maxWidth, null)
            if (count > 0) {
                lines.add(text.substring(currentStart, currentStart + count))
                currentStart += count
            } else {
                if (currentStart >= text.length) break
                val remaining = text.substring(currentStart)
                var breakPoint = remaining.indexOf(' ')
                if (breakPoint == -1 || breakPoint == 0 || breakPoint > maxCharsForceBreak) {
                    breakPoint = maxCharsForceBreak.coerceAtMost(remaining.length)
                }
                if (breakPoint == 0 && remaining.isNotEmpty()) breakPoint = 1

                lines.add(remaining.substring(0, breakPoint))
                currentStart += breakPoint
            }
            while (currentStart < text.length && text[currentStart] == ' ') {
                currentStart++
            }
        }
        return if (lines.isEmpty()) listOf("") else lines
    }

    private fun drawPdfTableRow(
        pm: PageManager,
        rowData: List<String>,
        xPositions: List<Float>,
        columnWidths: List<Float>,
        alignments: List<Paint.Align>,
        paints: List<Paint>, // Lista de Paints, um para cada célula
        rowBaseLineHeight: Float = LINE_HEIGHT_NORMAL,
        firstColumnTextWrap: Boolean = false
    ) {
        if (rowData.size != xPositions.size || rowData.size != columnWidths.size || rowData.size != alignments.size || rowData.size != paints.size) {
            Log.e("PdfGenerator", "drawPdfTableRow: Mismatch in array sizes. Data:${rowData.size}, XPos:${xPositions.size}, Widths:${columnWidths.size}, Align:${alignments.size}, Paints:${paints.size}")
            return
        }

        val textLinesForColumns = mutableListOf<List<String>>()
        var maxSubLines = 1

        rowData.forEachIndexed { index, text ->
            val currentPaint = paints[index]
            val lines = if (index == 0 && firstColumnTextWrap) {
                splitTextIntoLines(text, columnWidths[index] - 2 * CELL_PADDING_DEFAULT, currentPaint)
            } else {
                listOf(text)
            }
            textLinesForColumns.add(lines)
            if (lines.size > maxSubLines) {
                maxSubLines = lines.size
            }
        }

        val actualRowHeight = (if (maxSubLines > 1) LINE_HEIGHT_SMALL else rowBaseLineHeight) * maxSubLines
        pm.checkAndAddNewPageIfNeeded(actualRowHeight)

        val startYOfRow = pm.currentY

        for (lineIdx in 0 until maxSubLines) {
            val currentSubLineBaseY = startYOfRow + (if (lineIdx > 0) LINE_HEIGHT_SMALL * lineIdx else 0f) + (paints.first().textSize - paints.first().descent())

            rowData.forEachIndexed { colIndex, _ ->
                val textToDraw = textLinesForColumns[colIndex].getOrNull(lineIdx) ?: ""
                val currentPaint = paints[colIndex] // Usa o Paint específico para esta célula
                val originalAlign = currentPaint.textAlign
                currentPaint.textAlign = alignments[colIndex]

                val xDrawPos = if (currentPaint.textAlign == Paint.Align.LEFT) {
                    xPositions[colIndex] + CELL_PADDING_DEFAULT
                } else { // RIGHT or CENTER
                    xPositions[colIndex] + columnWidths[colIndex] - CELL_PADDING_DEFAULT
                }
                pm.currentCanvas.drawText(textToDraw, xDrawPos, currentSubLineBaseY, currentPaint)
                currentPaint.textAlign = originalAlign
            }
        }
        pm.advanceY(actualRowHeight)
    }

    // --- PDF INDIVIDUAL DO AGRICULTOR ---
    fun generateAgricultorPdf(
        context: Context,
        resultadoAgricultor: ResultadoAgricultorFeira,
        feiraDetails: FairDetails,
        agricultorInfo: Agricultor?
    ): File {
        val document = PdfDocument(); val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_A4, PAGE_HEIGHT_A4, 1).create()
        val pm = PageManager(document, pageInfo, headerPageHeight = 80f, footerPageHeight = 30f)
        val nomeAgricultorDisplay = agricultorInfo?.nome ?: "ID: ${resultadoAgricultor.agricultorId}"; val feiraId = feiraDetails.feiraId

        val colWidths = listOf(CONTENT_WIDTH * 0.35f, CONTENT_WIDTH * 0.17f, CONTENT_WIDTH * 0.16f, CONTENT_WIDTH * 0.16f, CONTENT_WIDTH * 0.16f)
        val xPos = mutableListOf(MARGIN).apply { for(i in 0 until colWidths.size -1) add(last() + colWidths[i]) }
        val alignments = listOf(Paint.Align.LEFT) + List(4) { Paint.Align.RIGHT }
        val headerTexts = listOf("Produto (#Nº)", "Entregue", "Perda Aloc.", "Vendido", "Valor Total")
        val headerPaints = List(headerTexts.size) { TABLE_HEADER_PAINT }

        pm.setNewPageCallback { _, pageMgr, pageNum ->
            pageMgr.drawTextCenteredAndAdvance("Relatório do Agricultor" + if (pageNum > 1) " (Cont.)" else "", TITLE_PAINT, LINE_HEIGHT_TITLE_SECTION, yOffsetForBaseline = if (pageNum > 1) LINE_HEIGHT_SMALL else 0f)
            pageMgr.drawTextCenteredAndAdvance("Feira Nº ${feiraDetails.feiraId} (${feiraDetails.startDate} a ${feiraDetails.endDate})", SUBTITLE_PAINT, LINE_HEIGHT_MEDIUM)
            pageMgr.drawTextCenteredAndAdvance("Agricultor: $nomeAgricultorDisplay", SUBTITLE_PAINT, LINE_HEIGHT_MEDIUM)
            pageMgr.advanceY(LINE_HEIGHT_MEDIUM / 2)
            pageMgr.drawTextAndAdvance("Itens Processados:", MARGIN, SECTION_HEADER_PAINT, LINE_HEIGHT_MEDIUM)
            pageMgr.advanceY(LINE_HEIGHT_EXTRA_SMALL)
            drawPdfTableRow(pageMgr, headerTexts, xPos, colWidths, alignments, headerPaints, rowBaseLineHeight = LINE_HEIGHT_TABLE_HEADER, firstColumnTextWrap = true)
            pageMgr.drawLineSeparator(LINE_HEIGHT_SMALL / 2)
        }

        if (resultadoAgricultor.itensProcessados.isEmpty()){
            pm.drawTextAndAdvance("Nenhum item processado.", MARGIN + CELL_PADDING_DEFAULT, ITALIC_TEXT_PAINT)
        } else {
            resultadoAgricultor.itensProcessados.forEach { item ->
                // <<< ALTERAÇÃO: Constrói a linha com o novo 'contribuicaoTotal' >>>
                val contribuicaoTotalTexto = "${formatQuantity(item.contribuicaoTotal)} ${item.produto.unidade}"

                // <<< NOVO: Texto de detalhe para mostrar a composição >>>
                val detalheContribuicao = if (item.quantidadeSobraAnterior > 0)
                    "(Sobra: ${formatQuantity(item.quantidadeSobraAnterior)} + Semana: ${formatQuantity(item.quantidadeEntradaSemana)})"
                else ""

                val rowData = listOf(
                    "${item.produto.item} (#${item.produto.numero})",
                    contribuicaoTotalTexto,
                    formatQuantity(item.quantidadePerdaAlocada),
                    formatQuantity(item.quantidadeVendida),
                    formatCurrency(item.valorTotalVendido)
                )
                val dataPaints = listOf(NORMAL_TEXT_PAINT, NORMAL_TEXT_PAINT, NORMAL_TEXT_PAINT, NORMAL_TEXT_PAINT, BOLD_TEXT_PAINT)
                drawPdfTableRow(pm, rowData, xPos, colWidths, alignments, dataPaints, firstColumnTextWrap = true)

                // Adiciona a linha de detalhe se houver sobra
                if (detalheContribuicao.isNotEmpty()) {
                    // Desenha o detalhe abaixo da linha principal, na coluna de "Total Entregue"
                    pm.drawTextAndAdvance(detalheContribuicao, xPos[1] + CELL_PADDING_DEFAULT, ITALIC_TEXT_PAINT, lineHeight = LINE_HEIGHT_SMALL, yOffsetForBaseline = -LINE_HEIGHT_EXTRA_SMALL/2)
                }
            }
        }
        pm.drawLineSeparator()

        pm.advanceY(LINE_HEIGHT_SMALL)
        pm.drawTextAndAdvance("Resumo Financeiro:", MARGIN, SECTION_HEADER_PAINT, LINE_HEIGHT_MEDIUM)
        pm.advanceY(LINE_HEIGHT_SMALL / 2)

        val xLabel = MARGIN
        val xValue = CONTENT_WIDTH + MARGIN - CELL_PADDING_DEFAULT
        val summaryItems = listOf(
            "Total Bruto Agricultor:" to resultadoAgricultor.totalVendidoBrutoAgricultor,
            "Valor Cooperativa (30%):" to resultadoAgricultor.valorCooperativa,
            "Valor Líquido Agricultor (70%):" to resultadoAgricultor.valorLiquidoAgricultor
        )
        summaryItems.forEach { (label, value) ->
            pm.checkAndAddNewPageIfNeeded(LINE_HEIGHT_NORMAL)
            val yForRow = pm.currentY // Salva o Y antes de desenhar os textos da linha
            pm.drawTextOnLine(label, xLabel, BOLD_TEXT_PAINT, yForTextBaseline = yForRow + (BOLD_TEXT_PAINT.textSize - BOLD_TEXT_PAINT.descent()))
            val originalAlign = BOLD_TEXT_PAINT.textAlign
            BOLD_TEXT_PAINT.textAlign = Paint.Align.RIGHT
            pm.drawTextOnLine(formatCurrency(value), xValue, BOLD_TEXT_PAINT, yForTextBaseline = yForRow + (BOLD_TEXT_PAINT.textSize - BOLD_TEXT_PAINT.descent()))
            BOLD_TEXT_PAINT.textAlign = originalAlign
            pm.advanceY(LINE_HEIGHT_NORMAL)
        }

        pm.finishDocument(context, "Feira ${feiraId} - ${nomeAgricultorDisplay}")
        val file = File(getPdfOutputDir(context), "Relatorio_Agricultor_${agricultorInfo?.id ?: resultadoAgricultor.agricultorId}_Feira_${feiraDetails.feiraId}.pdf")
        try { FileOutputStream(file).use { fos -> document.writeTo(fos) }; return file }
        catch (e: IOException) { e.printStackTrace(); throw e }
        finally { document.close() }
    }


    // --- PDF RESUMO GERAL DA FEIRA ---
    fun generateSummaryPdf(
        context: Context,
        resultadoGeral: ResultadoGeralFeira,
        listaDeAgricultores: List<Agricultor>
    ): File {
        val document = PdfDocument(); val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_A4, PAGE_HEIGHT_A4, 1).create()
        val pm = PageManager(document, pageInfo, headerPageHeight = 90f, footerPageHeight = 20f)
        val feiraDetails = resultadoGeral.fairDetails; val feiraId = feiraDetails.feiraId

        val colWidths = listOf(CONTENT_WIDTH * 0.40f, CONTENT_WIDTH * 0.20f, CONTENT_WIDTH * 0.20f, CONTENT_WIDTH * 0.20f)
        val xPos = mutableListOf(MARGIN).apply { for(i in 0 until colWidths.size -1) add(last() + colWidths[i]) }
        val alignments = listOf(Paint.Align.LEFT) + List(3) { Paint.Align.RIGHT }
        val headerTexts = listOf("Agricultor", "Vendido Bruto", "Cooperativa (30%)", "Líquido Agr. (70%)")
        val headerPaints = List(headerTexts.size) { TABLE_HEADER_PAINT }

        pm.setNewPageCallback { _, pageMgr, pageNum ->
            pageMgr.drawTextCenteredAndAdvance("Resumo Geral da Feira" + if (pageNum > 1) " (Cont.)" else "", TITLE_PAINT, LINE_HEIGHT_TITLE_SECTION, yOffsetForBaseline = if (pageNum > 1) LINE_HEIGHT_SMALL else 0f)
            pageMgr.drawTextCenteredAndAdvance("Feira Nº ${feiraDetails.feiraId} (${feiraDetails.startDate} a ${feiraDetails.endDate})", SUBTITLE_PAINT, LINE_HEIGHT_MEDIUM)
            pageMgr.advanceY(LINE_HEIGHT_MEDIUM/2)
            pageMgr.drawTextAndAdvance("Detalhes por Agricultor:", MARGIN, SECTION_HEADER_PAINT, LINE_HEIGHT_MEDIUM)
            pageMgr.advanceY(LINE_HEIGHT_EXTRA_SMALL)
            drawPdfTableRow(pageMgr, headerTexts, xPos, colWidths, alignments, headerPaints, rowBaseLineHeight = LINE_HEIGHT_TABLE_HEADER, firstColumnTextWrap = true)
            pageMgr.drawLineSeparator(LINE_HEIGHT_SMALL / 2)
        }

        if (resultadoGeral.resultadosPorAgricultor.isEmpty()) { pm.drawTextAndAdvance("Nenhum agricultor com resultados.", MARGIN + CELL_PADDING_DEFAULT, ITALIC_TEXT_PAINT) } else {
            resultadoGeral.resultadosPorAgricultor.forEach { agricultorRes ->
                val agricultorInfo = listaDeAgricultores.find { it.id == agricultorRes.agricultorId }
                val nomeDisplay = agricultorInfo?.nome ?: "ID: ${agricultorRes.agricultorId}"
                val rowData = listOf(nomeDisplay, formatCurrency(agricultorRes.totalVendidoBrutoAgricultor), formatCurrency(agricultorRes.valorCooperativa), formatCurrency(agricultorRes.valorLiquidoAgricultor))
                val dataPaints = List(rowData.size) { NORMAL_TEXT_PAINT }
                drawPdfTableRow(pm, rowData, xPos, colWidths, alignments, dataPaints, rowBaseLineHeight = LINE_HEIGHT_NORMAL, firstColumnTextWrap = true)
            }
        }
        pm.drawLineSeparator()
        pm.advanceY(LINE_HEIGHT_SMALL) // Um espaço menor após a linha
        pm.drawTextAndAdvance("Totais Gerais da Feira:", MARGIN, SECTION_HEADER_PAINT, LINE_HEIGHT_MEDIUM) // Desenha o título da seção e avança
        pm.advanceY(LINE_HEIGHT_SMALL / 2) // Mais um pequeno espaço

        val xLabel = MARGIN
        val xValue = CONTENT_WIDTH + MARGIN - CELL_PADDING_DEFAULT // Para alinhar valor à direita

        // <<< CÓDIGO COMPLETO PARA OS TOTAIS GERAIS >>>
        val summaryTotalsData = listOf(
            "Total Geral Vendido:" to resultadoGeral.totalGeralVendido,
            "Total para Cooperativa (30%):" to resultadoGeral.totalGeralCooperativa,
            "Total para Agricultores (70%):" to resultadoGeral.totalGeralAgricultores
        )

        summaryTotalsData.forEach { (label, value) ->
            pm.checkAndAddNewPageIfNeeded(LINE_HEIGHT_NORMAL) // Verifica espaço para a linha
            val yParaEstaLinha = pm.currentY // Pega o Y atual para desenhar ambos os textos na mesma altura

            // Desenha o Label (à esquerda)
            val originalAlignLabel = BOLD_TEXT_PAINT.textAlign
            BOLD_TEXT_PAINT.textAlign = Paint.Align.LEFT
            pm.drawTextOnLine(label, xLabel, BOLD_TEXT_PAINT, yForTextBaseline = yParaEstaLinha + (BOLD_TEXT_PAINT.textSize - BOLD_TEXT_PAINT.descent()))
            BOLD_TEXT_PAINT.textAlign = originalAlignLabel // Restaura alinhamento (embora drawTextOnLine não o modifique permanentemente)

            // Desenha o Valor (à direita)
            val originalAlignValue = BOLD_TEXT_PAINT.textAlign
            BOLD_TEXT_PAINT.textAlign = Paint.Align.RIGHT
            pm.drawTextOnLine(formatCurrency(value), xValue, BOLD_TEXT_PAINT, yForTextBaseline = yParaEstaLinha + (BOLD_TEXT_PAINT.textSize - BOLD_TEXT_PAINT.descent()))
            BOLD_TEXT_PAINT.textAlign = originalAlignValue // Restaura alinhamento

            pm.advanceY(LINE_HEIGHT_NORMAL) // Avança para a próxima linha
        }


        pm.finishDocument(context, "Feira ${feiraId}")
        val file = File(getPdfOutputDir(context), "Resumo_Geral_Feira_${feiraDetails.feiraId}.pdf")
        try { FileOutputStream(file).use { fos -> document.writeTo(fos) }; return file }
        catch (e: IOException) { e.printStackTrace(); throw e }
        finally { document.close() }
    }

    // --- PDF DE DESPESAS DA FEIRA ---
    fun generateDespesasFeiraPdf(
        context: Context,
        fairDetails: FairDetails,
        despesasDaFeira: List<DespesaFeiraUiItem>
    ): File {
        val document = PdfDocument(); val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_A4, PAGE_HEIGHT_A4, 1).create()
        val pm = PageManager(document, pageInfo, headerPageHeight = 70f, footerPageHeight = 20f)
        val feiraId = fairDetails.feiraId; val pdfFooterId = "DespFeira-$feiraId"

        val numDias = diasDaSemanaFeira.size.coerceAtLeast(1)
        val colItemWidth = CONTENT_WIDTH * 0.25f; val colObsWidth = CONTENT_WIDTH * 0.20f
        val colDiaWidth = (CONTENT_WIDTH * 0.45f) / numDias; val colTotalItemWidth = CONTENT_WIDTH * 0.10f

        val xPos = mutableListOf(MARGIN); val colWidths = mutableListOf(colItemWidth)
        xPos.add(xPos.last() + colWidths.last()); colWidths.add(colObsWidth)
        repeat(numDias) { xPos.add(xPos.last() + colWidths.last()); colWidths.add(colDiaWidth) }
        xPos.add(xPos.last() + colWidths.last()); colWidths.add(colTotalItemWidth)

        val headerTexts = listOf("Item Despesa", "Obs.") + diasDaSemanaFeira.map { it.take(3).uppercase() } + listOf("Total")
        val alignments = listOf(Paint.Align.LEFT, Paint.Align.LEFT) + List(numDias + 1) { Paint.Align.RIGHT }
        val headerPaints = List(headerTexts.size) { TABLE_HEADER_PAINT }

        pm.setNewPageCallback { _, pageMgr, pageNum ->
            pageMgr.drawTextCenteredAndAdvance("Despesas da Feira" + if (pageNum > 1) " (Cont.)" else "", TITLE_PAINT, LINE_HEIGHT_TITLE_SECTION, yOffsetForBaseline = if (pageNum > 1) LINE_HEIGHT_SMALL else 0f)
            pageMgr.drawTextCenteredAndAdvance("Feira Nº ${fairDetails.feiraId} (${fairDetails.startDate} - ${fairDetails.endDate})", SUBTITLE_PAINT, LINE_HEIGHT_MEDIUM)
            pageMgr.advanceY(LINE_HEIGHT_MEDIUM / 2)
            drawPdfTableRow(pageMgr, headerTexts, xPos, colWidths, alignments, headerPaints, rowBaseLineHeight = LINE_HEIGHT_TABLE_HEADER, firstColumnTextWrap = true)
            pageMgr.drawLineSeparator(LINE_HEIGHT_SMALL / 2)
        }

        if (despesasDaFeira.isEmpty()) { pm.drawTextAndAdvance("Nenhuma despesa lançada para esta feira.", MARGIN + CELL_PADDING_DEFAULT, ITALIC_TEXT_PAINT) } else {
            var totalGeralFeira = 0.0
            despesasDaFeira.sortedBy { it.itemDespesa.nome }.forEach { despesaItemUi ->
                var totalItem = 0.0
                val dailyValuesText = diasDaSemanaFeira.map { dia ->
                    val valorDouble = despesaItemUi.valoresPorDiaInput[dia]?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
                    totalItem += valorDouble
                    formatQuantity(valorDouble)
                }
                totalGeralFeira += totalItem
                val rowData = listOf(despesaItemUi.itemDespesa.nome, despesaItemUi.observacaoInput) + dailyValuesText + listOf(formatQuantity(totalItem))
                val dataPaints = listOf(NORMAL_TEXT_PAINT, ITALIC_TEXT_PAINT) + List(numDias) { NORMAL_TEXT_PAINT } + listOf(BOLD_TEXT_PAINT)
                drawPdfTableRow(pm, rowData, xPos, colWidths, alignments, dataPaints, rowBaseLineHeight = LINE_HEIGHT_NORMAL, firstColumnTextWrap = true)
            }
            pm.drawLineSeparator(); pm.advanceY(LINE_HEIGHT_SMALL)
            // ... (Total Geral como antes)
            val xLabelTotal = xPos[xPos.size-2] + colWidths[colWidths.size-2] - CELL_PADDING_DEFAULT - 120f
            val xValueTotal = xPos.last() + colWidths.last() - CELL_PADDING_DEFAULT
            val yTotal = pm.currentY
            val originalAlign = BOLD_TEXT_PAINT.textAlign
            BOLD_TEXT_PAINT.textAlign = Paint.Align.RIGHT
            pm.drawTextOnLine("TOTAL DESPESAS DA FEIRA:", xLabelTotal, BOLD_TEXT_PAINT, yForTextBaseline = yTotal + (BOLD_TEXT_PAINT.textSize - BOLD_TEXT_PAINT.descent()))
            pm.drawTextOnLine(formatCurrency(totalGeralFeira), xValueTotal, BOLD_TEXT_PAINT, yForTextBaseline = yTotal + (BOLD_TEXT_PAINT.textSize - BOLD_TEXT_PAINT.descent()))
            BOLD_TEXT_PAINT.textAlign = originalAlign
            pm.advanceY(LINE_HEIGHT_MEDIUM)
        }
        pm.finishDocument(context, pdfFooterId)
        val file = File(getPdfOutputDir(context), "Despesas_Feira_${feiraId}.pdf")
        try { FileOutputStream(file).use { fos -> document.writeTo(fos) }; return file }
        catch (e: IOException) { e.printStackTrace(); throw e }
        finally { document.close() }
    }

    // --- PDF DE DESPESAS MENSAIS (CONSOLIDADO) ---
    fun generateDespesasMensaisPdf(
        context: Context, ano: Int, mes: Int,
        feirasDasSemanasDoMes: List<FairDetails>,
        despesasDeCadaFeiraDoMes: Map<String, List<DespesaFeiraEntity>>,
        todosOsItensDeDespesa: List<ItemDespesaEntity>
    ): File {
        val document = PdfDocument(); val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_A4, PAGE_HEIGHT_A4, 1).create()
        val pm = PageManager(document, pageInfo, headerPageHeight = 90f, footerPageHeight = 20f) // Aumentado headerPageHeight
        val nomeMesDisplay = Calendar.getInstance().apply { set(Calendar.MONTH, mes - 1) }.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "BR"))?.replaceFirstChar { it.titlecase(Locale("pt", "BR")) } ?: "Mês $mes"
        val pdfFooterId = "DespMensal-${ano}-${String.format("%02d", mes)}"

        val colItemWidth = CONTENT_WIDTH * 0.28f; val colWeekWidth = (CONTENT_WIDTH * 0.60f) / 5f; val colTotalMesWidth = CONTENT_WIDTH * 0.12f
        val xPos = mutableListOf(MARGIN).apply { add(last() + colItemWidth); repeat(5) { add(last() + colWeekWidth) } } // 7 xPos (Item, S1-S5, Total)
        val colWidths = listOf(colItemWidth) + List(5){colWeekWidth} + listOf(colTotalMesWidth) // 7 larguras
        val alignments = listOf(Paint.Align.LEFT) + List(6) { Paint.Align.RIGHT } // 7 alinhamentos

        val weekHeaderTexts = feirasDasSemanasDoMes.take(5).mapIndexed { index, fair -> "Sem ${index + 1}\n(${fair.feiraId.take(6)})" }.toMutableList()
        while(weekHeaderTexts.size < 5) { weekHeaderTexts.add("Sem ${weekHeaderTexts.size + 1}") }
        val headerTexts = listOf("Item de Despesa") + weekHeaderTexts + listOf("Total Mês") // 7 cabeçalhos
        val headerPaints = List(headerTexts.size) { TABLE_HEADER_PAINT } // 7 paints

        pm.setNewPageCallback { _, pageMgr, pageNum ->
            pageMgr.drawTextCenteredAndAdvance("Relatório de Despesas Mensais" + if (pageNum > 1) " (Cont.)" else "", TITLE_PAINT, LINE_HEIGHT_TITLE_SECTION, yOffsetForBaseline = if (pageNum > 1) LINE_HEIGHT_SMALL else 0f)
            pageMgr.drawTextCenteredAndAdvance("$nomeMesDisplay de $ano", SUBTITLE_PAINT, LINE_HEIGHT_MEDIUM)
            pageMgr.advanceY(LINE_HEIGHT_MEDIUM / 2)
            // Passando isHeaderMultiLine = true para drawPdfTableRow se os cabeçalhos de semana podem ter quebra de linha.
            drawPdfTableRow(pageMgr, headerTexts, xPos, colWidths, alignments, headerPaints, rowBaseLineHeight = LINE_HEIGHT_TABLE_HEADER, firstColumnTextWrap = true)
            pageMgr.drawLineSeparator(LINE_HEIGHT_SMALL / 2)
        }

        if (todosOsItensDeDespesa.isEmpty()) { /* ... */ } else {
            var totalGeralDoMesInteiro = 0.0
            todosOsItensDeDespesa.sortedBy { it.nome }.forEach { itemDespesa ->
                val valoresSemanaisParaPdf = DoubleArray(5) { 0.0 }; var totalItemEsteMes = 0.0
                feirasDasSemanasDoMes.take(5).forEachIndexed { indexSemanaPdf, feiraInfo ->
                    // ... (lógica de cálculo de valoresSemanaisParaPdf e totalItemEsteMes como antes) ...
                    val despesasDaFeiraParaEsteItem = despesasDeCadaFeiraDoMes[feiraInfo.feiraId]?.find { it.itemDespesaId == itemDespesa.id }
                    if (despesasDaFeiraParaEsteItem != null) {
                        val mapaValoresDiarios = try { jsonFormat.decodeFromString<Map<String, Double>>(despesasDaFeiraParaEsteItem.valoresPorDiaJson) } catch (e: Exception) { emptyMap() }
                        val totalDaSemanaDaFeira = mapaValoresDiarios.values.sum()
                        valoresSemanaisParaPdf[indexSemanaPdf] = totalDaSemanaDaFeira
                        totalItemEsteMes += totalDaSemanaDaFeira
                    }
                }
                totalGeralDoMesInteiro += totalItemEsteMes
                val rowData = listOf(itemDespesa.nome) + valoresSemanaisParaPdf.map { formatQuantity(it) } + listOf(formatQuantity(totalItemEsteMes))
                val dataPaints = listOf(NORMAL_TEXT_PAINT) + List(5){NORMAL_TEXT_PAINT} + listOf(BOLD_TEXT_PAINT)
                drawPdfTableRow(pm, rowData, xPos, colWidths, alignments, dataPaints, rowBaseLineHeight = LINE_HEIGHT_NORMAL, firstColumnTextWrap = true)
            }
            pm.drawLineSeparator(); pm.advanceY(LINE_HEIGHT_SMALL)
            // ... (Total Geral como antes) ...
            val xLabelTotal = xPos[5] + colWidths[5] - CELL_PADDING_DEFAULT - 120f
            val xValueTotal = xPos.last() + colWidths.last() - CELL_PADDING_DEFAULT
            pm.checkAndAddNewPageIfNeeded(LINE_HEIGHT_MEDIUM)
            val yTotalGeral = pm.currentY
            val originalAlign = BOLD_TEXT_PAINT.textAlign
            BOLD_TEXT_PAINT.textAlign = Paint.Align.RIGHT
            pm.drawTextOnLine("TOTAL GERAL DO MÊS:", xLabelTotal, BOLD_TEXT_PAINT, yForTextBaseline = yTotalGeral + (BOLD_TEXT_PAINT.textSize - BOLD_TEXT_PAINT.descent()))
            pm.drawTextOnLine(formatCurrency(totalGeralDoMesInteiro), xValueTotal, BOLD_TEXT_PAINT, yForTextBaseline = yTotalGeral + (BOLD_TEXT_PAINT.textSize - BOLD_TEXT_PAINT.descent()))
            BOLD_TEXT_PAINT.textAlign = originalAlign
            pm.advanceY(LINE_HEIGHT_MEDIUM)
        }
        pm.finishDocument(context, pdfFooterId)
        val file = File(getPdfOutputDir(context), "Despesas_Mensais_${ano}_${String.format("%02d", mes)}.pdf")
        try { FileOutputStream(file).use { fos -> document.writeTo(fos) }; return file }
        catch (e: IOException) { e.printStackTrace(); throw e }
        finally { document.close() }
    }
}