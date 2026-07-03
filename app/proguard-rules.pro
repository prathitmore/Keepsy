# Add project specific ProGuard rules here.

# Keep Room related classes
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao
-keep class androidx.room.paging.LimitOffsetDataSource { *; }

# Firebase Authentication
-keep class com.google.firebase.auth.** { *; }

# Firebase Analytics & Crashlytics
-keep class com.google.firebase.analytics.** { *; }
-keep class com.google.firebase.crashlytics.** { *; }

# Moshi & JSON serialization
-keep class com.keepsy.app.model.** { *; }
-keepclassmembers class com.keepsy.app.model.** {
    <fields>;
}
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonQualifier interface *
-keepclassmembers class * {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Coil (Image Loading)
-keep class coil.** { *; }
-dontwarn coil.**

# Lottie (Animations)
-keep class com.airbnb.lottie.** { *; }

# WorkManager
-keep class androidx.work.** { *; }

# Compose
-keep class androidx.compose.material.icons.** { *; }

# Preserve the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
