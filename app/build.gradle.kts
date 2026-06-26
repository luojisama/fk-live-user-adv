plugins {
    id("com.android.application")
}

val appVersionCodeValue = providers.gradleProperty("appVersionCode").get().toInt()
val appVersionNameValue = providers.gradleProperty("appVersionName").get()
val releaseKeystoreFile = providers.environmentVariable("ANDROID_KEYSTORE_FILE").orNull
val releaseKeystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull

android {
    namespace = "moe.shiro.lsposed.contentfilter"
    compileSdk = 36

    defaultConfig {
        applicationId = "moe.shiro.lsposed.contentfilter"
        minSdk = 23
        targetSdk = 35
        versionCode = appVersionCodeValue
        versionName = appVersionNameValue
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        if (!releaseKeystoreFile.isNullOrBlank()
            && !releaseKeystorePassword.isNullOrBlank()
            && !releaseKeyAlias.isNullOrBlank()
            && !releaseKeyPassword.isNullOrBlank()) {
            create("release") {
                storeFile = file(releaseKeystoreFile)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    compileOnly(project(":xposed-stubs"))
}

