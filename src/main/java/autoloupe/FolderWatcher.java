package autoloupe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import static java.nio.file.FileSystems.getDefault;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class FolderWatcher {
    private static final Logger logger = LoggerFactory.getLogger(FolderWatcher.class);

    private final WatchConfig config;
    private final ImageFileDetector imageFileDetector;
    private final ImageAssessmentService assessmentService;

    private volatile boolean running;
    private WatchService watchService;

    public FolderWatcher(
            WatchConfig config,
            ImageFileDetector imageFileDetector,
            ImageAssessmentService assessmentService
    ) {
        this.config = config;
        this.imageFileDetector = imageFileDetector;
        this.assessmentService = assessmentService;
    }

    public void start() {
        validateWatchFolder();

        try (WatchService service = getDefault().newWatchService()) {
            watchService = service;
            config.folder().register(service, ENTRY_CREATE, ENTRY_MODIFY);

            running = true;

            logger.info("Folder watcher is running");

            while (running) {
                WatchKey key;

                try {
                    key = service.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (ClosedWatchServiceException e) {
                    break;
                }

                processWatchKey(key);

                boolean valid = key.reset();

                if (!valid) {
                    logger.warn("Watch key is no longer valid. Stopping watcher.");
                    break;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to watch folder: " + config.folder().toAbsolutePath(), e);
        } finally {
            running = false;
            watchService = null;
            logger.info("Folder watcher stopped");
        }
    }

    public void stop() {
        running = false;

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.warn("Failed to close watch service cleanly", e);
            }
        }
    }

    private void validateWatchFolder() {
        try {
            if (Files.notExists(config.folder())) {
                logger.info("Watch folder does not exist. Creating: {}", config.folder().toAbsolutePath());
                Files.createDirectories(config.folder());
            }

            if (!Files.isDirectory(config.folder())) {
                throw new IllegalArgumentException(
                        "Watch path must be a directory: " + config.folder().toAbsolutePath()
                );
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to prepare watch folder: " + config.folder().toAbsolutePath(), e);
        }
    }

    private void processWatchKey(WatchKey key) {
        for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent.Kind<?> kind = event.kind();

            if (kind != ENTRY_CREATE && kind != ENTRY_MODIFY) {
                continue;
            }

            Path changedPath = resolveChangedPath(event);

            if (!imageFileDetector.isSupportedImage(changedPath)) {
                logger.debug("Ignoring non-image file: {}", changedPath.getFileName());
                continue;
            }

            waitForFileToSettle(changedPath);
            assessmentService.assess(changedPath);
        }
    }

    private Path resolveChangedPath(WatchEvent<?> event) {
        Path relativePath = (Path) event.context();
        return config.folder().resolve(relativePath);
    }

    private void waitForFileToSettle(Path path) {
        if (config.settleDelayMillis() <= 0) {
            return;
        }

        logger.debug(
                "Waiting {} ms for file to settle: {}",
                config.settleDelayMillis(),
                path.getFileName()
        );

        try {
            Thread.sleep(config.settleDelayMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}