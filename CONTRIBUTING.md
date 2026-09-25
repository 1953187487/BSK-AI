# 灵犀 LingXi 二次开发指南（CONTRIBUTING）

本项目欢迎二改（二次开发）。本文档提供二改本项目所需的全部信息：原作者归属、环境搭建、项目结构、液态玻璃组件库用法、构建签名与版本发布流程。

## 〇、二改必读：原作者归属

二改（二次开发）并发布时，**必须**在二改项目中保留原作者信息：

- **原作者**：1953187487
- **主页**：https://github.com/1953187487
- **原仓库**：https://github.com/1953187487/BSK-AI
- **联系邮箱**：1953187487@qq.com

具体要求（Apache-2.0 归属条款）：

1. 保留仓库根目录的 `LICENSE` 与 `NOTICE` 文件，以及其中的原作者署名
2. 二改项目的 README（仓库介绍）中必须包含原作者主页与原仓库链接
3. 二改应用内（关于页或开源协议页）必须保留原作者归属信息
4. 版权声明与许可证文本随源码分发时必须完整保留

未保留原作者链接与署名的二改发布，视为违反许可证。

## 一、环境搭建

| 依赖 | 版本要求 |
|------|---------|
| JDK | OpenJDK 17 |
| Android SDK | Platform 34 + Build-Tools 34.0.0 |
| Gradle | 使用仓库自带 gradlew（8.x） |
| AGP | 8.2.2 |
| Kotlin | 1.9.22 |

SDK 路径通过 `local.properties` 指定（已被 .gitignore 排除）：

```properties
sdk.dir=/opt/android-sdk
```

## 二、项目结构

包名：`com.lingxi.ai`

```
app/src/main/java/com/lingxi/ai/
├── LingXiApp.kt               # Application，装配全部引擎与注册表
├── MainActivity.kt            # 主入口，协议重签逻辑（版本变更自动重新弹出协议）
├── SplashActivity.kt          # 启动页
├── agent/                     # AI 内核
│   ├── AgentEngine.kt         # 工具循环 / 流式对话 / 上下文预算 / 摘要 / 协作式取消
│   ├── LlmClient.kt           # OpenAI 兼容客户端（OkHttp + SSE）
│   ├── Coordinator.kt         # 纯文本 → AgentEngine 桥接
│   ├── VideoGenCoordinator.kt # 视频生成协调器
│   ├── tools/                 # AI 工具：run_shell / list_files / read_file / write_file / system_info / current_time
│   └── slash/                 # 斜杠命令：/ws /model /clear /help /compact /history
├── ui/                        # UI 层（Jetpack Compose + Material 3）
│   ├── LingXiScaffold.kt      # 主框架：底部导航（AI 聊天 / 设置）
│   ├── UiLabels.kt            # 用户可见文案常量
│   ├── glass/LiquidGlass.kt   # 液态玻璃组件库（核心 UI 资产，纯 Compose 绘制）
│   ├── chat/                  # 对话界面（AgentEngine 真实流式对话）
│   ├── settings/              # 设置界面（语言/主题/模型/终端后端等）
│   ├── ide/                   # IDE 界面（文件浏览器 + 代码编辑器 + 构建输出）
│   ├── terminal/              # 终端界面（LOCAL / SHIZUKU / DHIZUKU / ROOT 四后端）
│   ├── glass/                 # 玻璃组件与主题色板
│   ├── welcome/               # 引导页
│   ├── music/                 # 音乐播放界面
│   ├── account/               # 账号界面
│   ├── update/                # 版本更新界面
│   ├── legal/                 # 引导协议（语言→API→权限工具→协议 四步）
│   └── theme/                 # Material 3 配色
├── data/                      # AppSettings / SettingsRepository / Languages / Agreements
├── permission/                # ShizukuBridge.kt / DhizukuBridge.kt
├── terminal/                  # TerminalEngine（四后端分发 + 危险命令拦截）/ DevTools / AndroidDependencyManager
├── workspace/                 # WorkspaceManager（内部工作区 + SAF 外部工作区）
├── account/                   # LingXiApi / LingXiClient / LingXiDtos / LingXiHosts
├── update/                    # GitHubApi / UpdateInstaller / UpdateModels
├── i18n/LocaleManager.kt      # 运行时 Locale 切换
├── music/MusicEngine.kt       # Media3 ExoPlayer 音乐引擎
└── util/                      # 通用工具

app/src/main/java/com/kyant/backdrop/   # AndroidLiquidGlass 背景（Kyant0, Apache-2.0）
```

## 三、液态玻璃组件库用法

所有玻璃组件位于 `com.lingxi.ai.ui.glass.LiquidGlass.kt`，适配 Dark/Light 双模式。v3.0.0 起面板高光与描边改为纯 Compose 绘制，无 RuntimeShader / RenderEffect 依赖：

```kotlin
import com.lingxi.ai.ui.glass.*

// 玻璃面板（卡片容器）
GlassPanel(shape = RoundedCornerShape(20.dp)) { Text("内容") }

// 顶部胶囊导航
GlassTopNav(selected = tab, onSelect = { tab = it })

// 分段子导航（对话 / 终端 / IDE）
GlassSegmented(options = listOf("对话", "终端", "IDE"), selected = sub, onSelect = { sub = it })

// 玻璃芯片（可选中）
GlassChip(text = "LOCAL", selected = true, onClick = { ... })

// 玻璃气泡（聊天气泡，accent=true 为主题色填充）
GlassBubble(accent = true) { Text("消息") }

// 玻璃按钮 / 圆形图标按钮 / 玻璃输入框
GlassButton(text = "保存", onClick = { ... })
GlassIconButton(icon = Icons.Default.Add, contentDescription = "添加", onClick = { ... })
GlassTextField(value = input, onValueChange = { input = it }, placeholder = "输入…")

// 玻璃色板（自绘组件时使用）
val glass = rememberGlassColors()
Text("文字", color = glass.content)
```

自定义玻璃效果的基础修饰符：`Modifier.liquidGlass(shape, colors)`，可与任意组件组合。

## 四、构建与签名

```bash
# Debug
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk
./gradlew assembleDebug

# Release（签名密钥为仓库内 lingxi-release.keystore，alias lingxi）
export LINGXI_KEYSTORE=/path/to/lingxi-release.keystore
export LINGXI_KEYSTORE_PASSWORD=your_password
export LINGXI_KEY_PASSWORD=your_password
./gradlew assembleRelease
```

Gradle 默认堆为 3500m，在受限容器内存下可能卡死，可通过 `-Dorg.gradle.jvmargs="-Xmx2880m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8"` 覆盖。

产物位于 `app/build/outputs/apk/release/`。

## 五、版本发布流程

1. 更新 `app/build.gradle`：`versionCode`（整数递增）、`versionName`、`APP_VERSION`、`BUILD_NUMBER`
2. 在 `CHANGELOG.md` 顶部新增版本条目（中文，分组：品牌 / 提权 / AI 内核 / UI / 国际化 / 协议 / 其他）
3. 更新 `data/Agreements.kt` 中《用户须知》的版本说明（协议随版本自动重签，`MainActivity` 检测版本变化会重新弹出协议）
4. 更新 `README.md` 简介与版本号
5. 构建签名 APK，重命名后上传到 GitHub Release（如 `LingXi-3.0.0-release.apk`）
6. 提交改动并打 tag：`v3.0.0`，tag 必须指向本次发布的最新 commit

## 六、二改注意事项

- 品牌已彻底更名：包名 `com.lingxi.ai`，签名密钥 `lingxi-release.keystore`，alias `lingxi`，环境变量 `LINGXI_KEYSTORE` / `LINGXI_KEYSTORE_PASSWORD` / `LINGXI_KEY_PASSWORD`；旧品牌 AURA / com.bskai / aura-release.keystore / BSK_KEYSTORE 均已废弃
- 协议体系仅保留两份：《开源软件许可协议》与《用户须知》，系统公告已移除，公告类信息一律写入《用户须知》
- 导航架构：主框架采用底部导航（AI 聊天 / 设置），新功能请放入 AI 聊天区的子导航或设置页
- 新增 AI 能力请在 `agent/tools/` 注册新 Tool；新增斜杠命令请在 `agent/slash/` 注册
- 新增用户可见文案必须走字符串资源（`app/src/main/res/values/strings.xml` 与 `app/src/main/res/values-en/strings.xml`），禁止硬编码字符串
- 终端危险命令拦截逻辑位于 `terminal/TerminalEngine.kt`，二改时请保留
- 修改后请遵循现有代码风格（Kotlin + Jetpack Compose + Material 3，主色为全新紫色）
