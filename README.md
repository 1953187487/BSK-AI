# AURA 2.0.5 — AI 智能助手

AURA 是一款运行在 Android 设备上的 AI 智能助手应用，支持接入多种云端/本地 AI 模型，提供智能对话、工具调用、工作区管理、音乐播放等功能。

## 功能特性

- **多模型支持**：兼容 OpenAI / DeepSeek / Ollama / LM Studio / vLLM 等多种云端和本地 AI 提供商
- **思考模式**：3 级深度推理，可调节 AI 思考强度
- **应用开发模式**：AI 辅助开发 Android 应用，支持终端执行和文件读写
- **工具调用**：AI 可调用终端命令、读写文件等工具完成任务
- **斜杠命令**：`/ws` `/model` `/clear` `/help` 快速操作
- **工作区管理**：内置文件浏览器，支持项目文件管理
- **音乐播放器**：基于 Media3 ExoPlayer，支持播放/暂停/队列/随机/循环
- **本地 AI 模型下载**：一键刷新并下载 Ollama、LM Studio 等本地模型
- **应用内更新**：支持历史版本浏览、下载和安装
- **Shizuku 集成**：通过 Shizuku 执行高权限 ADB 命令
- **Material 3 主题**：动态颜色系统，支持 Dark/Light 模式

## 技术规格

| 项目 | 详情 |
|------|------|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose + Material 3 |
| 最低 SDK | Android 9 (API 28) |
| 目标 SDK | Android 14 (API 34) |
| 架构 | MVVM + 单例装配 |
| 网络 | OkHttp + Streaming |
| 媒体 | Media3 ExoPlayer 1.2.1 |

## 构建

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建
export BSK_KEYSTORE=/path/to/keystore
./gradlew assembleRelease
```

## 更新日志

查看 [Releases](https://github.com/1953187487/BSK-AI/releases) 获取完整版本历史。

## 许可证

本项目仅供学习和研究使用。
