# kotlinx.serialization 生成的序列化器需要保留
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.coursework.tracker.model.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
