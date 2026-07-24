/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.install;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.Objects;

/**
 * Offline full-directory pre-migration backup (FR-018a, contracts/backup-restore.md).
 *
 * <p>Uses portable NIO path APIs only (AGENTS cross-platform rules). Caller must ensure the
 * instance is stopped before invoking.
 */
public final class PSRepositoryOfflineBackup {

  private PSRepositoryOfflineBackup() {}

  /**
   * Result of a product offline backup.
   *
   * @param backupRoot destination directory that received the copy
   * @param bytesCopied approximate total bytes copied
   * @param filesCopied number of regular files copied
   */
  public record Result(Path backupRoot, long bytesCopied, long filesCopied) {}

  /**
   * Copy repository data directory and optional companion config files into {@code backupRoot}.
   *
   * @param repositoryDir live repository data directory (source)
   * @param backupRoot destination directory (created if missing)
   * @param companionConfigs optional companion files (e.g. rxrepository.properties); may be empty
   * @return result with sizes; never null
   * @throws IOException on I/O failure
   */
  public static Result copyRepositoryTree(
      Path repositoryDir, Path backupRoot, Path... companionConfigs) throws IOException {
    Objects.requireNonNull(repositoryDir, "repositoryDir");
    Objects.requireNonNull(backupRoot, "backupRoot");
    if (!Files.isDirectory(repositoryDir)) {
      throw new IOException("Repository directory does not exist or is not a directory: "
          + repositoryDir);
    }
    Files.createDirectories(backupRoot);
    Path dataTarget = backupRoot.resolve("repository-data");
    CopyStats stats = copyTree(repositoryDir, dataTarget);

    if (companionConfigs != null) {
      Path configTarget = backupRoot.resolve("companion-config");
      Files.createDirectories(configTarget);
      for (Path companion : companionConfigs) {
        if (companion == null) {
          continue;
        }
        if (!Files.isRegularFile(companion)) {
          continue;
        }
        Path dest = configTarget.resolve(companion.getFileName().toString());
        Files.copy(companion, dest, StandardCopyOption.REPLACE_EXISTING);
        stats.files++;
        stats.bytes += Files.size(dest);
      }
    }
    return new Result(backupRoot, stats.bytes, stats.files);
  }

  /**
   * Minimum free space check before pump/backup (QC-021).
   *
   * @param path path on the volume to check
   * @param requiredBytes minimum free bytes required
   * @return true if free space is at least {@code requiredBytes}
   * @throws IOException if free space cannot be determined
   */
  public static boolean hasSufficientDiskSpace(Path path, long requiredBytes) throws IOException {
    Objects.requireNonNull(path, "path");
    Path probe = path;
    while (probe != null && !Files.exists(probe)) {
      probe = probe.getParent();
    }
    if (probe == null) {
      throw new IOException("Cannot resolve existing path for disk space check: " + path);
    }
    long free = Files.getFileStore(probe).getUsableSpace();
    return free >= requiredBytes;
  }

  private static CopyStats copyTree(Path source, Path target) throws IOException {
    CopyStats stats = new CopyStats();
    // FOLLOW_LINKS so symlinked repository subtrees (if present) are copied as content,
    // not as empty placeholder directories. Offline backup only; operator has stopped the
    // instance (FR-020).
    Files.walkFileTree(
        source,
        EnumSet.of(FileVisitOption.FOLLOW_LINKS),
        Integer.MAX_VALUE,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
              throws IOException {
            Path rel = source.relativize(dir);
            Path destDir = target.resolve(rel.toString());
            Files.createDirectories(destDir);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            Path rel = source.relativize(file);
            Path dest = target.resolve(rel.toString());
            Files.createDirectories(dest.getParent());
            Files.copy(
                file,
                dest,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES);
            stats.files++;
            stats.bytes += attrs.size();
            return FileVisitResult.CONTINUE;
          }
        });
    return stats;
  }

  private static final class CopyStats {
    long files;
    long bytes;
  }
}
