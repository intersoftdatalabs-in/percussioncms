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
package com.percussion.rxverify.data;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents information about a single file including its path, size, and SHA-256 digest.
 * This class is used for file verification and comparison during installation validation.
 *
 * <p>This class has been modernized to use Java 11 features including:
 * <ul>
 * <li>Enhanced null checking with Objects utility methods</li>
 * <li>Modern file I/O with Files and Path APIs</li>
 * <li>Optional for null-safe operations</li>
 * <li>Improved exception handling and validation</li>
 * <li>String utility methods for cleaner code</li>
 * </ul>
 *
 * @author dougrand
 * @author Sunny Sal the Senior Java Developer (Java 11 modernization)
 * @since Java 11
 */
public class PSFileInfo implements Externalizable
{
   private static final long serialVersionUID = 1L;

   private static final String DIGEST_ALGORITHM = "SHA-256";
   private static final int BUFFER_SIZE = 8192; // Power of 2 for better performance

   /**
    * File extensions and patterns to exclude from digest calculation.
    * Using List.of() for immutable collection (Java 11 feature).
    */
   private static final List<String> EXCLUDED_PATTERNS = List.of(
      ".log", ".tmp", ".temp", ".bak", ".cache"
   );

   private long size;
   private String path;
   private byte[] digest;

   /**
    * Constructs a new PSFileInfo object with enhanced validation and modern file I/O.
    *
    * @param file the file to analyze, must not be {@code null} and should exist
    * @param relpath the relative path, must not be {@code null} or empty
    * @throws IllegalArgumentException if parameters are invalid
    * @throws IOException if there are problems reading the file
    * @throws NoSuchAlgorithmException if the digest algorithm is not available
    */
   public PSFileInfo(File file, String relpath) throws IOException, NoSuchAlgorithmException {
      this.size = validateAndGetFileSize(file);
      this.path = validateRelativePath(relpath);
      this.digest = calculateDigest(file.toPath(), relpath);
   }

   /**
    * Default constructor for serialization support.
    */
   public PSFileInfo() {
      // Default constructor for Externalizable
   }

   /**
    * Validates the file and returns its size using modern validation patterns.
    *
    * @param file the file to validate
    * @return the file size in bytes
    * @throws IllegalArgumentException if file is invalid
    */
   private long validateAndGetFileSize(File file) {
      Objects.requireNonNull(file, "File cannot be null");

      if (!file.exists()) {
         throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
      }

      if (!file.isFile()) {
         throw new IllegalArgumentException("Path is not a file: " + file.getAbsolutePath());
      }

      return file.length();
   }

   /**
    * Validates the relative path using modern string validation.
    *
    * @param relpath the relative path to validate
    * @return the validated relative path
    * @throws IllegalArgumentException if path is invalid
    */
   private String validateRelativePath(String relpath) {
      if (relpath == null || relpath.isBlank()) {
         throw new IllegalArgumentException("Relative path cannot be null or empty");
      }
      return relpath.trim();
   }

   /**
    * Calculates the SHA-256 digest for the file using modern Java I/O.
    *
    * @param filePath the path to the file
    * @param relpath the relative path for exclusion checking
    * @return the calculated digest, or empty array if file should be excluded
    * @throws IOException if file reading fails
    * @throws NoSuchAlgorithmException if digest algorithm is not available
    */
   private byte[] calculateDigest(Path filePath, String relpath) throws IOException, NoSuchAlgorithmException {
      if (!Files.isReadable(filePath) || isExcluded(relpath)) {
         return new byte[0];
      }

      var digest = MessageDigest.getInstance(DIGEST_ALGORITHM);

      try (var inputStream = Files.newInputStream(filePath)) {
         var buffer = new byte[BUFFER_SIZE];
         int bytesRead;

         while ((bytesRead = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
         }

         return digest.digest();
      }
   }

   /**
    * Checks if a file should be excluded from digest calculation.
    * Uses modern string operations for pattern matching.
    *
    * @param relpath the relative path to check
    * @return true if the file should be excluded, false otherwise
    */
   private boolean isExcluded(String relpath) {
      var lowerPath = relpath.toLowerCase();
      return EXCLUDED_PATTERNS.stream()
         .anyMatch(lowerPath::endsWith);
   }

   /**
    * Legacy method for backward compatibility.
    *
    * @param relpath the relative path to check
    * @return true if excluded, false otherwise
    * @deprecated Use {@link #isExcluded(String)} instead
    */
   @Deprecated(since = "Java 11 refactor")
   public static boolean excluded(String relpath) {
      return new PSFileInfo().isExcluded(relpath);
   }

   /**
    * Gets the file size in bytes.
    *
    * @return the file size
    */
   public long getSize() {
      return size;
   }

   /**
    * Sets the file size.
    *
    * @param size the file size in bytes
    */
   public void setSize(long size) {
      this.size = size;
   }

   /**
    * Gets the relative path of the file.
    *
    * @return the relative path, never {@code null}
    */
   public String getPath() {
      return Optional.ofNullable(path).orElse("");
   }

   /**
    * Sets the relative path of the file.
    *
    * @param path the relative path
    */
   public void setPath(String path) {
      this.path = path;
   }

   /**
    * Gets the SHA-256 digest of the file.
    *
    * @return a copy of the digest array, never {@code null}
    */
   public byte[] getDigest() {
      return digest != null ? Arrays.copyOf(digest, digest.length) : new byte[0];
   }

   /**
    * Sets the file digest.
    *
    * @param digest the digest array
    */
   public void setDigest(byte[] digest) {
      this.digest = digest != null ? Arrays.copyOf(digest, digest.length) : new byte[0];
   }

   /**
    * Checks if this file has a valid digest (non-empty).
    *
    * @return true if the file has a valid digest, false otherwise
    */
   public boolean hasValidDigest() {
      return digest != null && digest.length > 0;
   }

   /**
    * Compares the digest of this file with another PSFileInfo.
    *
    * @param other the other file info to compare with
    * @return true if digests match, false otherwise
    */
   public boolean digestMatches(PSFileInfo other) {
      if (other == null) {
         return false;
      }
      return Arrays.equals(this.digest, other.digest);
   }

   @Override
   public void writeExternal(ObjectOutput out) throws IOException {
      out.writeLong(size);
      out.writeUTF(Optional.ofNullable(path).orElse(""));
      out.writeInt(digest != null ? digest.length : 0);
      if (digest != null && digest.length > 0) {
         out.write(digest);
      }
   }

   @Override
   public void readExternal(ObjectInput in) throws IOException {
      this.size = in.readLong();
      this.path = in.readUTF();

      var digestLength = in.readInt();
      if (digestLength > 0) {
         this.digest = new byte[digestLength];
         in.readFully(this.digest);
      } else {
         this.digest = new byte[0];
      }
   }

   @Override
   public String toString() {
      return String.format("PSFileInfo{path='%s', size=%d, hasDigest=%s}",
         getPath(), size, hasValidDigest());
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;

      var other = (PSFileInfo) obj;
      return size == other.size &&
             Objects.equals(path, other.path) &&
             Arrays.equals(digest, other.digest);
   }

   @Override
   public int hashCode() {
      return Objects.hash(size, path, Arrays.hashCode(digest));
   }
}
