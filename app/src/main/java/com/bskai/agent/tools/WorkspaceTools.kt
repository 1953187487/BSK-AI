package com.bskai.agent.tools

import com.bskai.workspace.WorkspaceManager

class ListFilesTool(private val workspace: WorkspaceManager) : Tool {
    override val name = "list_files"
    override val description = "列出当前工作区根目录下的文件与文件夹。返回 JSON 数组，每项含 name/path/isDirectory/size。"
    override val parametersSchema = """{"type":"object","properties":{"path":{"type":"string","description":"相对于工作区根的子路径，例如 'docs'；留空列出根目录"}}}"""
    override suspend fun execute(argumentsJson: String): ToolResult {
        val path = parseString(argumentsJson, "path") ?: ""
        val nodes = workspace.listRelative(path)
        val arr = org.json.JSONArray()
        nodes.forEach { n ->
            arr.put(
                org.json.JSONObject().apply {
                    put("name", n.name)
                    put("path", n.path)
                    put("isDirectory", n.isDirectory)
                    put("size", n.size)
                }
            )
        }
        return ToolResult(name, arr.toString())
    }
}

class ReadFileTool(private val workspace: WorkspaceManager) : Tool {
    override val name = "read_file"
    override val description = "读取工作区中某个文本文件的全部内容。"
    override val parametersSchema = """{"type":"object","properties":{"path":{"type":"string","description":"相对工作区根的文件路径"}},"required":["path"]}"""
    override suspend fun execute(argumentsJson: String): ToolResult {
        val path = parseString(argumentsJson, "path")
            ?: return ToolResult(name, "缺少参数 path", true)
        val text = workspace.readRelative(path)
            ?: return ToolResult(name, "文件不存在或不可读：$path", true)
        return ToolResult(name, text)
    }
}

class WriteFileTool(private val workspace: WorkspaceManager) : Tool {
    override val name = "write_file"
    override val description = "在工作区创建或覆盖一个文本文件。"
    override val parametersSchema = """{"type":"object","properties":{"path":{"type":"string","description":"相对工作区根的文件路径"},"content":{"type":"string","description":"要写入的文本内容"}},"required":["path","content"]}"""
    override suspend fun execute(argumentsJson: String): ToolResult {
        val path = parseString(argumentsJson, "path")
            ?: return ToolResult(name, "缺少参数 path", true)
        val content = parseString(argumentsJson, "content")
            ?: return ToolResult(name, "缺少参数 content", true)
        val ok = workspace.writeRelative(path, content)
        return if (ok) ToolResult(name, "已写入：$path")
        else ToolResult(name, "写入失败：$path", true)
    }
}

internal fun parseString(json: String, key: String): String? {
    return try {
        val o = org.json.JSONObject(json)
        if (o.isNull(key)) null else o.optString(key, "").ifBlank { null }
    } catch (_: Exception) {
        null
    }
}
