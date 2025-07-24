# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep AI Agent classes
-keep class dev.aurakai.auraframefx.ai.** { *; }

# Keep AIDL interfaces
-keep class dev.aurakai.oracledrive.IAuraDriveService { *; }
-keep class dev.aurakai.oracledrive.IAuraDriveService$Stub { *; }
-keep class dev.aurakai.oracledrive.IAuraDriveServiceCallback { *; }

# Keep Xposed hooks
-keep class dev.aurakai.auraframefx.xposed.** { *; }

# Keep data classes
-keep class dev.aurakai.auraframefx.data.** { *; }

# Keep Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep Room entities
-keep class dev.aurakai.auraframefx.data.database.entities.** { *; }

# Keep Hilt components
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Retrofit interfaces
-keep interface dev.aurakai.auraframefx.** { *; }

# Keep OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep Gson (if used)
-keep class com.google.gson.** { *; }

# Keep kotlinx.coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep Timber
-keep class timber.log.** { *; }

# Keep custom exceptions
-keep class dev.aurakai.auraframefx.**Exception { *; }

# Optimization settings
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Keep names of classes that are referenced in AndroidManifest.xml
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void i(...);
    public static void w(...);
    public static void d(...);
    public static void e(...);
}
