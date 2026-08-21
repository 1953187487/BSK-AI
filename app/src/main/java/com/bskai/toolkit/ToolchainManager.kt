package com.bskai.toolkit

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

enum class ComponentStatus(val key: String) {
    CHECKING("checking"),
    DOWNLOADING("downloading"),
    READY("ready"),
    MISSING("missing");

    companion object {
        fun fromKey(key: String?): ComponentStatus =
            entries.firstOrNull { it.key == key } ?: MISSING
    }
}

data class ToolchainComponent(
    val id: String,
    val name: String,
    val description: String,
    val status: ComponentStatus,
    val detail: String,
    val installHint: String
)

class ToolchainManager(private val context: Context) {

    val rootDir: File get() =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "toolchain").apply { mkdirs() }

    fun androidJarFile(): File = File(rootDir, "android-34/android.jar")

    fun isTermuxInstalled(): Boolean =
        runCatching {
            context.packageManager.getPackageInfo("com.termux", 0)
            true
        }.getOrDefault(false)

    fun projectRoot(): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "projects").apply { mkdirs() }

    fun components(): List<ToolchainComponent> = listOf(
        ToolchainComponent(
            id = "android_jar",
            name = "android.jar (API 34)",
            description = "Android 平台类库，编译时 classpath 必需，由 BSK AI 自动下载",
            status = if (androidJarFile().exists()) ComponentStatus.READY else ComponentStatus.MISSING,
            detail = if (androidJarFile().exists()) "已就绪" else "未下载",
            installHint = "点击下载（约 24MB）"
        ),
        ToolchainComponent(
            id = "termux",
            name = "Termux 终端环境",
            description = "提供 JDK(javac)、aapt2、d8、apksigner 等构建工具",
            status = if (isTermuxInstalled()) ComponentStatus.READY else ComponentStatus.MISSING,
            detail = if (isTermuxInstalled()) "已安装" else "未安装",
            installHint = "请从 F-Droid 安装 Termux"
        ),
        ToolchainComponent(
            id = "jdk",
            name = "JDK 17 (javac)",
            description = "Java 编译器，在 Termux 中安装：pkg install openjdk-17",
            status = if (isTermuxInstalled()) ComponentStatus.CHECKING else ComponentStatus.MISSING,
            detail = "Termux: pkg install openjdk-17",
            installHint = "pkg install openjdk-17"
        ),
        ToolchainComponent(
            id = "build_tools",
            name = "构建工具 (aapt2/d8/apksigner/zipalign)",
            description = "Android 资源编译、字节码转换与签名，在 Termux 中安装",
            status = if (isTermuxInstalled()) ComponentStatus.CHECKING else ComponentStatus.MISSING,
            detail = "Termux: pkg install aapt2 d8 apksigner zipalign",
            installHint = "pkg install aapt2 d8 apksigner zipalign"
        )
    )

    suspend fun downloadAndroidJar(
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val target = androidJarFile()
        if (target.exists()) return@withContext true
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
        val candidates = listOf(
            "https://dl.google.com/android/repository/platform-34_r02.zip",
            "https://dl.google.com/android/repository/platform-34_r01.zip"
        )
        runCatching {
            var done = false
            for (url in candidates) {
                if (done) break
                runCatching {
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { resp ->
                        if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
                        val body = resp.body ?: throw Exception("空响应")
                        val tmp = File(rootDir, "platform-34.zip")
                        body.byteStream().use { input ->
                            tmp.outputStream().use { out -> input.copyTo(out, DEFAULT_BUFFER_SIZE) }
                        }
                        // 解压 android.jar
                        ZipInputStream(tmp.inputStream().buffered()).use { zip ->
                            var entry = zip.nextEntry
                            while (entry != null) {
                                if (entry.name == "android.jar") {
                                    target.parentFile?.mkdirs()
                                    zip.copyTo(target.outputStream())
                                    done = true
                                    break
                                }
                                zip.closeEntry()
                                entry = zip.nextEntry
                            }
                        }
                        tmp.delete()
                    }
                    onProgress(1f)
                }.onFailure { onProgress(0f) }
            }
            if (!done) throw Exception("下载失败")
            done
        }.getOrDefault(false)
    }

    /**
     * 生成 Termux 一键安装构建依赖的脚本。
     */
    fun buildToolchainScript(): String = """
        pkg update -y
        pkg install -y openjdk-17 aapt2 d8 apksigner zipalign
        echo "TOOLCHAIN_INSTALL_DONE"
    """.trimIndent()

    fun saveTermuxInstallScript(): File {
        val f = File(rootDir, "install_toolchain.sh")
        f.writeText(buildToolchainScript())
        return f
    }
}
