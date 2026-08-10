# FloatAI — 开源 Android AI 助手

支持 Android 8 ~ 14（API 26 ~ 34），四核设备可运行。

## 功能
- 底部导航：AI 聊天 / API 配置 / 设置
- AI 聊天：悬浮窗（可开关）、系统进程查看
- API 配置：支持所有 OpenAI 兼容 AI 服务商，测试模型连通性，保存配置
- 设置：主题颜色、导入字体、Shizuku / Dhizuku 授权
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
