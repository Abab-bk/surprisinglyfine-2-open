import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.rorokaiiworks.goodidlegame"
        compileSdk = 36
        minSdk = 23
        androidResources.enable = true
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.ui)
            api(libs.compose.foundation)
            api(libs.compose.resources)
            api(libs.compose.ui.tooling.preview)
            api(libs.compose.material3)

            api(libs.kermit)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime)
            implementation(libs.compose.nav3)

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.json.io)

            api(libs.koin.core)
            api(libs.koin.compose)
            api(libs.koin.viewmodel)

            implementation(libs.kotlinx.datetime)
            implementation(libs.kstore)

            api("name.kropp.kotlinx-gettext:kotlinx-gettext:0.7.0")
            api("com.squareup.okio:okio:3.16.2")
            implementation("dev.theolm:txtlogwriter:0.0.4")

            implementation("androidx.datastore:datastore:1.2.0")
            implementation("androidx.datastore:datastore-preferences:1.2.0")

            implementation("com.charleskorn.kaml:kaml:0.104.0")
            api("io.matthewnelson.kmp-file:file:0.6.0")

            implementation("com.mikepenz.hypnoticcanvas:hypnoticcanvas:0.4.1")
            implementation("com.svenjacobs.reveal:reveal-core:4.0.0")

            implementation("com.github.alorma.compose-settings:ui-tiles:2.23.0")
            implementation("com.github.alorma.compose-settings:ui-tiles-extended:2.23.0")
            implementation("com.github.alorma.compose-settings:ui-tiles-expressive:2.23.0")

            implementation("org.jetbrains.compose.material3.adaptive:adaptive:1.3.0-alpha02")

            implementation("com.composables:icons-feather-cmp:2.2.1")

            implementation("com.materialkolor:material-kolor:4.0.0")
            implementation("io.github.mohammedalaamorsi:colorpicker:1.0.2")

            implementation("io.github.3moly:compose-data-viz:0.1.0")
            implementation("com.mohamedrejeb.dnd:compose-dnd:0.3.0")

            implementation("io.github.ehsannarmani:compose-charts:0.2.5")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.ui.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.kstore.file)

            api("com.taptap.sdk:tap-core:4.10.0")
            api("com.taptap.sdk:tap-login:4.10.0")
            api("com.taptap.sdk:tap-leaderboard-androidx:4.10.0")
            implementation("com.taptap.sdk:tap-achievement:4.10.0")
            implementation("com.taptap.sdk:tap-compliance:4.10.0")
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.kstore.file)

            api("com.code-disaster.steamworks4j:steamworks4j:1.10.0")
            implementation("com.googlecode.soundlibs:vorbisspi:1.0.3.3")
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
}
