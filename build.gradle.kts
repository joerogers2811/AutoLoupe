plugins {
    java
    application
}

group = "com.autoloupe"
version = "1.0-SNAPSHOT"

dependencies {
    // 1. The core EXIF and MakerNotes metadata extraction framework
    implementation("com.drewnoakes:metadata-extractor:2.19.0")

    // 2. Logging Facade and simple console implementation
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("org.slf4j:slf4j-simple:2.0.13")

    // JUnit 5 for testing your upcoming TDD validation assertions
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    // Enforce compilation down to Java 21 to utilize Virtual Threads natively
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    // Sets up the main entry point class execution target
    mainClass.set("com.autoloupe.engine.Main")
}

tasks.test {
    useJUnitPlatform()
}