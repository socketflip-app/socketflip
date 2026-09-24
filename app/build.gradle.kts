plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.socketflip"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.socketflip"
        minSdk = 29
        targetSdk = 35
        versionCode = 5
        versionName = "1.4"
    }

    // Release signing comes from ~/.gradle/gradle.properties (never the repo):
    //   SOCKETFLIP_STORE_FILE, SOCKETFLIP_STORE_PASSWORD, optional SOCKETFLIP_KEY_ALIAS / SOCKETFLIP_KEY_PASSWORD.
    // Without them, assembleRelease produces an unsigned APK.
    val storeFilePath = providers.gradleProperty("SOCKETFLIP_STORE_FILE").orNull
    val releaseSigning = storeFilePath?.let {
        signingConfigs.create("release") {
            storeFile = file(it)
            storePassword = providers.gradleProperty("SOCKETFLIP_STORE_PASSWORD").get()
            keyAlias = providers.gradleProperty("SOCKETFLIP_KEY_ALIAS").orElse("socketflip").get()
            keyPassword = providers.gradleProperty("SOCKETFLIP_KEY_PASSWORD")
                .orElse(providers.gradleProperty("SOCKETFLIP_STORE_PASSWORD")).get()
        }
    }

    buildTypes {
        release {
            signingConfig = releaseSigning
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
