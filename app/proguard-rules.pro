# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Android\sdk/tools/proguard/proguard-android.txt
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

# Garde toutes les classes de l'application Android
-keep class fr.arnaudguyon.spacevertex.** { *; }

# Évite de supprimer les annotations importantes (ex: Room, Gson, Retrofit)
-keepattributes *Annotation*

# Garde les noms des classes et méthodes utilisées via réflexion
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

-assumenosideeffects class android.util.Log {
#    public static *** d(...);
#    public static *** v(...);
#    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Active le renommage et l'obfuscation
-dontpreverify
-repackageclasses
-allowaccessmodification