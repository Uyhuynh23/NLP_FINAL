plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.nlp_final"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.nlp_final"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        externalNativeBuild {
            cmake {
                cppFlags += ""
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }
    }

    // Prevent compression of ONNX model files in assets
    androidResources {
        noCompress += listOf("onnx", "tflite", "bin")
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

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    // Allow legacy jni packaging to include ONNX Runtime native libs
    packagingOptions {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    // Coroutines for background work
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // Android lifecycle (for lifecycleScope if desired)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // ONNX Runtime Android - Using verified available version
    // Published under ai.onnxruntime group on Maven Central
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}