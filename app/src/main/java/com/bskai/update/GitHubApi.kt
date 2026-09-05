package com.bskai.update

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

object GitHubApi {

    private const val OWNER = "1953187487"
    private const val REPO = "BSK-AI"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun listReleases(): List<RemoteRelease> = suspendCancellableCoroutine { cont ->
        val request = Request.Builder()
            .url("https://api.github.com/repos/$OWNER/$REPO/releases?per_page=30")
            .addHeader("Accept", "application/vnd.github+json")
            .build()
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                if (cont.isActive) cont.resume(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (cont.isActive) {
                        if (resp.isSuccessful) {
                            cont.resume(parseReleases(body))
                        } else {
                            cont.resume(emptyList())
                        }
                    }
                }
            }
        })
        cont.invokeOnCancellation { call.cancel() }
    }
}
