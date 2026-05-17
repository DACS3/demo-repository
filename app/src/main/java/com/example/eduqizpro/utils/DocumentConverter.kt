package com.example.eduqizpro.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.apache.poi.util.DefaultTempFileCreationStrategy
import org.apache.poi.util.TempFile
import org.apache.poi.xwpf.usermodel.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object DocumentConverter {

    private const val TAG = "DocumentConverter"

    init {
        // Prevent Apache POI from crashing on Android due to Log4j dependencies
        try {
            System.setProperty("org.apache.poi.util.POILogger", "org.apache.poi.util.NullLogger")
        } catch (t: Throwable) {
            // Ignore
        }
    }

    /**
     * Converts a Word (.docx) file to PDF using Android's native PdfDocument.
     * Uses a temporary file to ensure POI has a seekable source.
     */
    fun docxToPdf(context: Context, inputUri: Uri, outputStream: OutputStream): Boolean {
        Log.d(TAG, "docxToPdf started for $inputUri")
        var pdfDocument: PdfDocument? = null
        var doc: XWPFDocument? = null
        var tempFile: File? = null
        
        return try {
            // Configure temp directory for POI to avoid permission issues
            val poiTempDir = File(context.cacheDir, "poi-temp")
            if (!poiTempDir.exists()) poiTempDir.mkdirs()
            try {
                TempFile.setTempFileCreationStrategy(DefaultTempFileCreationStrategy(poiTempDir))
            } catch (e: Exception) {}

            // Copy URI content to a real file. POI works much better with Files than InputStreams.
            tempFile = File(context.cacheDir, "input_temp_${System.currentTimeMillis()}.docx")
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return false

            doc = XWPFDocument(tempFile.inputStream())

            pdfDocument = PdfDocument()
            val basePaint = TextPaint().apply {
                textSize = 11f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }
            val borderPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 0.5f
            }

            val pageWidth = 595 // A4 points
            val pageHeight = 842 // A4 points
            val margin = 50f
            val contentWidth = pageWidth - 2 * margin
            val pageContentHeight = pageHeight - 2 * margin

            var currentY = 0f
            var pageNumber = 1
            var currentPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = currentPage.canvas

            fun startNewPage() {
                pdfDocument.finishPage(currentPage)
                pageNumber++
                currentPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = currentPage.canvas
                currentY = 0f
            }

            fun checkNewPage(neededHeight: Float) {
                if (currentY + neededHeight > pageContentHeight) {
                    startNewPage()
                }
            }

            fun drawSpannable(spannable: SpannableStringBuilder, alignment: Layout.Alignment) {
                if (spannable.isEmpty()) return
                val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    StaticLayout.Builder.obtain(spannable, 0, spannable.length, basePaint, contentWidth.toInt())
                        .setAlignment(alignment)
                        .setLineSpacing(0f, 1.1f)
                        .setIncludePad(false)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    StaticLayout(spannable, basePaint, contentWidth.toInt(), alignment, 1.1f, 0f, false)
                }

                var startLine = 0
                while (startLine < layout.lineCount) {
                    var endLine = startLine
                    var blockHeight = 0f
                    while (endLine < layout.lineCount) {
                        val h = (layout.getLineBottom(endLine) - layout.getLineTop(startLine)).toFloat()
                        if (currentY + h > pageContentHeight) break
                        blockHeight = h
                        endLine++
                    }

                    if (startLine == endLine) {
                        if (currentY > 0) {
                            startNewPage()
                            continue
                        } else {
                            endLine = startLine + 1
                            blockHeight = (layout.getLineBottom(endLine) - layout.getLineTop(startLine)).toFloat()
                        }
                    }

                    canvas.save()
                    canvas.translate(margin, margin + currentY)
                    canvas.clipRect(0f, 0f, contentWidth, blockHeight)
                    canvas.translate(0f, -layout.getLineTop(startLine).toFloat())
                    layout.draw(canvas)
                    canvas.restore()

                    currentY += blockHeight
                    startLine = endLine
                    
                    if (startLine < layout.lineCount) {
                        startNewPage()
                    }
                }
                currentY += 5f
            }

            for (element in doc.bodyElements) {
                try {
                    when (element) {
                        is XWPFParagraph -> {
                            // Extract and draw images
                            for (run in element.runs) {
                                for (pic in run.embeddedPictures) {
                                    val data = pic.pictureData?.data ?: continue
                                    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size) ?: continue
                                    val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
                                    val targetWidth = if (bitmap.width > contentWidth) contentWidth else bitmap.width.toFloat()
                                    val targetHeight = targetWidth * aspectRatio
                                    
                                    checkNewPage(targetHeight + 10f)
                                    canvas.drawBitmap(bitmap, null, RectF(margin, margin + currentY, margin + targetWidth, margin + currentY + targetHeight), null)
                                    currentY += targetHeight + 10f
                                    bitmap.recycle()
                                }
                            }

                            val spannable = getSpannableFromParagraph(element)
                            val align = when (element.alignment) {
                                ParagraphAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
                                ParagraphAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
                                else -> Layout.Alignment.ALIGN_NORMAL
                            }

                            if (spannable.isNotEmpty()) {
                                drawSpannable(spannable, align)
                            } else if (element.runs.isEmpty()) {
                                currentY += 10f
                                checkNewPage(0f)
                            }
                            
                            if (element.isPageBreak) startNewPage()
                        }
                        is XWPFTable -> {
                            if (element.rows.isEmpty()) continue
                            val firstRow = element.getRow(0) ?: continue
                            val colCount = firstRow.tableCells?.size ?: 0
                            if (colCount == 0) continue
                            val colWidth = contentWidth / colCount

                            for (row in element.rows) {
                                var maxRowHeight = 0f
                                val cellLayouts = row.tableCells.map { cell ->
                                    val sb = SpannableStringBuilder()
                                    cell.paragraphs.forEach { p ->
                                        sb.append(getSpannableFromParagraph(p)).append("\n")
                                    }
                                    val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        StaticLayout.Builder.obtain(sb, 0, sb.length, basePaint, (colWidth - 10).toInt())
                                            .setLineSpacing(0f, 1.1f)
                                            .build()
                                    } else {
                                        @Suppress("DEPRECATION")
                                        StaticLayout(sb, basePaint, (colWidth - 10).toInt(), Layout.Alignment.ALIGN_NORMAL, 1.1f, 0f, false)
                                    }
                                    if (layout.height > maxRowHeight) maxRowHeight = layout.height.toFloat()
                                    layout
                                }
                                
                                val rowHeightNeeded = maxRowHeight + 10f
                                checkNewPage(rowHeightNeeded)

                                var currentX = 0f
                                for (i in row.tableCells.indices) {
                                    val x = margin + currentX
                                    val y = margin + currentY
                                    canvas.drawRect(x, y, x + colWidth, y + rowHeightNeeded, borderPaint)
                                    
                                    canvas.save()
                                    canvas.translate(x + 5f, y + 5f)
                                    if (i < cellLayouts.size) {
                                        cellLayouts[i].draw(canvas)
                                    }
                                    canvas.restore()
                                    currentX += colWidth
                                }
                                currentY += rowHeightNeeded
                            }
                            currentY += 10f
                            checkNewPage(0f)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing element", e)
                }
            }

            pdfDocument.finishPage(currentPage)
            pdfDocument.writeTo(outputStream)
            Log.d(TAG, "docxToPdf successful")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Fatal error in docxToPdf", t)
            false
        } finally {
            try {
                pdfDocument?.close()
                doc?.close()
                tempFile?.delete()
            } catch (e: Exception) {}
        }
    }

    private fun getSpannableFromParagraph(para: XWPFParagraph): SpannableStringBuilder {
        val spannable = SpannableStringBuilder()
        for (run in para.runs) {
            val start = spannable.length
            // More robust way to get text from XWPFRun in POI 4.1.2
            val text = try {
                run.text() ?: ""
            } catch (e: NoSuchMethodError) {
                run.getText(0) ?: ""
            } catch (e: Exception) {
                run.toString()
            }
            
            if (text.isEmpty()) continue
            spannable.append(text)
            
            if (run.isBold && run.isItalic) {
                spannable.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, spannable.length, 0)
            } else if (run.isBold) {
                spannable.setSpan(StyleSpan(Typeface.BOLD), start, spannable.length, 0)
            } else if (run.isItalic) {
                spannable.setSpan(StyleSpan(Typeface.ITALIC), start, spannable.length, 0)
            }
            
            if (run.underline != UnderlinePatterns.NONE) {
                spannable.setSpan(UnderlineSpan(), start, spannable.length, 0)
            }
            
            val fontSize = if (run.fontSize > 0) run.fontSize else 11
            spannable.setSpan(AbsoluteSizeSpan(fontSize, true), start, spannable.length, 0)
            
            val colorHex = run.color
            if (colorHex != null && colorHex.length == 6) {
                try {
                    val colorInt = Color.parseColor("#$colorHex")
                    spannable.setSpan(ForegroundColorSpan(colorInt), start, spannable.length, 0)
                } catch (e: Exception) {}
            }
        }
        return spannable
    }

    /**
     * Converts a PDF file to Word (.docx).
     * Extracts text from the PDF and populates a new Word document.
     */
    fun pdfToDocx(context: Context, inputUri: Uri, outputStream: OutputStream): Boolean {
        Log.d(TAG, "pdfToDocx started for $inputUri")
        var pdfDoc: PDDocument? = null
        return try {
            val inputStream = context.contentResolver.openInputStream(inputUri) ?: return false
            PDFBoxResourceLoader.init(context)
            pdfDoc = PDDocument.load(inputStream)
            
            val stripper = PDFTextStripper().apply {
                sortByPosition = true
                paragraphStart = "\n\n"
            }
            val text = stripper.getText(pdfDoc)
            pdfDoc.close()
            pdfDoc = null

            if (text.isNullOrBlank()) return false

            val doc = XWPFDocument()
            text.split("\n\n").forEach { pText ->
                if (pText.isNotBlank()) {
                    val paragraph = doc.createParagraph()
                    val run = paragraph.createRun()
                    run.setText(pText.trim())
                }
            }

            doc.write(outputStream)
            doc.close()
            Log.d(TAG, "pdfToDocx successful")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Fatal error in pdfToDocx", t)
            pdfDoc?.close()
            false
        }
    }
}
