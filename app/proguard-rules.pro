# Keep event classes for Moshi serialization
-keep class com.viis.rozkamai.domain.event.** { *; }
-keep class com.viis.rozkamai.domain.model.** { *; }

# Moshi
-keepclassmembers class * {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Hilt
-keep class dagger.hilt.** { *; }
