import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// ---------------------------------------------------------------------------
// نسخه‌ی اپ: version.properties تنها منبع حقیقت است
// ---------------------------------------------------------------------------
val versionProps = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}
val appVersionName: String = versionProps.getProperty("versionName", "1.0.0")
val appVersionCode: Int = versionProps.getProperty("versionCode", "1").trim().toInt()

android {
    namespace = "ir.marbledu.datausage"
    compileSdk = libs.versions.compileSdk.get().toInt()
    // برخی کتابخانه‌های جدید Compose به پلتفرم 37.x نیاز دارند
    compileSdkMinor = libs.versions.compileSdkMinor.get().toInt()

    defaultConfig {
        applicationId = "ir.marbledu.datausage"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
        resourceConfigurations += listOf("fa", "en")
    }

    // -----------------------------------------------------------------------
    // امضای یکسان برای همه‌ی نسخه‌ها (debug + release)
    // کلید در پوشه‌ی keystore/ ریپو نگه‌داری می‌شود تا هر بیلد CI و هر
    // توسعه‌دهنده دقیقاً همین امضا را تولید کند.
    // -----------------------------------------------------------------------
    val keystoreFile = rootProject.file("keystore/marbledu-release.jks")
    signingConfigs {
        if (keystoreFile.exists()) {
            create("marbledu") {
                storeFile = keystoreFile
                storePassword = System.getenv("MARBLEDU_STORE_PASSWORD") ?: "MarbleDU2026Release"
                keyAlias = System.getenv("MARBLEDU_KEY_ALIAS") ?: "marbledu"
                keyPassword = System.getenv("MARBLEDU_KEY_PASSWORD") ?: "MarbleDU2026Release"
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            signingConfig = signingConfigs.findByName("marbledu")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("marbledu")
        }
    }

    // APK جداگانه برای هر معماری + یک APK یونیورسال
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/DEPENDENCIES")
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
