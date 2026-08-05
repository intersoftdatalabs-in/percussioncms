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

package com.percussion.thumbnail;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Utility class for resizing on-disk thumbnail images using the standard Java ImageIO APIs. */
public class PSThumbnailImageUtils {

  /**
   * Default constructor; provided so the implicit default constructor has explicit Javadoc and
   * doclint does not warn about its use.
   */
  public PSThumbnailImageUtils() {
    // utility class - no instance state
  }

  private static final Logger log = LogManager.getLogger(PSThumbnailImageUtils.class);

  /**
   * Resizes the on-disk image at the supplied path to the standard thumbnail dimensions and
   * re-encodes it as a JPEG, waiting for the source file to become available and writable first.
   *
   * @param thumbnailFilePath the on-disk path of the thumbnail image, may not be <code>null</code>.
   * @throws InterruptedException if the wait for the source file to become available is
   *     interrupted.
   */
  public static void resizeThumbnail(String thumbnailFilePath) throws InterruptedException {

    File inImageFile = new File(thumbnailFilePath);

    int i = 0;
    while ((!inImageFile.exists() || !inImageFile.canWrite()) && i < 120) {
      Thread.sleep(500);
      i++;
    }

    if (inImageFile.exists() && !inImageFile.canWrite()) {
      File outImageFile = new File(thumbnailFilePath);

      try (FileInputStream inputStream = new FileInputStream(inImageFile)) {
        try (FileOutputStream output = new FileOutputStream(outImageFile)) {

          BufferedImage sourceImage = ImageIO.read(inputStream);
          Image thumbnail = sourceImage.getScaledInstance(290, 207, Image.SCALE_SMOOTH);
          BufferedImage bufferedThumbnail =
              new BufferedImage(
                  thumbnail.getWidth(null), thumbnail.getHeight(null), BufferedImage.TYPE_INT_RGB);
          bufferedThumbnail.getGraphics().drawImage(thumbnail, 0, 0, null);

          ImageWriter imageWriter = ImageIO.getImageWritersByFormatName("jpeg").next();

          float quality = 1.0f;
          JPEGImageWriteParam jpegParams = (JPEGImageWriteParam) imageWriter.getDefaultWriteParam();
          jpegParams.setCompressionMode(JPEGImageWriteParam.MODE_EXPLICIT);
          jpegParams.setCompressionQuality(quality);

          imageWriter.setOutput(output);

          IIOImage outimage = new IIOImage(bufferedThumbnail, null, null);
          imageWriter.write(null, outimage, jpegParams);
          imageWriter.dispose();
        }
      } catch (Exception e) {
        // FB: DMI_INVOKING_TOSTRING_ON_ARRAY NC 1-16-16
        log.debug("Failed to resize thumbnail at: " + thumbnailFilePath, e);
      }
    }
  }
}
