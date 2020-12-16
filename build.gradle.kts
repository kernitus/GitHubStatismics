plugins {
    kotlin("jvm") version "1.4.21"
}

repositories {
    mavenCentral()
    jcenter()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("org.kohsuke:github-api:1.117")
    implementation("com.github.kwebio:kweb-core:0.8.0")

    // This (or another SLF4J binding) is required for Kweb to log errors
    implementation(group = "org.slf4j", name = "slf4j-simple", version = "1.7.30")
}