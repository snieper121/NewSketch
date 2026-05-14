# ===========================================================
# Ai IDE — ProGuard / R8 rules
# ===========================================================
# Применяется в release builds (isMinifyEnabled = true).
# Keep line numbers for better crash reports.

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ===========================================================
# Kotlin
# ===========================================================
-keepattributes *Annotation*, InnerClasses
-keep class kotlin.Metadata { *; }
-dontnote kotlin.**
-dontwarn kotlin.reflect.jvm.internal.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ===========================================================
# kotlinx.serialization
# ===========================================================
# Serializer companions of top-level @Serializable types
-keep,includedescriptorclasses class my.company.ai.**$$serializer { *; }
-keepclassmembers class my.company.ai.** {
    *** Companion;
}
-keepclasseswithmembers class my.company.ai.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep @Serializable classes themselves
-keep @kotlinx.serialization.Serializable class my.company.ai.** { *; }
-keepclassmembers class * implements java.io.Serializable {
    private static final long serialVersionUID;
}

# ===========================================================
# Ktor Client
# ===========================================================
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }
-keep class kotlinx.atomicfu.** { *; }
-keepclassmembers class * {
    @io.ktor.util.InternalAPI *;
}

# OkHttp (Ktor engine)
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ===========================================================
# Room
# ===========================================================
-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# ===========================================================
# Jetpack Compose
# ===========================================================
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Compose Navigation
-keep class androidx.navigation.** { *; }

# ===========================================================
# KotlinPoet (runtime codegen library)
# ===========================================================
-dontwarn com.squareup.kotlinpoet.**
-keep class com.squareup.kotlinpoet.** { *; }

# ===========================================================
# DataStore
# ===========================================================
-keep class androidx.datastore.*.** { *; }

# ===========================================================
# EncryptedSharedPreferences (security-crypto)
# ===========================================================
-keep class androidx.security.crypto.** { *; }
-dontwarn com.google.crypto.tink.**

# ===========================================================
# Timber
# ===========================================================
-dontwarn org.jetbrains.annotations.**

# ===========================================================
# Ai IDE — own code
# ===========================================================
# Keep all ViewModels reachable by reflection (ViewModelFactory)
-keep class my.company.ai.ui.screens.**.*ViewModel { *; }

# Keep Application
-keep class my.company.ai.AiApp { *; }

# Keep BuildService (started from Manifest)
-keep class my.company.ai.build.BuildService { *; }

# Keep data/model classes fully (serialized to JSON)
-keep class my.company.ai.data.model.** { *; }
-keep class my.company.ai.data.remote.dto.** { *; }

# ===========================================================
# In-process toolchain (DexClassLoader)
# ===========================================================
# Loaded JARs are NOT subject to R8, but reflection keys must survive:
-keepnames class org.jetbrains.kotlin.cli.jvm.K2JVMCompiler { *; }
-keepnames class org.eclipse.jdt.internal.compiler.batch.Main { *; }

# ===========================================================
# Misc warnings
# ===========================================================
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.**
