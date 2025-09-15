# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep camera related classes
-keep class androidx.camera.** { *; }
-keep class com.siegelth.camera.** { *; }

# Keep CameraX classes
-dontwarn androidx.camera.**
-keep class androidx.camera.core.** { *; }
-keep class androidx.camera.lifecycle.** { *; }
-keep class androidx.camera.view.** { *; }

# Keep ExifInterface
-keep class androidx.exifinterface.** { *; }

# Keep data binding classes
-keep class * extends androidx.databinding.ViewDataBinding { *; }
-keep class * extends androidx.databinding.BaseObservable { *; }

# Keep view binding classes
-keep class **.*Binding { *; }

# Keep Bitmap and graphics related classes
-keep class android.graphics.** { *; }

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile