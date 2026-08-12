# Requirements — v1.0.3 FloatAI Package Hub

## Introduction

FloatAI v1.0.3 重构应用入口与设置结构：删除 ATK 模块、把底部导航替换为「项目 / AI 聊天」、把语言选择器从首次启动流挪到「设置」内一个按钮、把多个分散功能（包管理 / 插件 / 技能 / MCP）整合为统一的「Package Hub」并配备四 Tab。协议页加入开源协议与权限逐项声明，每个权限可在设置中「撤销」。

## Glossary

- **Package Hub**：v1.0.3 新增的统一容器页面，下含 Packages / Plugins / Skills / MCP 四个 Tab
- **Project**：用户当前在 Build 屏幕创建/管理的本地 Android 项目
- **Permission Revocation**：在 Android API 33+，应用只能撤销自己声明的运行时权限
- **VpnService Capture**：使用系统级 VpnService 抓取本机进出流量，仅本机流量，未离开设备

## Requirements

### Requirement 1 — 移除 ATK 模块

**User Story:** AS 用户, I want 移除 ATK 模块, so that 主导航只保留有价值的功能

#### Acceptance Criteria

1. WHEN 用户打开应用主界面, THE MainActivity SHALL 不再展示 ATK Tab 或入口
2. THE AtkScreen 与 nav_atk 字符串 SHALL 从 APK 中彻底移除
3. WHEN Drawer 渲染菜单项, THE AppShell SHALL 不包含 ATK 项

### Requirement 2 — 底部导航变为「聊天 + 项目」

**User Story:** AS 用户, I want 底部导航简化, so that 一键进入聊天或项目

#### Acceptance Criteria

1. THE NavigationBar SHALL 仅显示两个 Tab：AI 聊天 与 项目（Build）
2. WHEN 用户点击底部任意 Tab, THE AppShell SHALL 切换到对应目的地
3. THE Drawer SHALL 包含完整六个菜单：聊天 / 项目 / Package Hub / Settings / About / Updates

### Requirement 3 — Package Hub 四 Tab

**User Story:** AS 用户, I want 把分散的包管理/MCP 整合, so that 一个入口管理所有扩展

#### Acceptance Criteria

1. THE Package Hub SHALL 显示 4 个 Tab：Packages、Plugins、Skills、MCP
2. WHEN 用户切换到 Packages Tab, THE Hub SHALL 显示 v1.0.2 已实现的 PackageRegistry 内容
3. WHEN 用户切换到 Plugins Tab, THE Hub SHALL 显示插件列表（v1.0.4 实现创建逻辑）
4. WHEN 用户切换到 Skills Tab, THE Hub SHALL 显示技能列表（v1.0.4 实现创建逻辑）
5. WHEN 用户切换到 MCP Tab, THE Hub SHALL 显示 v1.0.2 已实现的 McpRegistry 内容
6. THE MCP 模块 SHALL 从 Drawer 移除（仅存在于 Hub）

### Requirement 4 — 语言选择器挪到设置

**User Story:** AS 用户, I want 启动流更短, so that 跳过首次选语言步骤

#### Acceptance Criteria

1. WHEN 用户首次启动应用, THE MainActivity SHALL 直接进入协议页（不再先选语言）
2. THE SettingsScreen SHALL 提供「语言」按钮，点击后弹窗选择 ZH / EN
3. THE LanguageFlow 组件 SHALL 从 APK 中彻底移除
4. THE MainActivity WHEN 用户已选过语言, THE 应用 SHALL 显示协议页；否则跳到协议页

### Requirement 5 — 协议页加入开源协议与权限声明

**User Story:** AS 用户, I want 协议同时说明权限, so that 一次签署完成所有知情

#### Acceptance Criteria

1. THE ProtocolFlow SHALL 显示三项内容：用户须知、开源协议、运行时权限说明
2. THE 运行时权限列表 SHALL 包含：悬浮窗（SYSTEM_ALERT_WINDOW）、通知（POST_NOTIFICATIONS）、麦克风（RECORD_AUDIO）、相机（CAMERA）、使用情况访问（PACKAGE_USAGE_STATS）、VpnService
3. THE 协议页 SHALL 配三组 Checkbox，每组对应上述三段内容
4. THE 用户 SHALL 必须勾选全部三组 Checkbox 后才能点击「同意并继续」
5. THE BuildConfig.PROTOCOL_VERSION SHALL 升级为 4 强制重签

### Requirement 6 — 设置中的「权限撤销」入口

**User Story:** AS 用户, I want 在设置里随时撤销权限, so that 收回不需要的能力

#### Acceptance Criteria

1. THE SettingsScreen SHALL 提供「权限」分组，列出全部已声明权限及当前状态
2. WHEN 用户点击任一权限的「撤销」按钮, THE 设置页面 SHALL 调用系统 API 撤销对应运行时权限（API 33+）
3. THE 特殊权限（悬浮窗 / 使用情况） SHALL 引导用户跳转到系统设置页手动关闭
4. THE 麦克风 / 相机 权限 SHALL 在撤销后立刻不再出现在 Android 13+ 设置中（系统行为，应用只触发 revokeSelfPermission）

### Requirement 7 — 设置中的「关于 / 检查更新 / 版本历史」

**User Story:** AS 用户, I want 一个集中的「关于」页, so that 检查更新与看历史更直观

#### Acceptance Criteria

1. THE Drawer SHALL 新增「关于」菜单项（route = `about`）
2. THE AboutScreen SHALL 显示：版本号、协议版本、构建时间、开源协议链接、Github 仓库链接
3. THE AboutScreen SHALL 提供「检查更新」按钮，调用 UpdateRepository 拉取最新 release
4. THE AboutScreen SHALL 显示最近 5 个 release 的列表（含 tag、发布时间、changelog 摘要）
5. THE SettingsScreen 中的「检查更新」入口 SHALL 移除（统一到关于页）

### Requirement 8 — 项目功能完善（Build 屏）

**User Story:** AS 用户, I want 在 App 内创建本地 Android 项目, so that 快速试用工具链

#### Acceptance Criteria

1. THE BuildScreen SHALL 提供「创建项目」表单：项目名、包名、最低 SDK（24/26/28/30/33/34）、目标 SDK、模板（Compose / XML）
2. WHEN 用户点击「创建」, THE ToolchainManager SHALL 在 filesDir/projects/{name}/ 写入完整 Gradle 项目脚手架（build.gradle、AndroidManifest.xml、MainActivity.kt、settings.gradle）
3. THE 项目 SHALL 显示在 BuildScreen 列表中，附「构建 / 删除 / 打开」操作
4. WHEN 用户选择已有项目并点击「构建」, THE ToolchainManager SHALL 排队一个构建任务（v1.0.4 接 ProcessBuilder，本版本只生成脚手架）

### Requirement 9 — 悬浮窗面板集成「抓包」入口

**User Story:** AS 用户, I want 在悬浮窗面板选择应用并抓包, so that AI 帮我分析流量

#### Acceptance Criteria

1. THE FloatService 面板 SHALL 第三个 Tab 为「抓包」（原第二个 Tab「应用」保留为第四）
2. WHEN 用户进入抓包 Tab, THE 面板 SHALL 显示本机已安装应用列表供选择
3. WHEN 用户点击应用并点击「开始抓包」, THE FloatService SHALL 启动 CaptureService（VpnService 子类）并以系统 VPN 通知呈现
4. THE CaptureService SHALL 把每条 HTTP/HTTPS 请求与响应落盘到 filesDir/captures/{sessionId}.jsonl
5. THE 抓包数据 SHALL 在 FloatService 抓包 Tab 内显示最近 20 条
6. WHEN 用户点击某条记录, THE 抓包详情视图 SHALL 调用已配置 AI 客户端分析该请求/响应内容
7. THE AI 分析 SHALL 仅在用户主动点击「AI 分析」按钮时调用，不自动触发

### Requirement 10 — 悬浮窗 UI 进一步美化

**User Story:** AS 用户, I want 悬浮窗视觉更现代, so that 与应用整体 M3 风格一致

#### Acceptance Criteria

1. THE 面板 SHALL 使用圆角 20dp、玻璃拟态背景（半透明 + blur 已通过 alpha 模拟）
2. THE Tab 切换 SHALL 使用滑动指示器（Underline）动画
3. THE 面板 SHALL 支持展开 / 收起动画（300ms tween）
4. THE FAB SHALL 在长按时显示「拖动我」气泡提示

## Out of Scope (v1.0.4+)

- 插件 / 技能的实际创建与运行逻辑（v1.0.3 仅展示空 Tab 与「即将推出」提示）
- 真实构建 Android APK（仍依赖沙盒外执行）
- ATK 模块回归
- 协议内容的具体文案撰写（v1.0.3 使用占位文案）
