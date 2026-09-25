# 灵犀 LingXi — Android AI 助手

灵犀 LingXi 是一款运行在 Android 设备上的开源 AI 智能助手，兼容多种云端与本地大模型，把对话、终端、开发工具与系统控制统一到一个设备端入口。它以 Jetpack Compose + Material 3 自研的液态玻璃 UI 呈现，支持多模型、思考深度、工具调用、流式输出与协作式取消；内置终端在本地、Shizuku、Dhizuku、ROOT 四后端之间自动分发命令，另含工作区管理、应用内 IDE、音乐播放与内置国际化。3.0.0 版本完成了品牌全新更名（AURA → 灵犀 LingXi，包名 com.lingxi.ai），新增 Dhizuku 提权方案，界面全量国际化，并对液态玻璃渲染、布局重叠与上下文/token 处理做了全面重写。

`Android 9+` · `Kotlin` · `Jetpack Compose` · `Material 3` · `Apache-2.0` · `versionCode 322 / versionName 3.0.0`

## 功能特性

| 分类 | 说明 |
|------|------|
| AI 对话 | 兼容 OpenAI / DeepSeek / Ollama / LM Studio / vLLM 等多种云端与本地模型；3 级思考深度一键调节；真实 SSE 流式输出；支持协作式取消（停止生成）；工具调用过程可视化 |
| AI 工具 | AI 可直接调用 run_shell / list_files / read_file / write_file / system_info / current_time；危险命令自动拦截；工具执行 60 秒超时、输出 6000 字符截断 |
| 上下文管理 | 按 token 预算（估算 chars/3，总预算 8000）截取上下文；永不拆散 assistant(toolCalls) 与 tool 结果配对；历史超出时自动插入早期对话摘要 |
| 斜杠命令 | `/ws` `/model` `/clear` `/help` `/compact` `/history` |
| 终端 | LOCAL / SHIZUKU / DHIZUKU / ROOT 四后端，按已授权后端自动分发命令；危险命令拦截；含本地权限逃生（可降级为普通用户权限执行） |
| 提权 | Shizuku（ADB 调试通道）与 Dhizuku（DeviceOwner 共享提权，Dhizuku API 2.5.4）双方案并存 |
| IDE | 玻璃双栏文件浏览器 + 等宽代码编辑器 + 构建输出面板；支持新建项目、依赖管理、APK 构建 |
| 工作区 | 内部工作区 + SAF 外部工作区导入；新建/重命名/删除/ZIP 导出 |
| 音乐播放 | 基于 Media3 ExoPlayer 1.2.1；播放/暂停/队列/随机/循环 |
| 应用内更新 | 通过 GitHub Releases 检查并安装历史版本 |
| 国际化 | 内置简体中文与英文，运行时切换即时生效；主界面所有用户可见文案走字符串资源 |
| 液态玻璃 UI | 自研 Liquid Glass 组件库（玻璃面板/胶囊导航/分段控件/玻璃气泡/玻璃按钮/玻璃输入框）；渐变流体背景；纯 Compose 绘制，无 RuntimeShader/RenderEffect |

## 开源技术栈

| 项目 | 详情 |
|------|------|
| 语言 | Kotlin 1.9.22（Apache-2.0） |
| UI 框架 | Jetpack Compose + Material 3（Apache-2.0） |
| 协程 | Kotlin Coroutines（Apache-2.0） |
| 网络 | OkHttp 4.12 + SSE 流式（Apache-2.0） |
| 媒体 | Media3 ExoPlayer 1.2.1（Apache-2.0） |
| 提权 | Shizuku / Sui（MIT）、Dhizuku API 2.5.4（GPL-3.0） |
| 液态玻璃背景 | AndroidLiquidGlass（Kyant0，Apache-2.0） |
| 序列化 | org.json |
| 架构 | MVVM + 单例装配 |
| minSdk | Android 9 (API 28) |
| targetSdk | Android 14 (API 34) |
| compileSdk | 34 |

## 构建

先准备 JDK 17 与 Android SDK Platform 34：

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk
./gradlew assembleDebug
```

Release 构建需要签名密钥（`lingxi-release.keystore`，alias `lingxi`）：

```bash
export LINGXI_KEYSTORE=/path/to/keystore
export LINGXI_KEYSTORE_PASSWORD=your_password
export LINGXI_KEY_PASSWORD=your_password
./gradlew assembleRelease
```

产物位于 `app/build/outputs/apk/release/`。

> Gradle 默认堆为 3500m，在受限容器（如 CI 沙箱）内存不足时会直接卡死。可通过如下覆盖降低：
> `-Dorg.gradle.jvmargs="-Xmx2880m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8"`

## 如何提权

### Shizuku

前置条件：

1. 设备已开启开发者选项与 USB 调试，或可通过无线调试连接；
2. 已安装 [Shizuku](https://shizuku.rikka.app/) 应用（13.1.5）。

授权方式：启动 Shizuku 后从开发者选项通过 ADB 启动，然后在灵犀的设置页打开高权限开关，灵犀会以普通应用身份向 Shizuku 申请权限，无需 ADB 通道直接执行命令。

### Dhizuku

前置条件：

1. 已安装 [Dhizuku](https://github.com/iamr0s/Dhizuku)（API 2.5.4）；
2. 在设备的「系统 → 设备管理器」里授予 Dhizuku 设备管理员权限。

授权方式：在灵犀设置页打开高权限开关，应用会通过 DeviceOwner 共享提权请求 Dhizuku 授权，不依赖 ADB 调试通道。

两种方案可以并存，终端引擎按当前可用授权自动选择最佳后端，任一授权即可使用高权限。

## 国际化

字符串资源：

- 简体中文：`app/src/main/res/values/strings.xml`
- 英文：`app/src/main/res/values-en/strings.xml`

新增语言的做法：

1. 复制 `values/strings.xml` 到 `values-<lang>/strings.xml`，`lang` 为 BCP-47 语言代码（如 `ja`、`de`、`es`）；
2. 在代码中注册新的语言代码（见 `app/src/main/java/com/lingxi/ai/i18n/`）；
3. 主界面文案全部通过字符串资源引用，新增文案必须走资源键，禁止硬编码字符串。

## 二次开发

二改本项目所需的开发环境搭建、项目结构说明、液态玻璃组件库用法、构建签名与版本发布流程，见 CONTRIBUTING.md。欢迎 Fork 与 PR。

## 原作者与项目来源

- **原作者**：1953187487
- **主页**：https://github.com/1953187487
- **原仓库**：https://github.com/1953187487/BSK-AI
- **联系方式**：1953187487@qq.com

任何基于本项目的二次开发（二改）作品，都必须在二改项目的 README 与应用关于页中保留上述原作者署名与原仓库链接，并遵循 Apache-2.0 许可证的归属要求。

## 更新日志

- 版本记录：CHANGELOG.md
- 在线发布：https://github.com/1953187487/BSK-AI/releases

## 许可证

本项目基于 Apache-2.0 开源，详见仓库根目录 `LICENSE`。应用内《开源软件许可协议》列出全部第三方组件归属。
