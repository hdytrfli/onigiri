plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.androidx.room3) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.aboutlibraries) apply false
    alias(libs.plugins.aboutlibraries.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktfmt) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        // Readium's PDF adapter depends on AndroidPdfViewer, which is published on JitPack.
        maven("https://jitpack.io") { content { includeGroup("com.github.marain87") } }
    }
}

subprojects {
    plugins.apply("com.ncorti.ktfmt.gradle")

    configure<com.ncorti.ktfmt.gradle.KtfmtExtension> {
        kotlinLangStyle()
    }
}

tasks.register<Delete>("clean") {
    description = "Deletes all build artifacts."
    delete(rootProject.layout.buildDirectory)
}
