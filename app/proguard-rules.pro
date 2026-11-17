# JNI boundary - never strip the native methods or their names.
-keep class org.servo.servoview.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Compose and Kotlin metadata used by reflection.
-dontwarn androidx.compose.**
-keep class com.servodroid.browser.model.** { *; }
