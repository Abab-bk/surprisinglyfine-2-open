-dontoptimize

-keep class * extends javax.sound.sampled.spi.AudioFileReader { *; }
-keep class * extends javax.sound.sampled.spi.FormatConversionProvider { *; }
-keep class * extends javax.sound.sampled.spi.AudioFileWriter { *; }
-keep class * extends javax.sound.sampled.spi.MixerProvider { *; }

-keep class com.jcraft.jogg.** { *; }
-keep class com.jcraft.jorbis.** { *; }
-keep class org.tritonus.** { *; }

-keepdirectories META-INF/services
