plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val releaseStorePath = providers.environmentVariable("LENOTE_KEYSTORE_PATH")
val releaseStorePassword = providers.environmentVariable("LENOTE_KEYSTORE_PASSWORD")
val releaseKeyAlias = providers.environmentVariable("LENOTE_KEY_ALIAS")
val releaseKeyPassword = providers.environmentVariable("LENOTE_KEY_PASSWORD")

val releaseKeystoreFile = releaseStorePath.orNull?.takeIf { it.isNotBlank() }?.let { file(it) }
val hasValidReleaseKeystore = releaseKeystoreFile != null &&
    releaseKeystoreFile.exists() &&
    releaseKeystoreFile.isFile &&
    !releaseStorePassword.orNull.isNullOrBlank() &&
    !releaseKeyAlias.orNull.isNullOrBlank() &&
    !releaseKeyPassword.orNull.isNullOrBlank()

abstract class VerifyReleaseSigningTask : DefaultTask() {
    @get:Input
    @get:Optional
    abstract val storePath: Property<String>

    @get:Input
    @get:Optional
    abstract val storePassword: Property<String>

    @get:Input
    @get:Optional
    abstract val keyAlias: Property<String>

    @get:Input
    @get:Optional
    abstract val keyPassword: Property<String>

    @TaskAction
    fun verify() {
        val path = storePath.orNull
        val sPass = storePassword.orNull
        val alias = keyAlias.orNull
        val kPass = keyPassword.orNull

        val missingVars = buildList {
            if (path.isNullOrBlank()) add("LENOTE_KEYSTORE_PATH")
            if (sPass.isNullOrBlank()) add("LENOTE_KEYSTORE_PASSWORD")
            if (alias.isNullOrBlank()) add("LENOTE_KEY_ALIAS")
            if (kPass.isNullOrBlank()) add("LENOTE_KEY_PASSWORD")
        }

        val ksFile = path?.takeIf { it.isNotBlank() }?.let { File(it) }
        val fileExistsAndValid = ksFile != null && ksFile.exists() && ksFile.isFile

        if (missingVars.isNotEmpty() || !fileExistsAndValid) {
            val fileProblem = if (!path.isNullOrBlank() && !fileExistsAndValid) {
                "\nArquivo de keystore não encontrado ou inválido em: $path"
            } else {
                ""
            }
            throw GradleException(
                "Build Release interrompido: assinatura de produção não configurada.\n" +
                    "Variáveis de ambiente obrigatórias ausentes: ${if (missingVars.isEmpty()) "nenhuma" else missingVars.joinToString(", ")}$fileProblem\n" +
                    "Defina exclusivamente as variáveis de ambiente LENOTE_KEYSTORE_PATH, LENOTE_KEYSTORE_PASSWORD, LENOTE_KEY_ALIAS e LENOTE_KEY_PASSWORD antes de executar tarefas da variante Release."
            )
        }
    }
}

val verifyReleaseSigning = tasks.register<VerifyReleaseSigningTask>("verifyReleaseSigning") {
    group = "verification"
    description = "Verifica se as variáveis de ambiente de assinatura de produção estão configuradas antes de gerar artefatos Release."
    storePath.set(releaseStorePath)
    storePassword.set(releaseStorePassword)
    keyAlias.set(releaseKeyAlias)
    keyPassword.set(releaseKeyPassword)
}

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
        versionCode = 4
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasValidReleaseKeystore) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseStorePassword.orNull
                keyAlias = releaseKeyAlias.orNull
                keyPassword = releaseKeyPassword.orNull
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Sem fallback para debug key: a assinatura de produção exige as 4 variáveis de ambiente.
            if (hasValidReleaseKeystore) {
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

// Vincula a tarefa verifyReleaseSigning a todas as tarefas da variante Release (preReleaseBuild,
// packageRelease, bundleRelease, assembleRelease, signReleaseBundle, validateSigningRelease, etc.).
// Totalmente compatível com Gradle 9.4.1, AGP 9.2.1 e Configuration Cache (--configuration-cache).
tasks.configureEach {
    if (name != "verifyReleaseSigning" &&
        (name.contains("Release", ignoreCase = false) || name == "assemble" || name == "bundle")
    ) {
        dependsOn(verifyReleaseSigning)
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
