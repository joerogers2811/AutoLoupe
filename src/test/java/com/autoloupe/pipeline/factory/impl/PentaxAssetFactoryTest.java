package com.autoloupe.pipeline.factory.impl;

import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.makernotes.PentaxMakernoteDirectory;
import com.drew.lang.Rational;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PentaxAssetFactoryTest {

    @Test
    void testSupports() {
        PentaxAssetFactory factory = new PentaxAssetFactory();
        
        Metadata metadata = new Metadata();
        ExifIFD0Directory ifd0 = new ExifIFD0Directory();
        ifd0.setString(ExifIFD0Directory.TAG_MAKE, "PENTAX Corporation");
        metadata.addDirectory(ifd0);
        
        assertTrue(factory.supports(metadata));
        
        Metadata otherMetadata = new Metadata();
        ExifIFD0Directory otherIfd0 = new ExifIFD0Directory();
        otherIfd0.setString(ExifIFD0Directory.TAG_MAKE, "Canon");
        otherMetadata.addDirectory(otherIfd0);
        
        assertFalse(factory.supports(otherMetadata));
    }

    @Test
    void testBuild_FullMetadata() {
        PentaxAssetFactory factory = new PentaxAssetFactory();
        Metadata metadata = new Metadata();
        
        ExifIFD0Directory ifd0 = new ExifIFD0Directory();
        ifd0.setString(ExifIFD0Directory.TAG_MODEL, "PENTAX K-1 Mark II");
        metadata.addDirectory(ifd0);
        
        ExifSubIFDDirectory subIfd = new ExifSubIFDDirectory();
        subIfd.setInt(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT, 100);
        subIfd.setRational(ExifSubIFDDirectory.TAG_EXPOSURE_TIME, new Rational(1, 200));
        subIfd.setDouble(ExifSubIFDDirectory.TAG_FNUMBER, 2.8);
        subIfd.setDouble(ExifSubIFDDirectory.TAG_FOCAL_LENGTH, 50.0);
        subIfd.setString(ExifSubIFDDirectory.TAG_LENS_MODEL, "HD PENTAX-D FA 50mm F1.8 SDM WR");
        metadata.addDirectory(subIfd);
        
        var asset = factory.build(Path.of("K1_test.DNG"), metadata);
        
        assertEquals("Pentax", asset.camera().make());
        assertEquals("PENTAX K-1 Mark II", asset.camera().model());
        assertEquals(100.0, asset.exposure().iso());
        assertEquals(1/200.0, asset.exposure().shutterSpeed());
        assertEquals(2.8, asset.exposure().fNumber().get());
        assertEquals(50.0, asset.exposure().focalLength().get());
        assertEquals("HD PENTAX-D FA 50mm F1.8 SDM WR", asset.camera().lens().modelName());
        assertTrue(asset.camera().lens().reportsElectronicData());
    }

    @Test
    void testBuild_ManualLensWithSR() {
        PentaxAssetFactory factory = new PentaxAssetFactory();
        Metadata metadata = new Metadata();
        
        ExifIFD0Directory ifd0 = new ExifIFD0Directory();
        ifd0.setString(ExifIFD0Directory.TAG_MODEL, "PENTAX K-3");
        metadata.addDirectory(ifd0);
        
        // No lens model in subIfd, but SR focal length in maker notes
        PentaxMakernoteDirectory pentaxNotes = new PentaxMakernoteDirectory();
        pentaxNotes.setDouble(0x001D, 28.0); // TAG_SR_FOCAL_LENGTH
        metadata.addDirectory(pentaxNotes);
        
        var asset = factory.build(Path.of("K3_manual.DNG"), metadata);
        
        assertEquals("Adapted Glass (28mm SR)", asset.camera().lens().modelName());
        assertFalse(asset.camera().lens().reportsElectronicData());
        assertEquals(28.0, asset.exposure().focalLength().get());
    }
}
