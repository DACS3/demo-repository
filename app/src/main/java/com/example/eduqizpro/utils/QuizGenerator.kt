package com.example.eduqizpro.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuizGenerator(context: Context) : BaseAIModel(context) {

    suspend fun generateQuiz(uri: Uri?, userInstruction: String): String = withContext(Dispatchers.IO) {
        val fileContent = uri?.let { DocumentReader.readTextFromUri(context, it) } ?: ""

        if (fileContent.isBlank() && uri != null) {
            return@withContext "Không thể trích xuất văn bản từ file. Vui lòng thử file khác."
        }

        val finalPrompt = """
Bạn là chuyên gia tạo đề trắc nghiệm chất lượng cao bằng tiếng Việt.

Yêu cầu của người dùng: $userInstruction

Nội dung tài liệu:
$fileContent

**YÊU CẦU BẮT BUỘC:**
- Trả về **DUY NHẤT** một mảng JSON hợp lệ. Không thêm bất kỳ chữ nào khác (không ```json, không giải thích).
- Định dạng chính xác:
[
  {
    "question": "Nội dung câu hỏi?",
    "options": ["Đáp án 1", "Đáp án 2", "Đáp án 3", "Đáp án 4"],
    "correctAnswer": 0
  }
]
- correctAnswer là số từ 0 đến 3 (0 = A).
""".trimIndent()

        return@withContext try {
            val response = generativeModel.generateContent(finalPrompt)
            val resultText = response.text?.trim() ?: ""
            if (resultText.isBlank()) "Lỗi: AI không trả về nội dung."
            else extractJsonArray(resultText)
        } catch (e: Exception) {
            "Lỗi AI: ${e.localizedMessage ?: e.message}"
        }
    }

    private fun extractJsonArray(input: String): String {
        val start = input.indexOf("[")
        val end = input.lastIndexOf("]")
        return if (start != -1 && end != -1 && end > start) {
            input.substring(start, end + 1)
        } else input.trim()
    }
}