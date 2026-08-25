-dontwarn javax.annotation.**
-keep class fm.libro.wearos.api.models.** { *; }
-keep class fm.libro.wearos.data.** { *; }
# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }

# AudioTrack reflection
-keep class android.media.AudioTrack { *; }

# Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep enums
-keepclassmembers enum * {
   public static **[] values();
   public static ** valueOf(java.lang.String);
}