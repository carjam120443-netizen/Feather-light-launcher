plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.carjam.featherlightlauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.carjam.featherlightlauncher"
        minSdk = 26
        targetSdk = 35
        versionCode = providers.gradleProperty("versionCode").map(String::toInt).getOrElse(1)
        versionName = providers.gradleProperty("versionName").getOrElse("0.1.0")
    }

    signingConfigs {
        create("release") {
            val keystorePath = providers.gradleProperty("keystorePath")
            val keystorePassword = providers.gradleProperty("keystorePassword")
            val keyAlias = providers.gradleProperty("keyAlias")
            val keyPassword = providers.gradleProperty("keyPassword")
            if (keystorePath.isPresent && keystorePassword.isPresent && keyAlias.isPresent && keyPassword.isPresent) {
                storeFile = file(keystorePath.get())
                storePassword = keystorePassword.get()
                this.keyAlias = keyAlias.get()
                this.keyPassword = keyPassword.get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
