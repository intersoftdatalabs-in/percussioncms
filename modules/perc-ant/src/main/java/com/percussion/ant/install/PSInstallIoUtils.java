/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.ant.install;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Portable install-time file I/O helpers replacing deprecated {@code
 * com.percussion.util.IOTools} usage inside perc-ant install tasks.
 *
 * <p>Semantics for backup/temp helpers match the former IOTools methods used by the installer.
 */
final class PSInstallIoUtils {

  private PSInstallIoUtils() {}

  /**
   * Reads file content as a UTF-8 string.
   *
   * @param file file to read, may not be {@code null}
   * @return content, never {@code null}
   * @throws IOException if the file cannot be read
   */
  static String getFileContent(File file) throws IOException {
    if (file == null) {
      throw new IllegalArgumentException("file may not be null");
    }
    return Files.readString(file.toPath(), StandardCharsets.UTF_8);
  }

  /**
   * Copies {@code source} to {@code dest}, overwriting when present.
   *
   * @param source source file, may not be {@code null}
   * @param dest destination file, may not be {@code null}
   * @throws IOException if the copy fails
   */
  static void copyFile(File source, File dest) throws IOException {
    if (source == null) {
      throw new IllegalArgumentException("source may not be null");
    }
    if (dest == null) {
      throw new IllegalArgumentException("dest may not be null");
    }
    FileUtils.copyFile(source, dest);
  }

  /**
   * Copies all characters from {@code in} to {@code out}. Neither stream is closed.
   *
   * @param in reader, may not be {@code null}
   * @param out writer, may not be {@code null}
   * @throws IOException if the copy fails
   */
  static void writeStream(Reader in, Writer out) throws IOException {
    if (in == null || out == null) {
      throw new IllegalArgumentException("Reader and Writer must be supplied.");
    }
    IOUtils.copy(in, out);
  }

  /**
   * Copies {@code file} (file or directory tree) into {@code targetDir} under the same name.
   *
   * @param file source file or directory, may not be {@code null}
   * @param targetDir destination parent directory, may not be {@code null}
   * @throws IOException if the copy fails
   */
  static void copyToDir(File file, File targetDir) throws IOException {
    if (file == null) {
      throw new IllegalArgumentException("file may not be null");
    }
    if (targetDir == null) {
      throw new IllegalArgumentException("targetDir may not be null");
    }
    FileUtils.copyToDirectory(file, targetDir);
  }

  /**
   * Creates a temp copy of {@code file} in the default temporary directory.
   *
   * @param file file to copy, may not be {@code null}
   * @return the temporary file
   * @throws IOException if the copy fails
   */
  static File createTempFile(File file) throws IOException {
    if (file == null) {
      throw new IllegalArgumentException("file may not be null");
    }

    String name = file.getName();
    String prefix = name;
    String suffix = null;
    int dotIndex = name.indexOf('.');
    if (dotIndex != -1) {
      prefix = name.substring(0, dotIndex);
      // File.createTempFile prepends '.' when suffix does not start with one.
      suffix = name.substring(dotIndex + 1);
    }
    if (prefix.length() < 3) {
      prefix = (prefix + "tmp");
      if (prefix.length() < 3) {
        prefix = "tmp";
      }
    }

    File tempFile = File.createTempFile(prefix, suffix);
    FileUtils.copyFile(file, tempFile);
    return tempFile;
  }

  /**
   * Creates a backup of {@code file} by appending {@code .000}, {@code .001}, etc. to its base
   * name.
   *
   * @param file existing file to back up, may not be {@code null}
   * @return the created backup file
   * @throws IOException if the backup cannot be written
   */
  static File createBackupFile(File file) throws IOException {
    if (file == null || !file.exists()) {
      throw new IllegalArgumentException("file must not be null and must exist");
    }

    String name = file.getName();
    int dotIndex = name.lastIndexOf('.');
    if (dotIndex != -1) {
      name = name.substring(0, dotIndex);
    }

    File parentFile = file.getParentFile();
    File backupFile = new File(parentFile, name + ".000");
    int backupNum = 1;

    while (backupFile.exists()) {
      String backupStr = Integer.toString(backupNum);
      if (backupStr.length() == 1) {
        backupStr = "00" + backupStr;
      } else if (backupStr.length() == 2) {
        backupStr = "0" + backupStr;
      }

      backupFile = new File(parentFile, name + "." + backupStr);
      backupNum++;
    }

    FileUtils.copyFile(file, backupFile);
    return backupFile;
  }
}
