plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.eduqizpro"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.eduqizpro"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true          // Nên bật khi release
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17   // Nâng lên 17 (khuyến nghị 2026)
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/versions/9/module-info.class"
            // Tránh duplicate với Woodstox / StAX
            pickFirsts += "META-INF/services/javax.xml.stream.XMLInputFactory"
            pickFirsts += "META-INF/services/javax.xml.stream.XMLOutputFactory"
            pickFirsts += "META-INF/services/javax.xml.stream.XMLEventFactory"
        }
    }
}

configurations {
    all {
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")

    // Đọc file PDF & Word
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("org.apache.poi:poi-ooxml:5.3.0")
    // Woodstox — XMLInputFactory implementation cho Apache POI trên Android
    implementation("com.fasterxml.woodstox:woodstox-core:6.7.0")
    implementation("org.codehaus.woodstox:stax2-api:4.2.2")

    // ==================== FIREBASE (2026) ====================
    implementation(platform("com.google.firebase:firebase-bom:34.12.0"))

    // Firebase AI Logic - Để BOM quản lý version
    implementation("com.google.firebase:firebase-ai")

    // Các Firebase khác
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // Khác
    implementation("com.google.code.gson:gson:2.12.0")   // Cập nhật
    implementation("io.coil-kt:coil-compose:2.7.0")       // Cập nhật
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}