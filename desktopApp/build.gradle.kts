import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":sharedUI"))
}

compose.desktop {
    application {
        mainClass = "MainKt"

        jvmArgs += listOf(
            "--enable-native-access=ALL-UNNAMED",

            "--add-opens", "java.base/sun.misc=ALL-UNNAMED",
            "--add-opens", "java.base/java.nio=ALL-UNNAMED",
        )

        nativeDistributions {
            modules("jdk.unsupported") // dataStore requires it

            buildTypes.release {
                proguard {
                    isEnabled = false
                    configurationFiles.from(project.file("proguard-rules.pro"))
                }
            }

            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "GoodIdleGame"
            packageVersion = "1.0.0"

            linux {
                iconFile.set(project.file("appIcons/LinuxIcon.png"))
            }
            windows {
                iconFile.set(project.file("appIcons/WindowsIcon.ico"))
            }
            macOS {
                iconFile.set(project.file("appIcons/MacosIcon.icns"))
                bundleID = "com.rorokaiiworks.goodidlegame.desktopApp"
            }
        }
    }
}

kotlin {
    jvmToolchain(21)
}
