plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val uploadStoreFile = System.getenv("ANDROID_KEYSTORE_FILE")
val uploadStorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val uploadKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
val uploadKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
val releaseSigningAvailable = !uploadStoreFile.isNullOrBlank() &&
    !uploadStorePassword.isNullOrBlank() &&
    !uploadKeyAlias.isNullOrBlank() &&
    !uploadKeyPassword.isNullOrBlank()

android {
    namespace = "com.scanfoodai.foodscanner.nutrition"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.scanfoodai.foodscanner.nutrition"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }
    signingConfigs {
        if (releaseSigningAvailable) {
            create("upload") {
                storeFile = file(uploadStoreFile!!)
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
            }
        }
    }
    buildTypes {
        getByName("release") {
            if (releaseSigningAvailable) {
                signingConfig = signingConfigs.getByName("upload")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}
