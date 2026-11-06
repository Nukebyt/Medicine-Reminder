# Keep kotlinx.serialization generated serializers for the JSON export/import format.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.nukebyt.medicinereminder.**$$serializer { *; }
-keepclassmembers class com.nukebyt.medicinereminder.** { *** Companion; }
-keepclasseswithmembers class com.nukebyt.medicinereminder.** { kotlinx.serialization.KSerializer serializer(...); }
