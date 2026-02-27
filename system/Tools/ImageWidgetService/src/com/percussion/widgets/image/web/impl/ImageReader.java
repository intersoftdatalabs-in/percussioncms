/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.percussion.widgets.image.web.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Utility class for reading images using standard Java ImageIO.
 * <p>
 * With TwelveMonkeys ImageIO plugins on the classpath, this handles
 * CMYK JPEGs, Adobe markers, YCCK color spaces, TIFF, and other formats
 * automatically without requiring Apache Commons Imaging.
 * </p>
 *
 * @author robertjohansen
 * @deprecated Use {@link javax.imageio.ImageIO} directly with TwelveMonkeys plugins.
 */
@Deprecated
public final class ImageReader {
   private static final Logger LOG = LogManager.getLogger(ImageReader.class);

   public static final class ImageReaderException extends Exception {

      private static final long serialVersionUID = 1L;

      /**
       * Empty Constructor
       */
      protected ImageReaderException() {
         super();
      }

      /**
       * Constructor with message and cause.
       *
       * @param message the detail message
       * @param cause the underlying cause
       */
      protected ImageReaderException(String message, Throwable cause) {
         super(message, cause);
      }
   }

   /**
    * Apply private construction: This is a static utility class
    */
   private ImageReader() {
   }

   /**
    * Reads an image byte array into a buffered image using Java ImageIO
    * with TwelveMonkeys plugins for extended format support (CMYK JPEG,
    * TIFF, etc.).
    *
    * @param imageByteArray A byte array containing image data
    * @return A BufferedImage, parsed from the byte array, or null if no
    *         registered ImageReader can decode the data.
    * @throws ImageReaderException if an error occurs reading the image
    */
   public static BufferedImage read(final byte[] imageByteArray) throws ImageReaderException {
      if (imageByteArray == null || imageByteArray.length == 0) {
         throw new ImageReaderException("Image byte array is null or empty", null);
      }
      try {
         BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageByteArray));
         if (image == null) {
            LOG.warn("ImageIO.read returned null - no registered reader could decode the image data");
         }
         return image;
      } catch (IOException e) {
         LOG.error("Unable to read image source: {}", e.getMessage());
         LOG.debug("Image read failure details", e);
         throw new ImageReaderException("Failed to read image", e);
      }
   }
}
