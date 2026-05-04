package com.example.eduqizpro.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DocumentSummarizer(context: Context) : BaseAIModel(context) {

    suspend fun summarizeDocument(uri: Uri, userPrompt: String): String = withContext(Dispatchers.IO) {
        val fileContent = try {
            DocumentReader.readTextFromUri(context, uri)
        } catch (e: Exception) {
            return@withContext "Lỗi: Không thể đọc file."
        }

        if (fileContent.isBlank()) {
            return@withContext "Lỗi: File trống hoặc không đọc được nội dung."
        }

        val finalPrompt = """
            Bạn là chuyên gia tóm tắt văn bản chuyên nghiệp bằng tiếng Việt.
            Yêu cầu cụ thể của người dùng: $userPrompt
            
            Nội dung văn bản cần tóm tắt:
            $fileContent
            
            Yêu cầu kết quả:
            - Trình bày súc tích, rõ ràng, dễ hiểu.
            - Sử dụng gạch đầu dòng (-) cho các ý chính.
            - Giữ nguyên số liệu, năm tháng, tên riêng quan trọng.
            - Bố cục: 
              + Tổng quan ngắn gọn
              + Các ý chính
              + Kết luận hoặc điểm nổi bật
        """.trimIndent()

        return@withContext try {
            val response = generativeModel.generateContent(finalPrompt)
            response.text ?: "AI không thể tóm tắt tài liệu này."
        } catch (e: Exception) {
            "Lỗi kết nối AI: ${e.localizedMessage ?: e.message}"
        }
    }
}