package com.example.eduqizpro.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FlashCardGenerator(context: Context) : BaseAIModel(context) {

    suspend fun generateFlashCardsFromImage(uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = getBitmapFromUri(uri)
            ?: return@withContext "ERROR: Không thể đọc hình ảnh."

        val finalPrompt = """
Bạn là một trợ lý giáo dục AI chuyên nghiệp.
Hãy phân tích hình ảnh được tải lên (hình ảnh này có thể là bảng từ vựng, danh sách câu hỏi/đáp án hoặc hình ảnh học tập).
Trích xuất tất cả các từ vựng, câu hỏi hoặc cặp thông tin học tập xuất hiện trong hình ảnh và chuyển chúng thành danh sách các Flashcards tương ứng.

**YÊU CẦU ĐỊNH DẠNG ĐẦU RA (BẮT BUỘC):**
- Chỉ trả về duy nhất một chuỗi mảng JSON hợp lệ. Không viết thêm bất kỳ chữ nào khác (không ```json, không giải thích).
- Cấu trúc JSON:
[
  {
    "front": "Từ vựng / Câu hỏi / Mặt trước",
    "back": "Nghĩa của từ / Câu trả lời / Mặt sau"
  }
]
- Đảm bảo trích xuất chính xác và đầy đủ các nội dung trong hình.
        """.trimIndent()

        return@withContext try {
            val inputContent = content {
                image(bitmap)
                text(finalPrompt)
            }
            val response = generativeModel.generateContent(inputContent)
            val resultText = response.text?.trim() ?: ""
            if (resultText.isBlank()) {
                "ERROR: AI không phản hồi nội dung."
            } else {
                extractJsonArray(resultText)
            }
        } catch (e: Exception) {
            "ERROR: ${e.localizedMessage ?: e.message}"
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
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
