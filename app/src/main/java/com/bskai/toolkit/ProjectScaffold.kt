package com.bskai.toolkit

import android.content.Context
import java.io.File

data class ProjectConfig(
    val name: String,
    val packageName: String,
    val appLabel: String,
    val minSdk: Int = 26,
    val targetSdk: Int = 34,
    val versionName: String = "1.0",
    val versionCode: Int = 1,
    val theme: String = "bsk_default"
)

/**
 * 在设备上生成可直接构建的 Java Android 项目骨架。
 */
object ProjectScaffold {

    fun create(context: Context, config: ProjectConfig): File? {
        val root = File(
            context.getExternalFilesDir(null) ?: context.filesDir,
            "projects/${config.name}"
        )
        if (!root.exists()) root.mkdirs()
        val pkgPath = config.packageName.replace('.', '/')

        write(root, "AndroidManifest.xml", manifest(config))
        write(root, "src/$pkgPath/MainActivity.java", mainActivity(config))
        write(root, "res/values/strings.xml", strings(config))
        write(root, "res/values/colors.xml", colors())
        write(root, "res/values/styles.xml", styles(config))
        write(root, "res/drawable/ic_launcher.xml", launcherIcon())
        write(root, "res/drawable/ic_launcher_round.xml", launcherIcon())
        write(root, "build.sh", buildScript(config))
        write(root, "build.properties", buildProperties(config))
        return root
    }

    private fun write(root: File, rel: String, content: String) {
        val f = File(root, rel)
        f.parentFile?.mkdirs()
        f.writeText(content)
    }

    private fun manifest(c: ProjectConfig) = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="${c.packageName}">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:label="@string/app_name"
        android:icon="@drawable/ic_launcher"
        android:allowBackup="true"
        android:theme="@style/AppTheme">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
"""

    private fun mainActivity(c: ProjectConfig) = """package ${c.packageName};

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.graphics.Color;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("${c.appLabel}\n构建成功! (BSK AI v1.0.6)");
        tv.setTextSize(20);
        tv.setPadding(32, 96, 32, 0);
        tv.setTextColor(Color.WHITE);
        setContentView(tv);
        getWindow().getDecorView().setBackgroundColor(Color.parseColor("#1E1B2E"));
    }
}
"""

    private fun strings(c: ProjectConfig) = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">${c.appLabel}</string>
</resources>
"""

    private fun colors() = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="bsk_accent">#6366F1</color>
</resources>
"""

    private fun styles(c: ProjectConfig) = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="AppTheme" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">#1E1B2E</item>
        <item name="android:statusBarColor">#1E1B2E</item>
        <item name="android:colorAccent">@color/bsk_accent</item>
    </style>
</resources>
"""

    private fun launcherIcon() = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#6366F1"
        android:pathData="M20,20h68a8,8 0 0 1 8,8v52a8,8 0 0 1 -8,8h-68a8,8 0 0 1 -8,-8v-52a8,8 0 0 1 8,-8z" />
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M40,40L68,40L68,46L40,46Z" />
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M40,50L58,50L58,56L40,56Z" />
    <path
        android:fillColor="#34D399"
        android:pathData="M40,62a4,4 0 1 0 0,8a4,4 0 0 0 0,-8Z" />
    <path
        android:fillColor="#F59E0B"
        android:pathData="M52,62a4,4 0 1 0 0,8a4,4 0 0 0 0,-8Z" />
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M64,62a4,4 0 1 0 0,8a4,4 0 0 0 0,-8Z" />
</vector>
"""

    private fun buildScript(c: ProjectConfig): String {
        val n = c.name
        val dl = "$"
        val lines = listOf(
            "#!/data/data/com.termux/files/usr/bin/bash",
            "# BSK AI 生成的构建脚本",
            "set -e",
            "PROJ=\"\$(cd \"\$(dirname \"\$0\")\" && pwd)\"",
            "NAME=\"\$1\"",
            "APK_NAME=\"${n}.apk\"",
            "ANDROID_JAR=\"\${HOME}/storage/downloads/BSKAI/android-34/android.jar\"",
            "",
            "echo \"[1/6] 编译资源 (aapt2)\"",
            "rm -rf \"\${PROJ}/build\" && mkdir -p \"\${PROJ}/build/classes\" \"\${PROJ}/build/dex\" \"\${PROJ}/build/apk\" \"\${PROJ}/build/gen\"",
            "aapt2 compile --dir \"\${PROJ}/res\" -o \"\${PROJ}/build/res.zip\"",
            "aapt2 link -o \"\${PROJ}/build/apk/base.apk\" -I \"\${ANDROID_JAR}\" --manifest \"\${PROJ}/AndroidManifest.xml\" \"\${PROJ}/build/res.zip\" --java \"\${PROJ}/build/gen\" --auto-add-overlay",
            "",
            "echo \"[2/6] 编译 Java (javac)\"",
            "find \"\${PROJ}/build/gen\" \"\${PROJ}/src\" -name \"*.java\" > \"\${PROJ}/build/sources.txt\"",
            "javac -d \"\${PROJ}/build/classes\" -classpath \"\${ANDROID_JAR}\" @\"\${PROJ}/build/sources.txt\"",
            "",
            "echo \"[3/6] 转换为 DEX (d8)\"",
            "find \"\${PROJ}/build/classes\" -name \"*.class\" > \"\${PROJ}/build/classes.txt\"",
            "d8 --release --output \"\${PROJ}/build/dex\" --lib \"\${ANDROID_JAR}\" \$(cat \"\${PROJ}/build/classes.txt\")",
            "",
            "echo \"[4/6] 打包 (zip + zipalign)\"",
            "cp \"\${PROJ}/build/apk/base.apk\" \"\${PROJ}/build/apk/unsigned.apk\"",
            "cd \"\${PROJ}/build/dex\" && zip -q -u \"\${PROJ}/build/apk/unsigned.apk\" classes.dex",
            "zipalign -f 4 \"\${PROJ}/build/apk/unsigned.apk\" \"\${PROJ}/build/apk/aligned.apk\"",
            "",
            "echo \"[5/6] 签名 (apksigner)\"",
            "if [ ! -f \"\${PROJ}/build/keystore.jks\" ]; then",
            "    keytool -genkeypair -keystore \"\${PROJ}/build/keystore.jks\" -alias bsk -keyalg RSA -keysize 2048 -validity 10000 \\",
            "        -storepass bsk2026 -keypass bsk2026 -dname \"CN=BSK AI\"",
            "fi",
            "mkdir -p \"\${PROJ}/dist\"",
            "apksigner sign --ks \"\${PROJ}/build/keystore.jks\" --ks-pass pass:bsk2026 --key-pass pass:bsk2026 \\",
            "    --out \"\${PROJ}/dist/\${APK_NAME}\" \"\${PROJ}/build/apk/aligned.apk\"",
            "",
            "echo \"[6/6] 完成: \${PROJ}/dist/\${APK_NAME}\"",
            "echo \"BUILD_DONE:\${APK_NAME}\""
        )
        return lines.joinToString("\n")
    }

    private fun buildProperties(c: ProjectConfig) = """
name=${c.name}
package=${c.packageName}
appLabel=${c.appLabel}
minSdk=${c.minSdk}
targetSdk=${c.targetSdk}
versionName=${c.versionName}
versionCode=${c.versionCode}
""".trimIndent()
}
