# Preserve line numbers and source file names for de-obfuscation in crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontwarn kotlinx.serialization.**
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Google Play Services & AdMob
-keep public class com.google.android.gms.ads.** {
    public *;
}
-keep public class com.google.ads.** {
    public *;
}
-keep class com.google.android.gms.internal.ads.** { *; }

# Multiplatform Settings
-dontwarn com.russhwolf.settings.**
-keep class com.russhwolf.settings.** { *; }

# WorkManager's Room database: old Room consumer rules don't keep the generated
# constructor, and R8 full mode strips it, breaking WorkManager init at startup.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
