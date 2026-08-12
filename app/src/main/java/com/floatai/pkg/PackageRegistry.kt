package com.floatai.pkg

import org.json.JSONObject

/**
 * 预置库描述：供用户项目导入，或供 AI 引用生成代码。
 *
 * 数据格式对齐 Maven 坐标 + Compose/Navigation 等常见依赖。
 * 用户也可以在 SettingsScreen 自行添加自定义库描述。
 */
data class PackageDescriptor(
    val group: String,
    val artifact: String,
    val version: String,
    val description: String,
    val tags: List<String> = emptyList()
) {
    /** Maven 坐标。 */
    val coordinate: String get() = "$group:$artifact:$version"

    /** Gradle implementation 行。 */
    val gradleLine: String get() = "implementation \"$coordinate\""

    fun toJson(): JSONObject = JSONObject()
        .put("group", group)
        .put("artifact", artifact)
        .put("version", version)
        .put("description", description)
        .put("tags", org.json.JSONArray(tags))
}

/**
 * 预置库列表（精选 Android 开发常用库）。
 *
 * 注意：所有库都不在应用内下载，仅作为元数据描述。用户导入后由
 * AppShell 中的「构建」功能按需在设备上下载对应 jar/aar。
 */
object PackageRegistry {

    val builtIn: List<PackageDescriptor> = listOf(
        // Core / AppCompat
        PackageDescriptor(
            group = "androidx.core",
            artifact = "core-ktx",
            version = "1.12.0",
            description = "Kotlin 扩展核心库",
            tags = listOf("core", "kotlin")
        ),
        PackageDescriptor(
            group = "androidx.appcompat",
            artifact = "appcompat",
            version = "1.6.1",
            description = "向后兼容支持库",
            tags = listOf("compat")
        ),
        PackageDescriptor(
            group = "com.google.android.material",
            artifact = "material",
            version = "1.11.0",
            description = "Material Components for Android",
            tags = listOf("ui", "material")
        ),

        // Lifecycle
        PackageDescriptor(
            group = "androidx.lifecycle",
            artifact = "lifecycle-runtime-ktx",
            version = "2.7.0",
            description = "Lifecycle runtime Kotlin 扩展",
            tags = listOf("lifecycle")
        ),
        PackageDescriptor(
            group = "androidx.lifecycle",
            artifact = "lifecycle-viewmodel-compose",
            version = "2.7.0",
            description = "ViewModel + Compose 集成",
            tags = listOf("lifecycle", "compose")
        ),

        // Compose
        PackageDescriptor(
            group = "androidx.compose.ui",
            artifact = "ui",
            version = "1.6.1",
            description = "Compose UI 核心",
            tags = listOf("compose", "ui")
        ),
        PackageDescriptor(
            group = "androidx.compose.material3",
            artifact = "material3",
            version = "1.2.0",
            description = "Material 3 设计组件",
            tags = listOf("compose", "material")
        ),
        PackageDescriptor(
            group = "androidx.compose.material",
            artifact = "material-icons-extended",
            version = "1.6.1",
            description = "扩展图标库",
            tags = listOf("compose", "icons")
        ),
        PackageDescriptor(
            group = "androidx.compose.runtime",
            artifact = "runtime",
            version = "1.6.1",
            description = "Compose runtime 基础",
            tags = listOf("compose")
        ),

        // Navigation
        PackageDescriptor(
            group = "androidx.navigation",
            artifact = "navigation-compose",
            version = "2.7.7",
            description = "Compose Navigation 路由",
            tags = listOf("navigation", "compose")
        ),

        // Activity
        PackageDescriptor(
            group = "androidx.activity",
            artifact = "activity-compose",
            version = "1.8.2",
            description = "Activity + Compose 桥接",
            tags = listOf("activity", "compose")
        ),

        // DataStore
        PackageDescriptor(
            group = "androidx.datastore",
            artifact = "datastore-preferences",
            version = "1.0.0",
            description = "Preferences DataStore（替代 SharedPreferences）",
            tags = listOf("storage")
        ),

        // 网络
        PackageDescriptor(
            group = "com.squareup.retrofit2",
            artifact = "retrofit",
            version = "2.9.0",
            description = "类型安全 HTTP 客户端",
            tags = listOf("network")
        ),
        PackageDescriptor(
            group = "com.squareup.okhttp3",
            artifact = "okhttp",
            version = "4.12.0",
            description = "OkHttp 网络栈",
            tags = listOf("network")
        ),
        PackageDescriptor(
            group = "com.squareup.moshi",
            artifact = "moshi",
            version = "1.15.0",
            description = "Kotlin JSON 解析",
            tags = listOf("json", "network")
        ),

        // 协程
        PackageDescriptor(
            group = "org.jetbrains.kotlinx",
            artifact = "kotlinx-coroutines-android",
            version = "1.7.3",
            description = "Kotlin 协程 Android 平台",
            tags = listOf("coroutines")
        ),

        // Room
        PackageDescriptor(
            group = "androidx.room",
            artifact = "room-runtime",
            version = "2.6.1",
            description = "Room 数据库运行时",
            tags = listOf("database")
        ),
        PackageDescriptor(
            group = "androidx.room",
            artifact = "room-ktx",
            version = "2.6.1",
            description = "Room Kotlin 扩展",
            tags = listOf("database", "kotlin")
        ),

        // Coil 图片加载
        PackageDescriptor(
            group = "io.coil-kt",
            artifact = "coil-compose",
            version = "2.5.0",
            description = "Compose 图片加载",
            tags = listOf("image", "compose")
        )
    )

    fun findByCoordinate(coordinate: String): PackageDescriptor? =
        builtIn.firstOrNull { it.coordinate == coordinate }

    fun search(query: String): List<PackageDescriptor> {
        if (query.isBlank()) return builtIn
        val q = query.lowercase()
        return builtIn.filter {
            it.coordinate.lowercase().contains(q) ||
            it.description.lowercase().contains(q) ||
            it.tags.any { tag -> tag.lowercase().contains(q) }
        }
    }
}
