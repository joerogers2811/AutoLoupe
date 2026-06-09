plugins {
    java
    application
    id("com.gradleup.shadow") version "8.3.5"
}

group = "com.autoloupe"
version = "1.0-SNAPSHOT"

dependencies {
    // 1. The core EXIF and MakerNotes metadata extraction framework
    implementation("com.drewnoakes:metadata-extractor:2.19.0")
    implementation("com.microsoft.onnxruntime:onnxruntime:1.18.0")

    // 2. Logging Facade and simple console implementation
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("org.slf4j:slf4j-simple:2.0.13")

    // JUnit 5 for testing your upcoming TDD validation assertions
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // The missing Mockito Core engine and JUnit 5 Extension binding
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
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

tasks.jar {
    manifest {
        attributes("Main-Class" to "com.autoloupe.pipeline.AutoloupeApplication")
    }
}

tasks.shadowJar {
    archiveBaseName.set("autoloupe-pipeline")
    archiveClassifier.set("all")
    archiveVersion.set("1.0-SNAPSHOT")

    // This explicitly guarantees your output filename is:
    // autoloupe-pipeline-1.0-SNAPSHOT-all.jar
}