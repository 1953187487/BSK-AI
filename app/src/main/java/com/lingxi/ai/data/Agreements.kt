package com.lingxi.ai.data

import com.lingxi.ai.BuildConfig

/** 协议区块：标题 + 正文。 */
data class AgreementSection(
    val title: String,
    val body: String
)

/**
 * 协议文本统一维护处。
 *
 * 3.0.0 起，启动协议仅保留两份：《开源软件许可协议》与《用户须知》。
 * 用户须知中的版本占位符 {VERSION} / {BUILD} 通过 [renderUserNotice] 注入。
 */
object Agreements {

    val openSource: AgreementSection = AgreementSection(
        title = "开源软件许可协议",
        body = """灵犀 LingXi 是一个开源 Android 应用，核心代码与所依赖的组件遵循 Apache-2.0、MIT 等宽松许可证发布。

本项目构建所使用的开源技术：
• Kotlin / Kotlin Coroutines（Apache-2.0）
• Jetpack Compose / Material 3（Apache-2.0）
• AndroidX / Lifecycle / Navigation（Apache-2.0）
• OkHttp / Retrofit / Gson（Apache-2.0 / BSD-3）
• Media3 ExoPlayer（Apache-2.0）
• AndroidLiquidGlass by Kyant0（Apache-2.0，https://github.com/Kyant0/AndroidLiquidGlass ，本版本的液态玻璃高光与折射渲染基于该项目移植实现）
• Shizuku / Sui（MIT）
• Dhizuku by iamr0s（GPL-3.0，https://github.com/iamr0s/Dhizuku ，本应用通过 Dhizuku API 接入其提权能力）
• org.json（JSON.org License）

在遵守上述许可证的前提下，你可以自由地使用、学习、修改和分发本软件，但须保留原始的版权声明与许可证文本。

二次开发（二改）归属要求：任何基于灵犀 LingXi 的衍生作品，必须在二改项目的仓库介绍（README）与应用内关于页中保留原作者署名及以下链接：
• 原作者主页：https://github.com/1953187487
• 原仓库地址：https://github.com/1953187487/BSK-AI

本软件按“原样”提供，不附带任何明示或默示的担保。作者不对因使用本软件产生的任何直接或间接损失承担责任。"""
    )

    val userNotice: AgreementSection = AgreementSection(
        title = "用户须知",
        body = """欢迎使用灵犀 LingXi {VERSION}。请在使用前仔细阅读以下条款：

1. 版本说明
   当前版本为灵犀 LingXi {VERSION}（build {BUILD}）。本版本采用全链路液态玻璃 UI（基于开源项目 AndroidLiquidGlass 移植实现），底部导航提供「AI 聊天 / 设置」两个主入口，终端与 IDE 从设置页进入。系统公告已并入本用户须知，不再单独弹出。

2. AI 对话与模型服务
   灵犀本身不采集、上传或存储你的对话内容。当你主动配置并连接第三方 AI 服务（OpenAI、DeepSeek、Ollama 等）时，你输入的对话文本会发送至该服务提供商。请仅在信任的服务商处填写 API 地址与密钥。

3. API 密钥与数据安全
   API 密钥仅保存在本机应用私有存储中，用于向服务商鉴权，灵犀不会读取、上传或向任何第三方泄露。

4. 终端与 Shell 执行
   内置终端提供 LOCAL、SHIZUKU、DHIZUKU、ROOT 四种后端。危险命令（rm -rf /、mkfs、dd、shutdown 等）会被自动拒绝。AI 代理可通过 run_shell 工具调用本终端。

5. 提权方案
   本应用提供 Shizuku 与 Dhizuku 两种可选提权方案，二者互不影响，可任选其一或同时授权。授权状态为拉取式检测，可随时撤回。提权仅用于执行你主动输入或确认的命令。

6. 工作区与文件访问
   工作区使用应用私有目录和用户授权的 SAF 存储位置。灵犀不会在未授权的情况下访问设备上的其他文件。

7. 权限说明
   本应用会请求存储、通知等运行时权限，仅用于工作区管理与后台提示。你可以在系统设置中随时撤回权限。

8. 条款变更
   如条款变更，更新到新版本时会重新弹出本协议供你审阅。如你不同意以上条款，请停止使用本应用。

9. 反馈与联系
   如有问题或建议，请联系：1953187487@qq.com"""
    )

    /** 渲染用户须知，注入当前版本号与 build 号。 */
    fun renderUserNotice(
        version: String = BuildConfig.APP_VERSION,
        build: Int = BuildConfig.BUILD_NUMBER
    ): String = userNotice.body
        .replace("{VERSION}", version)
        .replace("{BUILD}", build.toString())

    /** 开源协议（无占位符，直接渲染）。 */
    fun renderOpenSource(): String = openSource.body
}
