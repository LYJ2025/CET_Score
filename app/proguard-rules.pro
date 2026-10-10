# ============================================================
# Release 混淆规则（minifyEnabled = true + shrinkResources = true）
#
# 排查线上崩溃时用：
#   ./gradlew assembleRelease -Pshrink=false
# 或临时把 isMinifyEnabled / isShrinkResources 改为 false 后重新构建。
# ============================================================

# ---------- 保留注解与签名（AGP 默认，此处显式声明避免被裁）----------
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable

# ---------- Kotlin ----------
-keepclassmembers class ** {
    @kotlin.Metadata <methods>;
}
-dontwarn kotlin.**

# ---------- kotlinx.serialization ----------
# JSON 序列化靠反射拿 @Serializable 类的 serializer，混淆后类名/字段名变化
# 会导致 SerializationException。评分记录的 detailJson 依赖这一套。
-keep,includedescriptorclasses class com.cetscore.score.**$$serializer { *; }
-keepclassmembers class com.cetscore.score.** {
    *** Companion;
}
-keepclasseswithmembers class com.cetscore.score.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.cetscore.core.data.**$$serializer { *; }
-keepclassmembers class com.cetscore.core.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.cetscore.core.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn kotlinx.serialization.**

# ---------- Room ----------
# 实体字段名会被 SQLite 直接使用（列名按字段名生成），必须保留
-keep class com.cetscore.core.data.entity.** { *; }
-keep class com.cetscore.core.data.**$*_Impl { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ---------- Compose ----------
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# ---------- Haze（毛玻璃）----------
-dontwarn dev.chrisbanes.haze.**

# ---------- 枚举 ----------
# ExamType / QuestionType 通过 name 存取字符串，混淆枚举常量会导致
# 历史记录读回后类型失效
-keepclassmembers enum com.cetscore.score.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep enum com.cetscore.score.** { *; }

# ---------- 去除日志 ----------
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int i(...);
    public static int d(...);
}