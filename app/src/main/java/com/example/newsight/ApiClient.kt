package com.example.newsight

import okhttp3.*
import java.io.IOException

object ApiClient {
    private val client = OkHttpClient()
    private const val BASE_URL = "http://10.0.2.2:8000"

    fun postContact(
        userId: Int,
        name: String,
        phone: String,
        relationship: String?,
        address: String?,
        callback: ApiCallback
    ) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("user_id", userId.toString())
            .addFormDataPart("name", name)
            .addFormDataPart("phone", phone)
            .addFormDataPart("relationship", relationship ?: "")
            .addFormDataPart("address", address ?: "")
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/contacts")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                val message = response.body?.string() ?: "No response"
                callback.onResult(response.isSuccessful, message)
            }
        })
    }
}

