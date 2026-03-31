# Add project specific ProGuard rules here.
-keep class com.bluewificaller.model.** { *; }
-keep class com.bluewificaller.service.** { *; }
-keepattributes *Annotation*
-keepclassmembers class ** {
    @com.google.gson.annotations.SerializedName <fields>;
}
