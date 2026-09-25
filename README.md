# AURA 2.1.1 — 液态玻璃 AI 智能助手

AURA 是一款运行在 Android 设备上的开源 AI 智能助手，支持接入多种云端/本地 AI 模型，提供智能对话、内置终端、应用 IDE、工具调用与音乐播放等能力。2.1.1 版本在 2.1.0 液态玻璃 UI 基础上完成了导航结构回退为底部导航、新增 Dhizuku 提权、界面全量国际化重写与内核/协议重写。

## 功能特性

- **液态玻璃 UI 2.0**：全链路自研 Liquid Glass 组件库（玻璃面板/胶囊导航/分段控件/玻璃气泡），渐变流体背景
- **底部导航**：主界面采用底部导航栏，AI 聊天与设置一键切换，终端与 IDE 通过设置页入口进入
- **多模型支持**：兼容 OpenAI / DeepSeek / Ollama / LM Studio / vLLM 等多种云端和本地 AI 提供商
- **思考模式**：3 级深度推理，点击顶部芯片一键调节
- **对话界面全面重写**：接入 AgentEngine 真实流式对话，工具调用过程可视化，流式光标动画
- **终端 2.1.1**：LOCAL / SHIZUKU / DHIZUKU / ROOT 四后端玻璃面板终端，危险命令自动拦截，AI 可通过 run_shell 调用
- **双提权方案**：Shizuku（ADB 调试通道）与 Dhizuku（DeviceOwner 共享提权）并存，授权检测为拉取式，终端按后端能力自动分发命令
- **IDE 2.1.0**：玻璃双栏文件浏览器 + 等宽代码编辑器 + 构建输出面板，支持新建项目、依赖管理、APK 构建
- **界面全量国际化**：主界面所有用户可见文案走字符串资源，内置简体中文与英文，语言可运行时切换
- **工具调用**：AI 可调用终端命令、文件读写等工具完成任务，多轮工具历史完整保留
- **斜杠命令**：`/ws` `/model` `/clear` `/help` 快速操作
- **工作区管理**：内置文件浏览器，支持项目文件管理与 ZIP 导出
- **音乐播放器**：基于 Media3 ExoPlayer，支持播放/暂停/队列/随机/循环
- **应用内更新**：支持历史版本浏览、下载和安装
- **协议精简**：系统公告已移除并并入《用户须知》，启动协议仅保留《开源软件许可协议》与《用户须知》两份

## 开源技术栈

| 项目 | 详情 |
|------|------|
| 语言 | Kotlin 1.9（Apache-2.0） |
| UI 框架 | Jetpack Compose + Material 3（Apache-2.0） |
| 协程 | Kotlin Coroutines（Apache-2.0） |
| 网络 | OkHttp + SSE 流式（Apache-2.0） |
| 媒体 | Media3 ExoPlayer 1.2.1（Apache-2.0） |
| 提权 | Shizuku / Sui（MIT）、Dhizuku API 2.5.4（Apache-2.0） |
| 国际化 | Android 字符串资源 + 运行时 Locale 切换 |
| 序列化 | org.json |
| 最低 SDK | Android 9 (API 28) |
| 目标 SDK | Android 14 (API 34) |
| 架构 | MVVM + 单例装配 |

## 构建

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建
export BSK_KEYSTORE=/path/to/keystore
./gradlew assembleRelease
```

## 二次开发

二改本项目所需的开发环境搭建、项目结构说明、液态玻璃组件库用法与版本发布流程，见 [CONTRIBUTING.md](CONTRIBUTING.md)。欢迎 Fork 与 PR。

## 原作者与项目来源

- **原作者**：[1953187487](https://github.com/1953187487)
- **原仓库**：[BSK-AI / AURA](https://github.com/1953187487/BSK-AI)
- **联系方式**：1953187487@qq.com

任何基于本项目的二改（二次开发）作品，都必须在二改项目的 README 与应用关于页中保留上述原作者署名与原仓库链接，并遵循 [Apache-2.0](LICENSE) 许可证的归属要求。

## 更新日志

查看 [Releases](https://github.com/1953187487/BSK-AI/releases) 获取完整版本历史。

## 许可证

本项目基于 [Apache-2.0](LICENSE) 开源，详见应用内《开源软件许可协议》。
