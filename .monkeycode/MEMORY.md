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
  - BSK AI 为 Android/Gradle 项目,构建命令:`export ANDROID_HOME=/opt/android-sdk && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew assembleDebug --no-daemon`
  - Android SDK 安装于 /opt/android-sdk(含 platforms;android-34 与 build-tools;34.0.0),JDK 17 位于 /usr/lib/jvm/java-17-openjdk-amd64
  - 构建是长耗时任务(首次约 10 分钟,需下载 Gradle 8.5 与依赖),必须用 background terminal 执行并设资源限制
  - Gradle wrapper 版本 8.5,JVM 参数 -Xmx3500m(gradle.properties 配置)

[Project Knowledge Summary]
- Date: 2026-09-11
- Context: 落地 AURA 2.1.0 沙盒服务器(Python FastAPI + Redis)与 Android 端 Retrofit 对接
- Category: Operations & Deployment
- Instructions:
  - 沙盒服务器:cd server/sandbox && python3 -m uvicorn main:app --host 127.0.0.1 --port 18080
  - 依赖:fastapi/uvicorn/pyjwt/redis/httpx(AURA_OAUTH_MOCK=1 时不调真实 QQ/微信开放平台,便于离线测试)
  - Redis:redis-server --daemonize yes(已在 127.0.0.1:6379 运行),启动服务前先确认 redis-cli ping 返回 PONG
  - 首次启动会 seed 官方账号 ID=0001(账号 aura_official,密码 changeme-official-Initial!;生产用带外机制下发)
  - 冒烟测试:python3 server/sandbox/test_smoke.py(需服务在跑,24/24 通过基线)
  - 数据在 server/sandbox/data/aura.db(SQLite)+ Redis 键 aura:session:{user_id}(单端唯一)

[Project Knowledge Summary]
- Date: 2026-09-11
- Context: 补齐 Android 构建环境(新容器无预装)
- Category: Environment Configuration
- Instructions:
  - JDK 17:apt-get install -y openjdk-17-jdk
  - Android SDK:下载 cmdline-tools 9477386 解压到 /opt/android-sdk/cmdline-tools/latest,执行 sdkmanager "platforms;android-34" "build-tools;34.0.0"
  - 需 echo "sdk.dir=/opt/android-sdk" 写入 /workspace/local.properties
  - 依赖 retrofit:com.squareup.retrofit2:retrofit:2.11.0 + converter-gson:2.11.0 + gson:2.10.1(已加到 app/build.gradle)
