package com.example.eduqizpro.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.*
import java.util.concurrent.TimeUnit

object DocumentConverter {
    private const val TAG = "DocumentConverter"

    // Mã API Key Cloudmersive
    private const val CLOUDMERSIVE_API_KEY = "93056111-3ecd-44db-9a63-7396a203233d"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * CHUYỂN WORD SANG PDF QUA API
     * Trả về null nếu thành công, hoặc String mô tả chi tiết lỗi nếu thất bại
     */
    fun docxToPdf(context: Context, inputUri: Uri, outputStream: OutputStream): String? {
        Log.d(TAG, "docxToPdf via API: Bắt đầu cho $inputUri")
        var tempFile: File? = null

        return try {
            // Bước 1: Tạo file tạm từ Uri Word đầu vào
            tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.docx")
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return "Không thể đọc tệp Word đầu vào."

            // Bước 2: Xây dựng Request gửi file lên Cloudmersive API
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "inputFile",
                    tempFile.name,
                    tempFile.asRequestBody("application/vnd.openxmlformats-officedocument.wordprocessingml.document".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.cloudmersive.com/convert/docx/to/pdf")
                .addHeader("Apikey", CLOUDMERSIVE_API_KEY)
                .post(requestBody)
                .build()

            // Bước 3: Thực thi gọi API và nhận luồng dữ liệu PDF trả về
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val msg = response.message
                    Log.e(TAG, "API lỗi: $code - $msg")
                    return when (code) {
                        401 -> "Mã API Key không hợp lệ hoặc hết hạn."
                        403 -> "Không có quyền truy cập API hoặc vượt quá giới hạn."
                        else -> "Lỗi từ máy chủ chuyển đổi (API: $code - $msg)"
                    }
                }

                val responseBody = response.body
                if (responseBody != null) {
                    // Ghi trực tiếp mảng byte của PDF vào outputStream của ứng dụng
                    responseBody.byteStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    outputStream.flush()
                    Log.d(TAG, "Chuyển đổi Word sang PDF thành công qua API!")
                    null
                } else {
                    "Phản hồi từ máy chủ trống."
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gọi API chuyển đổi Word sang PDF", e)
            "Lỗi kết nối hoặc xử lý tệp: ${e.message}"
        } finally {
            tempFile?.delete() // Xóa file tạm để tránh rác bộ nhớ
        }
    }

    /**
     * CHUYỂN PDF SANG WORD QUA API
     * Trả về null nếu thành công, hoặc String mô tả chi tiết lỗi nếu thất bại
     */
    fun pdfToDocx(context: Context, inputUri: Uri, outputStream: OutputStream): String? {
        Log.d(TAG, "pdfToDocx via API: Bắt đầu cho $inputUri")
        var tempFile: File? = null

        return try {
            // Bước 1: Tạo file tạm từ Uri PDF đầu vào
            tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return "Không thể đọc tệp PDF đầu vào."

            // Bước 2: Xây dựng Request gửi file lên Cloudmersive API
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "inputFile",
                    tempFile.name,
                    tempFile.asRequestBody("application/pdf".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.cloudmersive.com/convert/pdf/to/docx")
                .addHeader("Apikey", CLOUDMERSIVE_API_KEY)
                .post(requestBody)
                .build()

            // Bước 3: Thực thi gọi API và nhận luồng dữ liệu Word trả về
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val msg = response.message
                    Log.e(TAG, "API lỗi: $code - $msg")
                    return when (code) {
                        401 -> "Mã API Key không hợp lệ hoặc hết hạn."
                        403 -> "Không có quyền truy cập API hoặc vượt quá giới hạn."
                        else -> "Lỗi từ máy chủ chuyển đổi (API: $code - $msg)"
                    }
                }

                val responseBody = response.body
                if (responseBody != null) {
                    // Ghi trực tiếp mảng byte của Word vào outputStream của ứng dụng
                    responseBody.byteStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    outputStream.flush()
                    Log.d(TAG, "Chuyển đổi PDF sang Word thành công qua API!")
                    null
                } else {
                    "Phản hồi từ máy chủ trống."
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gọi API chuyển đổi PDF sang Word", e)
            "Lỗi kết nối hoặc xử lý tệp: ${e.message}"
        } finally {
            tempFile?.delete() // Xóa file tạm để tránh rác bộ nhớ
        }
    }
}
