# FloatAI — 开源 Android AI 助手

支持 Android 8 ~ 14（API 26 ~ 34），四核设备可运行。

## 功能
- 底部导航：AI 聊天 / API 配置 / 设置（Navigation Compose）
- 全新 Material 3 动态主题：壁纸取色、自定义主题色、亮暗切换
- AI 聊天：对话历史持久化、历史记录管理、悬浮窗（可开关）
- API 配置：支持所有 OpenAI 兼容 AI 服务商，拉取模型列表，保存配置
- 设置：主题色、动态取色、悬浮窗、权限说明、检查更新
- 首次启动显示用户须知与开源协议
- 内置检查更新，自动连接 GitHub Release 获取版本更新

## 下载
前往 [GitHub Releases](https://github.com/1953187487/FloatAI/releases) 下载最新 APK。

## 更新记录
查看 [CHANGELOG.md](CHANGELOG.md)

## 构建
```bash
# 需要 Android SDK + JDK 17
export ANDROID_HOME=/path/to/android-sdk
gradle assembleDebug
```

## 开源
MIT License，仓库：[https://github.com/1953187487/FloatAI](https://github.com/1953187487/FloatAI)
