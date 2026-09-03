# ---------------------------------------------------------------------------
# Firestore-Datenmodelle
#
# Firestore mappt Dokumentfelder per Reflection auf die Feldnamen. Werden die
# von R8 umbenannt, liefert toObject() im Release-Build stumm Defaults statt
# Daten - im Debug-Build faellt das nie auf, weil dort nicht minifiziert wird.
#
# Betroffen sind nur die Modellklassen in Models.kt. Das restliche data-Paket
# (DeviceIdManager, *Preferences, local/) nutzt keine Reflection und darf von
# R8 optimiert werden.
# ---------------------------------------------------------------------------
-keep class com.beigel.list2share.data.Models* { *; }
-keepclassmembers class com.beigel.list2share.data.Models* {
    <init>();
    <fields>;
}

# @PropertyName und @Exclude muessen erhalten bleiben, sonst greifen sie nicht.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Hinweis: Ein pauschales -keep class com.google.firebase.** { *; } ist nicht
# noetig. Die Firebase-Artefakte liefern ihre eigenen Consumer-Rules mit, und
# der Blanket-Keep nimmt das komplette SDK von Shrinking und Optimierung aus.

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