package autoloupe;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public class ImageFileDetector {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "jpg",
            "jpeg",
            "png",
            "tif",
            "tiff",
            "bmp",
            "webp"
    );

    public boolean isSupportedImage(Path path) {
        return Files.isRegularFile(path)
                && SUPPORTED_EXTENSIONS.contains(extensionOf(path));
    }

    private String extensionOf(Path path) {
        String fileName = path.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');

        if (lastDotIndex < 0 || lastDotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
    }
}