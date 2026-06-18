plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.apollo)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

apollo {
    service("service") {
        packageName.set("com.tabletap")
    }
}

android {
    namespace = "com.tabletap.githubcontribsapp.data"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(project(":domain"))

    // Apollo GraphQL client (brings OkHttp + coroutines transitively)
    implementation(libs.apollo.runtime)

    // SharedPreferences.edit { } extension
    implementation(libs.androidx.core.ktx)

    // Logging
    implementation(libs.timber)

    // Hilt for dependency injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
}
