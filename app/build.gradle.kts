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
    compileSdk = 35  // תואם לגרסה הנתמכת של Android Gradle Plugin

    defaultConfig {
        applicationId = "com.example.storyhive"
        minSdk = 23
        targetSdk = 35 // שמרנו על תאימות
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        viewBinding = true
    }


}

dependencies {
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.recyclerview)

    // הגדרת גרסאות
    val navVersion = "2.7.7"
    val roomVersion = "2.6.1"
    val lifecycleVersion = "2.7.0"

    // Android Core
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

    // Retrofit for network calls
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.0.0")) // עדכון לגרסה האחרונה
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Picasso for image loading
    implementation(libs.picasso)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v351)

    // Glide
    implementation(libs.glide)
    annotationProcessor(libs.compiler)


    // Google Play Services Auth
    implementation(libs.play.services.auth)

    implementation("androidx.cardview:cardview:1.0.0")

}

// הפעלת Google Services

