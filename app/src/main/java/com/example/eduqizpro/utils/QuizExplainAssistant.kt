package com.example.eduqizpro.utils

import android.content.Context
import com.example.eduqizpro.data.model.Quiz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuizExplainAssistant(context: Context) : BaseAIModel(context) {

    suspend fun askAssistant(quiz: Quiz, userQuestion: String): String = withContext(Dispatchers.IO) {
        val quizContent = quiz.questions.mapIndexed { index, q ->
            val correctOption = q.options.getOrNull(q.correctAnswer) ?: "Chưa rõ"
            "Câu ${index + 1}: ${q.question}\n- Các đáp án: ${q.options.joinToString(" | ")}\n- Đáp án đúng: $correctOption"
        }.joinToString("\n\n")

        val finalPrompt = """
Bạn là một trợ lý giáo dục AI giải thích bài tập trong ứng dụng EduQizPro.
Nhiệm vụ duy nhất của bạn là giải đáp thắc mắc liên quan trực tiếp đến bộ đề trắc nghiệm sau đây:

Tên bộ đề: ${quiz.title}
Mô tả: ${quiz.description}

Nội dung câu hỏi và đáp án trong bộ đề:
$quizContent

Yêu cầu cực kỳ nghiêm ngặt:
1. Chỉ được trả lời nếu câu hỏi của người dùng liên quan trực tiếp đến kiến thức, khái niệm, câu hỏi, hoặc đáp án của bộ đề trắc nghiệm này.
2. Nếu câu hỏi của người dùng ngoài lề (ví dụ: các phép tính đơn giản như "1+1=?", kiến thức không có trong bộ đề, trò chuyện xã giao, yêu cầu viết code lập trình, v.v.), bạn bắt buộc phải trả lời:
"Xin lỗi, tôi chỉ có thể hỗ trợ giải đáp các câu hỏi liên quan đến bộ đề trắc nghiệm này."
3. Không trả lời bất kỳ thông tin ngoài lề nào nếu câu hỏi không thuộc nội dung hoặc kiến thức của bộ đề này.
4. Trả lời bằng tiếng Việt, ngắn gọn, dễ hiểu và tập trung vào bản chất câu hỏi.

Câu hỏi của người dùng: $userQuestion
        """.trimIndent()

        return@withContext try {
            val response = generativeModel.generateContent(finalPrompt)
            response.text?.trim() ?: "Xin lỗi, tôi không thể xử lý câu hỏi này lúc này."
        } catch (e: Exception) {
            "Lỗi AI: ${e.localizedMessage ?: e.message}"
        }
    }
}
