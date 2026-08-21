package com.bskai.models

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class ProviderConfig(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val models: List<String> = emptyList(),
    val isActive: Boolean = false
)

class ProviderStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("bsk_ai_providers", Context.MODE_PRIVATE)

    @Volatile
    var providers: List<ProviderConfig> = load()
        private set

    fun activeProvider(): ProviderConfig? = providers.firstOrNull { it.isActive }
        ?: providers.firstOrNull()

    fun snapshot(): List<ProviderConfig> = providers

    fun setActive(id: String) {
        providers = providers.map { it.copy(isActive = it.id == id) }
        persist()
    }

    fun upsert(config: ProviderConfig) {
        val existing = providers.firstOrNull { it.id == config.id }
        providers = if (existing != null) {
            providers.map {
                if (it.id == config.id) config.copy(isActive = it.isActive) else it
            }
        } else {
            providers + config.copy(isActive = providers.isEmpty())
        }
        persist()
    }

    fun remove(id: String) {
        val removed = providers.firstOrNull { it.id == id }
        providers = providers.filterNot { it.id == id }
        if (removed?.isActive == true && providers.isNotEmpty()) {
            providers = providers.mapIndexed { i, p -> if (i == 0) p.copy(isActive = true) else p }
        }
        persist()
    }

    fun reload() {
        providers = load()
    }

    private fun load(): List<ProviderConfig> {
        val raw = prefs.getString("providers", null) ?: return emptyList()
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val models = buildList {
                    val ms = o.optJSONArray("models")
                    if (ms != null) for (j in 0 until ms.length()) add(ms.optString(j))
                }
                add(
                    ProviderConfig(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        baseUrl = o.optString("baseUrl"),
                        apiKey = o.optString("apiKey"),
                        model = o.optString("model"),
                        models = models,
                        isActive = o.optBoolean("isActive")
                    )
                )
            }
        }
    }

    private fun persist() {
        val arr = JSONArray()
        providers.forEach { p ->
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("baseUrl", p.baseUrl)
                    .put("apiKey", p.apiKey)
                    .put("model", p.model)
                    .put("models", JSONArray().apply { p.models.forEach { put(it) } })
                    .put("isActive", p.isActive)
            )
        }
        prefs.edit().putString("providers", arr.toString()).apply()
    }
}
