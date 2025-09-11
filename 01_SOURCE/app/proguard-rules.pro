# Add project specific ProGuard rules here.

# Room specific rules
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcher {}

# Compose specific
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
-keep class androidx.work.impl.WorkManagerInitializer

# Glance (Widget)
-keep class androidx.glance.** { *; }
-dontwarn androidx.glance.**

# Keep model classes
-keep class de.beigel.list.data.** { *; }
-keep class de.beigel.list.viewmodel.** { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
# BacklogManager
-keep class de.beigel.list.data.BacklogManager { *; }

# Migration classes
-keep class androidx.room.migration.** { *; }
-keepclassmembers class androidx.room.migration.Migration {
    public <methods>;
}