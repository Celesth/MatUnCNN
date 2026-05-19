# Keep Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep FFmpegKit
-keep class com.arthenica.ffmpegkit.** { *; }

# Keep Coil
-keep class coil.** { *; }

# Keep DataStore
-keep class * extends androidx.datastore.core.DataStore { *; }

# Keep our app models
-keep class com.matuncnn.app.model.** { *; }
