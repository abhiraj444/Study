# Add project specific ProGuard rules here.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.studyflow.**$$serializer { *; }
-keepclassmembers class com.studyflow.** {
    *** Companion;
}
