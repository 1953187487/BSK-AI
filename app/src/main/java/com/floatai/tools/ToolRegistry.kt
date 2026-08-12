package com.floatai.tools

import android.content.Context
import com.floatai.lobster.bridge.LobsterAction
import com.floatai.lobster.bridge.LobsterBridge
import org.json.JSONObject

/**
 * AI 工具描述（OpenAI function calling format）。
 */
data class ToolDef(
    val name: String,
    val description: String,
    val parameters: JSONObject
) {
    fun toJson(): JSONObject = JSONObject()
        .put("type", "function")
        .put("function", JSONObject()
            .put("name", name)
            .put("description", description)
            .put("parameters", parameters))
}

/**
 * AI 工具注册中心 v1.0.4：
 *  - 提供 OpenAI 兼容的 tools 列表
 *  - 提供工具执行入口（call）
 *  - 支持小龙虾（控制手机）+ 通用工具（数学、时间、文件等）
 */
object ToolRegistry {

    private val tools = mutableListOf<ToolDef>()

    init {
        // 1. 小龙虾 - 控制手机（无障碍服务）
        register(
            ToolDef(
                name = "lobster_click",
                description = "小龙虾：按文本点击屏幕上的元素。需用户已启用小龙虾无障碍服务。",
                parameters = JSONObject()
                    .put("type", "object")
                    .put("properties", JSONObject().put(
                        "text", JSONObject()
                            .put("type", "string")
                            .put("description", "要点击的元素文本")
                    ))
                    .put("required", JSONArray("text"))
            )
        )
        register(
            ToolDef(
                name = "lobster_input",
                description = "小龙虾：在当前焦点输入框中输入文本。",
                parameters = JSONObject()
                    .put("type", "object")
                    .put("properties", JSONObject().put(
                        "text", JSONObject()
                            .put("type", "string")
                            .put("description", "要输入的文本")
                    ))
                    .put("required", JSONArray("text"))
            )
        )
        register(
            ToolDef(
                name = "lobster_launch_app",
                description = "小龙虾：启动指定包名的应用。",
                parameters = JSONObject()
                    .put("type", "object")
                    .put("properties", JSONObject().put(
                        "package_name", JSONObject()
                            .put("type", "string")
                            .put("description", "应用包名，如 com.tencent.mm")
                    ))
                    .put("required", JSONArray("package_name"))
            )
        )
        register(
            ToolDef(
                name = "lobster_dump",
                description = "小龙虾：导出当前界面节点树（用于辅助决策）。",
                parameters = JSONObject().put("type", "object").put("properties", JSONObject())
            )
        )
        register(
            ToolDef(
                name = "lobster_back",
                description = "小龙虾：模拟返回键。",
                parameters = JSONObject().put("type", "object").put("properties", JSONObject())
            )
        )
        register(
            ToolDef(
                name = "lobster_home",
                description = "小龙虾：模拟 Home 键。",
                parameters = JSONObject().put("type", "object").put("properties", JSONObject())
            )
        )

        // 2. 通用工具
        register(
            ToolDef(
                name = "get_current_time",
                description = "获取当前时间（毫秒时间戳 / 格式化字符串）。",
                parameters = JSONObject()
                    .put("type", "object")
                    .put("properties", JSONObject().put(
                        "format", JSONObject()
                            .put("type", "string")
                            .put("description", "iso 或 readable，默认 readable")
                    ))
            )
        )
        register(
            ToolDef(
                name = "calculate",
                description = "数学表达式求值（仅支持基本四则运算、括号、常用函数）。",
                parameters = JSONObject()
                    .put("type", "object")
                    .put("properties", JSONObject().put(
                        "expression", JSONObject()
                            .put("type", "string")
                            .put("description", "数学表达式，如 2+3*4")
                    ))
                    .put("required", JSONArray("expression"))
            )
        )
    }

    private fun register(def: ToolDef) {
        tools.add(def)
    }

    fun allDefs(): List<ToolDef> = tools.toList()

    fun toJsonArray(): org.json.JSONArray = org.json.JSONArray(
        tools.map { it.toJson().toString() }
    )

    /**
     * 同步执行工具调用，返回结果 JSON 字符串（作为 tool 消息回传给 AI）。
     */
    fun call(name: String, args: JSONObject, context: Context? = null): String {
        return try {
            when (name) {
                "lobster_click" -> {
                    val text = args.optString("text")
                    val r = LobsterBridge.perform(LobsterAction.ClickByText(text))
                    JSONObject().put("ok", r.ok).put("message", r.message).toString()
                }
                "lobster_input" -> {
                    val text = args.optString("text")
                    val r = LobsterBridge.perform(LobsterAction.InputText(text))
                    JSONObject().put("ok", r.ok).put("message", r.message).toString()
                }
                "lobster_launch_app" -> {
                    val pkg = args.optString("package_name")
                    val r = LobsterBridge.perform(LobsterAction.LaunchApp(pkg))
                    JSONObject().put("ok", r.ok).put("message", r.message).toString()
                }
                "lobster_dump" -> {
                    val r = LobsterBridge.perform(LobsterAction.Dump())
                    JSONObject().put("ok", r.ok).put("interface", r.message).toString()
                }
                "lobster_back" -> {
                    val r = LobsterBridge.perform(LobsterAction.GlobalBack)
                    JSONObject().put("ok", r.ok).put("message", r.message).toString()
                }
                "lobster_home" -> {
                    val r = LobsterBridge.perform(LobsterAction.GlobalHome)
                    JSONObject().put("ok", r.ok).put("message", r.message).toString()
                }
                "get_current_time" -> {
                    val format = args.optString("format", "readable")
                    val ts = System.currentTimeMillis()
                    val out = if (format == "iso") {
                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                            .format(java.util.Date(ts))
                    } else {
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            .format(java.util.Date(ts))
                    }
                    JSONObject().put("timestamp", ts).put("formatted", out).toString()
                }
                "calculate" -> {
                    val expr = args.optString("expression")
                    val result = MathEvaluator.eval(expr)
                    JSONObject().put("result", result).toString()
                }
                else -> JSONObject().put("ok", false).put("error", "unknown-tool: $name").toString()
            }
        } catch (e: Exception) {
            JSONObject().put("ok", false).put("error", e.message ?: e.javaClass.simpleName).toString()
        }
    }
}

/**
 * 极简数学表达式求值（仅支持 + - * / ( ) 和数字）。
 * 不支持函数（sin/cos 等），但用括号和四则足以覆盖大部分 AI 数学需求。
 */
object MathEvaluator {
    fun eval(expr: String): Double {
        val tokens = tokenize(expr)
        return parseExpression(tokens.toMutableList()).also {
            if (tokens.isNotEmpty()) throw RuntimeException("unexpected token: ${tokens.first()}")
        }
    }

    private sealed class Token {
        data class Num(val v: Double) : Token()
        data class Op(val c: Char) : Token()
    }

    private fun tokenize(expr: String): List<Token> {
        val out = mutableListOf<Token>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    var j = i
                    while (j < expr.length && (expr[j].isDigit() || expr[j] == '.')) j++
                    out.add(Token.Num(expr.substring(i, j).toDouble()))
                    i = j
                }
                c == '+' || c == '-' || c == '*' || c == '/' || c == '(' || c == ')' -> {
                    out.add(Token.Op(c)); i++
                }
                else -> throw RuntimeException("invalid char: $c")
            }
        }
        return out
    }

    // expr := term (('+'|'-') term)*
    private fun parseExpression(t: MutableList<Token>): Double {
        var v = parseTerm(t)
        while (t.isNotEmpty() && t.first() is Token.Op && (t.first() as Token.Op).c.let { it == '+' || it == '-' }) {
            val op = (t.removeAt(0) as Token.Op).c
            val r = parseTerm(t)
            v = if (op == '+') v + r else v - r
        }
        return v
    }

    // term := factor (('*'|'/') factor)*
    private fun parseTerm(t: MutableList<Token>): Double {
        var v = parseFactor(t)
        while (t.isNotEmpty() && t.first() is Token.Op && (t.first() as Token.Op).c.let { it == '*' || it == '/' }) {
            val op = (t.removeAt(0) as Token.Op).c
            val r = parseFactor(t)
            v = if (op == '*') v * r else v / r
        }
        return v
    }

    // factor := number | '(' expression ')'
    private fun parseFactor(t: MutableList<Token>): Double {
        return when (val tk = t.removeAt(0)) {
            is Token.Num -> tk.v
            is Token.Op -> {
                if (tk.c == '(') {
                    val v = parseExpression(t)
                    if (t.isEmpty() || t.removeAt(0) !is Token.Op || (t.first() as Token.Op).c != ')') {
                        throw RuntimeException("missing )")
                    }
                    v
                } else throw RuntimeException("unexpected: ${tk.c}")
            }
        }
    }
}

private fun JSONArray(s: String) = org.json.JSONArray("[$s]")
