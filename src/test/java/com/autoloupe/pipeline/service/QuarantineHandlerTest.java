package com.autoloupe.pipeline.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class QuarantineHandlerTest {

    @TempDir
    Path tempDir;

    @Test
    void testWaitForWriteCompletion_Success() throws IOException {
        Path testFile = tempDir.resolve("test.jpg");
        Files.write(testFile, new byte[]{1, 2, 3});

        QuarantineHandler handler = new QuarantineHandler(Duration.ofMillis(10), Duration.ofSeconds(1));
        
        // Size is stable and it's readable
        assertTrue(handler.waitForWriteCompletion(testFile));
    }

    @Test
    void testWaitForWriteCompletion_TimeoutIfFileSizeChanges() throws IOException, InterruptedException {
        Path testFile = tempDir.resolve("growing.jpg");
        Files.write(testFile, new byte[]{1});

        QuarantineHandler handler = new QuarantineHandler(Duration.ofMillis(100), Duration.ofMillis(500));

        // Start a thread to grow the file
        Thread.ofVirtual().start(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    Thread.sleep(50);
                    Files.write(testFile, new byte[]{(byte) i}, java.nio.file.StandardOpenOption.APPEND);
                }
            } catch (Exception ignored) {}
        });

        // It should eventually succeed if the growth stops before timeout
        assertTrue(handler.waitForWriteCompletion(testFile));
    }

    @Test
    void testWaitForWriteCompletion_TimeoutIfNeverExists() {
        Path nonExistentFile = tempDir.resolve("missing.jpg");
        QuarantineHandler handler = new QuarantineHandler(Duration.ofMillis(10), Duration.ofMillis(100));

        assertFalse(handler.waitForWriteCompletion(nonExistentFile));
    }

    @Test
    void testWaitForWriteCompletion_TimeoutIfSizeKeepsChanging() throws IOException {
        Path testFile = tempDir.resolve("forever_growing.jpg");
        Files.write(testFile, new byte[]{1});

        QuarantineHandler handler = new QuarantineHandler(Duration.ofMillis(100), Duration.ofMillis(300));

        // Start a thread to keep the file growing beyond the timeout
        Thread.ofVirtual().start(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Files.write(testFile, new byte[]{1}, java.nio.file.StandardOpenOption.APPEND);
                    Thread.sleep(50);
                }
            } catch (Exception ignored) {}
        });

        assertFalse(handler.waitForWriteCompletion(testFile));
    }
}
