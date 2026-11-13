import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

room {
    schemaDirectory("$projectDir/schemas")
}

val coverConfig = Properties().apply {
    val config = rootProject.file("local.properties")
    if (config.exists()) config.inputStream().use { load(it) }
}
fun coverSetting(name: String) = providers.gradleProperty(name).orElse(coverConfig.getProperty(name, "")).get()

android {
    namespace = "com.uvg.cc3087.myapp"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${coverSetting("cloudinary.cloudName").replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"${coverSetting("cloudinary.uploadPreset").replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        applicationId = "com.uvg.cc3087.myapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        // Runtime initialization prevents Kotlin from inlining a stale environment across builds.
        buildConfigField("boolean", "FIREBASE_EMULATORS", "java.lang.Boolean.parseBoolean(\"false\")")
        buildConfigField("boolean", "CLOUDINARY_UPLOADS_ENABLED", "java.lang.Boolean.parseBoolean(\"true\")")
        buildConfigField("boolean", "TELEMETRY_ENABLED", "true")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "TELEMETRY_ENABLED", providers.gradleProperty("telemetryEnabled").orElse("false").get())
            val firebaseEmulators = providers.gradleProperty("firebaseEmulators").orElse("false").get().toBooleanStrict()
            buildConfigField("boolean", "FIREBASE_EMULATORS", "java.lang.Boolean.parseBoolean(\"$firebaseEmulators\")")
            // Real Cloudinary uploads require a separate opt-in when Firebase is local.
            val cloudinaryWithEmulators = coverSetting("cloudinary.allowUploadsWithEmulators")
                .ifBlank { "false" }.toBooleanStrict()
            val cloudinaryUploads = !firebaseEmulators || cloudinaryWithEmulators
            buildConfigField("boolean", "CLOUDINARY_UPLOADS_ENABLED", "java.lang.Boolean.parseBoolean(\"$cloudinaryUploads\")")
        }
        release {
            optimization {
                enable = false
            }
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
    implementation(platform(libs.firebase.bom))
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.work.runtime)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // estas dependencias preparan el estado de las pantallas para mvvm
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.kotlinx.coroutines.android)
    // room guarda los borradores y ksp genera el acceso a la base
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    // podemos probar los flujos sin depender del reloj ni de un teléfono
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    implementation(libs.coil.svg)
    implementation(libs.sqlcipher)
    implementation(libs.androidx.exifinterface)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
}
