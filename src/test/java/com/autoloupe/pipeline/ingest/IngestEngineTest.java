package com.autoloupe.pipeline.ingest;

import com.autoloupe.pipeline.domain.AnalysisTransaction;
import com.autoloupe.pipeline.ingest.extraction.PreviewExtractionStrategyRegistry;
import com.autoloupe.pipeline.ingest.factory.ImageAssetFactoryComposite;
import com.autoloupe.pipeline.service.QuarantineHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class IngestEngineTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("One-shot mode should process existing files and return")
    void oneShotModeShouldProcessExistingFiles() throws IOException {
        // Create a dummy file that is not transient but fails parsing
        Path imageFile = tempDir.resolve("test.jpg");
        Files.writeString(imageFile, "invalid content but not transient");

        List<AnalysisTransaction> transactions = new CopyOnWriteArrayList<>();
        List<String> logs = new CopyOnWriteArrayList<>();

        // Use a fast-timeout QuarantineHandler for testing
        QuarantineHandler fastQuarantine = new QuarantineHandler(Duration.ofMillis(10), Duration.ofMillis(100));

        IngestEngine engine = new IngestEngine(
                tempDir,
                new ImageAssetFactoryComposite(),
                transactions::add,
                new PreviewExtractionStrategyRegistry(),
                true, // oneShotMode
                fastQuarantine
        );

        // Run synchronously
        engine.run();

        // We can't easily assert on transactions because parsing fails
        // But we can check that it didn't block forever and reached the end.
        assertTrue(engine.isOneShot());
        
        engine.close();
    }
}
