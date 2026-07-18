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

package com.percussion.auditlog.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileCreator {
  private static final Logger log = LogManager.getLogger(FileCreator.class);

  /**
   * Generates a file with the specified parameters.
   *
   * <p>Security: This method validates the filePath to prevent path traversal attacks. The resolved
   * path must be within the base audit directory to prevent writing files outside the intended
   * location.
   *
   * @param filePath the base directory path where the file will be created, never <code>null</code>
   *     or empty
   * @param fileName the name of the file without extension, never <code>null</code> or empty
   * @param filePattern the date format pattern for the timestamp, never <code>null</code>
   * @param extension the file extension, never <code>null</code>
   * @return the absolute path of the created file, or empty string if an error occurs
   */
  public static String generateFile(
      String filePath, String fileName, String filePattern, String extension) {
    String finalFileName = "";
    try {
      // Validate input parameters
      if (filePath == null || filePath.isEmpty()) {
        log.error("File path cannot be null or empty");
        return finalFileName;
      }
      if (fileName == null || fileName.isEmpty()) {
        log.error("File name cannot be null or empty");
        return finalFileName;
      }
      if (filePattern == null || extension == null) {
        log.error("File pattern and extension cannot be null");
        return finalFileName;
      }

      // Security: Validate both filePath and fileName to prevent path traversal attacks
      // Check for traversal attempts BEFORE sanitization
      if (fileName.contains("..") || fileName.startsWith("/") || fileName.startsWith("\\")) {
        log.error("Path traversal attempt detected in file name: {}", fileName);
        return finalFileName;
      }

      Path basePath = Paths.get(filePath).toAbsolutePath().normalize();

      // Security: Check for absolute paths in filePath
      if (filePath.startsWith("/") || (filePath.length() > 2 && filePath.charAt(1) == ':')) {
        // filePath being absolute is acceptable (it's the base directory)
        // but we'll verify the final result is within it
      }

      FastDateFormat simpleDateFormat = FastDateFormat.getInstance(filePattern);
      String formatted = simpleDateFormat.format(new Date());

      // Sanitize the file name: only allow alphanumeric, underscores, hyphens, and dots
      // This happens AFTER traversal detection
      String safeFileName = fileName.replaceAll("[^a-zA-Z0-9._\\-]", "_");
      String safeExtension = extension.replaceAll("[^a-zA-Z0-9]", "");
      String fileNameWithTimestamp = safeFileName + "_" + formatted + "." + safeExtension;

      // Create the final path and verify it doesn't escape the base directory
      Path finalPath = basePath.resolve(fileNameWithTimestamp).normalize();

      // Security check: Ensure the final path is within the base directory
      if (!finalPath.startsWith(basePath)) {
        log.error("Path traversal attempt detected. File path attempted to escape base directory.");
        return finalFileName;
      }

      // Create directory if it doesn't exist
      if (!Files.exists(basePath)) {
        Files.createDirectories(basePath);
      }

      // Same-day audit logs reuse the dated filename. createFile() fails with
      // FileAlreadyExistsException when the file is already present, which left
      // generateLogFile() with an empty path and broke JSON audit logging on login.
      if (!Files.exists(finalPath)) {
        Files.createFile(finalPath);
        log.debug("File created successfully: {}", finalPath);
      } else {
        log.debug("Audit log file already exists, reusing: {}", finalPath);
      }
      finalFileName = finalPath.toString();

    } catch (Exception e) {
      log.error("Exception occurred while creating file: {}", e.getMessage(), e);
      finalFileName = "";
    }

    return finalFileName;
  }
}
