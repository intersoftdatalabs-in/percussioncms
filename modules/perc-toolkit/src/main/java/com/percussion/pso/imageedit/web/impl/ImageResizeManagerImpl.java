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
package com.percussion.pso.imageedit.web.impl;

import com.percussion.pso.imageedit.data.ImageData;
import com.percussion.pso.imageedit.web.ImageResizeManager;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Default {@link ImageResizeManager} implementation that resizes images in memory using the
 * standard Java ImageIO APIs, applying a configurable compression level and rescaling step size.
 *
 * @author DavidBenua
 */
public class ImageResizeManagerImpl implements ImageResizeManager {
  /**
   * Creates a new ImageResizeManagerImpl.
   */
  public ImageResizeManagerImpl() {
    // default
  }

  private static final Logger log = LogManager.getLogger(ImageResizeManagerImpl.class);

  private String imageFormat = "jpeg";
  private String extension = "jpg";
  private String contentType = "image/jpeg";
  private float compression = 1.0f;

  /**
   * Controls the size of steps by which the image is rescaled. This value should always be a power
   * of 2 (2,4,8...). Larger values will increase performance, but add sampling noise to the
   * compressed image. Defaults to 2.
   */
  private int stepFactor = 2;

  /**
   * Maximum image size where interpolation is used. For images larger than this size, the image
   * pixels are not interpolated: the "nearest pixel" algorithm is used instead. Increasing the
   * value may increase image quality for images that are smaller than the specified size. It will
   * also decrease performance, sometimes significantly. Defaults to 1,000,000 pixels.
   */
  private int maxInterpolationSize = 1000000;

  /**
   * See referenced member.
   * @see ImageResizeManager#generateImage(InputStream, Rectangle, Dimension)
   * @param input the input
   * @param cropBox the crop box
   * @param size the size
   * @return the result
   * @throws Exception if an error occurs
   */
  public ImageData generateImage(InputStream input, Rectangle cropBox, Dimension size)
      throws Exception {
    ImageData result = new ImageData();
    result.setExt(this.getExtension());
    result.setMimeType(this.getContentType());

    log.debug("generating Image");

    // compute the desired size.

    Dimension outsize;
    BufferedImage inImage = ImageIO.read(input);

    log.debug("Image size is w {} h {}", inImage.getWidth(), inImage.getHeight());
    if (size != null && cropBox != null) {
      outsize = computeSizeFromAspectRatio(cropBox, size);
    } else if (size != null) {
      outsize = new Dimension(size);
    } else if (cropBox != null) {
      outsize = new Dimension(cropBox.width, cropBox.height);
    } else {
      outsize = new Dimension(inImage.getWidth(), inImage.getHeight());
    }
    log.debug("Output size is {}", outsize);
    if (outsize.height == 0 || outsize.width == 0) { // generate a zero size image
      result.setSize(0L);
      result.setBinary(new byte[0]);
      result.setHeight(outsize.height);
      result.setWidth(outsize.width);
      return result;
    }
    Rectangle sourceBox;
    if (cropBox != null) {
      sourceBox = new Rectangle(cropBox);
    } else {
      sourceBox = new Rectangle(0, 0, inImage.getWidth(), inImage.getHeight());
    }

    log.debug("Source Box is {}", sourceBox);

    long startTimer = System.currentTimeMillis();

    BufferedImage outImage = scaleImage(inImage, sourceBox, outsize);

    if (log.isDebugEnabled()) {
      long endTimer = System.currentTimeMillis();
      long elapsed = endTimer - startTimer;
      log.debug("Elapsed time is {}", elapsed);
    }
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    try {

      Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(getImageFormat());
      ImageWriter iw = iter.next();
      ImageWriteParam iwp = iw.getDefaultWriteParam();
      iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

      iwp.setCompressionQuality(compression);

      MemoryCacheImageOutputStream mcios = new MemoryCacheImageOutputStream(outStream);
      iw.setOutput(mcios);
      iw.write(null, new IIOImage(outImage, new ArrayList<BufferedImage>(), null), iwp);

      outStream.flush();
      result.setWidth(outsize.width);
      result.setHeight(outsize.height);
      result.setSize(outStream.size());
      log.debug("output size is " + result.getSize());
      result.setBinary(outStream.toByteArray());
      return result;
    } catch (Throwable ex) {
      log.error("Unexpected Exception Error: {}", ex.getMessage());
      log.debug(ex.getMessage(), ex);
      throw new RuntimeException("exception " + ex, ex);
    }
  }

  /**
   * scaleImage3 operation.
   *
   * @param inImage the in image
   * @param sourceBox the source box
   * @param outsize the outsize
   * @return the result
   */
  protected BufferedImage scaleImage3(
      BufferedImage inImage, Rectangle sourceBox, Dimension outsize) {
    log.debug("Scale image method 3");
    BufferedImage outImage =
        new BufferedImage(outsize.width, outsize.height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = outImage.createGraphics();
    boolean done =
        g2d.drawImage(
            inImage,
            0,
            0,
            outsize.width,
            outsize.height,
            sourceBox.x,
            sourceBox.y,
            (sourceBox.x + sourceBox.width),
            (sourceBox.y + sourceBox.height),
            null);
    if (!done) {
      log.info("Image not done");
      // what to do here?
    }
    return outImage;
  }

  /**
   * scaleImage operation.
   *
   * @param inImage the in image
   * @param sourceBox the source box
   * @param outSize the out size
   * @return the result
   */
  protected BufferedImage scaleImage(
      BufferedImage inImage, Rectangle sourceBox, Dimension outSize) {
    log.debug("Scaling image");
    // first we crop without scaling.
    BufferedImage croppedImage =
        inImage.getSubimage(sourceBox.x, sourceBox.y, sourceBox.width, sourceBox.height);

    while (croppedImage.getHeight() > outSize.height * stepFactor
        || croppedImage.getWidth() > outSize.width * stepFactor) {
      croppedImage = halfImage(croppedImage);
    }

    BufferedImage outImage =
        new BufferedImage(outSize.width, outSize.height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = outImage.createGraphics();
    g2d.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g2d.setRenderingHint(
        RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);

    g2d.drawImage(
        croppedImage,
        0,
        0,
        outSize.width,
        outSize.height,
        0,
        0,
        croppedImage.getWidth(),
        croppedImage.getHeight(),
        null);
    g2d.dispose();
    return outImage;
  }

  /**
   * halfImage operation.
   *
   * @param inImage the in image
   * @return the result
   */
  protected BufferedImage halfImage(BufferedImage inImage) {
    long timer = System.currentTimeMillis();
    int height = inImage.getHeight() / stepFactor;
    int width = inImage.getWidth() / stepFactor;
    log.debug("Scaling to image height {} width {}", height, width);
    BufferedImage halfImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D half = halfImage.createGraphics();
    if ((height * width) < maxInterpolationSize) {
      log.debug("using bilinear interpolation");
      half.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }
    /*half.setRenderingHint(RenderingHints.KEY_RENDERING,
    RenderingHints.VALUE_RENDER_QUALITY); */
    /* half.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
    RenderingHints.VALUE_COLOR_RENDER_QUALITY); */
    half.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    half.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);

    half.drawImage(
        inImage, 0, 0, width, height, 0, 0, inImage.getWidth(), inImage.getHeight(), null);
    if (log.isDebugEnabled()) {
      long timestop = System.currentTimeMillis();
      long elapsed = timestop - timer;
      log.debug("Time elapsed is {}", elapsed);
      // log.debug("half image height " + halfImage.getHeight() + " width " + halfImage.getWidth());
    }
    half.dispose();
    return halfImage;
  }

  /**
   * computeSizeFromAspectRatio operation.
   *
   * @param box the box
   * @param size the size
   * @return the result
   */
  public Dimension computeSizeFromAspectRatio(Rectangle box, Dimension size) {
    // Validate.isTrue(box.height > 0 && box.width > 0);
    Validate.isTrue(size.height > 0 || size.width > 0);
    int height = 0;
    int width = 0;
    Dimension outSize = size;
    double scale;
    if (size.height == 0) {
      log.debug("computing width from aspect ratio");
      width = size.width;
      if (box.height > 0) {
        scale = size.getWidth() / box.getWidth();
        log.debug("scale is {}", scale);
        height = (int) Math.round(box.getHeight() * scale);
      } else {
        height = 0;
      }
    } else if (size.width == 0) {
      height = size.height;
      if (box.height > 0) {
        scale = size.getHeight() / box.getHeight();
        width = (int) Math.round(box.getWidth() * scale);
      } else {
        width = 0;
      }
    }
    if (height > 0 || width > 0) {
      size = new Dimension(width, height);
    }
    return size;
  }

  /**
   * Returns the imageFormat.
   * @return the imageFormat
   */
  public String getImageFormat() {
    return imageFormat;
  }

  /**
   * Sets the imageFormat.
   * @param imageFormat the imageFormat to set
   */
  public void setImageFormat(String imageFormat) {
    this.imageFormat = imageFormat;
  }

  /**
   * Returns the extension.
   * @return the extension
   */
  public String getExtension() {
    return extension;
  }

  /**
   * Sets the extension.
   * @param extension the extension to set
   */
  public void setExtension(String extension) {
    this.extension = extension;
  }

  /**
   * Returns the contentType.
   * @return the contentType
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * Sets the contentType.
   * @param contentType the contentType to set
   */
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  /**
   * Returns the compression.
   * @return the compression
   */
  public float getCompression() {
    return compression;
  }

  /**
   * Sets the compression.
   * @param compression the compression to set
   */
  public void setCompression(float compression) {
    this.compression = compression;
  }

  /**
   * Returns the stepFactor.
   * @return the stepFactor
   */
  public int getStepFactor() {
    return stepFactor;
  }

  /**
   * Sets the stepFactor.
   * @param stepFactor the stepFactor to set
   */
  public void setStepFactor(int stepFactor) {
    this.stepFactor = stepFactor;
  }

  /**
   * Returns the maxInterpolationSize.
   * @return the maxInterpolationSize
   */
  public int getMaxInterpolationSize() {
    return maxInterpolationSize;
  }

  /**
   * Sets the maxInterpolationSize.
   * @param maxInterpolationSize the maxInterpolationSize to set
   */
  public void setMaxInterpolationSize(int maxInterpolationSize) {
    this.maxInterpolationSize = maxInterpolationSize;
  }
}
