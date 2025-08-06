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
package com.percussion.services.filestorage;

import com.percussion.services.filestorage.data.PSHashedColumn;
import com.percussion.services.filestorage.data.PSMeta;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * The file storage service manages files with hash-based deduplication using modern Java 11 patterns.
 *
 * <p>This service enables multiple revisions of an item or multiple items to use a single
 * binary source through SHA1-based content addressing. It provides comprehensive file
 * management with metadata extraction, text extraction, and lifecycle operations using
 * Optional-based safe access and Stream API for efficient processing.</p>
 *
 * @author peterfrontiero
 * @author stephenbolton
 * @since 6.0
 */
public interface IPSFileStorageService {

   /**
    * Stores the specified binary stream and populates metadata if it does not already exist.
    *
    * <p>This method stores the binary content using SHA1-based deduplication and extracts
    * comprehensive metadata including MIME type, encoding, and file properties.</p>
    *
    * @param inputStream the input stream to store, not {@code null}
    * @param contentType used for fine-grained detection, calculated if {@code null}
    * @param originalFilename used for type detection and identification, may be {@code null}
    * @param encoding used for type detection and identification, may be {@code null}
    * @return a unique SHA1 hash value for the file, never {@code null}
    *
    * @throws Exception if an error occurs during storage
    */
   String store(InputStream inputStream, String contentType, String originalFilename, String encoding)
      throws Exception;
   
   /**
    * Stores the specified File and populates metadata if it does not already exist.
    *
    * <p>If file is a PSPurgableTemp file type, encoding, and source filename
    * are used to help extraction, otherwise just the filename. The more information
    * provided, the better the content detection.</p>
    *
    * @param file the file to store, not {@code null}
    * @return a unique SHA1 hash value for the file, never {@code null}
    *
    * @throws Exception if an error occurs during storage
    */
   String store(File file) throws Exception;

   /**
    * Helper method to use Tika to extract the correct MIME type for a file.
    *
    * <p>Supports both regular files and PSPurgableTempFile objects where it will
    * use the sourceFilename for enhanced detection.</p>
    *
    * @param file the file to analyze, not {@code null}
    * @return the detected MIME type, never {@code null}
    */
   String getType(File file);

   /**
    * Deletes the file with the specified hash, along with its metadata.
    *
    * @param hash SHA1 hash of the file, cannot be {@code null} or empty
    * @throws Exception if an error occurs during deletion
    */
   void delete(String hash) throws Exception;

   /**
    * Retrieves metadata for the specified file with Optional wrapper for safe access.
    *
    * @param hash SHA1 hash of the file, not {@code null}
    * @return Optional containing the metadata, or empty if file doesn't exist
    */
   Optional<IPSFileMeta> getMetaSafely(String hash);

   /**
    * Retrieves metadata for the specified file.
    *
    * @param hash SHA1 hash of the file, not {@code null}
    * @return the metadata for the specified file, never {@code null}
    * @deprecated Use {@link #getMetaSafely(String)} for null-safe access
    */
   @Deprecated
   IPSFileMeta getMeta(String hash);

   /**
    * Extracts text content from the specified file with Optional wrapper.
    *
    * @param hash SHA1 hash of the file, not {@code null}
    * @return Optional containing the extracted text, or empty if extraction fails
    */
   Optional<String> getTextSafely(String hash);

   /**
    * Extracts text content from the specified file.
    *
    * @param hash SHA1 hash of the file, not {@code null}
    * @return the extracted text content, never {@code null}
    * @deprecated Use {@link #getTextSafely(String)} for null-safe access
    */
   @Deprecated
   String getText(String hash);

   /**
    * Gets an input stream to the specified file with Optional wrapper for safe access.
    *
    * @param hash SHA1 hash of the file, not {@code null}
    * @return Optional containing the input stream, or empty if file doesn't exist
    */
   Optional<InputStream> getStreamSafely(String hash);

   /**
    * Gets an input stream to the specified file.
    * 
    * @param hash SHA1 hash of the file, not {@code null}
    * @return an input stream to the specified file, may be {@code null} if file doesn't exist
    * @deprecated Use {@link #getStreamSafely(String)} for null-safe access
    */
   @Deprecated
   InputStream getStream(String hash);

   /**
    * Determines if the specified file has already been stored.
    *
    * @param hash SHA1 hash of the file, not {@code null}
    * @return {@code true} if the file exists, {@code false} otherwise
    * @throws Exception if an error occurs during existence check
    */
   boolean fileExists(String hash) throws Exception;
    
   /**
    * Counts items that have not been touched in the specified number of days.
    *
    * <p>Use {@link #touchAllHashes(Set)} to ensure known references in system are not removed.</p>
    *
    * @param days the number of days threshold
    * @return count of items older than specified days
    */
   long countOlderThan(int days);

   /**
    * Purges binary items if they have not been touched in the specified number of days.
    *
    * <p>Use {@link #touchAllHashes(Set)} to ensure known references in system are not removed.</p>
    *
    * @param days the number of days threshold
    * @return number of items deleted
    */
   long deleteOlderThan(int days);

   /**
    * Touch all binaries specified in the set of columns using Stream API for efficient processing.
    *
    * @param columns the set of hashed columns to touch, not {@code null}
    */
   void touchAllHashes(Set<PSHashedColumn> columns);

   /**
    * Updates the persisted original filename stored for the hash.
    *
    * <p>This filename is used for metadata generation to help identify type and item identification.
    * There is only one filename regardless of how many times it is in the system.
    * This may be used as a default filename, but actual filename should be stored and
    * maintained on the system.</p>
    *
    * @param hash the SHA1 hash, not {@code null}
    * @param filename the new filename, may be {@code null}
    */
   void updateFilename(String hash, String filename);

   /**
    * Modifies stored content type for the hash.
    *
    * <p>This is used to help metadata regeneration. Metadata regeneration
    * that is called after update type may modify the specified value if it is not correct.</p>
    *
    * @param hash the SHA1 hash, not {@code null}
    * @param contentType the new content type, may be {@code null}
    */
   void updateType(String hash, String contentType);

   /**
    * Modifies stored encoding for the hash.
    *
    * <p>This is used to help metadata regeneration. Metadata regeneration
    * that is called after update type may modify the specified value if it is not correct.</p>
    *
    * @param hash the SHA1 hash, not {@code null}
    * @param encoding the new encoding, may be {@code null}
    */
   void updateEncoding(String hash, String encoding);

   /**
    * Gets the list of known metadata keys from all processed binaries.
    *
    * @return immutable list of metadata keys, never {@code null}
    */
   List<String> getMetaKeys();

   /**
    * Gets a list of all metadata keys that have been disabled.
    *
    * @return immutable list of disabled key strings, never {@code null}
    */
   List<String> getDisabledMetaKeys();

   /**
    * Enables a metadata key.
    *
    * <p>Metadata would need to be regenerated to persist this data for existing items.</p>
    *
    * @param keyName the metadata key name, not {@code null}
    * @return {@code true} if the value was changed, {@code false} otherwise
    */
   boolean enableMetaKey(String keyName);

   /**
    * Disables a metadata key.
    *
    * <p>Metadata would need to be regenerated to persist this data for existing items.</p>
    *
    * @param keyName the metadata key name, not {@code null}
    * @return {@code true} if the value was changed, {@code false} otherwise
    */
   boolean disableMetaKey(String keyName);

   /**
    * Marks all items for reparsing of metadata and starts a thread to process them.
    *
    * @return {@code true} if reparsing was started, {@code false} if already running
    */
   boolean reparseMetaAll();

   /**
    * Regenerates the metadata for an individual item asynchronously.
    *
    * <p>This may help to identify parsing issues. If there was a parse error,
    * the metadata on the item can be accessed to identify the error.</p>
    *
    * @param hash the SHA1 hash, not {@code null}
    * @return CompletableFuture that completes with {@code true} if there was a parse error
    */
   CompletableFuture<Boolean> reparseMetaAsync(String hash);

   /**
    * Regenerates the metadata for an individual item.
    *
    * @param hash the SHA1 hash, not {@code null}
    * @return {@code true} if there was a parse error, {@code false} otherwise
    * @deprecated Use {@link #reparseMetaAsync(String)} for non-blocking operation
    */
   @Deprecated
   boolean reparseMeta(String hash);

   /**
    * Allows for bulk import of binaries into the system asynchronously.
    *
    * <p>The directory specified will be recursively processed to look for any files ending
    * with .sha1. If this file contains a sha1 hash that is already in the system
    * this item will be skipped. Otherwise a file in the same directory with the same name
    * excluding the .sha1 extension is imported into the system, and the .sha1 file is
    * updated if the hash is not present, or is incorrect.</p>
    *
    * @param rootPath the root path for import, not {@code null}
    * @return CompletableFuture that completes with {@code true} if import started successfully
    */
   CompletableFuture<Boolean> importAllBinaryAsync(String rootPath);

   /**
    * Allows for bulk import of binaries into the system.
    *
    * @param rootPath the root path for import, not {@code null}
    * @return {@code true} if the call started the processing, {@code false} if already running
    * @deprecated Use {@link #importAllBinaryAsync(String)} for non-blocking operation
    */
   @Deprecated
   boolean importAllBinary(String rootPath);

   /**
    * Exports all binaries to the filesystem asynchronously.
    *
    * <p>Binaries are output to a folder structure based upon the items sha1 hash.
    * The first 6 characters of the hash are used to create 3 sets of folders
    * with names 2 characters long. The rest creates a 4th level folder.</p>
    *
    * @param rootPath the root path for export, not {@code null}
    * @return CompletableFuture that completes with {@code true} if export started successfully
    */
   CompletableFuture<Boolean> exportAllBinaryAsync(String rootPath);

   /**
    * Exports all binaries to the filesystem.
    *
    * @param rootPath the root path for export, not {@code null}
    * @return {@code true} if the call started the processing, {@code false} if already running
    * @deprecated Use {@link #exportAllBinaryAsync(String)} for non-blocking operation
    */
   @Deprecated
   boolean exportAllBinary(String rootPath);

   /**
    * Exports all the binaries stored in the legacy PSX_BINARYSTORE table asynchronously.
    *
    * @param rootPath the root path for export, not {@code null}
    * @return CompletableFuture that completes with {@code true} if export started successfully
    */
   CompletableFuture<Boolean> exportAllLegacyBinaryAsync(String rootPath);

   /**
    * Exports all the binaries stored in the legacy PSX_BINARYSTORE table.
    *
    * @param rootPath the root path for export, not {@code null}
    * @return {@code true} if the call started the processing, {@code false} if already running
    * @deprecated Use {@link #exportAllLegacyBinaryAsync(String)} for non-blocking operation
    */
   @Deprecated
   boolean exportAllLegacyBinary(String rootPath);

   /**
    * Checks if the import/export thread is currently running.
    *
    * @return {@code true} if running, {@code false} otherwise
    */
   boolean isImpExpRunning();

   /**
    * Checks if the reparse meta thread is currently running.
    *
    * @return {@code true} if running, {@code false} otherwise
    */
   boolean isReparseMetaRunning();

   /**
    * Gets the hashing algorithm used.
    *
    * <p>Currently SHA1. This hashing is used for uniqueness identification only
    * so does not have security implications requiring more secure hashing algorithms.</p>
    *
    * @return the algorithm used for checksum generation, never {@code null}
    */
   String getAlgorithm();
}
