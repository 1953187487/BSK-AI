package com.bskai.data

data class AgreementSection(
    val title: String,
    val body: String
)

object Agreements {

    val openSource: AgreementSection = AgreementSection(
        title = "AURA 开源软件许可协议",
        body = """AURA 基于一系列优秀的开源项目构建，其核心模块遵循 Apache-2.0 与 MIT 等宽松许可证发布。

本项目使用了以下开源组件：
• Kotlin / Compose UI (Apache-2.0)
• OkHttp (Apache-2.0)
• Media3 ExoPlayer (Apache-2.0)
• Shizuku / Sui (MIT)
• 以及众多开源工具与模型权重。

你可以在遵守相应许可证的前提下自由使用、修改与分发本软件，但需保留原始的版权声明与许可证文本。

本软件按"原样"提供，不附带任何明示或默示的担保。项目作者不对因使用本软件产生的任何直接或间接损失承担责任。"""
    )

    val privacy: AgreementSection = AgreementSection(
        title = "AURA 使用协议与隐私说明",
        body = """1. AI 对话与模型服务
   AURA 本身不采集、上传或存储你的对话内容。当你主动配置并连接第三方 AI 服务（OpenAI、DeepSeek、Ollama 等）时，你输入的对话文本会发送至该服务提供商。请仅在信任的服务商处填写 API 地址与密钥。

2. API 密钥与数据安全
   API 密钥仅保存在本机应用私有存储中，用于向服务商鉴权，AURA 不会读取、上传或向任何第三方泄露。本地模型（GGUF 权重）仅存储于应用私有目录，不会自动同步到任何外部服务。

3. 终端与 Shell 执行
   内置终端提供 LOCAL、SHIZUKU、ROOT 三种后端。危险命令（rm -rf /、mkfs、dd、shutdown 等）会被自动拒绝。AI 代理可通过 run_shell 工具调用本终端，所有执行会写入审计日志。

4. 工作区与文件访问
   工作区使用应用私有目录和用户授权的 SAF 存储位置。AURA 不会在未授权的情况下访问设备上的其他文件。

5. 权限说明
   本应用会请求存储、通知、麦克风等运行时权限，仅用于工作区管理、后台提示与语音输入。你可以在系统设置中随时撤回权限。

6. 协议版本
   本次协议对应版本：{VERSION}。如条款变更，每次更新到新版本时将重新弹出本协议供你审阅。

7. 反馈与联系
   如你不同意以上条款，请退出并停止使用本应用。如有问题或建议，请联系：1953187487@qq.com"""
    )

    fun renderPrivacy(version: String): String = privacy.body.replace("{VERSION}", version)
}
