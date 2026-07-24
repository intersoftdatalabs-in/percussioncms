/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.widgets.image.services.impl;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.widgets.image.data.ImageData;
import com.percussion.widgets.image.data.MimeUtils;
import com.percussion.widgets.image.web.impl.ImageReader;
import com.percussion.widgets.image.web.impl.ImageReader.ImageReaderException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Image format coverage for {@link ImageReader} via ImageIO + TwelveMonkeys.
 */
public class ImageReaderTest {

    private static final Logger log = LogManager.getLogger(ImageReaderTest.class);

    private static final String RESOURCE_PATH = "com/percussion/widgets/image/services/impl/resources/";

    @BeforeAll
    public static void runBeforeClass() {
        ImageIO.scanForPlugins();
        System.out.println("=============================Testing Image Reader");
        String[] formats = ImageIO.getReaderFormatNames();
        Arrays.sort(formats);
        for (String s : formats) {
            System.out.println("Format Supported: " + s);
        }
    }

    @Test
    public void testPng() throws IOException {
        testImage("png_test.png");
    }

    @Test
    public void testAdobeIllistrator() throws IOException {
        testImage("Adobe_Illistrator_test.jpg");
    }

    @Test
    public void testAdobePhotoshop() throws IOException {
        testImage("Adobe_Photoshop_test.jpg");
    }

    @Test
    public void testAdobePhotoshopTwo() throws IOException {
        testImage("Adobe_Photoshop_test_2.jpg");
    }

    @Test
    public void testCmykJpeg() throws IOException {
        testImage("cmyk_jpg_test.jpg");
    }

    @Test
    public void testTif() throws IOException {
        testImage("tif_test.tif");
    }

    @Test
    @Disabled("SVG requires Batik transcoding beyond standard ImageIO")
    public void testSVG() throws IOException {
        testImage("anenome.svg");
    }

    @Test
    @Disabled("JPEG2000 requires a dedicated ImageIO plugin not currently on classpath")
    public void testJPEG2000() throws IOException {
        testImage("relax.jp2");
    }

    @Test
    public void testWebp() throws IOException {
        // TwelveMonkeys provides a WebP reader; resize/write is not required for ImageReader coverage.
        testImage("1.webp", false);
    }

    @Test
    public void testLineTif() throws IOException {
        testImage("small_tif_test.tif");
    }

    @Test
    public void testAdobeCmykEmbedded() throws IOException {
        testImage("embedded_jpg_test.jpg");
    }

    @Test
    public void testAdobeCmykNotEmbedded() throws IOException {
        testImage("not_embedded_jpg_test.jpg");
    }

    @Test
    public void testSmallGif() throws IOException {
        // This GIF in particular had an issue with site sucker.
        testImage("small_gif.gif");
    }

    @Test
    public void testReadRejectsNullAndEmpty() {
        assertThrows(NullPointerException.class, () -> ImageReader.read(null));
        assertThrows(ImageReaderException.class, () -> ImageReader.read(new byte[0]));
        assertFalse(ImageReader.isValidImageData(null));
        assertFalse(ImageReader.isValidImageData(new byte[0]));
        assertFalse(ImageReader.isValidImageData(new byte[] {0x00, 0x01, 0x02}));
    }

    private void testImage(String resourceFileName) throws IOException {
        testImage(resourceFileName, true);
    }

    private void testImage(String resourceFileName, boolean resize) throws IOException {
        String resourcePath = RESOURCE_PATH + resourceFileName;
        byte[] imageBytes = readBytesForImageResource(resourcePath);

        BufferedImage bufferedImage = readImage(imageBytes);
        assertNotNull(bufferedImage, "Buffered image is null after ImageReader.read: " + resourcePath);
        assertTrue(ImageReader.isValidImageData(imageBytes), "isValidImageData should pass for " + resourcePath);
        assertTrue(ImageReader.estimateMemoryUsage(imageBytes) > 0,
                "estimateMemoryUsage should be positive for " + resourcePath);

        logImageInfo(bufferedImage);
        if (resize) {
            testResize(resourcePath);
        }
    }

    private void testResize(String resourcePath) {
        ImageResizeManagerImpl resizeManager = new ImageResizeManagerImpl();
        try {
            String ext = FilenameUtils.getExtension(resourcePath);
            resizeManager.setExtension(ext);
            resizeManager.setContentType(MimeUtils.getMimeTypeByExtension(ext));

            ImageData resizedImage = resizeManager.generateImage(
                    getClass().getClassLoader().getResourceAsStream(resourcePath));

            Assertions.assertTrue(
                    validateImageData(resizedImage),
                    "Invalid ImageData for generateImage(InputStream input)");
        } catch (Exception e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            Assertions.fail("Caught exception on resize: " + e.getMessage());
        }
    }

    private boolean validateImageData(ImageData imageData) {
        if (imageData == null) {
            return false;
        }
        if (imageData.getBinary() == null || imageData.getBinary().length == 0) {
            Assertions.fail("Invalid ImageData returned after resize");
            return false;
        }
        return true;
    }

    private BufferedImage readImage(byte[] imageBytes) {
        try {
            long startTime = System.currentTimeMillis();
            BufferedImage bufferedImage = ImageReader.read(imageBytes);
            System.out.print("Image Read Time: "
                    + (System.currentTimeMillis() - startTime) + " ms\n");
            return bufferedImage;
        } catch (ImageReaderException imageReaderException) {
            log.error(imageReaderException.getMessage());
            log.debug(imageReaderException.getMessage(), imageReaderException);
            Assertions.fail("Caught image reader exception: " + imageReaderException.getMessage());
            return null;
        }
    }

    private void logImageInfo(BufferedImage image) {
        System.out.print("=============================Testing image"
                + " | Width: " + image.getWidth()
                + " | Height: " + image.getHeight()
                + " | Type: " + image.getType() + "\n");
    }

    private byte[] readBytesForImageResource(String resourceLocation) {
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream(resourceLocation)) {
            if (inputStream == null) {
                fail("unable to find image test resource: " + resourceLocation);
            }
            return IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            fail("unable to read image as test resource: " + resourceLocation);
            return new byte[0];
        }
    }
}
