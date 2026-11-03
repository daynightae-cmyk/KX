plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.kx.vpn"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kx.vpn"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // TASK_00 deliberately requests no permissions. INTERNET / FOREGROUND_SERVICE /
        // VpnService entries are introduced with the engine slice, not before it.
    }

    buildTypes {
        debug {
            // Debug only. No signing material is stored in this repository.
            isMinifyEnabled = false
        }
        release {
            // Minification stays off until a reviewed ProGuard/R8 ruleset and a real
            // signing pipeline exist. Release distribution is not claimed by TASK_00.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            // Robolectric renders the real Compose tree on the JVM; it needs merged
            // Android resources to do so.
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = true
        checkReleaseBuilds = true
        sarifReport = true
        htmlReport = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
    // No jvmToolchain() pin: the build runs on whichever JDK launches Gradle (21
    // locally and in CI) and emits 17 bytecode via the settings above. Pinning a
    // toolchain version that is not installed fails configuration, as it did here.
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.activity.compose)
    testImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Robolectric reaches into jdk.internal.access.SharedSecrets to set up the shadow
// FileDescriptor. On JDK 21 that package is not exported to the unnamed module, so
// without these flags every Robolectric test dies inside beforeTest() before the code
// under test ever runs. Configured on the task type rather than inside
// `testOptions.unitTests.all { }`, whose closure does not expose jvmArgs().
tasks.withType<Test>().configureEach {
    // Robolectric derives its Maven local repository from `user.home`. On Windows a
    // home directory containing a space (this machine: C:\Users\day night) is turned
    // into a file URL and never decoded back, so Robolectric looks for
    // C:\Users\day%20night\.m2\... and dies with NoSuchFileException before any test
    // body runs. Pointing the *test JVM* at a space-free home inside the build
    // directory avoids that without changing anything for CI, where home has no space.
    // Must live outside build/: `clean` deletes build/ and Robolectric then fails
    // to recreate its download lock mid-run. This path is gitignored.
    val spaceFreeHome = File(rootDir, ".robolectric-home")
    if (System.getProperty("user.home").contains(" ")) {
        spaceFreeHome.mkdirs()
        jvmArgs("-Duser.home=${spaceFreeHome.absolutePath}")
    }

    jvmArgs(
        "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
        "-XX:+EnableDynamicAgentLoading",
    )
    testLogging {
        events("passed", "skipped", "failed")
    }
}
