# WorkManager creates its Room database by reflection.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }

# kotlinx.serialization: keep generated serializers for our models.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.student.amizoneplus.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.student.amizoneplus.**$$serializer { *; }

# Jsoup ships optional re2j support we don't use.
-dontwarn com.google.re2j.**

# OkHttp optional platform classes.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
