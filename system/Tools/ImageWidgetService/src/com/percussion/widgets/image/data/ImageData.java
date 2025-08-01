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

package com.percussion.widgets.image.data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents complete image data including metadata and binary content.
 * Extends {@link ImageMetaData} to provide binary data storage capabilities.
 * This class provides defensive copying for thread safety.
 *
 * @since Java 11
 */
public class ImageData extends ImageMetaData implements Serializable {

   private static final long serialVersionUID = -135423469L;

   private byte[] binary;

   /**
    * Default constructor creating an empty ImageData instance.
    */
   public ImageData() {
      super();
   }

   /**
    * Copy constructor that creates a defensive copy of another ImageData instance.
    *
    * @param other the ImageData instance to copy, must not be {@code null}
    * @throws IllegalArgumentException if other is {@code null}
    */
   public ImageData(ImageData other) {
      super(other);
      Objects.requireNonNull(other, "ImageData to copy must not be null");

      if (other.binary != null) {
         this.binary = Arrays.copyOf(other.binary, other.binary.length);
      }
   }

   /**
    * Constructor that creates ImageData from metadata and binary content.
    *
    * @param metadata the image metadata, must not be {@code null}
    * @param binary the binary data, may be {@code null}
    * @throws IllegalArgumentException if metadata is {@code null}
    */
   public ImageData(ImageMetaData metadata, byte[] binary) {
      super(metadata);
      setBinary(binary);
   }

   /**
    * Gets a defensive copy of the binary image data.
    *
    * @return copy of the binary data, or {@code null} if no data is set
    */
   public byte[] getBinary() {
      return binary != null ? Arrays.copyOf(binary, binary.length) : null;
   }

   /**
    * Gets the binary data as an Optional.
    *
    * @return Optional containing a defensive copy of the binary data, or empty if no data is set
    */
   public Optional<byte[]> getBinaryOptional() {
      return Optional.ofNullable(binary)
         .map(data -> Arrays.copyOf(data, data.length));
   }

   /**
    * Sets the binary image data using defensive copying.
    *
    * @param binary the binary data to set, may be {@code null}
    */
   public void setBinary(byte[] binary) {
      this.binary = binary != null ? Arrays.copyOf(binary, binary.length) : null;
   }

   /**
    * Checks if this ImageData contains binary data.
    *
    * @return {@code true} if binary data is present and not empty, {@code false} otherwise
    */
   public boolean hasBinaryData() {
      return binary != null && binary.length > 0;
   }

   /**
    * Gets the size of the binary data in bytes.
    *
    * @return the size of binary data in bytes, or 0 if no data is present
    */
   public int getBinarySize() {
      return binary != null ? binary.length : 0;
   }

   /**
    * Validates that the binary data size matches the metadata size.
    *
    * @return {@code true} if sizes match or both are zero/null, {@code false} otherwise
    */
   public boolean isSizeConsistent() {
      var binarySize = getBinarySize();
      var metadataSize = getSize();

      return (binarySize == 0 && metadataSize == 0) || (binarySize == metadataSize);
   }

   /**
    * Clears the binary data and resets size to zero.
    */
   public void clearBinaryData() {
      this.binary = null;
      setSize(0);
   }

   /**
    * Creates a copy of this ImageData with only metadata (no binary data).
    *
    * @return new ImageMetaData instance with copied metadata
    */
   public ImageMetaData toMetadataOnly() {
      return new ImageMetaData(this);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
         return false;
      }
      if (!super.equals(obj)) {
         return false;
      }
      var that = (ImageData) obj;
      return Arrays.equals(binary, that.binary);
   }

   @Override
   public int hashCode() {
      return Objects.hash(super.hashCode(), Arrays.hashCode(binary));
   }

   @Override
   public String toString() {
      return String.format("ImageData{%s, binarySize=%d, hasData=%s}",
         super.toString().replace("ImageMetaData{", "").replace("}", ""),
         getBinarySize(),
         hasBinaryData());
   }
}
