package com.example.eduqizpro.utils

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.GenerativeModel

open class BaseAIModel(protected val context: Context) {

    protected val generativeModel: GenerativeModel by lazy {
        val firebaseAI = FirebaseAI.getInstance(
            FirebaseApp.getInstance(),
            GenerativeBackend.googleAI()
        )

        firebaseAI.generativeModel(
            // === Thứ tự ưu tiên khuyến nghị 2026 ===
            modelName = "gemini-2.5-flash-lite",     // ← Thử cái này trước (nhanh + ổn định hơn)
            // modelName = "gemini-2.5-flash",        // Fallback cũ của bạn
            // modelName = "gemini-1.5-flash",        // Stable lâu năm

            generationConfig = generationConfig {
                temperature = 0.3f      // Giảm xuống để ít sáng tạo hơn → ít lỗi hơn
                topK = 40
                topP = 0.95f
                maxOutputTokens = 8192  // Tăng lên 8192 để hỗ trợ tạo số lượng câu hỏi lớn (ví dụ: 50 câu) không bị cắt nửa chừng
            }
        )
    }
}