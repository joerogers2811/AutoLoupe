package com.autoloupe.pipeline.domain;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UnifiedImageAssetTest {

    @Test
    void testLensProfileFactories() {
        UnifiedImageAsset.LensProfile nativeLens = UnifiedImageAsset.LensProfile.nativeElectronic("50mm f/1.8");
        assertTrue(nativeLens.reportsElectronicData());
        assertEquals("50mm f/1.8", nativeLens.modelName());

        UnifiedImageAsset.LensProfile adaptedLens = UnifiedImageAsset.LensProfile.adaptedManual("Manual 35mm");
        assertFalse(adaptedLens.reportsElectronicData());
        assertEquals("Manual 35mm", adaptedLens.modelName());
    }

    @Test
    void testUnifiedImageAssetRecord() {
        Path path = Path.of("test.jpg");
        LocalDateTime now = LocalDateTime.now();
        UnifiedImageAsset.LensProfile lens = UnifiedImageAsset.LensProfile.nativeElectronic("Lens");
        UnifiedImageAsset.CameraProfile camera = new UnifiedImageAsset.CameraProfile("Make", "Model", lens);
        UnifiedImageAsset.ExposureProfile exposure = new UnifiedImageAsset.ExposureProfile(100, 1/100.0, Optional.of(2.8), Optional.of(50.0));
        UnifiedImageAsset.ImageDimensions dimensions = new UnifiedImageAsset.ImageDimensions(6000, 4000, 0);

        UnifiedImageAsset asset = new UnifiedImageAsset("id-123", path, now, camera, exposure, dimensions);

        assertEquals("id-123", asset.id());
        assertEquals(path, asset.rawFilePath());
        assertEquals(now, asset.captureTime());
        assertEquals(camera, asset.camera());
        assertEquals(exposure, asset.exposure());
        assertEquals(dimensions, asset.dimensions());
    }
}
