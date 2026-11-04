plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

import java.util.Properties

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

val releaseStorePath = System.getenv("LENOTE_KEYSTORE_PATH")
    ?: keystoreProperties.getProperty("storeFile")
val releaseStorePassword = System.getenv("LENOTE_KEYSTORE_PASSWORD")
    ?: keystoreProperties.getProperty("storePassword")
val releaseKeyAlias = System.getenv("LENOTE_KEY_ALIAS")
    ?: keystoreProperties.getProperty("keyAlias")
val releaseKeyPassword = System.getenv("LENOTE_KEY_PASSWORD")
    ?: keystoreProperties.getProperty("keyPassword")

val releaseKeystoreFile = releaseStorePath?.let { file(it) }
val hasReleaseSigning = releaseKeystoreFile != null &&
    releaseKeystoreFile.exists() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.example.lenote"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "io.github.lirielc.lenote"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // A assinatura de release SÓ é aplicada quando uma keystore de produção válida é fornecida
            // via variáveis de ambiente ou keystore.properties (nunca faz fallback para debug key).
            if (hasReleaseSigning) {
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Jetpack Ink (androidx.ink)
    implementation(libs.androidx.ink.authoring)
    implementation(libs.androidx.ink.brush)
    implementation(libs.androidx.ink.geometry)
    implementation(libs.androidx.ink.rendering)
    implementation(libs.androidx.ink.strokes)
    implementation(libs.androidx.ink.storage)

    // Google ML Kit Digital Ink Recognition (Reconhecimento de escrita à mão)
    implementation(libs.mlkit.digital.ink)

    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
