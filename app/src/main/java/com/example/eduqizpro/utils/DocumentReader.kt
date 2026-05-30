package com.example.eduqizpro.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.StringBuilder
import java.nio.charset.StandardCharsets

object DocumentReader {
    private const val TAG = "DocumentReader"
    
    private const val SYMBOL_MAP = " !∀#∃%&∍()*+,-./0123456789:;<=>?≅ΑΒΧΔΕΦΓΗΙϑΚΛΜΝΟΠΘΡΣΤΥςΩΞΨΖ[∴]⊥_‾αβχδεφγηιϕκλμνοπθρστυϖωξψζ{|}~" +
            "                                 " + 
            "ϒ′≤⁄∞ƒ♣♦♥♠↔←↑→↓°±″≥×∝∂•÷≠≡≈…⏐⎯↵ℵℑℜ℘⊗⊕∅∩∪⊃⊇⊄⊂⊆∈∉∠∇®©™∏√⋅¬∧∨⇔⇔⇐⇑⇒⇓◊⟨®©™∑⎛⎜⎝⎡⎢⎣⎧⎨⎩⎪"

    private fun setupThreadConfig() {
        try {
            val factory = "com.ctc.wstx.stax.WstxInputFactory"
            System.setProperty("javax.xml.stream.XMLInputFactory", factory)
            System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", factory)
            System.setProperty("javax.xml.stream.XMLOutputFactory", "com.ctc.wstx.stax.WstxOutputFactory")
            System.setProperty("javax.xml.stream.XMLEventFactory", "com.ctc.wstx.stax.WstxEventFactory")
            System.setProperty("org.apache.poi.util.XMLHelper.XMLInputFactory", factory)
            
            System.setProperty("org.apache.xmlbeans.impl.store.Locale.SaxLoader.canSetProperty", "false")
            System.setProperty("org.apache.xmlbeans.impl.store.Locale.SaxLoader.canSetLexicalHandler", "false")
            
            Thread.currentThread().contextClassLoader = DocumentReader::class.java.classLoader
        } catch (t: Throwable) {
            Log.e(TAG, "Config setup failed", t)
        }
    }

    fun readTextFromUri(context: Context, uri: Uri): String {
        setupThreadConfig()
        return try {
            val contentResolver = context.contentResolver
            val type = contentResolver.getType(uri) ?: ""
            val fileName = uri.lastPathSegment?.lowercase() ?: ""
            
            when {
                type.contains("pdf") || fileName.endsWith(".pdf") -> {
                    contentResolver.openInputStream(uri)?.use { readPdf(it, context) } ?: ""
                }
                type.contains("word") || type.contains("officedocument") || fileName.endsWith(".docx") -> {
                    readDocxDetailed(context, uri)
                }
                else -> {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Read error for $uri", e)
            "Lỗi khi đọc tệp: ${e.message}"
        }
    }

    private fun readPdf(inputStream: InputStream, context: Context): String {
        return try {
            PDFBoxResourceLoader.init(context)
            val document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper().apply {
                sortByPosition = true
            }
            val text = stripper.getText(document) ?: ""
            document.close()
            text
        } catch (e: Exception) {
            Log.e(TAG, "PDF read error", e)
            ""
        }
    }

    private fun readDocxDetailed(context: Context, uri: Uri): String {
        var tempFile: File? = null
        val fullText = StringBuilder()
        return try {
            tempFile = File(context.cacheDir, "temp_r_${System.currentTimeMillis()}.docx")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            }

            FileInputStream(tempFile).use { fis ->
                val doc = XWPFDocument(fis)
                for (element in doc.bodyElements) {
                    when (element) {
                        is XWPFParagraph -> {
                            val pText = extractParaWithSymbols(element)
                            if (pText.isNotBlank()) fullText.append(pText).append("\n")
                        }
                        is XWPFTable -> {
                            for (row in element.rows) {
                                val rowSb = StringBuilder()
                                for (cell in row.tableCells) {
                                    val cellSb = StringBuilder()
                                    for (p in cell.paragraphs) {
                                        cellSb.append(extractParaWithSymbols(p)).append(" ")
                                    }
                                    rowSb.append(cellSb.toString().trim()).append(" | ")
                                }
                                val rowText = rowSb.toString().trim().removeSuffix("|").trim()
                                if (rowText.isNotBlank()) fullText.append(rowText).append("\n")
                            }
                        }
                    }
                }
                doc.close()
            }
            fullText.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Docx read failed", e)
            ""
        } finally {
            tempFile?.delete()
        }
    }

    private fun extractParaWithSymbols(para: XWPFParagraph): String {
        val sb = StringBuilder()
        for (run in para.runs) {
            val text = run.getText(0) ?: run.toString()
            if (text.length == 1 && (text[0].code < 32 && text[0].code != 10 && text[0].code != 13)) continue
            sb.append(translateWordText(text))
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    private fun translateWordText(input: String): String {
        val sb = StringBuilder()
        for (char in input) {
            if (char.code in 0xF020..0xF0FF) {
                val index = char.code - 0xF020
                if (index in SYMBOL_MAP.indices) sb.append(SYMBOL_MAP[index])
                else sb.append(char)
            } else {
                sb.append(char)
            }
        }
        return sb.toString()
    }
}
