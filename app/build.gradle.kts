import java.util.Properties
import java.io.FileInputStream

// Reading API keys from an external file
val apiKeysPropertiesFile = rootProject.file("apikeys.properties")
val apiKeysProperties = Properties()
if (apiKeysPropertiesFile.exists()) {
    apiKeysProperties.load(FileInputStream(apiKeysPropertiesFile))
} else {
    println("WARNING: apikeys.properties file not found!")
}
// build.gradle.kts (Module Level - in app folder)
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.gms.google-services")
    kotlin("kapt")
}

android {
    namespace = "com.example.storyhive"
    compileSdk = 35  // Matches the supported version of the Android Gradle Plugin

    defaultConfig {
        applicationId = "com.example.storyhive"
        minSdk = 23
        targetSdk = 35 // Ensuring compatibility with target SDK version
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Adding the API key as a BuildConfig field
        buildConfigField(
            "String",
            "GOOGLE_BOOKS_API_KEY",
            "\"${apiKeysProperties.getProperty("GOOGLE_BOOKS_API_KEY", "")}\""
        )
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

    kotlinOptions {
        jvmTarget = "11"
    }


    buildFeatures {
        viewBinding = true  // Enabling View Binding
        buildConfig = true  // Enabling BuildConfig for using API keys
    }

}

dependencies {
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.foundation.android)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")

    // Defining library versions
    val navVersion = "2.7.7"
    val roomVersion = "2.6.1"
    val lifecycleVersion = "2.7.0"

    // Android Core Libraries
    implementation(libs.androidx.core.ktx.v1120)
    implementation(libs.androidx.appcompat.v161)
    implementation(libs.material.v1110)
    implementation(libs.androidx.constraintlayout.v214)
    implementation(libs.circleimageview)

    // Navigation Component
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // ViewModel & LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // WorkManager for background task scheduling
    implementation(libs.androidx.work.runtime.ktx)

    // Retrofit for network requests (HTTP)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Firebase (Authentication, Firestore, Storage)
    implementation(platform("com.google.firebase:firebase-bom:32.0.0")) // עדכון לגרסה האחרונה
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // Coroutines for asynchronous programming
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Picasso for image loading
    implementation(libs.picasso)

    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v351)

    // Glide for image loading
    implementation(libs.glide)
    annotationProcessor(libs.compiler)


    // Google Play Services Auth
    implementation(libs.play.services.auth)

    implementation("androidx.cardview:cardview:1.0.0")

    // WorkManager for handling background processes
    implementation("androidx.work:work-runtime-ktx:2.8.1")
}



