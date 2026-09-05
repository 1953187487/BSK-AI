package com.bskai.data

data class AgreementSection(
    val title: String,
    val body: String
)

object Agreements {

    val openSource: AgreementSection = AgreementSection(
        title = "《AURA 开源软件许可协议》",
        body = """AURA 基于一系列优秀的开源项目构建，其核心模块遵循 Apache-2.0 与 MIT 等宽松许可证发布。

你可以在遵守相应许可证的前提下自由使用、修改与分发本软件，但需保留原始的版权声明与许可证文本。

本软件按"原样"提供，不附带任何明示或默示的担保。项目作者不对因使用本软件产生的任何直接或间接损失承担责任。"""
    )

    val privacy: AgreementSection = AgreementSection(
        title = "《AURA 使用协议与隐私说明》",
        body = """1. 语音识别使用系统语音服务在线完成，录音仅用于识别该次指令，处理完毕后即时丢弃，AURA 不会收集或上传你的语音数据。

2. 当你主动配置并连接第三方 AI 服务时，你输入的对话文本会发送至该服务提供商。请仅在信任的服务商处填写 API 地址与密钥。

3. API 密钥仅保存在本机应用私有存储中，用于向服务商鉴权，AURA 不会读取、上传或向任何第三方泄露。

4. 本应用会请求录音、通知等运行时权限，仅用于语音识别、结果播报与后台服务提示。你可以在系统设置中随时撤回权限。

5. 本次协议对应版本：{VERSION}。如条款变更，每次更新到新版本时将重新弹出本协议供你审阅。

6. 如你不同意以上条款，请退出并停止使用本应用。"""
    )

    fun renderPrivacy(version: String): String = privacy.body.replace("{VERSION}", version)
}
