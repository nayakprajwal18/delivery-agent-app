import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

// ── Read Supabase credentials from local.properties ────────────────────────
val localProps = Properties().also { props ->
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) props.load(localPropsFile.inputStream())
}

android {
    namespace   = "com.yourteam.deliveryagent"
    compileSdk  = 36

    defaultConfig {
        applicationId   = "com.yourteam.deliveryagent"
        minSdk          = 26          // required by supabase-kt without desugaring
        targetSdk       = 36
        versionCode     = 1
        versionName     = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Expose Supabase credentials to code via BuildConfig
        buildConfigField(
            "String", "SUPABASE_URL",
            "\"${localProps.getProperty("SUPABASE_URL", "")}\""
        )
        buildConfigField(
            "String", "SUPABASE_ANON_KEY",
            "\"${localProps.getProperty("SUPABASE_ANON_KEY", "")}\""
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose      = true
        buildConfig  = true   // needed for BuildConfig.SUPABASE_URL etc.
    }
}

dependencies {
    // ── AndroidX core ────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // ── Jetpack Compose (BOM pins all compose library versions) ─────────
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // ── Supabase (BOM pins all supabase library versions) ────────────────
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)
    implementation(libs.ktor.client.android)    // Ktor HTTP engine for Android

    // ── Firebase (BOM pins all firebase library versions) ────────────────
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)     // FCM — no -ktx suffix from BoM v34+

    // ── Kotlin extras ────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}
