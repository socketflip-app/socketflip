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
        versionCode = 18
        // Test builds: ./gradlew assembleRelease -PtestBuild=3 gives "1.13-test3".
        versionName = "1.13" + (providers.gradleProperty("testBuild").orNull?.let { "-test$it" } ?: "")
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
            // v3 alongside v2, same key: it is what lets the key be rotated later
            // (with a lineage) without every install having to uninstall first.
            enableV2Signing = true
            enableV3Signing = true
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

    // Fail the release build on any call above minSdk, so nothing Android 11 only reaches Android 10.
    lint {
        fatal += "NewApi"
        checkReleaseBuilds = true
    }

    // Kotlin's metadata files are never read at run time; about 8% of the APK.
    packaging {
        resources {
            excludes += listOf("kotlin/**", "kotlin-tooling-metadata.json", "META-INF/*.version")
        }
    }

    // Leave out AGP's encrypted dependency blob so the APK rebuilds byte for byte.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

// No runtime dependencies at all. JUnit is for the plain JVM unit tests only
// (./gradlew testReleaseUnitTest) and never reaches the APK.
dependencies {
    testImplementation("junit:junit:4.13.2")
}
