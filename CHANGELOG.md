# FloatAI 更新记录

## v1.0.0（重构版）
- 推翻重做 UI 底层架构：MVVM + Navigation Compose + 全新设计系统
- 全新 Material 3 动态主题：支持 Android 12+ 壁纸动态取色、自定义主题色、亮暗切换
- 数据层重构：SettingsRepository / ChatRepository / OpenAiClient 分层，StateFlow 响应式状态
- AI 聊天：对话历史持久化、历史记录查看/删除/新建
- API 配置：模型列表拉取、常用模型快捷选择、自定义模型
- 设置：主题色选择器、动态取色开关、悬浮窗控制、权限说明、检查更新
- 悬浮窗服务优化：拖拽移动、展开菜单、进程列表
- 首次启动双步协议（用户须知 + 权限协议）

## v0.1（测试版）
- 首次发布，支持 Android 8~14（API 26~34）
- 底部导航：AI 聊天 / API 配置 / 设置
- AI 聊天：悬浮窗（可开关）、系统进程查看
- API 配置：支持所有 OpenAI 兼容服务商，可测试模型连通性并保存配置
- 设置：主题颜色、导入字体、Shizuku / Dhizuku 授权
- 首次启动显示用户须知与开源协议
- 内置检查更新（连接 GitHub Release）
- 修复：进入闪退问题、Android 14 前台服务崩溃
