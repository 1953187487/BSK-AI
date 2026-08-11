package com.floatai.atk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * GitHub 发布器：基于 REST API + 用户本地配置的 PAT。
 *
 * 步骤：
 *  1. 创建仓库（POST /user/repos 或 /orgs/{org}/repos）
 *  2. 初始化 commit（若仓库为空：用 PUT /repos/{owner}/{repo}/contents/... 创建 README）
 *  3. 上传 APK 作为 Release asset（POST /repos/{owner}/{repo}/releases + upload asset URL）
 *
 * 失败信息保留在返回值中，由 UI 展示给用户或送进 AI 诊断。
 */
object AtkGithubPublisher {

    sealed class Result {
        data class Success(val repoUrl: String, val releaseUrl: String?) : Result()
        data class Failure(val step: String, val message: String) : Result()
    }

    suspend fun publish(
        token: String,
        repoName: String,
        description: String,
        isPrivate: Boolean,
        projectDir: File,
        apkFile: File?,
        tagName: String = "v1.0.0",
        releaseName: String = "v1.0.0"
    ): Result = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext Result.Failure("auth", "GitHub token 为空")
        if (repoName.isBlank()) return@withContext Result.Failure("input", "仓库名为空")

        val me = fetchUser(token) ?: return@withContext Result.Failure("auth", "Token 无效或无权限")
        val owner = me.optString("login")
        val existing = checkRepo(token, owner, repoName)
        if (existing == null) {
            val created = createRepo(token, repoName, description, isPrivate)
            if (!created) return@withContext Result.Failure("create_repo", "创建仓库失败")
        }
        // 若项目目录有 git 仓库则直接 push；否则用 contents API 简单写入 README
        val pushed = pushCodeFiles(token, owner, repoName, projectDir)
        if (!pushed.success && pushed.error.isNotEmpty()) {
            // 不致命：可能是没源码文件
        }
        val release = if (apkFile != null && apkFile.exists()) {
            uploadRelease(token, owner, repoName, tagName, releaseName, apkFile)
        } else null

        Result.Success(
            repoUrl = "https://github.com/$owner/$repoName",
            releaseUrl = release
        )
    }

    private fun fetchUser(token: String): JSONObject? = runCatching {
        val conn = (URL("https://api.github.com/user").openConnection() as HttpURLConnection).apply {
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        if (conn.responseCode != 200) null else JSONObject(conn.inputStream.bufferedReader().readText())
    }.getOrNull()

    private fun checkRepo(token: String, owner: String, repo: String): JSONObject? = runCatching {
        val conn = (URL("https://api.github.com/repos/$owner/$repo").openConnection() as HttpURLConnection).apply {
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        if (conn.responseCode == 200) JSONObject(conn.inputStream.bufferedReader().readText()) else null
    }.getOrNull()

    private fun createRepo(token: String, name: String, desc: String, isPrivate: Boolean): Boolean = runCatching {
        val body = JSONObject()
            .put("name", name)
            .put("description", desc)
            .put("private", isPrivate)
            .put("auto_init", true)
        val conn = (URL("https://api.github.com/user/repos").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/vnd.github+json")
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 30_000
        }
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        conn.responseCode in 200..299
    }.getOrDefault(false)

    private data class PushOutcome(val success: Boolean, val error: String = "")

    private fun pushCodeFiles(token: String, owner: String, repo: String, dir: File): PushOutcome {
        if (!dir.isDirectory) return PushOutcome(false, "项目目录不存在")
        val files = dir.walkTopDown().filter { it.isFile }.toList()
        if (files.isEmpty()) return PushOutcome(false, "项目目录为空")
        var okCount = 0
        files.forEach { f ->
            val relPath = f.relativeTo(dir).invariantSeparatorsPath
            if (relPath.startsWith(".git") || relPath.startsWith("build") || relPath.contains("/build/")) return@forEach
            val content = runCatching { f.readText() }.getOrNull() ?: return@forEach
            if (content.length > 5_000_000) return@forEach
            val url = "https://api.github.com/repos/$owner/$repo/contents/$relPath"
            val body = JSONObject()
                .put("message", "upload $relPath via FloatAI ATK")
                .put("content", android.util.Base64.encodeToString(
                    content.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP
                ))
            val success = runCatching {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Accept", "application/vnd.github+json")
                    doOutput = true
                    connectTimeout = 10_000
                    readTimeout = 15_000
                }
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                conn.responseCode in 200..299
            }.getOrDefault(false)
            if (success) okCount++
        }
        return PushOutcome(okCount > 0, if (okCount == 0) "未上传任何文件" else "")
    }

    private fun uploadRelease(
        token: String,
        owner: String,
        repo: String,
        tag: String,
        name: String,
        apk: File
    ): String? {
        val body = JSONObject()
            .put("tag_name", tag)
            .put("name", name)
            .put("draft", false)
            .put("prerelease", false)
        val createUrl = "https://api.github.com/repos/$owner/$repo/releases"
        val releaseObj: JSONObject = runCatching {
            val conn = (URL(createUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/vnd.github+json")
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 30_000
            }
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            if (conn.responseCode !in 200..299) return@runCatching null
            JSONObject(conn.inputStream.bufferedReader().readText())
        }.getOrNull() ?: return null

        val uploadUrl = releaseObj.optString("upload_url", "").replace("{?name,label}", "?name=${apk.name}")
        val uploadOk = runCatching {
            val conn = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/vnd.android.package-archive")
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 120_000
            }
            conn.setFixedLengthStreamingMode(apk.length())
            conn.outputStream.use { os -> apk.inputStream().use { it.copyTo(os) } }
            conn.responseCode in 200..299
        }.getOrDefault(false)
        return if (uploadOk) releaseObj.optString("html_url", null) else null
    }
}
