# AURA 二次开发指南（CONTRIBUTING）

本项目欢迎二改（二次开发）。本文档提供二改本项目所需的全部信息：环境搭建、项目结构、液态玻璃组件库用法、构建签名与版本发布流程。

## 〇、二改必读：原作者归属

二改（二次开发）并发布时，**必须**在二改项目中保留原作者信息：

- **原作者**：[1953187487](https://github.com/1953187487)
- **原仓库**：[BSK-AI / AURA](https://github.com/1953187487/BSK-AI)

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
sdk.dir=/path/to/android-sdk
```

## 二、项目结构

```
app/src/main/java/com/bskai/
├── MainActivity.kt          # 主入口，协议重签逻辑（版本变更自动重新弹出协议）
├── SplashActivity.kt        # 启动页
├── AuraApp.kt               # Application，装配全部引擎与注册表
├── agent/                   # AI 引擎：LlmClient(OpenAI 兼容)、AgentEngine(工具循环/流式)、Coordinator
│   ├── tools/               # AI 工具：run_shell / list_files / read_file / write_file
│   └── slash/               # 斜杠命令：/ws /model /clear /help
├── terminal/                # 终端引擎（LOCAL/SHIZUKU/ROOT 三后端）+ 开发依赖管理
├── workspace/               # 工作区管理（内部工作区 + SAF 外部）
├── music/                   # Media3 ExoPlayer 音乐引擎
├── permission/              # Shizuku 桥接
├── data/                    # AppSettings / SettingsRepository / Agreements（协议，仅两份）
├── update/                  # GitHub API 更新检查与 APK 下载
└── ui/
    ├── AuraScaffold.kt      # 主框架：顶部导航（AI聊天/设置）+ 顶部子导航（对话/终端/IDE）
    ├── glass/LiquidGlass.kt # 液态玻璃组件库（核心 UI 资产）
    ├── chat/                # 对话界面（AgentEngine 真实流式对话）
    ├── terminal/            # 终端界面
    ├── ide/                 # IDE 界面
    ├── settings/            # 设置界面
    ├── legal/               # 引导协议（语言→API→权限工具→协议 四步）
    └── theme/               # Material 3 配色
```

## 三、液态玻璃组件库用法

所有玻璃组件位于 `com.bskai.ui.glass.LiquidGlass.kt`，适配 Dark/Light 双模式：

```kotlin
import com.bskai.ui.glass.*

// 玻璃面板（卡片容器）
GlassPanel(shape = RoundedCornerShape(20.dp)) { Text("内容") }

// 顶部胶囊导航（当前为 AI 聊天 / 设置 两项）
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
./gradlew assembleDebug

# Release（签名配置读取环境变量，缺省使用仓库内 aura-release.keystore）
export BSK_KEYSTORE=/path/to/keystore
export BSK_KEYSTORE_PASSWORD=your_password
export BSK_KEY_PASSWORD=your_password
./gradlew assembleRelease
```

产物位于 `app/build/outputs/apk/release/`。

## 五、版本发布流程

1. 更新 `app/build.gradle`：`versionCode`（整数递增）、`versionName`、`APP_VERSION`、`BUILD_NUMBER`
2. 在 `CHANGELOG.md` 顶部新增版本条目
3. 更新 `data/Agreements.kt` 中《用户须知》的版本说明（协议随版本自动重签，`MainActivity` 检测版本变化会重新弹出协议）
4. 更新 `README.md` 简介与版本号
5. 构建签名 APK，重命名后放入仓库根目录（如 `AURA-2.1.0-release.apk`）
6. 提交并打 tag：`v2.1.0`

## 六、二改注意事项

- 协议体系仅保留两份：《开源软件许可协议》与《用户须知》，系统公告已移除，公告类信息一律写入《用户须知》
- 导航架构：顶部导航只保留「AI 聊天 / 设置」两个入口，新功能请放入 AI 聊天区的子导航（对话/终端/IDE）或设置页
- 新增 AI 能力请在 `agent/tools/` 注册新 Tool；新增斜杠命令请在 `agent/slash/` 注册
- 终端危险命令拦截逻辑位于 `TerminalEngine`，二改时请保留
- 修改后请遵循现有代码风格（Kotlin + Jetpack Compose + Material 3）
