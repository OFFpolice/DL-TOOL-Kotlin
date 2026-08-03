# Optimization and Shrinking Rules
-optimizationpasses 5
-dontpreverify
-allowaccessmodification
-repackageclasses ''

# Chaquopy preservation rules for python runtime
-keep class com.chaquo.python.** { *; }
-dontwarn com.chaquo.python.**

# Room Database preservation
-keep class * extends androidx.room.RoomDatabase
-keep class com.offpolice.dltool.data.** { *; }

