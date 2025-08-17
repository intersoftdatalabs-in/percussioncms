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

package com.percussion.widgets.image.data;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents metadata for image files including dimensions, size, and file information.
 * This class is immutable after construction when using the copy constructor and
 * provides defensive copying for thread safety.
 *
 * @since Java 11
 */
public class ImageMetaData implements Serializable {

   private static final long serialVersionUID = -13542359L;

   private String mimeType;
   private String ext;
   private String filename;
   private long size = 0L;
   private int width = 0;
   private int height = 0;
   private int thumbWidth = 0;

   /**
    * Default constructor creating an empty ImageMetaData instance.
    */
   public ImageMetaData() {
      // Default constructor
   }

   /**
    * Copy constructor that creates a defensive copy of another ImageMetaData instance.
    *
    * @param other the ImageMetaData instance to copy, must not be {@code null}
    * @throws IllegalArgumentException if other is {@code null}
    */
   public ImageMetaData(ImageMetaData other) {
      Objects.requireNonNull(other, "ImageMetaData to copy must not be null");

      this.filename = other.getFilename();
      this.ext = other.getExt();
      this.mimeType = other.getMimeType();
      this.size = other.getSize();
      this.height = other.getHeight();
      this.width = other.getWidth();
      this.thumbWidth = other.getThumbWidth();
   }

   /**
    * Gets the MIME type of the image.
    *
    * @return the MIME type, may be {@code null}
    */
   public String getMimeType() {
      return mimeType;
   }

   /**
    * Sets the MIME type of the image.
    *
    * @param mimeType the MIME type to set, may be {@code null}
    */
   public void setMimeType(String mimeType) {
      this.mimeType = mimeType;
   }

   /**
    * Gets the file extension of the image.
    *
    * @return the file extension without leading dot, may be {@code null}
    */
   public String getExt() {
      return ext;
   }

   /**
    * Sets the file extension of the image.
    * Leading dots are automatically removed.
    *
    * @param ext the file extension, must not be {@code null} or empty
    * @throws IllegalArgumentException if ext is {@code null} or empty
    */
   public void setExt(String ext) {
      if (StringUtils.isBlank(ext)) {
         throw new IllegalArgumentException("Extension is required and must not be blank");
      }
      this.ext = ext.replace(".", "");
   }

   /**
    * Gets the filename of the image.
    *
    * @return the filename, may be {@code null}
    */
   public String getFilename() {
      return filename;
   }

   /**
    * Gets the filename as an Optional.
    *
    * @return Optional containing the filename, or empty if filename is null or blank
    */
   public Optional<String> getFilenameOptional() {
      return Optional.ofNullable(filename)
         .filter(StringUtils::isNotBlank);
   }

   /**
    * Sets the filename of the image.
    *
    * @param filename the filename to set, may be {@code null}
    */
   public void setFilename(String filename) {
      this.filename = filename;
   }

   /**
    * Gets the file size in bytes.
    *
    * @return the file size in bytes, always >= 0
    */
   public long getSize() {
      return size;
   }

   /**
    * Sets the file size in bytes.
    *
    * @param size the file size in bytes, must be >= 0
    * @throws IllegalArgumentException if size is negative
    */
   public void setSize(long size) {
      if (size < 0) {
         throw new IllegalArgumentException("Size must not be negative");
      }
      this.size = size;
   }

   /**
    * Gets the image width in pixels.
    *
    * @return the width in pixels, always >= 0
    */
   public int getWidth() {
      return width;
   }

   /**
    * Sets the image width in pixels.
    *
    * @param width the width in pixels, must be >= 0
    * @throws IllegalArgumentException if width is negative
    */
   public void setWidth(int width) {
      if (width < 0) {
         throw new IllegalArgumentException("Width must not be negative");
      }
      this.width = width;
   }

   /**
    * Gets the image height in pixels.
    *
    * @return the height in pixels, always >= 0
    */
   public int getHeight() {
      return height;
   }

   /**
    * Sets the image height in pixels.
    *
    * @param height the height in pixels, must be >= 0
    * @throws IllegalArgumentException if height is negative
    */
   public void setHeight(int height) {
      if (height < 0) {
         throw new IllegalArgumentException("Height must not be negative");
      }
      this.height = height;
   }

   /**
    * Gets the thumbnail width in pixels.
    *
    * @return the thumbnail width in pixels, always >= 0
    */
   public int getThumbWidth() {
      return thumbWidth;
   }

   /**
    * Sets the thumbnail width in pixels.
    *
    * @param thumbWidth the thumbnail width in pixels, must be >= 0
    * @throws IllegalArgumentException if thumbWidth is negative
    */
   public void setThumbWidth(int thumbWidth) {
      if (thumbWidth < 0) {
         throw new IllegalArgumentException("Thumbnail width must not be negative");
      }
      this.thumbWidth = thumbWidth;
   }

   /**
    * Checks if this metadata represents a valid image with non-zero dimensions.
    *
    * @return {@code true} if width and height are both greater than 0
    */
   public boolean hasValidDimensions() {
      return width > 0 && height > 0;
   }

   /**
    * Calculates the aspect ratio of the image.
    *
    * @return Optional containing the aspect ratio (width/height), or empty if height is 0
    */
   public Optional<Double> getAspectRatio() {
      return height > 0 ? Optional.of((double) width / height) : Optional.empty();
   }

   /**
    * Gets the human-readable file size.
    *
    * @return formatted file size string (e.g., "1.5 MB", "256 KB")
    */
   public String getFormattedSize() {
      if (size == 0) {
         return "0 B";
      }

      var units = new String[]{"B", "KB", "MB", "GB", "TB"};
      var digitGroups = (int) (Math.log10(size) / Math.log10(1024));

      return String.format("%.1f %s",
         size / Math.pow(1024, digitGroups),
         units[digitGroups]);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
         return false;
      }
      var that = (ImageMetaData) obj;
      return size == that.size &&
             width == that.width &&
             height == that.height &&
             thumbWidth == that.thumbWidth &&
             Objects.equals(mimeType, that.mimeType) &&
             Objects.equals(ext, that.ext) &&
             Objects.equals(filename, that.filename);
   }

   @Override
   public int hashCode() {
      return Objects.hash(mimeType, ext, filename, size, width, height, thumbWidth);
   }

   @Override
   public String toString() {
      return String.format("ImageMetaData{filename='%s', mimeType='%s', ext='%s', " +
                          "size=%s, dimensions=%dx%d, thumbWidth=%d}",
         filename, mimeType, ext, getFormattedSize(), width, height, thumbWidth);
   }
}
