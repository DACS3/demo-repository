package com.example.eduqizpro.utils

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.GenerativeModel

open class BaseAIModel(protected val context: Context) {

    protected val generativeModel: GenerativeModel by lazy {
        // Sử dụng googleAI() backend để đảm bảo tương thích tốt nhất trên mọi khu vực và tránh lỗi kết nối
        val firebaseAI = FirebaseAI.getInstance(
            FirebaseApp.getInstance(),
            GenerativeBackend.googleAI() 
        )

        firebaseAI.generativeModel(
            // Gemini 1.5 Flash là model ổn định nhất, hỗ trợ xử lý văn bản dài rất tốt cho việc tóm tắt
            modelName = "gemini-1.5-flash",

            generationConfig = generationConfig {
                temperature = 0.2f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 8192
            }
        )
    }
}
