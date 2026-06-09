plugins {
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.compose.multiplatform).apply(false)
    alias(libs.plugins.kotlin.android).apply(false)
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.android.kmp.library).apply(false)
    alias(libs.plugins.kotlin.jvm).apply(false)
    alias(libs.plugins.kotlinx.serialization).apply(false)
}

val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val platformSuffix = if (isWindows) "win" else "linux"

allprojects {
    layout.buildDirectory.set(layout.projectDirectory.dir("build-$platformSuffix"))
}