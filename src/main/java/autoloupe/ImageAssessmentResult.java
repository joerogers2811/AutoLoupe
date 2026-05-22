package autoloupe;

public record ImageAssessmentResult(
        String fileName,
        int width,
        int height,
        double megapixels
) {
}