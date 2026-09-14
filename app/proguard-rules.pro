# ---------------------------------------------------------------------------
# Firestore-Datenmodelle
#
# Firestore mappt Dokumentfelder per Reflection auf die Feldnamen und braucht
# einen parameterlosen Konstruktor. Werden die von R8 umbenannt oder entfernt,
# scheitert toObject() im Release-Build - im Debug-Build faellt das nie auf,
# weil dort nicht minifiziert wird.
#
# ACHTUNG: "Models*" trifft NUR die Datei-Fassade ModelsKt. Kotlin legt die
# Klassen aus Models.kt als eigenstaendige Klassen an, nicht als Models$...
# Die Regel muss sie deshalb einzeln nennen. Kommt eine neue Modellklasse
# dazu, gehoert sie hierher - sonst funktioniert sie nur im Debug-Build.
#
# Das restliche data-Paket (DeviceIdManager, *Preferences, local/) nutzt keine
# Reflection und darf von R8 optimiert werden.
# ---------------------------------------------------------------------------
-keep class com.beigel.list2share.data.TodoList { *; }
-keep class com.beigel.list2share.data.TodoItem { *; }
-keep class com.beigel.list2share.data.Subtask { *; }
-keep class com.beigel.list2share.data.Comment { *; }
-keep class com.beigel.list2share.data.Recurrence { *; }
-keep class com.beigel.list2share.data.AppNotification { *; }
-keep class com.beigel.list2share.data.Invite { *; }
-keep class com.beigel.list2share.data.ListCounts { *; }

-keepclassmembers class com.beigel.list2share.data.TodoList { <init>(); <fields>; }
-keepclassmembers class com.beigel.list2share.data.TodoItem { <init>(); <fields>; }
-keepclassmembers class com.beigel.list2share.data.Subtask { <init>(); <fields>; }
-keepclassmembers class com.beigel.list2share.data.Comment { <init>(); <fields>; }
-keepclassmembers class com.beigel.list2share.data.Recurrence { <init>(); <fields>; }
-keepclassmembers class com.beigel.list2share.data.AppNotification { <init>(); <fields>; }
-keepclassmembers class com.beigel.list2share.data.Invite { <init>(); <fields>; }

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