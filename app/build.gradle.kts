import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jlleitschuh.gradle.ktlint")
}

fun optionalProperties(file: File) = Properties().apply {
    if (file.exists()) file.inputStream().use { load(it) }
}

val releaseSigning = optionalProperties(rootProject.file("keystore.properties"))

fun signingValue(environmentName: String, propertyName: String): String? =
    providers.environmentVariable(environmentName).orNull ?: releaseSigning.getProperty(propertyName)

val signingStoreFile = signingValue("MEMORY_KEYSTORE_PATH", "storeFile")
val signingStorePassword = signingValue("MEMORY_KEYSTORE_PASSWORD", "storePassword")
val signingKeyAlias = signingValue("MEMORY_KEY_ALIAS", "keyAlias")
val signingKeyPassword = signingValue("MEMORY_KEY_PASSWORD", "keyPassword")
val signingValues = listOf(signingStoreFile, signingStorePassword, signingKeyAlias, signingKeyPassword)
val hasReleaseSigning = signingValues.all { !it.isNullOrBlank() }

require(hasReleaseSigning || signingValues.all { it == null }) {
    "Incomplete release signing configuration. See RELEASING.md."
}

android {
    namespace = "dev.collinsthomas.memhogs"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.collinsthomas.memhogs.light"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(requireNotNull(signingStoreFile))
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    testImplementation("junit:junit:4.13.2")
}
