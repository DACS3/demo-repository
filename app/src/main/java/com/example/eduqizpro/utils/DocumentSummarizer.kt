package com.example.eduqizpro.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DocumentSummarizer(context: Context) : BaseAIModel(context) {

    suspend fun summarizeDocument(uri: Uri, userPrompt: String): String = withContext(Dispatchers.IO) {
        val fileContent = try {
            DocumentReader.readTextFromUri(context, uri)
        } catch (e: Exception) {
            Log.e("DocumentSummarizer", "Read error", e)
            return@withContext "Lỗi: Không thể đọc nội dung file."
        }

        if (fileContent.isBlank() || fileContent.startsWith("Lỗi")) {
            return@withContext "Lỗi: File trống hoặc không đọc được nội dung văn bản."
        }

        // Giới hạn độ dài văn bản để tránh quá tải token (khoảng 100k ký tự cho Gemini 1.5 Flash là an toàn)
        val limitedContent = if (fileContent.length > 100000) {
            fileContent.substring(0, 100000) + "... [Văn bản quá dài, đã bị cắt bớt]"
        } else {
            fileContent
        }

        val finalPrompt = """
            Bạn là một chuyên gia phân tích và tóm tắt văn bản chuyên nghiệp.
            Hãy giúp tôi tóm tắt tài liệu dưới đây bằng tiếng Việt.
            
            Yêu cầu riêng từ người dùng: $userPrompt
            
            Nội dung tài liệu:
            ---
            $limitedContent
            ---
            
            Hãy trình bày kết quả theo cấu trúc sau:
            1. **Tổng quan**: 1-2 câu về chủ đề chính của tài liệu.
            2. **Các điểm chính**: Liệt kê các ý quan trọng nhất dưới dạng gạch đầu dòng (-).
            3. **Kết luận/Đánh giá**: Tóm lược giá trị của tài liệu này.
            
            Lưu ý: Giữ giọng văn khách quan, chính xác và chuyên nghiệp.
        """.trimIndent()

        return@withContext try {
            val response = generativeModel.generateContent(finalPrompt)
            response.text ?: "AI không thể trích xuất nội dung tóm tắt. Vui lòng thử lại."
        } catch (e: Exception) {
            Log.e("DocumentSummarizer", "AI Error", e)
            "Lỗi kết nối AI: ${e.localizedMessage ?: "Vui lòng kiểm tra kết nối mạng hoặc API Key."}"
        }
    }
}
