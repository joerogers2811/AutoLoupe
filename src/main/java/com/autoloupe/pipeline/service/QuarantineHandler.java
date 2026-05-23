package com.autoloupe.pipeline.service;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;

public class QuarantineHandler {

    private final Duration pollInterval;
    private final Duration timeoutWindow;

    /**
     * Default constructor for production environments.
     */
    public QuarantineHandler() {
        this(Duration.ofMillis(250), Duration.ofSeconds(15));
    }

    /**
     * Overloaded constructor allowing injection of custom timings for testing.
     */
    public QuarantineHandler(Duration pollInterval, Duration timeoutWindow) {
        this.pollInterval = pollInterval;
        this.timeoutWindow = timeoutWindow;
    }

    /**
     * Blocks execution on the calling thread until the file size is verified stable
     * and an exclusive read/write lock check passes.
     *
     * @param filePath Target path to verify.
     * @return true if the file is stable and ready for ingest; false if the operation timed out.
     */
    public boolean waitForWriteCompletion(Path filePath) {
        Instant startTime = Instant.now();
        long lastKnownSize = -1;

        while (true) {
            if (Duration.between(startTime, Instant.now()).compareTo(timeoutWindow) > 0) {
                return false; // Crossed our safety timeout threshold
            }

            try {
                if (Files.exists(filePath)) {
                    long currentSize = Files.size(filePath);

                    // Check 1: Has the file size stopped growing?
                    if (currentSize > 0 && currentSize == lastKnownSize) {

                        // Check 2: Can we actually acquire a quick read handle on it?
                        if (isChannelReadable(filePath)) {
                            return true; // Size is stable and file handle is openable
                        }
                    }

                    lastKnownSize = currentSize;
                }
            } catch (IOException e) {
                // Filesystem metadata reads can throw if the OS is shuffling handles; drop through to retry loop
            }

            // Yield virtual thread execution smoothly back to the scheduler
            try {
                Thread.sleep(pollInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    private boolean isChannelReadable(Path filePath) {
        // Attempt to open an explicit channel with write access to test if the OS lock is cleared
        try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.WRITE)) {
            return true;
        } catch (IOException e) {
            return false; // OS still holding an exclusive lock on the transfer
        }
    }
}
