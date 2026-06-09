package com.autoloupe.pipeline.ingest.factory;

import com.autoloupe.pipeline.ingest.factory.ImageAssetFactoryComposite;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageAssetFactoryCompositeTest {

    @Test
    void testProcess_WithPentaxMetadata() {
        ImageAssetFactoryComposite composite = new ImageAssetFactoryComposite();
        Metadata metadata = new Metadata();
        ExifIFD0Directory ifd0 = new ExifIFD0Directory();
        ifd0.setString(ExifIFD0Directory.TAG_MAKE, "PENTAX");
        metadata.addDirectory(ifd0);

        // This should use PentaxAssetFactory
        // Note: PentaxAssetFactory.build() might fail if other directories are missing, 
        // but we just want to see if it's selected and called.
        // Actually, PentaxAssetFactory.build() handles null directories.
        
        var asset = composite.process(Path.of("test.pef"), metadata);
        assertNotNull(asset);
        assertEquals("Pentax", asset.camera().make());
    }

    @Test
    void testProcess_WithFallback() {
        ImageAssetFactoryComposite composite = new ImageAssetFactoryComposite();
        Metadata metadata = new Metadata();
        // No specific maker notes or Pentax make
        
        var asset = composite.process(Path.of("test.jpg"), metadata);
        assertNotNull(asset);
        assertEquals("Generic", asset.camera().make());
    }
}
