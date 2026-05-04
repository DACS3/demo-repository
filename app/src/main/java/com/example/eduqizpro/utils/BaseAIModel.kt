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
            // === CÁC MODEL ĐANG HOẠT ĐỘNG TỐT (thử theo thứ tự) ===
            modelName = "gemini-2.5-flash",           // ← Khuyến nghị dùng tạm (ổn định)
            // modelName = "gemini-2.5-flash-lite",   // Nhanh & rẻ hơn
            // modelName = "gemini-1.5-flash",        // Fallback cũ
            // modelName = "gemini-3.1-flash",        // Thử nếu muốn model mới

            generationConfig = generationConfig {
                temperature = 0.25f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 8192
            }
        )
    }
}