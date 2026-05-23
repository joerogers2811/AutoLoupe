# Auto Loupe — Pipeline Agent Architecture Blueprint

Welcome, Agent. This document outlines the architectural patterns, strict design choices, and structural paradigms governing the **Auto Loupe** automated image triage pipeline. 

Adhere to these patterns explicitly when modifying the codebase, generating new features, or writing tests.

---

## 1. System Overview & Core Paradigms

Auto Loupe is a high-throughput, low-latency automated raw image analysis and triage system written in **Java 21**. It operates locally, parsing RAW formats (`.DNG`, `.PEF`, etc.) to extract, unify, and run automated analysis over photographic assets.

### Key Paradigms
*   **Reactive & Non-Blocking I/O:** Stage 2 ingestion handles file system updates reactively via Java `WatchService`. 
*   **Virtual Threads (Project Loom):** All file-locking calculations, heavy I/O extraction, and individual processing tracks are run on an unbinned, lightweight Virtual Thread Executor (`Executors.newVirtualThreadPerTaskExecutor()`). Do not pool virtual threads.
*   **Domain Immutability:** Data transfer and internal state tracking utilize pure Java `record` components. 

---

## 2. Global Package Namespace

The application uses a strict package hierarchy. **Do not introduce top-level packages outside this domain structure.**

```text
com.autoloupe.pipeline
├── Main.java                        # App Bootstrap & Pipeline Composition
├── domain/                          # Immutable Domain Records (State/Data)
├── service/                         # State Execution & Async Ingestion Engines
└── factory/                         # Strategy Patterns for Extraction
    └── impl/                        # Manufacturer-specific Extractions

```

---

## 3. Structural & Architectural Decisions

### A. Polymorphic Metadata Extraction (Strategy Pattern)

Rather than writing an monolithic, branching engine to parse diverse manufacturer EXIF configurations, the system implements **Runtime Strategy Polymorphism**.

1. **`ImageAssetFactory` (Interface):** Defines the strict contract for asset construction. Methods **must be instance-based**, never static. This supports dynamic substitution, mock injection, and future framework migration (e.g., Spring Beans).
2. **`ImageAssetFactoryComposite` (Registry):** Acts as the central coordinator. It maintains an internal strategy checklist, querying the `.supports(Metadata)` variant before executing a build.

### B. Defensive Null Handling Over Checked Exceptions

When dealing with external byte-extraction libraries (e.g., `metadata-extractor`), **primitive metadata getters must be avoided.**

* *Bad:* `directory.getDouble(TAG)` — throws a checked `MetadataException` if the tag is missing or unreadable, leading to nested `try-catch` blocks.
* *Good:* `directory.getDoubleObject(TAG)` — gracefully returns a nullable wrapper class object (`Double`) without throwing. Use null-checks or `Optional.ofNullable()` to maintain clean execution flows.

### C. Downstream Pipeline Decoupling

The `IngestEngine` must remain decoupled from subsequent pipeline operations (e.g., local LLM log analytics or AI inference engines). It exposes a functional interface consumer (`Consumer<UnifiedImageAsset> downstreamPipeline`) to hand off processed assets without tracking downstream implementations.

---

## 4. Current Core Implementation Blueprints

### The Immutable Domain Model (`domain/UnifiedImageAsset.java`)

```java
package com.autoloupe.pipeline.domain;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

public record UnifiedImageAsset(
    String id,
    Path rawFilePath,
    LocalDateTime captureTime,
    CameraProfile camera,
    ExposureProfile exposure,
    ImageDimensions dimensions
) {
    public record CameraProfile(String make, String model, LensProfile lens) {}
    
    public record ExposureProfile(
        double iso, 
        double shutterSpeed, 
        Optional<Double> fNumber,      
        Optional<Double> focalLength   
    ) {}
    
    public record ImageDimensions(int width, int height, int orientationDegrees) {}

    public record LensProfile(String modelName, boolean reportsElectronicData) {
        public static LensProfile nativeElectronic(String name) {
            return new LensProfile(name, true);
        }
        public static LensProfile adaptedManual(String fallbackName) {
            return new LensProfile(fallbackName, false);
        }
    }
}

```

### The Strategy Contract (`factory/ImageAssetFactory.java`)

```java
package com.autoloupe.pipeline.factory;

import com.drew.metadata.Metadata;
import com.autoloupe.pipeline.domain.UnifiedImageAsset;
import java.nio.file.Path;

public interface ImageAssetFactory {
    boolean supports(Metadata metadata);
    UnifiedImageAsset build(Path filePath, Metadata metadata);
}

```

---

## 5. Development and Testing Rules

### Test-Driven Development (TDD)

* All additions to the `factory/` or `service/` spaces must be accompanied by comprehensive tests using **JUnit 5**.
* Because `ImageAssetFactory` instances use instance methods, dependencies must be verified via clean object stubbing or standard Mockito testing. **Avoid using specialized mocking frameworks to stub static targets.**

### Stream Locking Guardrail

When reacting to directory watch events, you must assume a file write is actively occurring. Always call `quarantineUntilWriteCompletes(Path path)` before attempting to extract metadata to block processing until the OS finishes writing the raw stream.

```</Double></Double>

```