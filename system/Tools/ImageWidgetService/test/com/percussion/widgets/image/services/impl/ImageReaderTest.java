/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

/**
 * 
 */
package com.percussion.widgets.image.services.impl;

import com.percussion.error.PSExceptionUtils;
import com.percussion.widgets.image.data.ImageData;
import com.percussion.widgets.image.data.MimeUtils;
import com.percussion.widgets.image.web.impl.ImageReader;
import com.percussion.widgets.image.web.impl.ImageReader.ImageReaderException;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.shaded.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.fail;

/**
 * @author matthew.ernewein
 * 
 */
public class ImageReaderTest
{

   private static final Logger log = LogManager.getLogger(ImageReaderTest.class);

   @BeforeClass
   public static void runBeforeClass()
   {

       ImageIO.scanForPlugins();
       System.out.println("=============================Testing Image Reader");
       String[] formats = ImageIO.getReaderFormatNames();
       Arrays.sort(formats);
       for(String s : formats){
          System.out.println("Format Supported: " + s);
       }
   }

   @Test
   public void testPng() throws IOException
   {
      testImage("png_test.png"); 
   }
   
   @Test
   public void testAdobeIllistrator()throws IOException
   {
      testImage("Adobe_Illistrator_test.jpg");
   }
   
   @Test
   public void testAdobePhotoshop()throws IOException
   {
      testImage("Adobe_Photoshop_test.jpg"); 
   }
   
   @Test
   public void testAdobePhotoshopTwo()throws IOException
   {
      testImage("Adobe_Photoshop_test_2.jpg"); 
   }

   @Test
   public void testCmykJpeg()throws IOException
   {
      testImage("cmyk_jpg_test.jpg");
   }

   @Test
   public void testTif()throws IOException
   {
      testImage("tif_test.tif");
   }

   @Test
   @Ignore //TODO: Should be passing once svg support is working
   public void testSVG() throws IOException{
      testImage("anenome.svg");
   }

   @Test
   @Ignore //TODO: Should be passing once jpeg2000 support is working
   public void testJPEG2000() throws IOException{
      testImage("relax.jp2");
   }

   @Test
   @Ignore //TODO: Should be passing once webp support is working
   public void testWebp() throws IOException{
      testImage("1.webp");
   }

   @Test
   public void testLineTif() throws IOException
   {
      testImage("small_tif_test.tif");
   }

   @Test
   public void testAdobeCmykEmbedded() throws IOException
   {
      testImage("embedded_jpg_test.jpg");
   }

   @Test
   public void testAdobeCmykNotEmbedded()throws IOException
   {
      testImage("not_embedded_jpg_test.jpg");
   }

   @Test
   @Ignore
   public void testSmallGif() throws IOException
   {
      //This GIF in particular had an issue with site sucker.
      testImage("small_gif.gif");
   }

   private void testImage(String resourceFileName) throws IOException
   {
      String path = "com/percussion/widgets/image/services/impl/resources/";
      String resourcePath = path + resourceFileName;

      BufferedImage bufferedImage = ImageIO.read(getClass().getClassLoader()
              .getResourceAsStream(resourcePath));
      testResize(resourcePath);
      Assert.assertNotNull("Buffered image is null after read:" + resourcePath,
            bufferedImage);
   }

   private boolean testResize(String resourcePath)
   {
      ImageResizeManagerImpl resizeManager = new ImageResizeManagerImpl();
      boolean success = true;
      try
      {
         String ext = FilenameUtils.getExtension(resourcePath);
         resizeManager.setExtension(ext);
         resizeManager.setContentType(MimeUtils.getMimeTypeByExtension(ext));

         ImageData resizedImage = resizeManager.generateImage(getClass()
               .getClassLoader().getResourceAsStream(resourcePath));

         Assert.assertTrue(
               "Invalid ImageData for generateImage(InputStream input)",
               validateImageData(resizedImage));
      }
      catch (Exception e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));

         Assert.fail("Caught exception on resize");

      }
      return success;
   }

   private boolean validateImageData(ImageData imageData)
   {
      boolean valid = true;
      if (imageData == null)
      {
         valid = false;
      }
      if ((imageData.getBinary() == null)
            || (imageData.getBinary().length == 0))
      {
         valid = false;
         Assert.fail("Invalid ImageData returned after resize");
      }
      return valid;
   }

   private BufferedImage readImage(byte[] imageBytes)
   {
      BufferedImage bufferedImage = null;
      try
      {
         long startTime = System.currentTimeMillis();
         bufferedImage = ImageReader.read(imageBytes);
         System.out.print("Image Read Time: "
               + (System.currentTimeMillis() - startTime) + " ms\n");
      }
      catch (ImageReaderException imageReaderException)
      {
         Assert.fail("Caught image reader exception");
         log.error(imageReaderException.getMessage());
         log.debug(imageReaderException.getMessage(), imageReaderException);
      }
      return bufferedImage;
   }

   private ImageInfo getImageInfo(byte[] imageBytes)
   {
      ImageInfo imageInfo = null;
      try
      {
         imageInfo = Imaging.getImageInfo(imageBytes);

         System.out.print("=============================Testing "
               + imageInfo.getFormatName() + " | Color Type: "
               + imageInfo.getColorType() + "\n");

         System.out.print(imageInfo.toString());
      }
      catch (ImageReadException e)
      {
         Assert.fail("Caught image read exception getting image information:"
               + e.getMessage());
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
      catch (IOException e)
      {
         Assert.fail("Caught IO exception getting image information:"
               + e.getMessage());
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
      return imageInfo;
   }

   private byte[] readBytesForImageResource(String resourceLocation)
   {
      InputStream inputStream = getClass().getClassLoader()
            .getResourceAsStream(resourceLocation);
      byte[] imageBytes = null;
      try
      {
         imageBytes = IOUtils.toByteArray(inputStream);
      }
      catch (IOException e)
      {
         fail("unable to read image as test resource");
      }
      return imageBytes;
   }
}
