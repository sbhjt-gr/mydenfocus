plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp") version "1.9.10-1.0.13"
}

android.applicationVariants.all {
    val variantName = name
    val task = project.tasks.register<Copy>("copy${variantName.capitalize()}EnvFile") {
        from(rootProject.file(".env"))
        into("$buildDir/generated/assets/src/$variantName/assets")
    }
    
    project.tasks.findByName("merge${variantName.capitalize()}Assets")?.dependsOn(task)
}

android {
    namespace = "com.gorai.myedenfocus"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gorai.myedenfocus"
        minSdk = 26
        targetSdk = 34
        versionCode = 9
        versionName = "1.0.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/*.kotlin_module"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.espresso.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0-alpha02")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")

    val destinationVersion = "1.9.52"
    implementation("io.github.raamcosta.compose-destinations:core:$destinationVersion")
    ksp("io.github.raamcosta.compose-destinations:ksp:$destinationVersion")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("com.google.code.gson:gson:2.10.1")

    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.8")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    implementation("androidx.compose.animation:animation:1.5.4")

    implementation("androidx.navigation:navigation-compose:2.7.6")

    implementation("io.github.raamcosta.compose-destinations:animations-core:1.9.52")

    implementation("androidx.compose.material:material-icons-extended:1.5.4")

    implementation("androidx.datastore:datastore-preferences:1.0.0")

    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")

    implementation("com.google.android.material:material:1.11.0")

    implementation("com.google.android.play:review-ktx:2.0.1")
    
    // Gemini API
    implementation("com.google.ai.client.generativeai:generativeai:0.2.1")
    
    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    
    // Document handling
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.documentfile:documentfile:1.0.1")
    
    // PDF Processing
    implementation("org.apache.pdfbox:pdfbox:2.0.27")
    implementation("org.apache.pdfbox:fontbox:2.0.27")
    
    // Word Document Processing (DOC and DOCX)
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("org.apache.poi:poi-scratchpad:5.2.3")
}