plugins {
    alias(gradleLibs.plugins.android.library)
    alias(gradleLibs.plugins.compose.compiler)
    alias(gradleLibs.plugins.google.ksp)
    alias(gradleLibs.plugins.google.services) apply false
    alias(gradleLibs.plugins.kotlin.android)
    alias(gradleLibs.plugins.kotlin.serialization)
}

android {
    namespace = AppConfiguration.appId+".tv"
    compileSdk = AppConfiguration.compileSdk

    defaultConfig {
        minSdk = AppConfiguration.minSdk
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("r8Test") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("alpha") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    lint {
        targetSdk = AppConfiguration.targetSdk
    }

    testOptions {
        targetSdk = AppConfiguration.targetSdk
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(AppConfiguration.jdk))
    }
}

dependencies {
    implementation(project(":app:shared"))
}