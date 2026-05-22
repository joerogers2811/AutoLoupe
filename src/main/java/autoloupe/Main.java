package autoloupe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Path watchFolder = parseWatchFolder(args);

        if (watchFolder == null) {
            printUsage();
            System.exit(1);
        }

        WatchConfig config = new WatchConfig(
                watchFolder,
                1_500L
        );

        FolderWatcher watcher = new FolderWatcher(
                config,
                new ImageFileDetector(),
                new ImageAssessmentService()
        );

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown requested. Stopping AutoLoupe...");
            watcher.stop();
        }));

        logger.info("Starting AutoLoupe");
        logger.info("Watching folder: {}", config.folder().toAbsolutePath());

        watcher.start();
    }

    private static Path parseWatchFolder(String[] args) {
        if (args.length != 1) {
            return null;
        }

        return Path.of(args[0]);
    }

    private static void printUsage() {
        System.out.println("""
                AutoLoupe
                
                Usage:
                  ./gradlew run --args="<folder-to-watch>"
                
                Example:
                  ./gradlew run --args="D:\\Photos\\Incoming"
                """);
    }
}