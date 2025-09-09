# Daily List ProGuard Rules
# Optimized for release builds with Smart Focus features

# ============================================================================
# GENERAL ANDROID RULES
# ============================================================================

# Keep all public classes that extend Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep native method names
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep constructors for activities, services, and broadcast receivers
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ============================================================================
# ROOM DATABASE RULES
# ============================================================================

# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# Keep Room generated implementations
-keep class * extends androidx.room.RoomDatabase$Callback
-keep class androidx.room.** { *; }

# Keep all TaskEntity fields for database operations
-keep class de.beigel.list.data.TaskEntity { *; }
-keep class de.beigel.list.data.TaskDao { *; }
-keep class de.beigel.list.data.TaskDatabase { *; }
-keep class de.beigel.list.data.Converters { *; }

# Keep enums used in database
-keep enum de.beigel.list.data.TaskPriority { *; }
-keep enum de.beigel.list.data.EnergyLevel { *; }
-keep enum de.beigel.list.data.TaskContext { *; }
-keep enum de.beigel.list.data.RecurrencePattern { *; }
-keep enum de.beigel.list.data.UrgencyLevel { *; }

# ============================================================================
# KOTLIN COROUTINES
# ============================================================================

# Keep coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcher {}

# Keep coroutines dispatcher
-keep class kotlinx.coroutines.android.** { *; }
-keep class kotlinx.coroutines.** { *; }

# ============================================================================
# JETPACK COMPOSE RULES
# ============================================================================

# Keep Compose runtime
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# Keep Compose material design components
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.foundation.** { *; }

# Keep Compose navigation
-keep class androidx.navigation.** { *; }
-keep class androidx.navigation.compose.** { *; }

# Keep our Composable functions
-keep @androidx.compose.runtime.Composable class *
-keep class de.beigel.list.ui.** { *; }

# ============================================================================
# WORKMANAGER RULES
# ============================================================================

# Keep WorkManager classes
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
-keep class androidx.work.impl.WorkManagerInitializer
-keep class androidx.work.** { *; }

# Keep our custom workers
-keep class de.beigel.list.service.MidnightResetWorker { *; }
-keep class de.beigel.list.notification.NotificationWorker { *; }
-keep class de.beigel.list.data.DatabaseMaintenanceWorker { *; }

# ============================================================================
# GLANCE (APP WIDGET) RULES
# ============================================================================

# Keep Glance widgets
-keep class androidx.glance.** { *; }
-dontwarn androidx.glance.**

# Keep our widget classes
-keep class de.beigel.list.widget.** { *; }

# ============================================================================
# VIEWMODEL & LIFECYCLE RULES
# ============================================================================

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep our ViewModels and Factories
-keep class de.beigel.list.viewmodel.** { *; }

# Keep Lifecycle components
-keep class androidx.lifecycle.** { *; }

# ============================================================================
# SERIALIZATION RULES
# ============================================================================

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep serializable classes
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    # synthetic methods for serialization
    synthetic <methods>;
    # serialization plugin
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================================================
# SMART FOCUS SYSTEM RULES
# ============================================================================

# Keep Smart Focus core classes
-keep class de.beigel.list.utils.TaskPriorityCalculator { *; }
-keep class de.beigel.list.utils.NaturalLanguageProcessor { *; }
-keep class de.beigel.list.utils.UserContext { *; }
-keep class de.beigel.list.utils.TaskReasoning { *; }
-keep class de.beigel.list.utils.ScoreCategory { *; }

# Keep repository classes
-keep class de.beigel.list.repository.** { *; }

# Keep settings manager
-keep class de.beigel.list.settings.SettingsManager { *; }
-keep enum de.beigel.list.settings.** { *; }

# Keep data classes for Smart Focus
-keep class de.beigel.list.repository.ContextualRecommendations { *; }
-keep class de.beigel.list.repository.ProductivityInsights { *; }
-keep class de.beigel.list.repository.RecommendedAction { *; }

# ============================================================================
# MODEL CLASSES RULES
# ============================================================================

# Keep all model/data classes
-keep class de.beigel.list.data.** { *; }

# Keep UI state classes
-keep class de.beigel.list.viewmodel.SmartFocusUiState { *; }
-keep class de.beigel.list.viewmodel.TaskUiState { *; }
-keep class de.beigel.list.viewmodel.DialogState { *; }
-keep class de.beigel.list.viewmodel.ViewMode { *; }

# Keep dialog classes
-keep class de.beigel.list.ui.dialogs.ParsedTask { *; }

# ============================================================================
# THIRD-PARTY LIBRARIES
# ============================================================================

# Keep Accompanist
-keep class com.google.accompanist.** { *; }

# Keep DataStore
-keep class androidx.datastore.** { *; }

# Keep Startup
-keep class androidx.startup.** { *; }

# ============================================================================
# PERFORMANCE OPTIMIZATIONS
# ============================================================================

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Remove debug code
-assumenosideeffects class de.beigel.list.BuildConfig {
    public static final boolean DEBUG return false;
}

# Optimize method calls
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Remove unnecessary metadata
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault

# ============================================================================
# REFLECTION RULES
# ============================================================================

# Keep classes used via reflection
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Keep classes with @Keep annotation
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# ============================================================================
# WARNINGS TO IGNORE
# ============================================================================

# Ignore warnings from external libraries
-dontwarn org.jetbrains.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**

# Ignore warnings from Kotlin metadata
-dontwarn kotlin.Metadata

# Ignore warnings from Compose compiler
-dontwarn androidx.compose.compiler.**

# ============================================================================
# DEBUG SPECIFIC RULES
# ============================================================================

# Keep debug information for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep custom exception classes for better crash reports
-keep public class * extends java.lang.Exception

# ============================================================================
# ADAPTIVE ICON RULES
# ============================================================================

# Keep adaptive icon resources
-keep class **.R$drawable { *; }
-keep class **.R$mipmap { *; }

# ============================================================================
# WIDGET PREVIEW RULES
# ============================================================================

# Keep widget preview classes
-keep class androidx.glance.preview.** { *; }
-keep @androidx.glance.preview.Preview class *

# ============================================================================
# BACKUP RULES
# ============================================================================

# Keep backup agent classes
-keep class * extends android.app.backup.BackupAgent

# ============================================================================
# ANALYTICS RULES (if added later)
# ============================================================================

# Keep analytics classes (placeholder for future use)
# -keep class com.google.firebase.analytics.** { *; }
# -keep class com.google.android.gms.measurement.** { *; }

# ============================================================================
# CUSTOM APP SPECIFIC RULES
# ============================================================================

# Keep our main application class
-keep class de.beigel.list.DailyListApplication { *; }

# Keep MainActivity
-keep class de.beigel.list.MainActivity { *; }

# Keep all public methods in public classes (for API compatibility)
-keepclassmembers public class * {
    public <methods>;
}

# Keep field names for Gson/JSON serialization (if used)
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============================================================================
# FINAL OPTIMIZATION SETTINGS
# ============================================================================

# Enable aggressive optimization
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively

# Don't skip non-public library classes
-dontskipnonpubliclibraryclasses

# Print mapping information
-printmapping mapping.txt
-printusage usage.txt
-printseeds seeds.txt