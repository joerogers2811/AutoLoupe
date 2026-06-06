package com.autoloupe.pipeline.analysis.evaluators;

import com.autoloupe.pipeline.analysis.ImageProcessingContext;
import com.autoloupe.pipeline.analysis.domain.TriageMetric;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TargetAreaFocusEvaluatorTest {

    private final TargetAreaFocusEvaluator evaluator = new TargetAreaFocusEvaluator();

    @Test
    @DisplayName("Should pass when the detected subject area contains strong high-frequency edge detail")
    void shouldPassWhenSubjectAreaIsSharp() {
        BufferedImage image = createSharpCheckerboardImage(120, 120);
        ImageProcessingContext context = new ImageProcessingContext(
                null,
                image,
                Optional.of(new Rectangle(20, 20, 80, 80))
        );

        TriageMetric metric = evaluator.evaluate(context);

        assertEquals("TARGET_FOCUS", metric.ruleName());
        assertEquals("PASS", metric.status());
        assertTrue(metric.assessment().contains("Subject focus verified sharp"));
    }

    @Test
    @DisplayName("Should reject when the detected subject area is visually soft and lacks edge detail")
    void shouldRejectWhenSubjectAreaIsSoft() {
        BufferedImage image = createSoftLowContrastImage(120, 120);
        ImageProcessingContext context = new ImageProcessingContext(
                null,
                image,
                Optional.of(new Rectangle(20, 20, 80, 80))
        );

        TriageMetric metric = evaluator.evaluate(context);

        assertEquals("TARGET_FOCUS", metric.ruleName());
        assertEquals("REJECT", metric.status());
        assertTrue(metric.assessment().contains("fails edge sharpness verification"));
    }

    @Test
    @DisplayName("Should evaluate the full frame when no detected subject area is available")
    void shouldUseFullFrameWhenSubjectAreaIsMissing() {
        BufferedImage image = createSharpCheckerboardImage(80, 80);
        ImageProcessingContext context = new ImageProcessingContext(
                null,
                image,
                Optional.empty()
        );

        TriageMetric metric = evaluator.evaluate(context);

        assertEquals("TARGET_FOCUS", metric.ruleName());
        assertEquals("PASS", metric.status());
    }

    private BufferedImage createSharpCheckerboardImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean whiteSquare = ((x / 4) + (y / 4)) % 2 == 0;
                image.setRGB(x, y, whiteSquare ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
            }
        }

        return image;
    }

    private BufferedImage createSoftLowContrastImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Color softGray = new Color(128, 128, 128);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, softGray.getRGB());
            }
        }

        return image;
    }

}