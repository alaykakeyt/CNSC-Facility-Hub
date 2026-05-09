plugins {
    alias(libs.plugins.android.application)


//    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.cnscfacilityhubproject"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.cnscfacilityhubproject"
        minSdk = 23
        targetSdk = 36
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
}

dependencies {

    implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
    implementation("com.google.firebase:firebase-auth:22.0.0")
    implementation("com.google.firebase:firebase-firestore:24.0.0")
    implementation(libs.activity.ktx)


    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}