# User Instruction Memory

This file records user instructions, preferences, and teachings for reference in future interactions.

## Format

### User Instruction Entry
User instruction entries should follow this format:

[User Instruction Summary]
- Date: [YYYY-MM-DD]
- Context: [Mentioned scenario or time]
- Instructions:
  - [Content of user teaching or instruction, described line by line]

### Project Knowledge Entry
Entries discovered by the Agent during task execution should follow this format:

[Project Knowledge Summary]
- Date: [YYYY-MM-DD]
- Context: Discovered by Agent while performing [specific task description]
- Category: [Operations & Deployment|Build Methods|Testing Methods|Troubleshooting & Debugging|Workflow & Collaboration|Environment Configuration]
- Instructions:
  - [Specific knowledge points, described line by line]

## Deduplication Strategy
- Before adding a new entry, check for similar or identical instructions.
- If a duplicate is found, skip the new entry or merge it with the existing one.
- When merging, update the context or date information.
- This helps avoid redundant entries and keeps the memory file tidy.

## Entries

[Project Knowledge Summary]
- Date: 2026-08-24
- Context: Discovered by Agent while performing v1.0.8 构建验证
- Category: Build Methods
- Instructions:
  - BSK AI 为 Android/Gradle 项目，构建命令：`export ANDROID_HOME=/opt/android-sdk && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew assembleDebug --no-daemon`
  - Android SDK 安装于 /opt/android-sdk（含 platforms;android-34 与 build-tools;34.0.0），JDK 17 位于 /usr/lib/jvm/java-17-openjdk-amd64
  - 构建是长耗时任务（首次约 10 分钟，需下载 Gradle 8.5 与依赖），必须用 background terminal 执行并设资源限制
  - Gradle wrapper 版本 8.5，JVM 参数 -Xmx3500m（gradle.properties 配置）
