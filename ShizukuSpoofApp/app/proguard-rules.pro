# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Shizuku specific rules
-keep class rikka.shizuku.** { *; }
-keep class org.lsposed.hiddenapibypass.** { *; }

# AIDL interfaces
-keep interface com.example.spoofer.IShizukuLocationService { *; }
-keep class com.example.spoofer.IShizukuLocationService$Stub { *; }

# Location service
-keep class com.example.spoofer.ShizukuLocationService { *; }

# Reflection usage for system context
-keepclassmembers class android.app.ActivityThread {
    public static android.app.ActivityThread currentActivityThread();
    public android.content.Context getSystemContext();
}

# Keep generic signatures
-keepattributes Signature

# Keep source file names and line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
