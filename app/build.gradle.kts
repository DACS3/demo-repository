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
            isMinifyEnabled = true
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
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/NOTICE*"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/git.properties"
            
            // Exclude Log4j configurations that might cause issues
            excludes += "org/apache/logging/log4j/**"
            
            pickFirsts += "META-INF/versions/9/module-info.class"
        }
    }
}

configurations {
    all {
        // Force exclusion of log4j to prevent NoClassDefFoundError on Android
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
        exclude(group = "org.apache.logging.log4j", module = "log4j-core")
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

    // PDF Support
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    
    // Apache POI - Using 4.1.2 as it's more Android-friendly
    implementation("org.apache.poi:poi-ooxml:4.1.2") {
        exclude(group = "org.apache.xmlgraphics", module = "batik-all")
        exclude(group = "xml-apis", module = "xml-apis")
    }
    implementation("javax.xml.stream:stax-api:1.0-2")
    implementation("com.fasterxml.woodstox:woodstox-core:6.5.1")

    // Firebase (2026)
    implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
    implementation("com.google.firebase:firebase-ai")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // Miscellaneous
    implementation("com.google.code.gson:gson:2.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
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