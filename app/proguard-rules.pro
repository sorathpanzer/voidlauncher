# --- General ---
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.lifecycle.**
-dontwarn androidx.annotation.**

# Keep Kotlin metadata for reflection and serialization
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-keep class kotlin.jvm.internal.** { *; }

# Keep Parcelable implementations (needed for Android)
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# --- Keep your app's specific classes for reflection and serialization ---

# Keep your app settings data classes and annotations
-keep class app.voidlauncher.data.settings.AppSettings { *; }
-keepclassmembers class app.voidlauncher.data.settings.AppSettings { *; }
-keep @app.voidlauncher.data.settings.Setting class * { *; }
-keepclasseswithmembers class * {
    @app.voidlauncher.data.settings.Setting <fields>;
}

-keepclassmembers class app.voidlauncher.data.settings.AppSettings {
    <fields>;
    <methods>;
}

-keep class app.voidlauncher.data.settings.SettingsManager { *; }

-keep class app.voidlauncher.ui.screens.SettingsScreenKt { *; }

# Keep all Setting annotations and enums used in annotations
-keep @interface app.voidlauncher.data.settings.Setting
-keepclassmembers enum app.voidlauncher.data.settings.SettingCategory { *; }
-keepclassmembers enum app.voidlauncher.data.settings.SettingType { *; }

# Keep annotations & runtime visible annotations for reflection & tools
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations, Signature, EnclosingMethod, InnerClasses

# --- Obfuscation and shrinking enabled ---
# Remove unused code & rename classes/methods/fields not explicitly kept
# This helps reduce size and improve performance and security

# Don't disable obfuscation or shrinking unless necessary
# So no "-dontobfuscate" or "-dontshrink"

# Keep line numbers for crash stack trace debugging (optional)
# -keepattributes SourceFile,LineNumberTable

# --- Optimization ---
# You can enable optimizations, but some projects disable to avoid issues
# If you want, you can add:
# -optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# --- Misc ---
# You can add specific rules for libraries or dependencies as needed

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
