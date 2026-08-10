# FloatAI — 开源 Android AI 助手

版本：0.1 测试版

支持 Android 8 ~ 14（API 26 ~ 34），四核设备可运行。

## 功能
- 导航栏：AI 聊天 / API 配置 / 设置
- AI 聊天：支持自定义服务商 API，悬浮窗模式（可开关），进程查看
- API 配置：输入服务商 URL / Key，测试模型连通性
- 设置：颜色主题、导入字体、更换 UI 资源、悬浮窗授权（Shizuku / Dhizuku）

## 下载
- v0.1 APK（Debug 构建）：`app/build/outputs/apk/debug/app-debug.apk`

## 构建
```bash
# 需要 Android SDK + JDK 17
export ANDROID_HOME=/path/to/android-sdk
gradle assembleDebug
```

## 开源
MIT License，仓库名：`FloatAI`
