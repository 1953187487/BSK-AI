package com.bskai.models

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class ModelStatus(val key: String) {
    NOT_DOWNLOADED("not_downloaded"),
    DOWNLOADING("downloading"),
    READY("ready"),
    FAILED("failed");

    companion object {
        fun fromKey(key: String?): ModelStatus =
            entries.firstOrNull { it.key == key } ?: NOT_DOWNLOADED
    }
}

data class LocalModel(
    val catalogId: String,
    val name: String,
    val fileName: String,
    val url: String,
    val sizeHint: String,
    val parameters: String,
    val quant: String,
    val status: ModelStatus = ModelStatus.NOT_DOWNLOADED,
    val progress: Float = 0f,
    val localPath: String = "",
    val downloadedBytes: Long = 0
) {
    fun withStatus(status: ModelStatus, progress: Float = this.progress, localPath: String = this.localPath, downloadedBytes: Long = this.downloadedBytes) =
        copy(status = status, progress = progress, localPath = localPath, downloadedBytes = downloadedBytes)
}

class ModelStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("bsk_ai_models", Context.MODE_PRIVATE)

    @Volatile
    var state: List<LocalModel> = load()
        private set

    fun modelsDir(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "models")

    fun snapshot(): List<LocalModel> = state

    fun updateAll(context: Context, transform: (LocalModel) -> LocalModel) {
        state = state.map(transform)
        persist()
        // 将 ready 模型的磁盘占用同步回 state
        state = state.map { m ->
            if (m.status == ModelStatus.READY && m.localPath.isNotEmpty()) {
                val f = File(m.localPath)
                if (f.exists()) m.copy(downloadedBytes = f.length()) else m
            } else m
        }
        persist()
    }

    fun updateOne(context: Context, catalogId: String, transform: (LocalModel) -> LocalModel) {
        state = state.map { if (it.catalogId == catalogId) transform(it) else it }
        persist()
    }

    fun addCustom(context: Context, name: String, url: String, fileName: String, sizeHint: String) {
        val entry = LocalModel(
            catalogId = "custom_" + System.currentTimeMillis(),
            name = name,
            fileName = fileName,
            url = url,
            sizeHint = sizeHint,
            parameters = "custom",
            quant = "GGUF"
        )
        state = state + entry
        persist()
    }

    fun remove(context: Context, catalogId: String) {
        val target = state.firstOrNull { it.catalogId == catalogId }
        if (target != null && target.localPath.isNotEmpty()) {
            File(target.localPath).delete()
        }
        state = state.filterNot { it.catalogId == catalogId }
        persist()
    }

    private fun load(): List<LocalModel> {
        val raw = prefs.getString("models", null) ?: return ModelCatalog.entries.map { it.toLocalModel() }
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return ModelCatalog.entries.map { it.toLocalModel() }
        val list = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    LocalModel(
                        catalogId = o.optString("catalogId"),
                        name = o.optString("name"),
                        fileName = o.optString("fileName"),
                        url = o.optString("url"),
                        sizeHint = o.optString("sizeHint"),
                        parameters = o.optString("parameters"),
                        quant = o.optString("quant"),
                        status = ModelStatus.fromKey(o.optString("status")),
                        progress = o.optDouble("progress", 0.0).toFloat(),
                        localPath = o.optString("localPath"),
                        downloadedBytes = o.optLong("downloadedBytes")
                    )
                )
            }
        }
        // 保证内置目录始终存在
        val ids = list.map { it.catalogId }
        val merged = ModelCatalog.entries.filterNot { ids.contains(it.id) }.map { it.toLocalModel() } + list
        return merged
    }

    private fun persist() {
        val arr = JSONArray()
        state.forEach { m ->
            arr.put(
                JSONObject()
                    .put("catalogId", m.catalogId)
                    .put("name", m.name)
                    .put("fileName", m.fileName)
                    .put("url", m.url)
                    .put("sizeHint", m.sizeHint)
                    .put("parameters", m.parameters)
                    .put("quant", m.quant)
                    .put("status", m.status.key)
                    .put("progress", m.progress.toDouble())
                    .put("localPath", m.localPath)
                    .put("downloadedBytes", m.downloadedBytes)
            )
        }
        prefs.edit().putString("models", arr.toString()).apply()
    }
}

private fun ModelCatalogEntry.toLocalModel() = LocalModel(
    catalogId = id,
    name = name,
    fileName = fileName,
    url = url,
    sizeHint = sizeHint,
    parameters = parameters,
    quant = quant
)
