# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# WA Notification Critical Classes
-keep class com.example.wanotification.listener.** { *; }
-keep class com.example.wanotification.audio.** { *; }
-keep class com.example.wanotification.parser.** { *; }
-keep class com.example.wanotification.filter.** { *; }
-keep class com.example.wanotification.config.** { *; }
-keep class com.example.wanotification.service.** { *; }
-keep class com.example.wanotification.receiver.** { *; }
-keep class com.example.wanotification.queue.** { *; }
-keep class com.example.wanotification.cooldown.** { *; }
-keep class com.example.wanotification.model.** { *; }

# Keep inner classes
-keepclasseswithmembernames class com.example.wanotification.** {
    native <methods>;
}

# Preserve annotation processing
-keep interface * { *; }
-keep @interface * { *; }

