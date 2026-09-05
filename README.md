# AURA - 智能语音助手

AURA（Autonomous Utterance Response Assistant）是一款运行在 Android 设备上的智能语音助手应用。通过语音指令即可完成音乐控制、文件管理、系统设置切换、拨打电话等日常操作，取代传统语音助手的部分功能。

## 功能特性

- **实时语音监听**：支持后台持续监听，语音唤醒指令响应
- **智能意图识别**：基于规则的中文语音指令解析引擎
- **文字转语音**：使用系统 TTS 引擎朗读回复内容
- **文件管理**：通过语音指令移动、复制、删除手机文件
- **媒体控制**：播放/暂停/下一首/上一首/调节音量
- **系统操控**：蓝牙、WiFi、手电筒等快捷开关
- **通讯功能**：拨打电话、发送消息
- **自动更新**：启动时检查新版本并提示更新
- **无障碍服务**：通过 AccessibilityService 捕获屏幕内容辅助操作
- **开机自启**：系统重启后自动恢复监听服务

## 技术架构

- **语言**：Kotlin 100%
- **UI 框架**：Jetpack Compose + Material Design 3
- **架构模式**：MVVM（Model-View-ViewModel）
- **语音引擎**：Android SpeechRecognizer + TextToSpeech
- **后台服务**：Foreground Service + AccessibilityService
- **数据持久化**：Android DataStore Preferences

## 权限需求

| 权限 | 用途 |
|------|------|
| RECORD_AUDIO | 语音识别输入 |
| FOREGROUND_SERVICE | 后台监听服务 |
| POST_NOTIFICATIONS | 通知栏状态显示 |
| READ/WRITE_EXTERNAL_STORAGE | 文件管理操作 |
| SYSTEM_ALERT_WINDOW | 悬浮窗（可选） |
| RECEIVE_BOOT_COMPLETED | 开机自启 |
| QUERY_ALL_PACKAGES | 查询已安装应用列表 |

## 构建

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建（需配置签名密钥环境变量）
export BSK_KEYSTORE=/path/to/keystore.jks
export BSK_KEYSTORE_PASSWORD=your_password
export BSK_KEY_PASSWORD=your_key_password
./gradlew assembleRelease
```

## 版本历史

### v2.0.1
- 设置页全面重构：自定义服务商、工作区管理、语言选择、更新中心、关于页
- 权限修复：录音/通知去授权
- 移除顶栏检查更新/历史版本入口（已移入设置）
- minSdk 31，仅支持 Android 12+
- 修复工作区无法直接选择问题

### v2.0.0
- 内置终端：LOCAL / Shizuku / ROOT 三后端
- AI 工具调用：多轮工具调用历史完整保留
- 工作区：默认内部 + SAF 外部导入
- 斜杠命令：/ws、/model、/clear、/help
- 自定义模型列表

### v2.0.0-beta.1
- 全面重写为语音助手应用
- 全新 AURA 品牌 UI/UX
- 新增 VoiceService 后台监听
- 新增 IntentRegistry 意图识别
- 新增 FileController 文件管理
- 新增 AudioController 媒体控制
- 新增 VoiceAccessibilityService 无障碍服务
- 新增 BootReceiver 开机自启
- 新增完整权限管理系统

### v1.0.9
- 修复多工具调用历史不完整
- 新增会话持久化
- 新增可切换工作区

---

**包名**：`com.bskai`  
**最低 Android 版本**：Android 8.0 (API 26)  
**目标 Android 版本**：Android 14 (API 34)
