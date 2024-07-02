plugins {
    id("com.android.library")
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.devtools.ksp") version "2.0.0-1.0.22"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(libs.symbol.processing.api)
    implementation(libs.auto.service)
    implementation(libs.androidx.lifecycle.common.jvm)
    implementation(libs.androidx.appcompat)
    ksp(libs.auto.service)
    implementation(kotlin("reflect"))
}