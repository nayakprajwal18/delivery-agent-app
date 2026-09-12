# Add project specific ProGuard rules here.

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }

# Keep data classes used with kotlinx.serialization
-keep @kotlinx.serialization.Serializable class * { *; }

# Supabase / Ktor
-dontwarn io.ktor.**
-dontwarn io.github.jan.supabase.**
