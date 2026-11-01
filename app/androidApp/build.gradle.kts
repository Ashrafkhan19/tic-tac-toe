import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":app:shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.playServicesAds)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.example.tictactoe"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "studio.gameloop.tictactoe"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = project.findProperty("versionCode")?.toString()?.toIntOrNull() ?: 1
        versionName = project.findProperty("versionName")?.toString() ?: "1.0"
    }
    val envKeystorePath = System.getenv("KEYSTORE_PATH")
    val keystoreFile = envKeystorePath?.takeIf { it.isNotBlank() }?.let { path ->
        val candidate = file(path)
        if (candidate.exists()) candidate else rootProject.file(path)
    }
    val envKeystorePassword = System.getenv("KEYSTORE_PASSWORD")
    val envKeyAlias = System.getenv("KEY_ALIAS")
    val envKeyPassword = System.getenv("KEY_PASSWORD")

    signingConfigs {
        if (keystoreFile != null && keystoreFile.exists()) {
            create("release") {
                storeFile = keystoreFile
                storePassword = envKeystorePassword
                keyAlias = envKeyAlias
                keyPassword = envKeyPassword
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}