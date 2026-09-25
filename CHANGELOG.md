# 灵犀 LingXi 更新记录

## v3.0.0
- **品牌**：全新更名 AURA → 灵犀 LingXi，包名 com.bskai → com.lingxi.ai；应用图标全新设计（犀角之光标记 + 全新紫色主色板）；协议与关于页全部更名
- **品牌**：全新签名密钥（因包名变更无法覆盖升级，需先卸载旧版本后再安装）
- **提权**：新增 Dhizuku 提权（Dhizuku API 2.5.4），与 Shizuku 并存的第二种高权限方案，DeviceOwner 共享提权，无需 ADB 调试通道
- **提权**：终端引擎升级为四后端 LOCAL / SHIZUKU / DHIZUKU / ROOT，按已授权后端自动分发命令，含本地权限逃生（可降级为普通用户权限执行）
- **AI 内核**：上下文按 token 预算截取（估算 chars/3，总预算 8000）
- **AI 内核**：永不拆散 assistant(toolCalls) 与 tool 结果配对，多轮工具历史保持完整
- **AI 内核**：历史超出预算时自动插入早期对话摘要，保证关键上下文不丢失
- **AI 内核**：工具执行 60 秒超时；工具输出 6000 字符截断
- **AI 内核**：支持协作式取消（停止生成），中断即时生效
- **AI 内核**：新增 `/compact` 与 `/history` 斜杠命令，用于主动压缩与查看历史
- **AI 内核**：新增 system_info 与 current_time 工具
- **AI 内核**：run_shell 危险命令拦截与输出截断，降低误操作风险
- **聊天**：生成中发送按钮切换为「停止」按钮，一键中断当前回复
- **UI**：液态玻璃渲染重写，面板高光与描边改为纯 Compose 绘制，移除每个面板上的 RuntimeShader/RenderEffect 依赖
- **UI**：消除引导页首帧挂载面板时的崩溃风险
- **UI**：布局重叠修复，引导页、版本更新页、底部导航页重新划分固定区/滚动区，新增系统栏与导航栏内边距
- **国际化**：界面全量国际化，内置简体中文与英文，语言运行时切换；主界面所有用户可见文案全部走字符串资源
- **协议**：协议与关于页全部更名；开源协议新增 Dhizuku 归属
- **其他**：minSdk 28，targetSdk 34，compileSdk 34，versionCode 322，versionName 3.0.0

## v2.1.1
- 导航结构回退：移除顶部小导航栏，改为底部导航栏，AI 聊天与设置为一等入口，终端与 IDE 从设置页进入
- 新增 Dhizuku 提权：与 Shizuku 并存的第二种高权限方案（DeviceOwner 共享提权，Dhizuku API 2.5.4），无需 ADB 调试通道
- 终端四后端：LOCAL / SHIZUKU / DHIZUKU / ROOT，按已授权后端自动分发命令，新增本地权限逃生（可降级为普通用户权限执行）
- 终端界面重写：双授权状态检测 + 授权引导对话框，Shizuku/Dhizuku 任一授权即可用高权限
- 界面全量国际化：主界面所有用户可见文案全部走字符串资源，内置简体中文与英文，语言运行时切换即时生效
- 内核重写：LLM 客户端、AgentEngine、工具链、模型协调器全面重写，流式对话与工具调用更稳定
- 协议与引导重写：协议文案精简重排，引导流程语言 → API → 权限与开发工具 → 协议，权限步骤新增 Dhizuku 选项
- 设置中心重写：语言/主题/模型管理/开发工具/终端后端等全部对话框国际化并重做玻璃风外观
- IDE 界面重写：文件浏览器、代码编辑器、构建输出面板交互与布局重做
- 应用内更新中心重写：版本列表、下载进度与安装流程重做
- 清理：删除废弃的 Announcement.kt，合并重复字符串资源，规范资源键命名
- minSdk 28，targetSdk 34
- versionCode 321, versionName 2.1.1

## v2.1.0
- 全链路液态玻璃 UI：新增自研 Liquid Glass 组件库（玻璃面板/胶囊导航/分段控件/玻璃气泡/玻璃按钮/玻璃输入框），所有界面统一换装
- 导航结构重写：移除底部导航栏，新增顶部小导航栏（仅 AI 聊天 / 设置），液态渐变流体背景
- 终端与 IDE 从底部导航移入顶部导航区，两个界面彻底重写：玻璃状态栏 + 玻璃面板 + 深色等宽输出区
- 聊天界面全面重写：接入 AgentEngine 真实流式对话，玻璃气泡 + 流式光标 + 工具调用可视化 + 思考深度一键切换
- 系统公告彻底移除，公告信息并入《用户须知》
- 协议全面重写：仅保留《开源软件许可协议》与《用户须知》两份，引导步骤重排为 语言 → API 配置 → 权限与开发工具 → 协议
- README 主页简介重写，开源技术栈说明
- versionCode 320, versionName 2.1.0

## v2.0.4
- 修复 Shizuku 授权 bug：正确检测 binder 状态，添加死亡监听器，修复未授权就提示已授权的问题
- 新增应用开发模式：AI 辅助开发 Android 应用，支持构建 APK
- 消息队列功能：可添加多条消息到队列，一键发送全部
- 长按消息支持复制和重发
- 反馈弹窗：每次进入应用提示反馈邮箱，支持"一天后提醒"和"本次取消"
- 4步引导协议：第1步选择语言 → 第2步配置API → 第3步授权权限（可跳过） → 第4步开源协议与用户须知
- 本地模型 AI 下载提供商：点击提供商自动加载可下载模型列表，显示下载速度
- ChatScreen 全面重写：优化 UI 交互，支持消息队列显示
- SettingsScreen 全面重写：新增应用开发模式切换、开发依赖下载
- minSdk 28，targetSdk 34
- versionCode 304

## v2.0.3
- 移除所有后台服务（VoiceService/BootReceiver/MediaButtonReceiver）
- 彻底删除语音引擎（VoiceEngine/VoiceCoordinator/AudioController）
- 简化为单角色灵犀 LingXi + 思考模式（3 级深度）
- iOS 27 液态玻璃 UI：新增 LIQUID 主题风格
- 本地 AI 提供商选择：Ollama/LM Studio/vLLM/Jan/Custom，自动拉取模型列表
- 模型选择后自动替换到聊天框
- 斜杠命令自动弹出：输入 / 后显示命令列表
- AI 回答流式输出 + 工具调用显示
- SettingsScreen 全面重写（外观/思考模式/模型管理/工具与工作区/权限/更新/关于）
- Coordinator 替代 VoiceCoordinator，纯文本→AgentEngine 桥梁
- minSdk 28，targetSdk 34
- versionCode 303

## v2.0.2
- 角色系统：聊天框顶部圆形头像+名字，支持 5 个默认角色（灵犀 LingXi/代码专家/写作助手/数据分析师/知识导师），可自建角色并支持 AI 自动生成 prompt
- 模式切换：8 种内置模式（聊天/思考/分析/开发/创意/编程/翻译/教学），思考模式支持 3 级深度调节
- 模型选择器：半宽长方形 Surface，分本地模型/API 模型两类展示
- 聊天输入栏重设计：圆角输入框 + 圆形发送按钮，带流式光标动画
- ChatScreen 全面重写：顶部栏含角色+模型+模式，ChatBubble 带角色头像，EmptyHint 引导页
- 彻底移除语音：ttsEnabled 默认 false，移除录音按钮、语音状态、语音引导文案
- minSdk 28，targetSdk 34
- versionCode 302

## v2.0.1
- 设置页全面重构：统一布局，分区清晰（外观、语音与反馈、模型管理、工具与工作区、后台服务、权限、更新、关于）
- 模型管理：新增本地模型下载（支持 HuggingFace / ModelScope / Ollama / 本地文件），外接模型（API）配置，模型选择界面支持两种模式
- 自定义服务商：合并 URL + Key + 模型名 + 拉取模型列表 + 测试连接 + 自定义模型增删
- 工作区管理：直接点击选择，支持新建/导入/删除
- 语言选择：40+ 种语言 RadioButton 列表
- 更新中心：最新+历史 Tab，支持下载/安装
- 清除安装包：自动扫描并删除旧版本 APK
- 权限修复：录音/通知权限去授权，Shizuku 权限触发请求
- 移除顶栏检查更新/历史版本入口（已移入设置）
- minSdk 28，支持 Android 9+
- 修复工作区无法直接选择问题
- 合并振动反馈与波形动画为单一开关

## v2.0.0（正式版）
- 内置终端：LOCAL / Shizuku / ROOT 三后端，危险命令自动拦截
- AI 工具调用：LLM 可调用 run_shell / list_files / read_file / write_file，多轮工具调用历史完整保留
- 工作区：默认内部工作区 + SAF 外部工作区导入，可切换、重命名、删除
- 斜杠命令：/ws、/model、/clear、/help
- 自定义模型列表：设置页增删，对话页联动展示
- 设置页：AI 工具开关、工作区管理、内置终端入口、Shizuku 权限状态
- Manifest：补全 Shizuku provider 声明与权限
- 修复 TerminalScreen ROOT 后端 enabled 逻辑
- 修复 WorkspaceTools ListFilesTool parametersSchema

## v2.0.0-beta.1（预测试版）
- 应用全面转型为手机语音助手「灵犀 LingXi」
- 全新深色紫蓝渐变 UI，Material 3 设计语言
- 新增 VoiceService 后台语音监听服务（前台通知）
- 新增 VoiceEngine：Android SpeechRecognizer + TTS 双引擎
- 新增 IntentRegistry：基于正则的中文语音意图识别引擎
- 新增 FileController：文件浏览/读取/写入/移动/复制/删除/搜索
- 新增 AudioController：媒体播放控制 + 音量管理
- 新增 VoiceAccessibilityService：无障碍服务辅助屏幕操作
- 新增 BootReceiver：系统开机自动恢复监听服务
- 新增 PermissionManager：完整的动态权限请求管理
- 新增 AgentEngine：语音驱动的主控制引擎
- 新增四大页面：首页 / 语音 / 技能 / 设置
- 新增自动更新检查机制
- 新增锁屏可见 + 唤醒屏幕支持
- 最小 SDK 26，目标 SDK 34

## v1.0.9
- 修复多工具调用历史不完整
- 新增会话持久化
- 新增可切换工作区（SAF）

## v1.0.8
- 修复多工具调用历史不完整
- 新增会话持久化与多会话管理
- SAF 工作区支持
