package com.floatai.data

import android.content.Context
import com.floatai.data.model.Character
import com.floatai.data.model.DEFAULT_CHARACTER
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 角色仓库 v1.0.4：
 *  - 持久化到 filesDir/characters.json
 *  - 默认包含一个内置角色（不可删除）
 *  - 当前激活角色 id 保存在 SharedPreferences
 */
class CharacterRepository(context: Context) {

    private val app = context.applicationContext
    private val file: File = File(app.filesDir, "characters.json")
    private val prefs = app.getSharedPreferences("float_ai_prefs", Context.MODE_PRIVATE)

    private val _characters = MutableStateFlow(loadFromDisk())
    val characters: StateFlow<List<Character>> = _characters.asStateFlow()

    private val _activeId = MutableStateFlow(prefs.getString(KEY_ACTIVE_ID, DEFAULT_CHARACTER.id) ?: DEFAULT_CHARACTER.id)
    val activeId: StateFlow<String> = _activeId.asStateFlow()

    fun active(): Character =
        _characters.value.firstOrNull { it.id == _activeId.value } ?: DEFAULT_CHARACTER

    fun setActive(id: String) {
        if (_characters.value.none { it.id == id }) return
        _activeId.value = id
        prefs.edit().putString(KEY_ACTIVE_ID, id).apply()
    }

    fun add(character: Character) {
        _characters.update { list -> list + character }
        saveToDisk(_characters.value)
    }

    fun update(character: Character) {
        _characters.update { list -> list.map { if (it.id == character.id) character else it } }
        saveToDisk(_characters.value)
    }

    fun delete(id: String) {
        val list = _characters.value
        if (list.firstOrNull { it.id == id }?.builtin == true) return  // 内置不可删
        _characters.update { it.filterNot { c -> c.id == id } }
        saveToDisk(_characters.value)
        if (_activeId.value == id) setActive(DEFAULT_CHARACTER.id)
    }

    private fun loadFromDisk(): List<Character> {
        if (!file.exists()) return listOf(DEFAULT_CHARACTER)
        return try {
            val arr = JSONArray(file.readText())
            buildList {
                add(DEFAULT_CHARACTER)  // 内置永远在第一位
                for (i in 0 until arr.length()) {
                    val c = fromJson(arr.getJSONObject(i))
                    if (c.id != DEFAULT_CHARACTER.id) add(c)
                }
            }
        } catch (_: Exception) {
            listOf(DEFAULT_CHARACTER)
        }
    }

    private fun saveToDisk(list: List<Character>) {
        val arr = JSONArray()
        list.filter { !it.builtin }.forEach { arr.put(toJson(it)) }
        file.writeText(arr.toString())
    }

    private fun toJson(c: Character) = JSONObject()
        .put("id", c.id)
        .put("name", c.name)
        .put("avatar", c.avatar ?: JSONObject.NULL)
        .put("systemPrompt", c.systemPrompt)
        .put("greeting", c.greeting)
        .put("temperature", c.temperature.toDouble())
        .put("createdAt", c.createdAt)

    private fun fromJson(o: JSONObject) = Character(
        id = o.optString("id"),
        name = o.optString("name"),
        avatar = if (o.isNull("avatar")) null else o.optString("avatar"),
        systemPrompt = o.optString("systemPrompt"),
        greeting = o.optString("greeting"),
        temperature = o.optFloat("temperature", 0.7f),
        builtin = false,
        createdAt = o.optLong("createdAt")
    )

    companion object {
        private const val KEY_ACTIVE_ID = "active_character_id"
    }
}
