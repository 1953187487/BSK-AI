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
- Date: 2026-09-25
- Context: Discovered by Agent while performing v2.1.1 与 v3.0.0 签名 release 构建及 GitHub Release 发布(含品牌更名 AURA→灵犀 LingXi 与包名迁移)
- Category: Build Methods
- Instructions:
  - v2.1.1 及以前签名密钥 /workspace/aura-release.keystore(非 gradle 默认 $HOME/aura-release.keystore),需 `export BSK_KEYSTORE=/workspace/aura-release.keystore`,alias=aura,密码 aura2026
  - v3.0.0 起(品牌更名 灵犀 LingXi,包名 com.lingxi.ai)签名密钥 /workspace/lingxi-release.keystore,需 `export LINGXI_KEYSTORE=/workspace/lingxi-release.keystore`,alias=lingxi,密码 lingxi2026;RSA 4096 / PKCS12 / SHA384withRSA,有效至 2056,CN=LingXi,OU=Dev,O=LingXi
  - 缺 export 时 validateSigningRelease 报 Keystore file not found;`keytool -list -keystore <ks> -storepass <pw>` 校验密钥,`apksigner verify --print-certs` 与 `aapt dump badging` 校验 APK
  - APK 名 LingXi-<version>-release.apk;`cp app/build/outputs/apk/release/app-release.apk LingXi-3.0.0-release.apk` 后入库(gitignore 仅排除 *.keystore/*.jks)
  - 签名密钥严禁入库,已在 .gitignore 加入 `*.keystore` 与 `*.jks`;但历史 commit 1466a5c 已把 aura-release.keystore 提交进仓库(gitignore 不影响已跟踪文件),需用户自行决定是否轮换该旧密钥并清理历史
  - 本环境 cgroup 内存受限,gradle.properties 的 -Xmx3500m 会导致 assembleRelease 在 optimizeReleaseResources 卡死(RSS 顶满内存上限后 CPU 跌到个位数百分比)。改用 `./gradlew assembleRelease --no-daemon -Dorg.gradle.jvmargs="-Xmx2880m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8"`,约 3 分钟完成
  - 编译校验:`./gradlew :app:compileReleaseKotlin` 约 1.5 分钟;资源问题(重复字符串键)在 :app:mergeReleaseResources 才暴露
  - GitHub 凭据来自 git credential helper: `TOK=$(printf "protocol=https\nhost=github.com\n" | git credential fill | sed -n 's/^password=//p')`,export GH_TOKEN 后即可用 gh api/gh release
  - gh 2.89 中 `-F` 是 --notes-file;附加资产用位置参数,显示标签用 `#` 前缀:`gh release create v<ver> "LingXi-3.0.0-release.apk#灵犀 LingXi v3.0.0 APK" --title "..." --notes-file <body.md> --verify-tag`
  - 上传完成判定为 assets[].upload_state 为 null 且 download_url 非空(约需 1-2 分钟)
  - 发版前需确认 tag 指向最新 commit:若 tag 已存在但指向旧 commit,需 `git tag -d v<ver> && git tag -a v<ver> -m "..." HEAD && git push --force origin v<ver>`(仅在 tag 尚未绑定任何 release 时)
  - 当前 git credential helper 令牌缺少仓库元数据编辑权限:`gh repo edit` / PATCH repos 返回 403 Resource not accessible by integration,只能 push/建 tag/发 release。仓库名与描述需用户在 GitHub 网页自行修改(用户要求保留 URL github.com/1953187487/BSK-AI)

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

[Project Knowledge Summary]
- Date: 2026-09-12
- Context: 排查 AURA 2.1.1 进入引导(OnboardingDialog) API 配置步点"下一步"闪退
- Category: Troubleshooting & Debugging
- Instructions:
  - 闪退根因:引导 step0→step1 切换时首帧挂载 ShizukuStepContent 的 GlassPanel,触发液态玻璃 AGSL RuntimeShader(折射/色散)在 API31-33 部分设备渲染失败崩溃
  - 修复点(app/src/main/java/com/kyant/backdrop/LiquidGlassBackdrop.kt):buildRenderEffect 加 runtimeShadersOk 探针(RuntimeShader 构造+build 验证),API<31 不设置 renderEffect,液面/边缘高光 shader 全部 try/catch(Throwable) 兜底
  - Compose 1.5.4 中 androidx.compose.ui.graphics.asComposeRenderEffect/android.graphics.RenderEffect.isSupported 可用(javap 验证),RenderEffect.isSupported() 在 API<31 返回 false
  - v3.0.0 彻底方案:com/lingxi/ai/ui/glass/LiquidGlass.kt 移除 RuntimeShader/RenderEffect 依赖,改为纯 Compose drawBehind 三段渐变(fill / 顶部 gloss / 对角 rim light);LiquidGlassBackdrop.kt 保留但已无调用方
  - 引导布局重叠/崩溃排查顺序:外层 statusBarsPadding()+navigationBarsPadding() → 步骤容器 weight(1f).fillMaxWidth().clipToBounds() → 内部再 fillMaxSize().verticalScroll;API 预设等横向排列改用 FlowRow 避免溢出
