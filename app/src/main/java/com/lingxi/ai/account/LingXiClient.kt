package com.lingxi.ai.account

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Retrofit 客户端:对接 AURA 沙盒服务器账号/管理员 API。
 */
object LingXiClient {
    private val gson = GsonBuilder().create()

    fun create(baseUrl: String = LingXiHosts.DEFAULT_BASE): LingXiApi {
        val http = OkHttpClient()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(LingXiApi::class.java)
    }
}
