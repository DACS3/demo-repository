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
import org.apache.poi.xwpf.usermodel.*
import java.io.*

object DocumentConverter {
    private const val TAG = "DocumentConverter"

    // Bảng ký hiệu Symbol font (Private Use Area F020–F0FF → unicode tương đương)
    private const val SYMBOL_MAP =
        " !∀#∃%&∍()*+,-./0123456789:;<=>?≅ΑΒΧΔΕΦΓΗΙϑΚΛΜΝΟΠΘΡΣΤΥςΩΞΨΖ[∴]⊥_‾αβχδεφγηιϕκλμνοπθρστυϖωξψζ{|}~" +
        "                                 " +
        "ϒ′≤⁄∞ƒ♣♦♥♠↔←↑→↓°±″≥×∝∂•÷≠≡≈…⏐⎯↵ℵℑℜ℘⊗⊕∅∩∪⊃⊇⊄⊂⊆∈∉∠∇®©™∏√⋅¬∧∨⇔⇔⇐⇑⇒⇓◊⟨®©™∑⎛⎜⎝⎡⎢⎣⎧⎨⎩⎪"

    // ─────────────────────────────────────────────────────────────
    //  Cấu hình XMLInputFactory → dùng Woodstox (BẮT BUỘC cho POI trên Android)
    // ─────────────────────────────────────────────────────────────
    private fun setupXmlFactories() {
        try {
            val inputFactory  = "com.ctc.wstx.stax.WstxInputFactory"
            val outputFactory = "com.ctc.wstx.stax.WstxOutputFactory"
            val eventFactory  = "com.ctc.wstx.stax.WstxEventFactory"

            System.setProperty("javax.xml.stream.XMLInputFactory",  inputFactory)
            System.setProperty("javax.xml.stream.XMLOutputFactory", outputFactory)
            System.setProperty("javax.xml.stream.XMLEventFactory",  eventFactory)

            // POI-specific overrides (Apache POI đọc riêng các key này trên Android)
            System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory",  inputFactory)
            System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", outputFactory)
            System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory",  eventFactory)

            // Tắt SAX lexical handler để tránh lỗi với xmlbeans trên Android
            System.setProperty(
                "org.apache.xmlbeans.impl.store.Locale.SaxLoader.canSetLexicalHandler",
                "false"
            )

            Thread.currentThread().contextClassLoader =
                DocumentConverter::class.java.classLoader

            Log.d(TAG, "setupXmlFactories: Woodstox đã được cấu hình thành công")
        } catch (t: Throwable) {
            Log.w(TAG, "setupXmlFactories: Cấu hình thất bại (có thể không ảnh hưởng)", t)
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  WORD (DOCX) → PDF
    //  Trả về null nếu thành công, hoặc thông báo lỗi nếu thất bại
    // ─────────────────────────────────────────────────────────────
    fun docxToPdf(context: Context, inputUri: Uri, outputStream: OutputStream): String? {
        Log.d(TAG, "docxToPdf: Bắt đầu")
        setupXmlFactories()

        var tempFile: File? = null
        var doc: XWPFDocument? = null
        val pdfDoc = PdfDocument()

        return try {
            // 1. Copy DOCX vào cache (tránh lỗi SAF Uri với FileInputStream)
            tempFile = File(context.cacheDir, "conv_docx_${System.currentTimeMillis()}.docx")
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                FileOutputStream(tempFile).use { it.write(input.readBytes()) }
            } ?: return "Không thể đọc tệp đầu vào. Thử chọn lại tệp."

            // 2. Mở DOCX
            try {
                doc = XWPFDocument(FileInputStream(tempFile))
            } catch (e: Exception) {
                Log.e(TAG, "docxToPdf: Lỗi mở DOCX", e)
                return "Tệp .docx không hợp lệ hoặc bị hỏng.\n(${e.javaClass.simpleName}: ${e.message?.take(80)})"
            }

            // 3. Paint cho text
            val normalPaint = TextPaint().apply {
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
                color = Color.BLACK
            }
            val boldPaint = TextPaint(normalPaint).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 14f
            }

            val pageWidth   = 595
            val pageHeight  = 842
            val margin      = 56f
            val contentWidth = (pageWidth - 2 * margin).toInt()
            val pageLimitY  = pageHeight - margin
            val paraSpacing = 6f

            var currentY   = margin
            var pageNumber = 1
            var activePage: PdfDocument.Page? = null

            fun nextPage(): Canvas {
                activePage?.let { pdfDoc.finishPage(it) }
                val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
                activePage = pdfDoc.startPage(info)
                currentY = margin
                return activePage!!.canvas
            }
            var canvas = nextPage()

            // 4. Render từng phần tử
            for (el in doc.bodyElements ?: emptyList()) {
                try {
                    when (el) {
                        is XWPFParagraph -> {
                            val text = extractParaText(el)
                            if (text.isBlank()) {
                                currentY += 10f
                                if (currentY > pageLimitY) canvas = nextPage()
                                continue
                            }
                            val isHeading = el.style?.lowercase()?.startsWith("heading") == true
                            val isBold    = el.runs.any { it.isBold }
                            val paint     = if (isHeading || isBold) boldPaint else normalPaint
                            paint.textSize = if (isHeading) 14f else 12f

                            val layout = StaticLayout.Builder
                                .obtain(text, 0, text.length, paint, contentWidth)
                                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                .setLineSpacing(2f, 1.15f)
                                .setIncludePad(false)
                                .build()

                            if (currentY + layout.height > pageLimitY) canvas = nextPage()
                            canvas.withTranslation(margin, currentY) { layout.draw(this) }
                            currentY += layout.height + paraSpacing
                        }

                        is XWPFTable -> {
                            for (row in el.rows) {
                                val rowText = row.tableCells.joinToString("  |  ") { cell ->
                                    cell.paragraphs.joinToString(" ") { extractParaText(it) }
                                }.trim()
                                if (rowText.isBlank()) continue

                                val layout = StaticLayout.Builder
                                    .obtain(rowText, 0, rowText.length, normalPaint, contentWidth)
                                    .setIncludePad(false).build()

                                if (currentY + layout.height > pageLimitY) canvas = nextPage()

                                // Viền đơn giản
                                canvas.drawRect(
                                    margin, currentY - 2f,
                                    margin + contentWidth, currentY + layout.height + 4f,
                                    Paint().apply { color = Color.LTGRAY; style = Paint.Style.STROKE; strokeWidth = 0.5f }
                                )
                                canvas.withTranslation(margin + 4f, currentY + 2f) { layout.draw(this) }
                                currentY += layout.height + 8f
                            }
                            currentY += 4f
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Bỏ qua phần tử bị lỗi: ${e.message}")
                }
            }

            activePage?.let { pdfDoc.finishPage(it) }
            pdfDoc.writeTo(outputStream)
            outputStream.flush()
            Log.d(TAG, "docxToPdf: Xong, ${pageNumber - 1} trang")
            null // thành công

        } catch (t: Throwable) {
            Log.e(TAG, "docxToPdf: Thất bại không mong đợi", t)
            "Lỗi không xác định: ${t.javaClass.simpleName}\n${t.message?.take(100)}"
        } finally {
            try { doc?.close() } catch (_: Exception) {}
            pdfDoc.close()
            tempFile?.delete()
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  PDF → WORD (DOCX)
    //  Trả về null nếu thành công, hoặc thông báo lỗi nếu thất bại
    // ─────────────────────────────────────────────────────────────
    fun pdfToDocx(context: Context, inputUri: Uri, outputStream: OutputStream): String? {
        Log.d(TAG, "pdfToDocx: Bắt đầu")

        // ① Cấu hình Woodstox TRƯỚC KHI tạo bất kỳ đối tượng POI nào
        setupXmlFactories()

        // ② Khởi tạo PDFBox resource (font, encoding maps, v.v.)
        try {
            PDFBoxResourceLoader.init(context)
        } catch (e: Exception) {
            Log.e(TAG, "pdfToDocx: PDFBoxResourceLoader.init thất bại", e)
            return "Không thể khởi tạo thư viện đọc PDF.\n(${e.message?.take(80)})"
        }

        var tempFile: File? = null
        var pdfDoc: PDDocument? = null

        return try {
            // ③ Copy PDF vào cache (pdfbox-android cần random access)
            tempFile = File(context.cacheDir, "conv_pdf_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                FileOutputStream(tempFile).use { it.write(input.readBytes()) }
            } ?: return "Không thể đọc tệp PDF. Thử chọn lại tệp."

            Log.d(TAG, "pdfToDocx: Đã copy PDF, kích thước = ${tempFile.length()} bytes")

            // ④ Mở PDF
            try {
                pdfDoc = PDDocument.load(tempFile)
            } catch (e: Exception) {
                Log.e(TAG, "pdfToDocx: Lỗi mở PDF", e)
                val hint = when {
                    e.message?.contains("encrypted", ignoreCase = true) == true ->
                        "PDF được mã hoá/bảo vệ bằng mật khẩu. Hãy bỏ mật khẩu trước."
                    else ->
                        "Tệp PDF không hợp lệ hoặc bị hỏng.\n(${e.message?.take(80)})"
                }
                return hint
            }

            if (pdfDoc.isEncrypted) {
                return "PDF đang bị mã hoá. Vui lòng bỏ bảo vệ mật khẩu trước khi chuyển đổi."
            }

            Log.d(TAG, "pdfToDocx: ${pdfDoc.numberOfPages} trang")

            // ⑤ Trích xuất văn bản
            val fullText = try {
                PDFTextStripper().apply {
                    sortByPosition = true
                    spacingTolerance = 0.5f
                    averageCharTolerance = 0.3f
                    wordSeparator = " "
                    lineSeparator = "\n"
                }.getText(pdfDoc).ifBlank { null }
            } catch (e: Exception) {
                Log.e(TAG, "pdfToDocx: Lỗi trích xuất text", e)
                null
            }

            if (fullText == null) {
                return "Không thể trích xuất văn bản từ PDF này.\nCó thể PDF chỉ chứa hình ảnh quét (scan). Hãy dùng OCR trước."
            }

            // ⑥ Tạo DOCX — Woodstox phải được set ở ① mới không crash ở đây
            val docx = try {
                XWPFDocument()
            } catch (e: Exception) {
                Log.e(TAG, "pdfToDocx: Không tạo được XWPFDocument", e)
                return "Lỗi tạo file Word: ${e.javaClass.simpleName}\n${e.message?.take(80)}"
            }

            // ⑦ Ghi từng dòng vào DOCX
            val lines = fullText.split('\n')
            for (line in lines) {
                val para = docx.createParagraph()
                val run  = para.createRun()
                run.setText(line.trimEnd())
                run.fontFamily = "Times New Roman"
                run.fontSize   = 12
            }

            // ⑧ Xuất ra OutputStream
            try {
                docx.write(outputStream)
                outputStream.flush()
            } catch (e: Exception) {
                Log.e(TAG, "pdfToDocx: Lỗi ghi file DOCX", e)
                return "Lỗi khi ghi file Word: ${e.message?.take(80)}"
            } finally {
                try { docx.close() } catch (_: Exception) {}
            }

            Log.d(TAG, "pdfToDocx: Xong, ${lines.size} dòng")
            null // thành công

        } catch (t: Throwable) {
            Log.e(TAG, "pdfToDocx: Thất bại không mong đợi", t)
            "Lỗi không xác định: ${t.javaClass.simpleName}\n${t.message?.take(100)}"
        } finally {
            try { pdfDoc?.close() } catch (_: Exception) {}
            tempFile?.delete()
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Helper: trích xuất text của một đoạn XWPFParagraph
    // ─────────────────────────────────────────────────────────────
    private fun extractParaText(para: XWPFParagraph): String {
        val sb = StringBuilder()
        for (run in para.runs) {
            val text = run.getText(0) ?: continue
            val filtered = text.filter { it.code >= 32 || it == '\n' || it == '\r' }
            if (filtered.isNotEmpty()) sb.append(translateSymbolFont(filtered))
        }
        return sb.toString().replace(Regex("[ \t]+"), " ").trim()
    }

    // ─────────────────────────────────────────────────────────────
    //  Helper: Symbol font PUA F020-F0FF → unicode
    // ─────────────────────────────────────────────────────────────
    private fun translateSymbolFont(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val code = ch.code
            if (code in 0xF020..0xF0FF) {
                val idx = code - 0xF020
                sb.append(if (idx in SYMBOL_MAP.indices) SYMBOL_MAP[idx] else ch)
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}
