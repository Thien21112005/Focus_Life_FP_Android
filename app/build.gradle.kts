import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { input ->
            load(input)
        }
    }
}

fun localOrGradleProperty(name: String, defaultValue: String = ""): String {
    return localProperties.getProperty(name)
        ?: providers.gradleProperty(name).orElse(defaultValue).get()
}

fun String.toBuildConfigString(): String {
    return "\"" + this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"") + "\""
}

val geminiApiKey = localOrGradleProperty("GEMINI_API_KEY")
val mapboxToken = localOrGradleProperty("MAPBOX_ACCESS_TOKEN")
val cloudinaryCloudName = localOrGradleProperty("CLOUDINARY_CLOUD_NAME")
val cloudinaryUploadPreset = localOrGradleProperty("CLOUDINARY_UPLOAD_PRESET")
val cloudinaryApiKey = localOrGradleProperty("CLOUDINARY_API_KEY")
val cloudinaryBaseFolder = localOrGradleProperty("CLOUDINARY_BASE_FOLDER", "focuslife/nutrition")

android {
    namespace = "com.hcmute.edu.vn.focus_life"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
        resValues = true
    }

    defaultConfig {
        applicationId = "com.hcmute.edu.vn.focus_life"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GEMINI_API_KEY", geminiApiKey.toBuildConfigString())

        buildConfigField("String", "MAPBOX_ACCESS_TOKEN", mapboxToken.toBuildConfigString())
        resValue("string", "mapbox_access_token", mapboxToken)

        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", cloudinaryCloudName.toBuildConfigString())
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", cloudinaryUploadPreset.toBuildConfigString())
        buildConfigField("String", "CLOUDINARY_API_KEY", cloudinaryApiKey.toBuildConfigString())
        buildConfigField("String", "CLOUDINARY_BASE_FOLDER", cloudinaryBaseFolder.toBuildConfigString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("androidx.lifecycle:lifecycle-livedata:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.3")
    implementation("androidx.navigation:navigation-fragment:2.8.0")
    implementation("androidx.navigation:navigation-ui:2.8.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.work:work-runtime:2.9.1")

    implementation(platform("com.google.firebase:firebase-bom:33.2.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.android.gms:play-services-fitness:21.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("com.mapbox.mapboxsdk:mapbox-android-sdk:9.7.1")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation("com.google.mlkit:image-labeling:17.0.9")
}