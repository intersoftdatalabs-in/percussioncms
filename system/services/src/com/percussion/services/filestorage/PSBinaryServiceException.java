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

import java.util.Objects;
import java.util.Optional;

/**
 * Exception thrown by binary service operations using modern Java 11 patterns.
 *
 * <p>This exception provides enhanced error handling with factory methods for common
 * scenarios, Optional-based safe access to error context, and comprehensive error
 * categorization for file storage operations.</p>
 *
 * @author Percussion Software
 * @since 6.0
 */
public class PSBinaryServiceException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * The hash of the file associated with this exception, if applicable.
     */
    private final String fileHash;

    /**
     * The operation that caused this exception, if applicable.
     */
    private final String operation;

    /**
     * Creates a new binary service exception with the specified message.
     *
     * @param message the error message, not {@code null}
     */
    public PSBinaryServiceException(String message) {
        super(Objects.requireNonNull(message, "message cannot be null"));
        this.fileHash = null;
        this.operation = null;
    }

    /**
     * Creates a new binary service exception with the specified message and cause.
     *
     * @param message the error message, not {@code null}
     * @param cause the underlying cause, may be {@code null}
     */
    public PSBinaryServiceException(String message, Throwable cause) {
        super(Objects.requireNonNull(message, "message cannot be null"), cause);
        this.fileHash = null;
        this.operation = null;
    }

    /**
     * Creates a new binary service exception with enhanced context information.
     *
     * @param message the error message, not {@code null}
     * @param cause the underlying cause, may be {@code null}
     * @param fileHash the hash of the associated file, may be {@code null}
     * @param operation the operation that failed, may be {@code null}
     */
    public PSBinaryServiceException(String message, Throwable cause, String fileHash, String operation) {
        super(Objects.requireNonNull(message, "message cannot be null"), cause);
        this.fileHash = fileHash;
        this.operation = operation;
    }

    /**
     * Gets the file hash associated with this exception, if available.
     *
     * @return Optional containing the file hash, or empty if not available
     */
    public Optional<String> getFileHash() {
        return Optional.ofNullable(fileHash);
    }

    /**
     * Gets the operation that caused this exception, if available.
     *
     * @return Optional containing the operation name, or empty if not available
     */
    public Optional<String> getOperation() {
        return Optional.ofNullable(operation);
    }

    /**
     * Factory method for file not found errors.
     *
     * @param fileHash the hash of the file that was not found
     * @return a new PSBinaryServiceException for file not found
     */
    public static PSBinaryServiceException fileNotFound(String fileHash) {
        return new PSBinaryServiceException(
            String.format("File not found with hash: %s", fileHash),
            null, fileHash, "retrieve");
    }

    /**
     * Factory method for storage errors.
     *
     * @param cause the underlying storage error
     * @return a new PSBinaryServiceException for storage failures
     */
    public static PSBinaryServiceException storageError(Throwable cause) {
        return new PSBinaryServiceException(
            "Failed to store file: " + cause.getMessage(),
            cause, null, "store");
    }

    /**
     * Factory method for checksum errors.
     *
     * @param fileHash the hash that failed validation
     * @param cause the underlying checksum error
     * @return a new PSBinaryServiceException for checksum failures
     */
    public static PSBinaryServiceException checksumError(String fileHash, Throwable cause) {
        return new PSBinaryServiceException(
            String.format("Checksum validation failed for hash: %s", fileHash),
            cause, fileHash, "checksum");
    }

    /**
     * Factory method for metadata extraction errors.
     *
     * @param fileHash the hash of the file with metadata issues
     * @param cause the underlying metadata error
     * @return a new PSBinaryServiceException for metadata failures
     */
    public static PSBinaryServiceException metadataError(String fileHash, Throwable cause) {
        return new PSBinaryServiceException(
            String.format("Metadata extraction failed for hash: %s", fileHash),
            cause, fileHash, "metadata");
    }

    /**
     * Factory method for import/export errors.
     *
     * @param operation the import/export operation that failed
     * @param path the path being processed
     * @param cause the underlying error
     * @return a new PSBinaryServiceException for import/export failures
     */
    public static PSBinaryServiceException importExportError(String operation, String path, Throwable cause) {
        return new PSBinaryServiceException(
            String.format("%s operation failed for path: %s", operation, path),
            cause, null, operation);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder(super.toString());

        getFileHash().ifPresent(hash ->
            sb.append(" [fileHash=").append(hash).append("]"));

        getOperation().ifPresent(op ->
            sb.append(" [operation=").append(op).append("]"));

        return sb.toString();
    }
}
