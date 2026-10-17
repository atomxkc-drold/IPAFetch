# ApiVault proguard rules
# Keep our domain models (used with Kotlin serialization @Serializable)
-keepclassmembers class com.apivault.app.domain.** {
    public static final java.lang.Class[] a();
}

# Keep Tink keyset serialization
-keep class com.google.crypto.tink.** { *; }

# Keep SQLCipher
-keep class net.sqlcipher.** { *; }

# Keep Room metadata
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Don't obfuscate serialization annotations
-keepattributes RuntimeVisibleAnnotations
-keepattributes *Annotation*
