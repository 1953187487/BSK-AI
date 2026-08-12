# FloatAI ProGuard / R8 rules (v1.0.3 hardening)

# ===== 通用 =====
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 不要打印警告
-dontwarn org.apache.http.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn java.beans.**
-dontwarn javax.naming.**

# ===== AndroidX / Compose =====
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }
-keep class kotlin.coroutines.Continuation
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-dontwarn androidx.compose.**

# Compose Composable / ViewModel 等需要反射
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * implements androidx.lifecycle.ViewModelProvider.Factory { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ===== Application / Activity / Service（AndroidManifest 引用的）=====
-keep class com.floatai.App { *; }
-keep class com.floatai.MainActivity { *; }
-keep class com.floatai.SplashActivity { *; }
-keep class com.floatai.service.FloatService { *; }
-keep class com.floatai.capture.CaptureService { *; }

# ===== JSON / 网络 =====
-keep class com.floatai.data.model.** { *; }
-keep class com.floatai.data.remote.** { *; }
-keep class com.floatai.data.remote.OpenAiClient { *; }

# ===== MCP / NanoHTTPD =====
-keep class fi.iki.elonen.** { *; }
-dontwarn fi.iki.elonen.**
-keep class org.nanohttpd.** { *; }

# ===== ProtocolFlow / VersionGate / SettingsRepository =====
-keep class com.floatai.core.versioning.** { *; }
-keep class com.floatai.core.versioning.VersionGate { *; }

# ===== 保留 enum values()/valueOf() =====
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===== Parcelable =====
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ===== Serializable =====
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===== Compose 反射（由 R8 自动生成）=====
-keepclassmembers class **.R$* { *; }
-keep class kotlin.reflect.jvm.internal.impl.** { *; }

# ===== 字符串加密（用 DexGuard-style 名字混淆 + 类名混淆）=====
-allowaccessmodification
-repackageclasses 'a'
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# ===== 调试痕迹移除（移除 Log 类除错信息）=====
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ===== 反调试（可选，加固后部分机型可能闪退，谨慎开启）=====
# -keep class com.floatai.security.AntiDebug { *; }
