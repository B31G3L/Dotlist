# Firebase Firestore
-keep class com.google.firebase.** { *; }

# Datenmodelle: Firestore mappt Dokumentfelder per Reflection auf die
# Feldnamen. Werden die von R8 umbenannt, liefert toObject() im Release-Build
# stumm Defaults statt Daten – im Debug-Build faellt das nie auf, weil dort
# nicht minifiziert wird.
-keep class com.beigel.list2share.data.** { *; }
-keepclassmembers class com.beigel.list2share.data.** {
    <init>();
    <fields>;
}

# @PropertyName und @Exclude muessen erhalten bleiben, sonst greifen sie nicht.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
