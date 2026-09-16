plugins {
    alias(libs.plugins.agp.app)
}

android {
    namespace = "com.github.rove24.pixel"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.github.rove24.pixel"
        minSdk = 28
        targetSdk = 34
        versionCode = 5
        versionName = "0.5"
    }

    signingConfigs {
        getByName("debug") {
            // Default debug keystore
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "/kotlin/**"
            excludes += "/META-INF/*.version"
            excludes += "/META-INF/*.kotlin_module"
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    compileOnly(libs.libxposed.api)
    compileOnly("androidx.annotation:annotation:1.7.0")
}