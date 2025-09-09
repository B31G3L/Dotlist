plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp) // KSP statt Kapt
    alias(libs.plugins.kotlin.serialization) // Für JSON Serialization
}

android {
    namespace = "de.beigel.list"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.beigel.list"
        minSdk = 29
        targetSdk = 35
        versionCode = 2
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        // Room schema export directory
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
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
            signingConfig = signingConfigs.getByName("debug") // Temporär für Tests

            // Optimizations
            isDebuggable = false
            isJniDebuggable = false
            renderscriptOptimLevel = 3
        }

        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += listOf("release")
            isDebuggable = false
            proguardFiles("benchmark-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11

        // Enable Java 8+ API desugaring
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"

        // Compose Compiler optimizations
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
            excludes += "**/attach_hotspot_windows.dll"
            excludes += "META-INF/licenses/**"
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = true
        disable += setOf("MissingTranslation", "VectorPath")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    // Desugaring for older Android versions
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Compose BOM - wichtig für Version-Synchronisation
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // ViewModel & Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Room Database (mit KSP)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // WorkManager für Background Tasks
    implementation(libs.androidx.work.runtime.ktx)

    // Widget support (Glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // DataStore für Settings (Alternative zu SharedPreferences)
    implementation(libs.androidx.datastore.preferences)

    // Paging für große Listen
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)

    // Date/Time handling
    implementation(libs.androidx.core.ktx)

    // Performance & Memory
    implementation(libs.androidx.tracing.ktx)
    implementation(libs.androidx.metrics.performance)

    // Analytics (optional)
    implementation(libs.androidx.startup.runtime)

    // Permission handling
    implementation(libs.accompanist.permissions)

    // System UI Controller
    implementation(libs.accompanist.systemuicontroller)

    // Adaptive layouts
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)

    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.androidx.test.core)

    // Android Test dependencies
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.androidx.work.testing)

    // Debug dependencies
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.leakcanary.android)
}

// Task für Schemas-Verzeichnis
tasks.register("createSchemaDir") {
    doLast {
        File("$projectDir/schemas").mkdirs()
    }
}

// Ensure schema directory exists before KSP runs
tasks.withType<org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask> {
    dependsOn("createSchemaDir")
}

// Custom task für APK-Größenanalyse
tasks.register("analyzeApkSize") {
    doLast {
        val apkDir = File("$buildDir/outputs/apk/release")
        if (apkDir.exists()) {
            apkDir.listFiles()?.filter { it.extension == "apk" }?.forEach { apk ->
                println("APK: ${apk.name}, Größe: ${apk.length() / 1024 / 1024} MB")
            }
        }
    }
}

// Performance optimization tasks
tasks.register("optimizeForRelease") {
    group = "optimization"
    description = "Optimiert die App für Release-Builds"

    doLast {
        println("Führe Release-Optimierungen durch...")
        // Hier könnten weitere Optimierungen hinzugefügt werden
    }
}

// Code quality checks
tasks.register("codeQuality") {
    group = "verification"
    description = "Führt Code-Quality-Checks durch"

    dependsOn("lint", "test")
}

// Custom configuration für Feature-Flags
android {
    buildTypes {
        debug {
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
            buildConfigField("boolean", "ENABLE_EXPERIMENTAL_FEATURES", "true")
            buildConfigField("String", "API_BASE_URL", "\"https://api-dev.dailylist.app/\"")
        }
        release {
            buildConfigField("boolean", "ENABLE_LOGGING", "false")
            buildConfigField("boolean", "ENABLE_EXPERIMENTAL_FEATURES", "false")
            buildConfigField("String", "API_BASE_URL", "\"https://api.dailylist.app/\"")
        }
    }
}

// Flavor dimensions für verschiedene App-Varianten
android {
    flavorDimensions += "version"

    productFlavors {
        create("free") {
            dimension = "version"
            applicationIdSuffix = ".free"
            versionNameSuffix = "-free"

            buildConfigField("boolean", "IS_PREMIUM", "false")
            buildConfigField("int", "MAX_TASKS_PER_DAY", "10")
        }

        create("premium") {
            dimension = "version"
            applicationIdSuffix = ".premium"
            versionNameSuffix = "-premium"

            buildConfigField("boolean", "IS_PREMIUM", "true")
            buildConfigField("int", "MAX_TASKS_PER_DAY", "999")
        }
    }
}