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

import jakarta.activation.DataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * A DataSource implementation that stores data in a byte array.
 * This class provides thread-safe access to binary data with defensive copying
 * and supports various initialization patterns.
 *
 * @since Java 11
 */
public class ByteArrayDataSource implements DataSource {

   private final ByteArrayOutputStream store;
   private final String name;
   private final String contentType;

   /**
    * Creates a new ByteArrayDataSource with default settings.
    */
   public ByteArrayDataSource() {
      this.store = new ByteArrayOutputStream();
      this.name = null;
      this.contentType = null;
   }

   /**
    * Creates a new ByteArrayDataSource with specified name and content type.
    *
    * @param name the name of the data source, may be {@code null}
    * @param contentType the content type of the data, may be {@code null}
    */
   public ByteArrayDataSource(String name, String contentType) {
      this.store = new ByteArrayOutputStream();
      this.name = name;
      this.contentType = contentType;
   }

   /**
    * Creates a new ByteArrayDataSource with specified name, content type, and initial capacity.
    *
    * @param name the name of the data source, may be {@code null}
    * @param contentType the content type of the data, may be {@code null}
    * @param initialCapacity the initial capacity of the internal buffer, must be >= 0
    * @throws IllegalArgumentException if initialCapacity is negative
    */
   public ByteArrayDataSource(String name, String contentType, int initialCapacity) {
      if (initialCapacity < 0) {
         throw new IllegalArgumentException("Initial capacity must not be negative");
      }
      this.store = new ByteArrayOutputStream(initialCapacity);
      this.name = name;
      this.contentType = contentType;
   }

   /**
    * Creates a new ByteArrayDataSource from existing byte array data.
    * The data is copied defensively to prevent external modification.
    *
    * @param name the name of the data source, may be {@code null}
    * @param contentType the content type of the data, may be {@code null}
    * @param data the initial data, may be {@code null}
    */
   public ByteArrayDataSource(String name, String contentType, byte[] data) {
      this.name = name;
      this.contentType = contentType;
      this.store = new ByteArrayOutputStream();

      if (data != null) {
         try {
            this.store.write(data);
         } catch (IOException e) {
            // Should never happen with ByteArrayOutputStream
            throw new IllegalStateException("Failed to write initial data", e);
         }
      }
   }

   @Override
   public String getContentType() {
      return contentType;
   }

   /**
    * Gets the content type as an Optional.
    *
    * @return Optional containing the content type, or empty if not set
    */
   public Optional<String> getContentTypeOptional() {
      return Optional.ofNullable(contentType)
         .filter(StringUtils::isNotBlank);
   }

   @Override
   public InputStream getInputStream() throws IOException {
      // Create a new input stream from a defensive copy of the data
      var data = getBytes();
      return new ByteArrayInputStream(data);
   }

   @Override
   public String getName() {
      return name;
   }

   /**
    * Gets the name as an Optional.
    *
    * @return Optional containing the name, or empty if not set
    */
   public Optional<String> getNameOptional() {
      return Optional.ofNullable(name)
         .filter(StringUtils::isNotBlank);
   }

   @Override
   public OutputStream getOutputStream() throws IOException {
      return store;
   }

   /**
    * Gets a defensive copy of the stored bytes.
    *
    * @return copy of the stored byte array, never {@code null}
    */
   public byte[] getBytes() {
      var storeBytes = store.toByteArray();
      return Arrays.copyOf(storeBytes, storeBytes.length);
   }

   /**
    * Gets the bytes as an Optional.
    *
    * @return Optional containing a copy of the bytes, or empty if no data is stored
    */
   public Optional<byte[]> getBytesOptional() {
      var bytes = getBytes();
      return bytes.length > 0 ? Optional.of(bytes) : Optional.empty();
   }

   /**
    * Gets the size of the stored data in bytes.
    *
    * @return the size in bytes, always >= 0
    */
   public int getSize() {
      return store.size();
   }

   /**
    * Checks if this data source contains any data.
    *
    * @return {@code true} if data is present, {@code false} otherwise
    */
   public boolean hasData() {
      return store.size() > 0;
   }

   /**
    * Checks if this data source is empty.
    *
    * @return {@code true} if no data is stored, {@code false} otherwise
    */
   public boolean isEmpty() {
      return store.size() == 0;
   }

   /**
    * Clears all stored data.
    */
   public void clear() {
      store.reset();
   }

   /**
    * Writes the specified bytes to this data source.
    *
    * @param data the data to write, may be {@code null}
    * @throws IOException if an I/O error occurs
    */
   public void writeBytes(byte[] data) throws IOException {
      if (data != null) {
         store.write(data);
      }
   }

   /**
    * Writes data from an InputStream to this data source.
    *
    * @param input the input stream to read from, must not be {@code null}
    * @throws IOException if an I/O error occurs
    * @throws IllegalArgumentException if input is {@code null}
    */
   public void writeFromInputStream(InputStream input) throws IOException {
      Objects.requireNonNull(input, "InputStream must not be null");

      var buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = input.read(buffer)) != -1) {
         store.write(buffer, 0, bytesRead);
      }
   }

   /**
    * Creates a copy of this ByteArrayDataSource with the same data.
    *
    * @return new ByteArrayDataSource with copied data
    */
   public ByteArrayDataSource copy() {
      return new ByteArrayDataSource(name, contentType, getBytes());
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
         return false;
      }
      var that = (ByteArrayDataSource) obj;
      return Objects.equals(name, that.name) &&
             Objects.equals(contentType, that.contentType) &&
             Arrays.equals(getBytes(), that.getBytes());
   }

   @Override
   public int hashCode() {
      return Objects.hash(name, contentType, Arrays.hashCode(getBytes()));
   }

   @Override
   public String toString() {
      return String.format("ByteArrayDataSource{name='%s', contentType='%s', size=%d}",
         name, contentType, getSize());
   }
}
