plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseKeys =
    listOf("NOCA_KEYSTORE_PATH", "NOCA_KEYSTORE_PASSWORD", "NOCA_KEY_ALIAS", "NOCA_KEY_PASSWORD")
val releaseSigning = releaseKeys.associateWith { providers.environmentVariable(it).orNull }

require(
    releaseSigning.values.none { it != null } || releaseSigning.values.all { !it.isNullOrBlank() }
) {
    "Set all four NOCA release signing variables, or leave all unset for an unsigned build."
}

android {
    namespace = "app.noca"
    compileSdk = 37
    ndkVersion = "28.2.13676358"
    defaultConfig {
        applicationId = "app.noca"
        minSdk = 29
        targetSdk = 37
        versionCode = 7
        versionName = "1.3.1"
        ndk { abiFilters += "arm64-v8a" }
        externalNativeBuild { cmake { arguments += "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON" } }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets.getByName("test").resources.srcDir("src/main/assets/stations")
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    signingConfigs {
        if (releaseSigning.values.all { !it.isNullOrBlank() }) {
            create("distribution") {
                storeFile = file(releaseSigning.getValue("NOCA_KEYSTORE_PATH")!!)
                storePassword = releaseSigning.getValue("NOCA_KEYSTORE_PASSWORD")
                keyAlias = releaseSigning.getValue("NOCA_KEY_ALIAS")
                keyPassword = releaseSigning.getValue("NOCA_KEY_PASSWORD")
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("distribution")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    packaging {
        jniLibs.useLegacyPackaging = false
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

val buildRust by
    tasks.registering(Exec::class) {
        val cargo = providers.environmentVariable("NOCA_CARGO").orElse("cargo")
        commandLine(
            cargo.get(),
            "build",
            "--manifest-path",
            rootProject.file("native/rust/Cargo.toml"),
            "--target",
            "aarch64-linux-android",
            "--release",
            "--locked",
        )
        inputs.dir(rootProject.file("native/rust/src"))
        inputs.file(rootProject.file("native/rust/Cargo.toml"))
        inputs.file(rootProject.file("native/rust/Cargo.lock"))
        inputs.file(rootProject.file("rust-toolchain.toml"))
        outputs.file(
            rootProject.file("native/rust/target/aarch64-linux-android/release/libnoca_core.a")
        )
    }

tasks.named("preBuild").configure { dependsOn(buildRust) }

tasks.configureEach {
    if (name.startsWith("configureCMake") || name.startsWith("buildCMake")) dependsOn(buildRust)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.12.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.compose.material3:material3:1.5.0-beta01")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.20.0")
    testImplementation("org.json:json:20260814")
    testImplementation("org.xerial:sqlite-jdbc:3.50.3.0")
    testRuntimeOnly("org.slf4j:slf4j-nop:2.0.17")
}
