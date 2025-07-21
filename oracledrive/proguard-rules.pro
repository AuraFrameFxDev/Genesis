# OracleDrive specific ProGuard rules
-keep class dev.aurakai.auraframefx.oracledrive.** { *; }
-keep class dev.aurakai.auraframefx.oracledrive.api.** { *; }
-keep class dev.aurakai.auraframefx.oracledrive.storage.** { *; }
-keep class dev.aurakai.auraframefx.oracledrive.security.** { *; }

# Oracle Database connectivity
-keep class oracle.** { *; }
-dontwarn oracle.**

# Cloud storage
-keep class com.google.cloud.storage.** { *; }
-dontwarn com.google.cloud.storage.**

# Encryption and security
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }