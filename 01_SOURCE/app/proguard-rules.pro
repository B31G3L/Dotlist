# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

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

# Room specific rules
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# Keep model classes
-keep class de.beigel.list.data.** { *; }
-keep class de.beigel.list.viewmodel.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcher {}

# Compose specific
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
-keep class androidx.work.impl.WorkManagerInitializer
-keep class de.beigel.list.service.** { *; }
-keep class de.beigel.list.notification.** { *; }

# Glance (Widget)
-keep class androidx.glance.** { *; }
-keep class de.beigel.list.widget.** { *; }
-dontwarn androidx.glance.**

# BacklogManager and related classes
-keep class de.beigel.list.data.BacklogManager { *; }
-keep class de.beigel.list.repository.** { *; }
-keep class de.beigel.list.settings.** { *; }

# Migration classes
-keep class androidx.room.migration.** { *; }
-keepclassmembers class androidx.room.migration.Migration {
    public <methods>;
}

# Navigation Component
-keep class androidx.navigation.** { *; }
-keep class * extends androidx.fragment.app.Fragment{}

# Lifecycle
-keep class androidx.lifecycle.** { *; }

# Material Components
-keep class com.google.android.material.** { *; }

# Receivers
-keep class de.beigel.list.receiver.** { *; }

# Application class
-keep class de.beigel.list.DailyListApplication { *; }
-keep class de.beigel.list.MainActivity { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# Notification specific
-keep class de.beigel.list.notification.** { *; }

# Widget specific
-keep class de.beigel.list.widget.** { *; }

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Serialization
-keepattributes Signature
-keepattributes *Annotation*

# For native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Performance optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile