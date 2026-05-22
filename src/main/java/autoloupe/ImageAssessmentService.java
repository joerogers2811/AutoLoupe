package autoloupe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class ImageAssessmentService {
    private static final Logger logger = LoggerFactory.getLogger(ImageAssessmentService.class);

    public void assess(Path path) {
        logger.info("Assessing image: {}", path.getFileName());

        BufferedImage image;

        try {
            image = ImageIO.read(path.toFile());
        } catch (IOException e) {
            logger.warn("Could not read image: {}", path.getFileName(), e);
            return;
        }

        if (image == null) {
            logger.warn("Could not decode image: {}", path.getFileName());
            return;
        }

        ImageAssessmentResult result = new ImageAssessmentResult(
                path.getFileName().toString(),
                image.getWidth(),
                image.getHeight(),
                image.getWidth() * image.getHeight() / 1_000_000.0
        );

        logger.info(
                "Assessment complete: file={}, dimensions={}x{}, megapixels={}",
                result.fileName(),
                result.width(),
                result.height(),
                String.format("%.2f", result.megapixels())
        );
    }
}