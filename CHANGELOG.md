# AURA 更新记录

## v2.0.2
- 角色系统：聊天框顶部圆形头像+名字，支持 5 个默认角色（AURA/代码专家/写作助手/数据分析师/知识导师），可自建角色并支持 AI 自动生成 prompt
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
- 应用全面转型为手机语音助手「AURA」
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
