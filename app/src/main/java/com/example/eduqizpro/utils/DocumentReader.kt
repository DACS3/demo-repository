package com.example.eduqizpro.utils

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.InputStream

object DocumentReader {

    fun readTextFromUri(context: Context, uri: Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
            PDFBoxResourceLoader.init(context)

            val type = context.contentResolver.getType(uri) ?: ""
            val fileName = uri.toString().lowercase()

            when {
                type.contains("pdf") || fileName.contains(".pdf") -> readPdf(inputStream)
                type.contains("word") || fileName.endsWith(".doc") || fileName.endsWith(".docx") -> readDocx(inputStream)
                else -> inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun readPdf(inputStream: InputStream): String {
        return try {
            val document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper().apply {
                startPage = 1
                endPage = 200 // Increased page limit for conversion
                setSortByPosition(true)
            }
            val text = stripper.getText(document)
            document.close()
            text
        } catch (e: Exception) {
            ""
        }
    }

    private fun readDocx(inputStream: InputStream): String {
        return try {
            val doc = XWPFDocument(inputStream)
            val extractor = XWPFWordExtractor(doc)
            val text = extractor.text
            extractor.close()
            doc.close()
            text
        } catch (e: Exception) {
            ""
        }
    }
}
