package com.example.eduqizpro.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.*
import android.util.Log
import androidx.core.graphics.withTranslation
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.openxml4j.opc.PackageAccess
import org.apache.poi.xwpf.usermodel.*
import java.io.*

object DocumentConverter {
    private const val TAG = "DocumentConverter"

    private const val SYMBOL_MAP = " !∀#∃%&∍()*+,-./0123456789:;<=>?≅ΑΒΧΔΕΦΓΗΙϑΚΛΜΝΟΠΘΡΣΤΥςΩΞΨΖ[∴]⊥_‾αβχδεφγηιϕκλμνοπθρστυϖωξψζ{|}~" +
            "                                 " +
            "ϒ′≤⁄∞ƒ♣♦♥♠↔←↑→↓°±″≥×∝∂•÷≠≡≈…⏐⎯↵ℵℑℜ℘⊗⊕∅∩∪⊃⊇⊄⊂⊆∈∉∠∇®©™∏√⋅¬∧∨⇔⇔⇐⇑⇒⇓◊⟨®©™∑⎛⎜⎝⎡⎢⎣⎧⎨⎩⎪"

    private fun setupThreadConfig() {
        try {
            val factory = "com.ctc.wstx.stax.WstxInputFactory"
            val outputFactory = "com.ctc.wstx.stax.WstxOutputFactory"
            val eventFactory = "com.ctc.wstx.stax.WstxEventFactory"
            val props = mapOf(
                "javax.xml.stream.XMLInputFactory" to factory,
                "javax.xml.stream.XMLOutputFactory" to outputFactory,
                "javax.xml.stream.XMLEventFactory" to eventFactory,
                "org.apache.poi.javax.xml.stream.XMLInputFactory" to factory,
                "org.apache.poi.javax.xml.stream.XMLOutputFactory" to outputFactory,
                "org.apache.poi.javax.xml.stream.XMLEventFactory" to eventFactory,
                "org.apache.poi.util.XMLHelper.XMLInputFactory" to factory,
                "org.apache.poi.util.XMLHelper.XMLOutputFactory" to outputFactory,
                "org.apache.poi.util.XMLHelper.XMLEventFactory" to eventFactory,
                "org.apache.xmlbeans.impl.store.Locale.SaxLoader.canSetProperty" to "false",
                "org.apache.xmlbeans.impl.store.Locale.SaxLoader.canSetLexicalHandler" to "false"
            )
            props.forEach { (k, v) -> System.setProperty(k, v) }
            Thread.currentThread().contextClassLoader = DocumentConverter::class.java.classLoader
        } catch (t: Throwable) {
            Log.e(TAG, "Config setup failed", t)
        }
    }

    /**
     * CHUYỂN WORD SANG PDF
     */
    fun docxToPdf(context: Context, inputUri: Uri, outputStream: OutputStream): Boolean {
        setupThreadConfig()
        Log.d(TAG, "docxToPdf: Bắt đầu chuyển đổi cho $inputUri")

        var tempFile: File? = null
        var opcPackage: OPCPackage? = null
        var doc: XWPFDocument? = null
        val pdfDoc = PdfDocument()

        return try {
            tempFile = File(context.cacheDir, "temp_docx_${System.currentTimeMillis()}.docx")
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            } ?: return false

            try {
                opcPackage = OPCPackage.open(tempFile, PackageAccess.READ)
                doc = XWPFDocument(opcPackage)
            } catch (e: Exception) {
                Log.e(TAG, "docxToPdf: Lỗi mở file Word (đảm bảo tệp .docx hợp lệ)", e)
                return false
            }

            val elements = doc.bodyElements ?: emptyList()
            val textPaint = TextPaint().apply {
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
                color = Color.BLACK
            }

            val pageWidth = 595 // A4
            val pageHeight = 842
            val margin = 50f
            val contentWidth = (pageWidth - 2 * margin).toInt()
            val pageLimitY = pageHeight - margin

            var currentY = 0f
            var pageNumber = 1
            var activePage: PdfDocument.Page? = null

            fun startNewPage(): Canvas {
                activePage?.let { pdfDoc.finishPage(it) }
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
                val page = pdfDoc.startPage(pageInfo)
                activePage = page
                currentY = 0f
                return page.canvas
            }

            var canvas = startNewPage()

            for (element in elements) {
                try {
                    when (element) {
                        is XWPFParagraph -> {
                            val rawText = extractParaText(element)
                            if (rawText.isBlank()) {
                                if (margin + currentY + 15f > pageLimitY) canvas = startNewPage()
                                currentY += 15f
                                continue
                            }

                            val layout = StaticLayout.Builder
                                .obtain(rawText, 0, rawText.length, textPaint, contentWidth)
                                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                .setLineSpacing(0f, 1.1f)
                                .setIncludePad(false) 
                                .build()

                            var line = 0
                            while (line < layout.lineCount) {
                                val lineTop = layout.getLineTop(line)
                                var linesInBlock = 0
                                while (line + linesInBlock < layout.lineCount) {
                                    val blockBottom = layout.getLineBottom(line + linesInBlock)
                                    if (margin + currentY + (blockBottom - lineTop) > pageLimitY) break
                                    linesInBlock++
                                }

                                if (linesInBlock == 0) {
                                    if (currentY > 0f) canvas = startNewPage()
                                    linesInBlock = 1
                                }

                                val blockHeight = (layout.getLineBottom(line + linesInBlock - 1) - lineTop).toFloat()
                                canvas.withTranslation(margin, margin + currentY) {
                                    clipRect(0f, 0f, contentWidth.toFloat(), blockHeight)
                                    translate(0f, -lineTop.toFloat())
                                    layout.draw(this)
                                }
                                currentY += blockHeight
                                line += linesInBlock
                            }
                            currentY += 10f
                        }
                        is XWPFTable -> {
                            for (row in element.rows) {
                                val rowText = row.tableCells.joinToString(" | ") { cell ->
                                    cell.paragraphs.joinToString(" ") { extractParaText(it) }
                                }.trim()

                                if (rowText.isBlank()) continue

                                val layout = StaticLayout.Builder
                                    .obtain(rowText, 0, rowText.length, textPaint, contentWidth)
                                    .setIncludePad(false)
                                    .build()

                                if (margin + currentY + layout.height > pageLimitY) canvas = startNewPage()

                                canvas.withTranslation(margin, margin + currentY) {
                                    layout.draw(this)
                                }
                                currentY += layout.height.toFloat() + 8f
                            }
                            currentY += 10f
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Bỏ qua phần tử Word bị lỗi render", e)
                }
            }

            activePage?.let { pdfDoc.finishPage(it) }
            pdfDoc.writeTo(outputStream)
            outputStream.flush()
            true
        } catch (t: Throwable) {
            Log.e(TAG, "docxToPdf failed", t)
            false
        } finally {
            try { doc?.close() } catch (_: Exception) {}
            try { opcPackage?.close() } catch (_: Exception) {}
            pdfDoc.close()
            tempFile?.delete()
        }
    }

    private fun extractParaText(para: XWPFParagraph): String {
        val sb = StringBuilder()
        for (run in para.runs) {
            val text = run.getText(0) ?: continue
            val filtered = text.filter { ch -> ch.code >= 32 || ch == '\n' || ch == '\r' }
            if (filtered.isNotEmpty()) sb.append(translateText(filtered))
        }
        return sb.toString().replace(Regex("[ \t]+"), " ").trim()
    }

    private fun translateText(input: String): String {
        val sb = StringBuilder(input.length)
        for (char in input) {
            val code = char.code
            if (code in 0xF020..0xF0FF) {
                val index = code - 0xF020
                if (index in SYMBOL_MAP.indices) sb.append(SYMBOL_MAP[index])
                else sb.append(char)
            } else sb.append(char)
        }
        return sb.toString()
    }

    /**
     * CHUYỂN PDF SANG WORD
     */
    fun pdfToDocx(context: Context, inputUri: Uri, outputStream: OutputStream): Boolean {
        setupThreadConfig()
        var pdfDoc: PDDocument? = null
        return try {
            val inputStream = context.contentResolver.openInputStream(inputUri) ?: return false
            PDFBoxResourceLoader.init(context)
            pdfDoc = PDDocument.load(inputStream)
            val doc = XWPFDocument()

            val stripper = PDFTextStripper().apply {
                sortByPosition = true
                // 0.4f nhạy bén hơn mức mặc định 0.5f để bắt dấu cách tốt hơn
                spacingTolerance = 0.4f 
                averageCharTolerance = 0.3f
                wordSeparator = " "
            }

            val text = stripper.getText(pdfDoc)
            text.split('\n').forEach { line ->
                val trimmed = line.trimEnd()
                if (trimmed.isNotEmpty()) {
                    val p = doc.createParagraph()
                    val r = p.createRun()
                    r.setText(trimmed)
                } else {
                    doc.createParagraph()
                }
            }
            doc.write(outputStream)
            outputStream.flush()
            true
        } catch (t: Throwable) {
            Log.e(TAG, "pdfToDocx failed", t)
            false
        } finally {
            try { pdfDoc?.close() } catch (_: Exception) {}
        }
    }
}
