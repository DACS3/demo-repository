package com.example.eduqizpro.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// ────────────────────────────────────────────────────────────────────────────
// CẤU HÌNH SEPAY — điền API Token sau khi đăng ký tại https://sepay.vn
// Bước 1: Đăng ký tài khoản miễn phí tại https://sepay.vn
// Bước 2: Liên kết tài khoản ngân hàng của bạn (MBBank, Vietcombank...)
// Bước 3: Vào API Access -> Tạo API Token mới -> Dán vào đây
// ────────────────────────────────────────────────────────────────────────────
const val SEPAY_API_KEY = "" // ← API Token từ sepay.vn

sealed class PaymentStatus {
    object Waiting : PaymentStatus()
    data class Detected(val transactionId: String, val amount: Long) : PaymentStatus()
    data class Error(val message: String) : PaymentStatus()
}

class SePayRepository {

    /**
     * Polling SePay API mỗi [intervalMs] ms để tìm giao dịch khớp txCode và amount.
     * Flow tự động emit PaymentStatus.Detected khi tìm thấy giao dịch chuyển khoản khớp.
     */
    fun pollForPayment(
        txCode: String,
        amount: Long,
        intervalMs: Long = 5_000L
    ): Flow<PaymentStatus> = flow {
        emit(PaymentStatus.Waiting)

        // Chỉ poll nếu API key đã được cấu hình
        if (SEPAY_API_KEY == "DIEN_API_KEY_SEPAY_VAO_DAY" || SEPAY_API_KEY.isBlank()) {
            emit(PaymentStatus.Error("Chưa cấu hình SEPAY_API_KEY"))
            return@flow
        }

        android.util.Log.d("SePay", "🔍 Bắt đầu polling | txCode=$txCode | amount=$amount")

        while (true) {
            try {
                // ✅ Phải dùng Dispatchers.IO — Android cấm HTTP trên main thread
                val transactions = withContext(Dispatchers.IO) { fetchRecentTransactions() }
                android.util.Log.d("SePay", "📋 Nhận được ${transactions.size} giao dịch từ SePay")
                transactions.forEachIndexed { i, tx ->
                    android.util.Log.d("SePay", "  [$i] amountIn=${tx.amountIn} | content='${tx.transactionContent}'")
                }

                // Chỉ cần khớp txCode — txCode đã đủ unique (NX + uid6 + random)
                val matched = transactions.find { tx ->
                    tx.transactionContent.contains(txCode, ignoreCase = true)
                }
                if (matched != null) {
                    android.util.Log.d("SePay", "✅ Tìm thấy! id=${matched.id} amount=${matched.amountIn} content='${matched.transactionContent}'")
                    emit(PaymentStatus.Detected(matched.id, matched.amountIn))
                    return@flow
                } else {
                    android.util.Log.d("SePay", "⏳ Chưa khớp txCode='$txCode' trong ${transactions.size} giao dịch")
                    transactions.take(3).forEach {
                        android.util.Log.d("SePay", "   → '${it.transactionContent}'")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SePay", "❌ Lỗi khi poll: ${e.message}")
            }
            delay(intervalMs)
        }
    }

    /**
     * Gọi SePay API v2 để lấy danh sách giao dịch gần nhất (tối đa 20 giao dịch)
     */
    private fun fetchRecentTransactions(): List<SePayTransaction> {
        val url = URL("https://userapi.sepay.vn/v2/transactions?limit=20")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $SEPAY_API_KEY")
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 10_000
            readTimeout = 10_000
        }

        val responseCode = connection.responseCode
        if (responseCode != 200) {
            val errBody = connection.errorStream?.bufferedReader()?.readText() ?: ""
            android.util.Log.e("SePay", "❌ HTTP $responseCode | body=$errBody")
            throw Exception("SePay API error: HTTP $responseCode | $errBody")
        }

        val responseBody = connection.inputStream.bufferedReader().readText()
        android.util.Log.d("SePay", "📡 Raw JSON: $responseBody")
        connection.disconnect()

        return parseSePayResponse(responseBody)
    }

    /**
     * Dùng để debug: lấy nội dung giao dịch gần nhất dạng text hiển thị trên màn hình
     */
    suspend fun fetchDebugInfo(): String {
        return try {
            val txs = withContext(Dispatchers.IO) { fetchRecentTransactions() }
            if (txs.isEmpty()) return "⚠️ SePay trả về 0 giao dịch!\nKiểm tra lại liên kết ngân hàng."
            txs.take(5).joinToString("\n---\n") { tx ->
                "💰 ${tx.amountIn}đ\n📝 '${tx.transactionContent}'\n🕒 ${tx.transactionDate}"
            }
        } catch (e: Exception) {
            "❌ Lỗi: ${e.message}"
        }
    }

    private fun parseSePayResponse(json: String): List<SePayTransaction> {
        val root = JSONObject(json)
        val status = root.optString("status")
        if (status != "success") {
            android.util.Log.e("SePay", "❌ API trả về status='$status' (cần 'success')")
            return emptyList()
        }

        val data = root.optJSONArray("data") ?: return emptyList()

        return buildList {
            for (i in 0 until data.length()) {
                val obj = data.getJSONObject(i)
                add(
                    SePayTransaction(
                        id = obj.optString("id", ""),
                        amountIn = obj.optLong("amount_in", 0L),
                        transactionContent = obj.optString("transaction_content", ""),
                        transactionDate = obj.optString("transaction_date", "")
                    )
                )
            }
        }
    }
}

data class SePayTransaction(
    val id: String,
    val amountIn: Long,
    val transactionContent: String,
    val transactionDate: String
)
