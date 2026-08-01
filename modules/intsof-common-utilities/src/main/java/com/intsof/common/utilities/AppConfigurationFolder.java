/*
 * Copyright 2026 Intersoft Data Labs (https://intsof.com)
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intsof.common.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An application-specific folder under {@code ~/.intsof/&lt;application-name&gt;/}.
 *
 * <p>All file operations return NIO {@link Path} handles. File names are validated as single path
 * segments and resolved paths are constrained to this folder.
 */
public final class AppConfigurationFolder {

  private final String applicationName;
  private final Path path;

  AppConfigurationFolder(String applicationName, Path path) {
    this.applicationName = applicationName;
    this.path = path;
  }

  /**
   * Registered application name (single path segment).
   *
   * @return application name
   */
  public String getApplicationName() {
    return applicationName;
  }

  /**
   * Absolute normalized path to this application folder.
   *
   * @return folder path
   */
  public Path getPath() {
    return path;
  }

  /**
   * List regular files in this folder (non-recursive), sorted by file name.
   *
   * @return list of absolute paths (never null; may be empty)
   * @throws IOException if the directory cannot be read
   */
  public List<Path> listFiles() throws IOException {
    List<Path> files = new ArrayList<>();
    if (!Files.isDirectory(path)) {
      return files;
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
      for (Path entry : stream) {
        if (Files.isRegularFile(entry)) {
          files.add(entry.toAbsolutePath().normalize());
        }
      }
    }
    files.sort(Comparator.comparing(p -> p.getFileName().toString()));
    return files;
  }

  /**
   * Ensure a file exists under this folder. If the file already exists it is left unchanged;
   * otherwise an empty file is created.
   *
   * @param fileName single path segment
   * @return path to the file
   * @throws IOException if the file cannot be created
   * @throws IllegalArgumentException if the name is invalid
   */
  public Path addFile(String fileName) throws IOException {
    Path target = resolveFile(fileName);
    if (!Files.exists(target)) {
      Files.createFile(target);
    }
    return target;
  }

  /**
   * Create or overwrite a file with the given byte content.
   *
   * @param fileName single path segment
   * @param content file bytes; null is treated as empty
   * @return path to the file
   * @throws IOException if the file cannot be written
   * @throws IllegalArgumentException if the name is invalid
   */
  public Path addFile(String fileName, byte[] content) throws IOException {
    Path target = resolveFile(fileName);
    byte[] bytes = content == null ? new byte[0] : content;
    Files.write(
        target,
        bytes,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE);
    return target;
  }

  /**
   * Create or overwrite a file by copying from an input stream. The stream is not closed by this
   * method.
   *
   * @param fileName single path segment
   * @param content content stream; must not be null
   * @return path to the file
   * @throws IOException if the file cannot be written
   * @throws IllegalArgumentException if the name is invalid
   * @throws NullPointerException if {@code content} is null
   */
  public Path addFile(String fileName, InputStream content) throws IOException {
    Objects.requireNonNull(content, "content");
    Path target = resolveFile(fileName);
    try (OutputStream out =
        Files.newOutputStream(
            target,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE)) {
      content.transferTo(out);
    }
    return target;
  }

  /**
   * Return the path to an existing file, if present.
   *
   * @param fileName single path segment
   * @return empty when the file does not exist
   * @throws IllegalArgumentException if the name is invalid
   */
  public Optional<Path> get(String fileName) {
    Path target = resolveFile(fileName);
    if (!Files.isRegularFile(target)) {
      return Optional.empty();
    }
    return Optional.of(target);
  }

  /**
   * Return the path to a file, optionally creating an empty file when missing.
   *
   * @param fileName single path segment
   * @param createIfMissing when true, create an empty file if absent
   * @return path when the file exists or was created; never null when {@code createIfMissing} is
   *     true and creation succeeds
   * @throws IOException if creation is requested and fails
   * @throws IllegalArgumentException if the name is invalid
   * @throws java.util.NoSuchElementException if {@code createIfMissing} is false and the file is
   *     missing (callers that prefer Optional should use {@link #get(String)})
   */
  public Path get(String fileName, boolean createIfMissing) throws IOException {
    Path target = resolveFile(fileName);
    if (Files.isRegularFile(target)) {
      return target;
    }
    if (createIfMissing) {
      if (!Files.exists(target)) {
        Files.createFile(target);
      }
      return target;
    }
    throw new java.util.NoSuchElementException("Config file not found: " + fileName);
  }

  /**
   * Delete a file if it exists.
   *
   * @param fileName single path segment
   * @return true if a file was deleted
   * @throws IOException if deletion fails
   * @throws IllegalArgumentException if the name is invalid
   */
  public boolean removeFile(String fileName) throws IOException {
    Path target = resolveFile(fileName);
    return Files.deleteIfExists(target);
  }

  /**
   * Whether a regular file with the given name exists in this folder.
   *
   * @param fileName single path segment
   * @return true when present
   * @throws IllegalArgumentException if the name is invalid
   */
  public boolean fileExists(String fileName) {
    return get(fileName).isPresent();
  }

  private Path resolveFile(String fileName) {
    String name = ConfigNames.requireValidSegment(fileName, "file name");
    return PathsUnder.resolveUnder(path, name);
  }
}
