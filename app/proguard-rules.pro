# Add project specific ProGuard rules here.
# By default, the flags in this file are applied to all build types.

# Keep Retrofit interfaces
-keepattributes Signature
-keepattributes Exceptions
-keep class com.smarthome.gitops.data.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
