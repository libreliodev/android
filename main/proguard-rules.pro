# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/michael/Development/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Use unique member names to make stack trace reading easier
-useuniqueclassmembernames

# Google Play Services

-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# EventBus

-keepclassmembers class ** {
    public void onEvent*(**);
}

# Two way view

-keep class org.lucasr.twowayview.** { *; }

# OkHttp

-dontwarn com.squareup.okhttp.internal.huc.**

# Okio

-dontwarn okio.**

# Internal code

-keep public interface com.librelio.view.SubscriberCodeDialog$OnSubscriberCodeListener {*;}

-keep public interface com.librelio.view.UsernamePasswordLoginDialog$OnUsernamePasswordLoginListener {*;}

-keep public class com.librelio.activity.BillingActivity { *; }
-keepclassmembers public class com.librelio.activity.BillingActivity { *; }

# RxJava

-dontwarn rx.**

# Remove logging calls
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

-dontwarn com.librelio.**