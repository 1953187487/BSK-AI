package com.bskai.data

import com.bskai.BuildConfig

data class AgreementSection(
    val title: String,
    val body: String
)

object Agreements {

    val openSource: AgreementSection = AgreementSection(
        title = "开源软件许可协议",
        body = """AURA 是一个开源 Android 应用，核心代码与所依赖的组件遵循 Apache-2.0、MIT 等宽松许可证发布。

本项目构建所使用的开源技术：
• Kotlin / Kotlin Coroutines（Apache-2.0）
• Jetpack Compose / Material 3（Apache-2.0）
• AndroidX / Lifecycle / Navigation（Apache-2.0）
• OkHttp（Apache-2.0）
• Media3 ExoPlayer（Apache-2.0）
• AndroidLiquidGlass by Kyant0（Apache-2.0，https://github.com/Kyant0/AndroidLiquidGlass ，本版本 2.1.1 的液态玻璃折射与高光渲染基于该项目移植实现）
• Shizuku / Sui（MIT）
• org.json（JSON.org License）

在遵守上述许可证的前提下，你可以自由地使用、学习、修改和分发本软件，但须保留原始的版权声明与许可证文本。

二次开发（二改）归属要求：任何基于 AURA 的衍生作品，必须在二改项目的仓库介绍（README）与应用内保留原作者署名及以下链接：
• 原作者主页：https://github.com/1953187487
• 原仓库地址：https://github.com/1953187487/BSK-AI

本软件按"原样"提供，不附带任何明示或默示的担保。作者不对因使用本软件产生的任何直接或间接损失承担责任。"""
    )

    val userNotice: AgreementSection = AgreementSection(
        title = "用户须知",
        body = """欢迎使用 AURA {VERSION}。请在使用前仔细阅读以下条款：

1. 版本说明
   当前版本为 AURA {VERSION}（build {BUILD}）。本版本采用全链路液态玻璃 UI（基于开源项目 AndroidLiquidGlass 移植实现），导航结构更新为底部导航栏（AI 聊天 / 设置），系统公告已并入本用户须知，不再单独弹出。

2. AI 对话与模型服务
   AURA 本身不采集、上传或存储你的对话内容。当你主动配置并连接第三方 AI 服务（OpenAI、DeepSeek、Ollama 等）时，你输入的对话文本会发送至该服务提供商。请仅在信任的服务商处填写 API 地址与密钥。

3. API 密钥与数据安全
   API 密钥仅保存在本机应用私有存储中，用于向服务商鉴权，AURA 不会读取、上传或向任何第三方泄露。

4. 终端与 Shell 执行
   内置终端提供 LOCAL、SHIZUKU、ROOT 三种后端。危险命令（rm -rf /、mkfs、dd、shutdown 等）会被自动拒绝。AI 代理可通过 run_shell 工具调用本终端。

5. 工作区与文件访问
   工作区使用应用私有目录和用户授权的 SAF 存储位置。AURA 不会在未授权的情况下访问设备上的其他文件。

6. 权限说明
   本应用会请求存储、通知等运行时权限，仅用于工作区管理与后台提示。你可以在系统设置中随时撤回权限。

7. 条款变更
   如条款变更，更新到新版本时会重新弹出本协议供你审阅。如你不同意以上条款，请停止使用本应用。

8. 反馈与联系
   如有问题或建议，请联系：1953187487@qq.com"""
    )

    fun renderUserNotice(version: String = BuildConfig.APP_VERSION): String =
        userNotice.body
            .replace("{VERSION}", version)
            .replace("{BUILD}", BuildConfig.BUILD_NUMBER.toString())
}
