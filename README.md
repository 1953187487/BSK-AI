# BSK AI — Android 端 AI 编码智能体

支持 Android 8 ~ 14（API 26 ~ 34），基于 Claude Code × OpenClaw 技术架构。

## 功能

- **AI 编码智能体（Claude Code 风格）**：流式输出、工具调用（读取/编辑/搜索文件、Shell 执行、创建项目、构建 APK）、权限提示、会话管理
- **多智能体编排（OpenClaw 风格）**：规划器 → 编码器 → 审查器流水线，支持迭代修订
- **模型中心**：下载本地 GGUF 模型（Qwen、Llama、Gemma 等），支持自定义 URL
- **自定义服务商配置**：支持所有 OpenAI 兼容 API，测试连接，多服务商切换
- **Android 开发工具链**：内置 android.jar 下载，与 Termux 协作完成 javac/d8/aapt2/apksigner 构建
- **管理员系统**：隐藏入口（版本号连点 3 次），账号密码登录，发布公告与编辑公告
- **全新 UI**：Material 3 + 液态玻璃风格，靛蓝/翠绿/琥珀主题色，重绘启动图标

## 下载

前往 [GitHub Releases](https://github.com/1953187487/bsk-ai/releases) 下载最新 APK。

## 更新记录

查看 [CHANGELOG.md](CHANGELOG.md)

## 构建

```bash
# 需要 JDK 17 + Android SDK
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug
```

## 管理员账号

版本号（设置页底部）连点 3 次触发隐藏入口，输入账号密码登录后可发布/编辑公告。  
（凭据以 SHA-256 哈希形式内置，如需修改请编辑 `core/admin/AdminAuth.kt`）

## 开源

MIT License，仓库：[https://github.com/1953187487/bsk-ai](https://github.com/1953187487/bsk-ai)
