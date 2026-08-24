# Requirements — v1.0.8 BSK AI Agent 编码能力完善

## Introduction

BSK AI v1.0.8 围绕「AI 编码智能体」做三块增强：

1. 修复多工具调用（multi-tool-call）解析缺陷——此前一轮推理返回多个 tool_call 时，消息历史只记录第一条，其余工具调用在后续推理中丢失，导致上下文不完整、Agent 行为漂移。
2. 引入会话持久化——对话与工具历史保存到本地，重启可恢复，支持多会话切换与新建/删除。
3. 引入可切换工作区——Agent 文件类工具的执行根目录从固定目录改为用户可选择的目录（含工具链项目目录）。

## Glossary

- **System**：BSK AI Android 应用
- **Agent**：AI 编码智能体，由 AgentEngine 驱动
- **Tool**：Agent 可调用的能力单元（read_file / write_file / edit_file / shell 等）
- **Tool Call**：LLM 在一次回复中声明的工具调用，含 id / name / arguments
- **Session（会话）**：一次 Agent 对话的完整消息历史（用户消息、assistant 消息、工具调用与结果）
- **Workspace（工作区）**：Agent 文件类工具的相对根目录，路径解析的基准
- **Application Private Directory**：应用私有存储目录（filesDir / getExternalFilesDir(null)），无需运行时权限即可读写

## Requirements

### Requirement 1 — 多工具调用历史完整性

**User Story:** AS Agent 用户, I want 一轮推理返回多个工具调用时历史保持完整, so that Agent 后续推理基于全部工具结果继续

#### Acceptance Criteria

1. WHEN Agent 引擎一次推理返回 2 个以上 tool_call, THE AgentEngine SHALL 将全部 tool_call（含 id、name、arguments）写入 assistant 消息历史
2. WHEN 一条 assistant 消息包含多个 tool_call, THE AgentMessage 数据结构 SHALL 保留每个 tool_call 的独立 id / name / args
3. WHEN 消息序列化到 API 请求, THE AgentMessage.toWire SHALL 输出该 assistant 消息的全部 tool_calls 数组
4. WHEN 一轮包含多个 tool_call, THE AgentEngine SHALL 依次执行每个工具，并把每个工具结果按 tool_call_id 关联写入后续 tool 角色消息
5. IF 某个工具执行失败或用户拒绝, THE AgentEngine SHALL 将失败/拒绝结果写入对应 tool_call_id 的消息并继续执行剩余工具
6. IF 多个 tool_call 中混有未知工具名, THE AgentEngine SHALL 为未知工具写入错误结果并继续执行其他工具

### Requirement 2 — 会话持久化

**User Story:** AS 用户, I want Agent 会话可保存、恢复与切换, so that 重启应用后工作不丢失

#### Acceptance Criteria

1. WHEN 用户发送消息, THE AgentViewModel SHALL 将用户消息与后续 assistant / 工具消息实时写入当前会话存储
2. WHEN 用户重启应用, THE Agent 页面 SHALL 显示会话列表（含标题与最近更新时间）
3. WHEN 用户点击某个历史会话, THE System SHALL 加载该会话的完整消息历史，并以其为上下文继续对话
4. WHEN 用户点击「新建会话」, THE System SHALL 创建空白会话并清空当前聊天界面
5. WHEN 用户删除会话, THE System SHALL 删除对应会话数据文件
6. THE 会话数据 SHALL 以 JSON 文件形式保存在应用私有目录，不设消息条数上限
7. WHEN 会话包含首条用户消息, THE System SHALL 生成会话标题（取首条用户消息的前 20 个字符）
8. WHEN 恢复会话并继续对话, THE System SHALL 以该会话完整历史为上下文继续推理，但不重新执行历史中的任何工具调用
9. WHEN 恢复会话并继续对话, THE System SHALL 将历史工具调用与结果原样展示在聊天界面中，标记为历史记录

### Requirement 3 — 可切换工作区

**User Story:** AS 用户, I want 为 Agent 选择工作目录, so that 智能体可直接操作工具链项目或其他目录

#### Acceptance Criteria

1. WHEN 用户进入 Agent 页面, THE System SHALL 展示当前工作区路径或目录描述
2. WHEN 用户点击「切换工作区」, THE System SHALL 通过系统目录选择器（SAF / ACTION_OPEN_DOCUMENT_TREE）允许选择设备任意目录，并持久化该目录的访问授权
3. WHEN 用户确认选择, THE System SHALL 将所选目录记录为 Agent 的工作区根目录并持久化
4. WHEN 用户未选择任何目录, THE System SHALL 使用默认 workspace 目录
5. WHILE Agent 会话运行, THE 文件类工具（read_file / write_file / edit_file / list_dir / new_project / build_project）SHALL 以当前工作区为相对路径解析基准，工作区为 SAF 目录时通过 ContentResolver 读写
6. WHEN 用户切换工作区, THE System SHALL 立即对后续工具调用生效，不中断运行中的会话
7. WHEN 工作区为普通文件系统目录且目录不存在, THE System SHALL 自动创建该目录
8. WHEN 工作区为 SAF 目录, THE shell 工具与依赖真实文件系统路径的功能 SHALL 返回错误提示，指引用户切换到应用私有目录

## Out of Scope（本次不实现，列入后续版本候选）

- shell 命令超时控制与输出截断策略调整
- 工作区路径穿越（`../`）拦截
- 新增 grep_search 等代码搜索工具
- 基于 token 预算的上下文裁剪与 continue 续写
- 会话云端同步
- 会话导出 / 分享
