# ProGuard rules for DeepSeek Chat

# Keep model classes used by Room and JSON parsing
-keep class com.deepseek.chat.domain.model.** { *; }
-keep class com.deepseek.chat.data.local.entity.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep JS Bridge
-keepclassmembers class com.deepseek.chat.ui.chat.ChatActivity$ChatBridge {
   @android.webkit.JavascriptInterface <methods>;
}

# OkHttp / Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

# EncryptedSharedPreferences
-dontwarn com.google.crypto.tink.**
-keep class com.google.crypto.tink.** { *; }
