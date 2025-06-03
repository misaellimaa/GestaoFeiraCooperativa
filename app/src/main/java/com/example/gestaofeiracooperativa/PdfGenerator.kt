package com.example.gestaofeiracooperativa

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log // Para logs, se necessário
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar
import java.util.Locale

// Certifique-se de que estas classes de dados estão acessíveis/importadas
// Ex: import com.example.gestaofeiracooperativa.FairDetails
// Se estiverem no mesmo pacote, não precisa.

object PdfGenerator {

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
    private const val LINE_HEIGHT_TITLE = 32f
    private const val LINE_HEIGHT_TABLE_HEADER = 20f // Renomeado de LINE_HEIGHT_HEADER

    // Paints (Estilos de Texto)
    private val TITLE_PAINT = Paint().apply {
        textSize = 18f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        color = android.graphics.Color.BLACK
    }
    private val SUBTITLE_PAINT = Paint().apply {
        textSize = 14f
        textAlign = Paint.Align.CENTER
        color = android.graphics.Color.DKGRAY
    }
    private val SECTION_HEADER_PAINT = Paint().apply {
        textSize = 12f
        isFakeBoldText = true
        color = android.graphics.Color.BLACK
    }
    private val TABLE_HEADER_PAINT = Paint().apply {
        textSize = 9f
        isFakeBoldText = true
        color = android.graphics.Color.BLACK
    }
    private val NORMAL_TEXT_PAINT = Paint().apply {
        textSize = 9f
        color = android.graphics.Color.BLACK
    }
    private val BOLD_TEXT_PAINT = Paint().apply { // Usado para totais em tabelas e resumos
        textSize = 10f
        isFakeBoldText = true
        color = android.graphics.Color.BLACK
    }
    private val ITALIC_TEXT_PAINT = Paint().apply {
        textSize = 9f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        color = android.graphics.Color.GRAY
    }
    private val LINE_PAINT = Paint().apply {
        strokeWidth = 0.5f
        color = android.graphics.Color.LTGRAY
    }

    private class PageManager(
        private val document: PdfDocument,
        private val pageInfo: PdfDocument.PageInfo,
        private val initialYPos: Float = MARGIN, // Corrigido: 'val' para ser propriedade
        private val headerPageHeight: Float = 0f, // Renomeado de headerHeight para clareza
        private val footerPageHeight: Float = 0f  // Renomeado de footerHeight para clareza
    ) {
        lateinit var currentPage: PdfDocument.Page
        lateinit var currentCanvas: Canvas
        var currentY: Float = 0f
            private set
        var pageNumber = 0
            private set

        private var onNewPageCallback: ((Canvas, PageManager, Int) -> Unit)? = null

        init {
            startNewPage() // Inicia a primeira página e chama o callback
        }

        fun setNewPageCallback(callback: (Canvas, PageManager, Int) -> Unit) {
            this.onNewPageCallback = callback
            // Chama para a primeira página se já foi iniciada e o Y está no início
            if (pageNumber == 1 && currentY == initialYPos) {
                this.onNewPageCallback?.invoke(currentCanvas, this, pageNumber)
            }
        }

        private fun startNewPage() {
            if (pageNumber > 0) { // Só finaliza se não for a primeira vez (no init)
                document.finishPage(currentPage)
            }
            pageNumber++
            currentPage = document.startPage(pageInfo)
            currentCanvas = currentPage.canvas
            currentY = initialYPos
            onNewPageCallback?.invoke(currentCanvas, this, pageNumber)
        }

        fun checkAndAddNewPageIfNeeded(spaceNeeded: Float = LINE_HEIGHT_NORMAL) {
            if (currentY + spaceNeeded > PAGE_HEIGHT_A4 - MARGIN - footerPageHeight) {
                startNewPage()
            }
        }

        fun advanceY(space: Float) {
            currentY += space
        }

        // Desenha texto em uma posição Y específica (relativa ao currentY ou explícita)
        // O yOffset é para o início da linha de texto, o alinhamento vertical é com base na descida da fonte
        fun drawTextOnLine(text: String, x: Float, paint: Paint, yOffset: Float = 0f) {
            val textY = currentY + yOffset + (paint.textSize - paint.descent())
            currentCanvas.drawText(text, x, textY, paint)
        }

        fun drawTextAndAdvance(text: String, x: Float, paint: Paint, lineHeight: Float = LINE_HEIGHT_NORMAL, yOffset: Float = 0f) {
            checkAndAddNewPageIfNeeded(lineHeight + yOffset)
            drawTextOnLine(text, x, paint, yOffset)
            currentY += lineHeight + yOffset
        }

        fun drawTextCenteredAndAdvance(text: String, paint: Paint, lineHeight: Float = LINE_HEIGHT_NORMAL, yOffset: Float = 0f) {
            val originalAlign = paint.textAlign
            paint.textAlign = Paint.Align.CENTER
            drawTextAndAdvance(text, pageInfo.pageWidth / 2f, paint, lineHeight, yOffset)
            paint.textAlign = originalAlign
        }

        fun drawLineSeparator(yOffset: Float = LINE_HEIGHT_SMALL / 2, advanceTotal: Float = LINE_HEIGHT_SMALL) {
            checkAndAddNewPageIfNeeded(advanceTotal + yOffset)
            val lineY = currentY + yOffset
            currentCanvas.drawLine(MARGIN, lineY, pageInfo.pageWidth - MARGIN, lineY, LINE_PAINT)
            currentY += advanceTotal
        }

        private fun drawPageFooter(context: Context, feiraId: String?) { // Tornou-se privado, chamado por finishDocument
            val originalPaintSize = ITALIC_TEXT_PAINT.textSize // Usar um paint diferente para rodapé
            val originalPaintColor = ITALIC_TEXT_PAINT.color
            ITALIC_TEXT_PAINT.textSize = 8f
            ITALIC_TEXT_PAINT.color = android.graphics.Color.DKGRAY
            ITALIC_TEXT_PAINT.textAlign = Paint.Align.CENTER

            val footerText = "Relatório AgroCooper © ${Calendar.getInstance().get(Calendar.YEAR)} | Feira: ${feiraId ?: "N/A"} | Página $pageNumber"
            // Posiciona o rodapé um pouco acima da margem inferior
            currentCanvas.drawText(footerText, pageInfo.pageWidth / 2f, PAGE_HEIGHT_A4 - (MARGIN / 2) - 5f, ITALIC_TEXT_PAINT)

            ITALIC_TEXT_PAINT.textSize = originalPaintSize // Restaura
            ITALIC_TEXT_PAINT.color = originalPaintColor
            ITALIC_TEXT_PAINT.textAlign = Paint.Align.LEFT
        }

        fun finishDocument(context: Context, feiraIdForFooter: String?) {
            drawPageFooter(context, feiraIdForFooter)
            document.finishPage(currentPage)
        }
    }

    private fun getPdfOutputDir(context: Context): File {
        val outputDir = File(context.filesDir, "pdfs")
        if (!outputDir.exists()) { outputDir.mkdirs() }
        return outputDir
    }

    private fun formatCurrency(value: Double, incluirSimbolo: Boolean = true): String { // Parâmetro renomeado
        val prefix = if (incluirSimbolo) "R$ " else ""
        return prefix + String.format(Locale("pt", "BR"), "%.2f", value)
    }

    private fun formatQuantity(value: Double): String {
        return if (value == 0.0) "-" else if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale("pt", "BR"), "%.2f", value)
    }

    private fun splitTextIntoLines(text: String, maxWidth: Float, paint: Paint): List<String> {
        val lines = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            val count = paint.breakText(text, start, text.length, true, maxWidth, null)
            if (count == 0) {
                if (start == text.length) break // Evita loop se o texto terminar exatamente
                val remaining = text.substring(start)
                // Tenta quebrar em palavra ou em um número fixo de caracteres se a palavra for muito longa
                var breakPoint = remaining.indexOf(' ')
                if (breakPoint == -1 || breakPoint > 20) breakPoint = 20.coerceAtMost(remaining.length)
                if (breakPoint == 0 && remaining.isNotEmpty()) breakPoint = 1 // Garante progresso mínimo

                lines.add(remaining.substring(0, breakPoint))
                start += breakPoint
                while (start < text.length && text[start] == ' ') start++ // Pula espaços
            } else {
                lines.add(text.substring(start, start + count))
                start += count
            }
        }
        return if (lines.isEmpty() && text.isNotEmpty()) listOf(text) else lines
    }

    // --- PDF INDIVIDUAL DO AGRICULTOR ---
    fun generateAgricultorPdf(
        context: Context,
        resultadoAgricultor: ResultadoAgricultorFeira,
        feiraDetails: FairDetails,
        agricultorInfo: Agricultor?
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_A4, PAGE_HEIGHT_A4, 1).create()
        val pm = PageManager(document, pageInfo, headerPageHeight = 80f, footerPageHeight = 30f)

        val nomeAgricultorDisplay = agricultorInfo?.nome ?: "ID: ${resultadoAgricultor.agricultorId}"
        val feiraId = feiraDetails.feiraId

        val colWidths = floatArrayOf(CONTENT_WIDTH * 0.35f, CONTENT_WIDTH * 0.17f, CONTENT_WIDTH * 0.16f, CONTENT_WIDTH * 0.16f, CONTENT_WIDTH * 0.16f)
        val xPos = mutableListOf(MARGIN)
        colWidths.forEachIndexed { index, width -> if (index > 0) xPos.add(xPos.last() + colWidths[index-1]) }

        val drawHeaderCallback = { _: Canvas, pageMgr: PageManager, pageNum: Int ->
            pageMgr.drawTextCenteredAndAdvance("Relatório do Agricultor" + if (pageNum > 1) " (Cont.)" else "", TITLE_PAINT, LINE_HEIGHT_TITLE, yOffset = if (pageNum > 1) LINE_HEIGHT_SMALL else 0f)
            pageMgr.drawTextCenteredAndAdvance("Feira Nº ${feiraDetails.feiraId} (${feiraDetails.startDate} a ${feiraDetails.endDate})", SUBTITLE_PAINT, LINE_HEIGHT_MEDIUM)
            pageMgr.drawTextCenteredAndAdvance("Agricultor: $nomeAgricultorDisplay", SUBTITLE_PAINT, LINE_HEIGHT_MEDIUM)
            pageMgr.advanceY(LINE_HEIGHT_MEDIUM / 2)

            val headers = listOf("Produto (#Nº)", "Entregue", "Perda Aloc.", "Vendido", "Valor Total")
            pageMgr.checkAndAddNewPageIfNeeded(LINE_HEIGHT_TABLE_HEADER)
            headers.forEachIndexed { index, headerText ->
                val paintToUse = TABLE_HEADER_PAINT
                val originalAlign = paintToUse.textAlign
                paintToUse.textAlign = if (index == 0) Paint.Align.LEFT else Paint.Align.RIGHT
                val xPosition = if (index == 0) xPos[index] + CELL_PADDING_DEFAULT else xPos[index] + colWidths[index] - CELL_PADDING_DEFAULT
                pageMgr.drawTextOnLine(headerText, xPosition, paintToUse)
                paintToUse.textAlign = originalAlign
            }
            pageMgr.advanceY(LINE_HEIGHT_TABLE_HEADER)
            pageMgr.drawLineSeparator(LINE_HEIGHT_SMALL / 2)
        }
        pm.setNewPageCallback(drawHeaderCallback)

        pm.drawTextAndAdvance("Itens Processados:", MARGIN, SECTION_HEADER_PAINT, LINE_HEIGHT_MEDIUM)
        pm.advanceY(LINE_HEIGHT_SMALL / 2)

        if (resultadoAgricultor.itensProcessados.isEmpty()){
            pm.drawTextAndAdvance("Nenhum item processado.", MARGIN + 5f, ITALIC_TEXT_PAINT)
        } else {
            resultadoAgricultor.itensProcessados.forEach { item ->
                val produtoText = "${item.produto.item} (#${item.produto.numero})"
                val productNameLines = splitTextIntoLines(produtoText, colWidths[0] - 2 * CELL_PADDING_DEFAULT, NORMAL_TEXT_PAINT)
                val rowHeight = (LINE_HEIGHT_SMALL * productNameLines.size.coerceAtLeast(1)) + (LINE_HEIGHT_EXTRA_SMALL/2) // Adiciona um pouco de padding
                pm.checkAndAddNewPageIfNeeded(rowHeight)

                var lineY = pm.currentY
                productNameLines.forEachIndexed { index, line ->
                    pm.drawTextOnLine(line, xPos[0] + CELL_PADDING_DEFAULT, NORMAL_TEXT_PAINT, yOffset = if (index > 0) LINE_HEIGHT_SMALL else 0f)
                    if(index > 0) lineY += LINE_HEIGHT_SMALL
                }

                val dataValues = listOf(
                    "${formatQuantity(item.quantidadeEntregueTotalSemana)} ${item.produto.unidade}",
                    formatQuantity(item.quantidadePerdaAlocada),
                    formatQuantity(item.quantidadeVendida),
                    formatCurrency(item.valorTotalVendido)
                )
                dataValues.forEachIndexed { indexVal, text ->
                    val currentPaint = if (indexVal == dataValues.size -1) BOLD_TEXT_PAINT else NORMAL_TEXT_PAINT
                    val originalAlign = currentPaint.textAlign
                    currentPaint.textAlign = Paint.Align.RIGHT
                    pm.drawTextOnLine(text, xPos[indexVal+1] + colWidths[indexVal+1] - CELL_PADDING_DEFAULT, currentPaint, yOffset = 0f) // Usa o Y da primeira linha do nome
                    currentPaint.textAlign = originalAlign
                }
                pm.advanceY(rowHeight)
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
            val yForRow = pm.currentY
            pm.drawTextOnLine(label, xLabel, BOLD_TEXT_PAINT, yOffset = 0f)
            val originalAlign = BOLD_TEXT_PAINT.textAlign
            BOLD_TEXT_PAINT.textAlign = Paint.Align.RIGHT
            pm.drawTextOnLine(formatCurrency(value), xValue, BOLD_TEXT_PAINT, yOffset = 0f)
            BOLD_TEXT_PAINT.textAlign = originalAlign
            pm.advanceY(LINE_HEIGHT_NORMAL)
        }

        pm.finishDocument(context, feiraId)
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
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_A4, PAGE_HEIGHT_A4, 1).create()
        val pm = PageManager(document, pageInfo, headerPageHeight = 90f, footerPageHeight = 20f)

        val feiraDetails = resultadoGeral.fairDetails
        val feiraId = feiraDetails.feiraId

        val colWidths = floatArrayOf(CONTENT_WIDTH * 0.40f, CONTENT_WIDTH * 0.20f, CONTENT_WIDTH * 0.20f, CONTENT_WIDTH * 0.20f)
        val xPos = mutableListOf(MARGIN)
        colWidths.forEachIndexed { index, width -> if (index > 0) xPos.add(xPos.last() + colWidths[index-1]) }

        val drawHeaderCallback = { _: Canvas, pageMgr: PageManager, pageNum: Int ->
            pageMgr.drawTextCenteredAndAdvance("Resumo Geral da Feira" + if (pageNum > 1) " (Cont.)" else "", TITLE_PAINT, LINE_HEIGHT_TITLE, yOffset = if(pageNum > 1) LINE_HEIGHT_SMALL else 0f)
            pageMgr.drawTextCenteredAndAdvance("Feira Nº ${feiraDetails.feiraId} (${feiraDetails.startDate} a ${feiraDetails.endDate})", SUBTITLE_PAINT, LINE_HEIGHT_MEDIUM)
            pageMgr.advanceY(LINE_HEIGHT_MEDIUM/2)

            pageMgr.drawTextAndAdvance("Detalhes por Agricultor:", MARGIN, SECTION_HEADER_PAINT, LINE_HEIGHT_MEDIUM)
            pageMgr.advanceY(LINE_HEIGHT_SMALL / 2)

            val headers = listOf("Agricultor", "Vendido Bruto", "Cooperativa", "Líquido Agr.")
            pm.checkAndAddNewPageIfNeeded(LINE_HEIGHT_TABLE_HEADER)
            headers.forEachIndexed { index, headerText ->
                val paintToUse = TABLE_HEADER_PAINT
                val originalAlign = paintToUse.textAlign
                paintToUse.textAlign = if (index == 0) Paint.Align.LEFT else Paint.Align.RIGHT
                val xPosition = if (index == 0) xPos[index] + CELL_PADDING_DEFAULT else xPos[index] + colWidths[index] - CELL_PADDING_DEFAULT
                pageMgr.drawTextOnLine(headerText, xPosition, paintToUse)
                paintToUse.textAlign = originalAlign
            }
            pageMgr.advanceY(LINE_HEIGHT_TABLE_HEADER)
            pageMgr.drawLineSeparator(LINE_HEIGHT_SMALL / 2)
        }
        pm.setNewPageCallback(drawHeaderCallback)

        if (resultadoGeral.resultadosPorAgricultor.isEmpty()) {
            pm.drawTextAndAdvance("Nenhum agricultor com resultados para exibir.", MARGIN + 5f, ITALIC_TEXT_PAINT)
        } else {
            resultadoGeral.resultadosPorAgricultor.forEach { agricultorRes ->
                val agricultorInfo = listaDeAgricultores.find { it.id == agricultorRes.agricultorId }
                val nomeDisplay = agricultorInfo?.nome ?: "ID: ${agricultorRes.agricultorId}"

                val nomeLines = splitTextIntoLines(nomeDisplay, colWidths[0] - 2 * CELL_PADDING_DEFAULT, NORMAL_TEXT_PAINT)
                val rowHeight = (LINE_HEIGHT_SMALL * nomeLines.size.coerceAtLeast(1)) + (LINE_HEIGHT_EXTRA_SMALL/2)
                pm.checkAndAddNewPageIfNeeded(rowHeight)

                var lineY = pm.currentY
                nomeLines.forEachIndexed { idx, line ->
                    pm.drawTextOnLine(line, xPos[0] + CELL_PADDING_DEFAULT, NORMAL_TEXT_PAINT, yOffset = if(idx > 0) LINE_HEIGHT_SMALL else 0f)
                    if(idx > 0) lineY += LINE_HEIGHT_SMALL
                }

                val dataValues = listOf(
                    formatCurrency(agricultorRes.totalVendidoBrutoAgricultor),
                    formatCurrency(agricultorRes.valorCooperativa),
                    formatCurrency(agricultorRes.valorLiquidoAgricultor)
                )
                dataValues.forEachIndexed { index, text ->
                    val currentPaint = NORMAL_TEXT_PAINT
                    val originalAlign = currentPaint.textAlign
                    currentPaint.textAlign = Paint.Align.RIGHT
                    pm.drawTextOnLine(text, xPos[index+1] + colWidths[index+1] - CELL_PADDING_DEFAULT, currentPaint, yOffset = 0f) // Usa o Y da primeira linha do nome
                    currentPaint.textAlign = originalAlign
                }
                pm.advanceY(rowHeight)
            }
        }
        pm.drawLineSeparator()

        pm.advanceY(LINE_HEIGHT_MEDIUM)
        pm.drawTextAndAdvance("Totais Gerais da Feira:", MARGIN, SECTION_HEADER_PAINT, LINE_HEIGHT_MEDIUM)
        pm.advanceY(LINE_HEIGHT_SMALL / 2)

        val xLabel = MARGIN
        val xValue = CONTENT_WIDTH + MARGIN - CELL_PADDING_DEFAULT

        val summaryTotals = listOf(
            "Total Geral Vendido:" to resultadoGeral.totalGeralVendido,
            "Total para Cooperativa (30%):" to resultadoGeral.totalGeralCooperativa,
            "Total para Agricultores (70%):" to resultadoGeral.totalGeralAgricultores
        )
        summaryTotals.forEach { (label, value) ->
            pm.checkAndAddNewPageIfNeeded(LINE_HEIGHT_NORMAL)
            val yForRow = pm.currentY
            pm.drawTextOnLine(label, xLabel, BOLD_TEXT_PAINT, yOffset = 0f)
            val originalAlign = BOLD_TEXT_PAINT.textAlign
            BOLD_TEXT_PAINT.textAlign = Paint.Align.RIGHT
            pm.drawTextOnLine(formatCurrency(value), xValue, BOLD_TEXT_PAINT, yOffset = 0f)
            BOLD_TEXT_PAINT.textAlign = originalAlign
            pm.advanceY(LINE_HEIGHT_NORMAL)
        }

        pm.finishDocument(context, feiraId)
        val file = File(getPdfOutputDir(context), "Resumo_Geral_Feira_${feiraDetails.feiraId}.pdf")
        try { FileOutputStream(file).use { fos -> document.writeTo(fos) }; return file }
        catch (e: IOException) { e.printStackTrace(); throw e }
        finally { document.close() }
    }

    // --- NOVAS FUNÇÕES PARA PDF DE DESPESAS ---
    fun generateDespesasMensaisPdf(
        context: Context,
        ano: Int,
        mes: Int,
        lancamentosDoMes: List<LancamentoMensalDespesaEntity>,
        todosItensDespesa: List<ItemDespesaEntity>
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_A4, PAGE_HEIGHT_A4, 1).create()
        val pm = PageManager(document, pageInfo, headerPageHeight = 70f, footerPageHeight = 20f)

        val nomeMesDisplay = Calendar.getInstance().apply { set(Calendar.MONTH, mes - 1) }
            .getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "BR"))
            ?.replaceFirstChar { it.titlecase(Locale("pt", "BR")) } ?: "Mês $mes"
        val feiraIdForFooter = "Desp-${ano}-${String.format("%02d", mes)}"

        val colWidths = floatArrayOf(CONTENT_WIDTH * 0.28f, // Item
            CONTENT_WIDTH * 0.10f, // Sem1
            CONTENT_WIDTH * 0.10f, // Sem2
            CONTENT_WIDTH * 0.10f, // Sem3
            CONTENT_WIDTH * 0.10f, // Sem4
            CONTENT_WIDTH * 0.10f, // Sem5
            CONTENT_WIDTH * 0.22f) // Total Mês
        val xPos = mutableListOf(MARGIN)
        colWidths.forEachIndexed { index, width -> if (index > 0) xPos.add(xPos.last() + colWidths[index-1]) }


        val drawHeaderCallback = { _: Canvas, pageMgr: PageManager, pageNum: Int ->
            pageMgr.drawTextCenteredAndAdvance("Relatório de Despesas Mensais" + if (pageNum > 1) " (Cont.)" else "", TITLE_PAINT, LINE_HEIGHT_TITLE, yOffset = if(pageNum > 1) LINE_HEIGHT_SMALL else 0f)
            pageMgr.advanceY(5f)
            pageMgr.drawTextCenteredAndAdvance("$nomeMesDisplay de $ano", SUBTITLE_PAINT, LINE_HEIGHT_MEDIUM)
            pageMgr.advanceY(LINE_HEIGHT_MEDIUM/2)

            val headers = listOf("Item de Despesa", "Sem 1", "Sem 2", "Sem 3", "Sem 4", "Sem 5", "Total Mês")
            pm.checkAndAddNewPageIfNeeded(LINE_HEIGHT_TABLE_HEADER)
            headers.forEachIndexed { index, headerText ->
                val paintToUse = TABLE_HEADER_PAINT
                val originalAlign = paintToUse.textAlign
                paintToUse.textAlign = if (index == 0) Paint.Align.LEFT else Paint.Align.RIGHT
                val xPosition = if (index == 0) xPos[index] + CELL_PADDING_DEFAULT
                else xPos[index] + colWidths[index] - CELL_PADDING_DEFAULT
                pageMgr.drawTextOnLine(headerText, xPosition, paintToUse)
                paintToUse.textAlign = originalAlign
            }
            pageMgr.advanceY(LINE_HEIGHT_TABLE_HEADER)
            pageMgr.drawLineSeparator(LINE_HEIGHT_SMALL / 2)
        }
        pm.setNewPageCallback(drawHeaderCallback)

        if (todosItensDespesa.isEmpty()) {
            pm.drawTextAndAdvance("Nenhum item de despesa cadastrado.", MARGIN + 5f, ITALIC_TEXT_PAINT)
        } else {
            var totalGeralMes = 0.0
            todosItensDespesa.sortedBy { it.nome }.forEach { itemDespesa ->
                val lancamento = lancamentosDoMes.find { it.itemDespesaId == itemDespesa.id }
                val valoresSemana = listOf(
                    lancamento?.valorSemana1 ?: 0.0, lancamento?.valorSemana2 ?: 0.0,
                    lancamento?.valorSemana3 ?: 0.0, lancamento?.valorSemana4 ?: 0.0,
                    lancamento?.valorSemana5 ?: 0.0
                )
                val totalItemMes = valoresSemana.sum()
                totalGeralMes += totalItemMes

                val nomeLines = splitTextIntoLines(itemDespesa.nome, colWidths[0] - 2 * CELL_PADDING_DEFAULT, NORMAL_TEXT_PAINT)
                val rowHeight = (LINE_HEIGHT_SMALL * nomeLines.size.coerceAtLeast(1)) + (LINE_HEIGHT_EXTRA_SMALL/2)
                pm.checkAndAddNewPageIfNeeded(rowHeight)

                var lineY = pm.currentY
                nomeLines.forEachIndexed { idx, line ->
                    pm.drawTextOnLine(line, xPos[0] + CELL_PADDING_DEFAULT, NORMAL_TEXT_PAINT, yOffset = if(idx > 0) LINE_HEIGHT_SMALL else 0f)
                    if(idx > 0) lineY += LINE_HEIGHT_SMALL
                }

                val dataValues = valoresSemana.map { formatQuantity(it) } + listOf(formatQuantity(totalItemMes))

                dataValues.forEachIndexed { index, text ->
                    val currentPaint = if (index == dataValues.size -1) BOLD_TEXT_PAINT else NORMAL_TEXT_PAINT
                    val originalAlign = currentPaint.textAlign
                    currentPaint.textAlign = Paint.Align.RIGHT
                    pm.drawTextOnLine(text, xPos[index+1] + colWidths[index+1] - CELL_PADDING_DEFAULT, currentPaint, yOffset = 0f) // Usa o Y da primeira linha do nome
                    currentPaint.textAlign = originalAlign
                }
                pm.advanceY(rowHeight)
            }
            pm.drawLineSeparator()
            pm.advanceY(LINE_HEIGHT_SMALL)

            pm.checkAndAddNewPageIfNeeded(LINE_HEIGHT_MEDIUM)
            val xLabelTotal = xPos[5] + colWidths[5] - CELL_PADDING_DEFAULT - 120f // Ajustar X para o label
            val originalAlignTotal = BOLD_TEXT_PAINT.textAlign
            BOLD_TEXT_PAINT.textAlign = Paint.Align.RIGHT
            pm.drawTextOnLine("TOTAL GERAL DO MÊS:", xLabelTotal , BOLD_TEXT_PAINT, yOffset = 0f)
            pm.drawTextOnLine(formatCurrency(totalGeralMes), xPos.last() + colWidths.last() - CELL_PADDING_DEFAULT, BOLD_TEXT_PAINT, yOffset = 0f)
            BOLD_TEXT_PAINT.textAlign = originalAlignTotal
            pm.advanceY(LINE_HEIGHT_MEDIUM)
        }

        pm.finishDocument(context, feiraIdForFooter)
        val file = File(getPdfOutputDir(context), "Despesas_Mensais_${ano}_${String.format("%02d", mes)}.pdf")
        try { FileOutputStream(file).use { fos -> document.writeTo(fos) }; return file }
        catch (e: IOException) { e.printStackTrace(); throw e }
        finally { document.close() }
    }

    fun generateDespesasSemanaisPdf(
        context: Context,
        ano: Int,
        mes: Int,
        semanaNum: Int, // 1-5
        lancamentosDoMes: List<LancamentoMensalDespesaEntity>, // Lançamentos do mês inteiro
        todosItensDespesa: List<ItemDespesaEntity>
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_A4, PAGE_HEIGHT_A4, 1).create()
        val pm = PageManager(document, pageInfo, headerPageHeight = 70f, footerPageHeight = 20f)

        val nomeMesDisplay = Calendar.getInstance().apply { set(Calendar.MONTH, mes - 1) }
            .getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "BR"))
            ?.replaceFirstChar { it.titlecase(Locale("pt", "BR")) } ?: "Mês $mes"
        val feiraIdForFooter = "Desp-${ano}-${String.format("%02d", mes)}-S${semanaNum}"

        val colItemWidth = CONTENT_WIDTH * 0.70f
        val colValorWidth = CONTENT_WIDTH * 0.30f
        val xPos = listOf(MARGIN, MARGIN + colItemWidth)

        val drawHeaderCallback = { _: Canvas, pageMgr: PageManager, pageNum: Int ->
            pageMgr.drawTextCenteredAndAdvance("Relatório de Despesas Semanais" + if (pageNum > 1) " (Cont.)" else "", TITLE_PAINT, LINE_HEIGHT_TITLE, yOffset = if(pageNum > 1) LINE_HEIGHT_SMALL else 0f)
            pageMgr.advanceY(5f)
            pageMgr.drawTextCenteredAndAdvance("Semana $semanaNum de $nomeMesDisplay de $ano", SUBTITLE_PAINT, LINE_HEIGHT_MEDIUM)
            pageMgr.advanceY(LINE_HEIGHT_MEDIUM/2)

            val headers = listOf("Item de Despesa", "Valor na Semana")
            pm.checkAndAddNewPageIfNeeded(LINE_HEIGHT_TABLE_HEADER)
            headers.forEachIndexed{ index, headerText ->
                val paintToUse = TABLE_HEADER_PAINT
                val originalAlign = paintToUse.textAlign
                paintToUse.textAlign = if (index == 0) Paint.Align.LEFT else Paint.Align.RIGHT
                val xPosition = if (index == 0) xPos[index] + CELL_PADDING_DEFAULT
                else xPos[index] + colValorWidth - CELL_PADDING_DEFAULT
                pageMgr.drawTextOnLine(headerText, xPosition, paintToUse)
                paintToUse.textAlign = originalAlign
            }
            pageMgr.advanceY(LINE_HEIGHT_TABLE_HEADER)
            pageMgr.drawLineSeparator(LINE_HEIGHT_SMALL / 2)
        }
        pm.setNewPageCallback(drawHeaderCallback)

        var totalGeralSemana = 0.0
        var itemCountInReport = 0

        todosItensDespesa.sortedBy { it.nome }.forEach { itemDespesa ->
            val lancamento = lancamentosDoMes.find { it.itemDespesaId == itemDespesa.id }
            val valorSemanaEspecifica = when (semanaNum) {
                1 -> lancamento?.valorSemana1; 2 -> lancamento?.valorSemana2
                3 -> lancamento?.valorSemana3; 4 -> lancamento?.valorSemana4
                5 -> lancamento?.valorSemana5; else -> 0.0 // Default to 0.0 if semanaNum is out of range
            } ?: 0.0

            if (valorSemanaEspecifica != 0.0) {
                itemCountInReport++
                totalGeralSemana += valorSemanaEspecifica

                val nomeLines = splitTextIntoLines(itemDespesa.nome, colItemWidth - 2 * CELL_PADDING_DEFAULT, NORMAL_TEXT_PAINT)
                val rowHeight = (LINE_HEIGHT_SMALL * nomeLines.size.coerceAtLeast(1)) + (LINE_HEIGHT_EXTRA_SMALL/2)
                pm.checkAndAddNewPageIfNeeded(rowHeight)

                var lineY = pm.currentY
                nomeLines.forEachIndexed { idx, line ->
                    pm.drawTextOnLine(line, xPos[0] + CELL_PADDING_DEFAULT, NORMAL_TEXT_PAINT, yOffset = if(idx > 0) LINE_HEIGHT_SMALL else 0f)
                    if(idx > 0) lineY += LINE_HEIGHT_SMALL
                }

                val originalAlign = NORMAL_TEXT_PAINT.textAlign
                NORMAL_TEXT_PAINT.textAlign = Paint.Align.RIGHT
                pm.drawTextOnLine(formatCurrency(valorSemanaEspecifica), xPos[1] + colValorWidth - CELL_PADDING_DEFAULT, NORMAL_TEXT_PAINT, yOffset=0f)
                NORMAL_TEXT_PAINT.textAlign = originalAlign

                pm.advanceY(rowHeight)
            }
        }

        if (itemCountInReport == 0) {
            pm.drawTextAndAdvance("Nenhuma despesa registrada para esta semana.", MARGIN + 5f, ITALIC_TEXT_PAINT)
        } else {
            pm.drawLineSeparator()
        }
        pm.advanceY(LINE_HEIGHT_SMALL)

        pm.checkAndAddNewPageIfNeeded(LINE_HEIGHT_MEDIUM)
        val xLabelTotal = xPos[0] + colItemWidth - CELL_PADDING_DEFAULT - 150f // Ajustar para alinhar label do total
        val originalAlignTotal = BOLD_TEXT_PAINT.textAlign
        BOLD_TEXT_PAINT.textAlign = Paint.Align.RIGHT
        pm.drawTextOnLine("TOTAL DA SEMANA $semanaNum:", xLabelTotal, BOLD_TEXT_PAINT, yOffset = 0f)
        pm.drawTextOnLine(formatCurrency(totalGeralSemana), xPos[1] + colValorWidth - CELL_PADDING_DEFAULT, BOLD_TEXT_PAINT, yOffset = 0f)
        BOLD_TEXT_PAINT.textAlign = originalAlignTotal
        pm.advanceY(LINE_HEIGHT_MEDIUM)

        pm.finishDocument(context, feiraIdForFooter)
        val file = File(getPdfOutputDir(context), "Despesas_Semanal_${ano}_${String.format("%02d", mes)}_Sem${semanaNum}.pdf")
        try { FileOutputStream(file).use { fos -> document.writeTo(fos) }; return file }
        catch (e: IOException) { e.printStackTrace(); throw e }
        finally { document.close() }
    }
}