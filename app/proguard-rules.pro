# kotlinx.serialization keeps generated serializers for our @Serializable models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.jackiechen.quotadroid.** {
    *** Companion;
}
-keepclasseswithmembers class com.jackiechen.quotadroid.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.jackiechen.quotadroid.**$$serializer { *; }

# OkHttp ships its own consumer rules; these silence optional platform hooks.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
