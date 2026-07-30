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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Offline full-directory pre-migration backup (FR-018a, contracts/backup-restore.md).
 *
 * <p>Uses portable NIO path APIs only (AGENTS cross-platform rules). Caller must ensure the
 * instance is stopped before invoking. When engine lock markers are present, {@link
 * #copyRepositoryTree} refuses by default (T088 / FR-020).
 *
 * <p>Upgrade-driven FR-018a product backup (CMS/DTS migrators after a confirmed-offline server
 * check) clears stale Derby/H2 lock markers from the live tree, then copies with {@code
 * refuseIfLive=false}. Lock markers are never included in the backup artifact so a restore does not
 * reintroduce files that can block clean startup. Direct operator tools should keep the default
 * refuse-if-live behavior unless they have independently confirmed the instance is offline and call
 * {@link #clearStaleLiveMarkers(Path)}.
 */
public final class PSRepositoryOfflineBackup {

  private static final Logger LOG = Logger.getLogger(PSRepositoryOfflineBackup.class.getName());

  /**
   * When {@code true}, allow offline backup even if live engine lock markers are detected
   * (emergency override only; still unsupported for consistency).
   */
  public static final String ALLOW_LIVE_BACKUP_PROPERTY = "perc.migration.allowLiveBackup";

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
   * Refuses when the repository appears live unless {@link #ALLOW_LIVE_BACKUP_PROPERTY} is set.
   * Engine lock marker files are never copied into the backup tree.
   *
   * @param repositoryDir live repository data directory (source)
   * @param backupRoot destination directory (created if missing)
   * @param companionConfigs optional companion files (e.g. rxrepository.properties); may be empty
   * @return result with sizes; never null
   * @throws IOException on I/O failure or when live markers are present and not overridden
   */
  public static Result copyRepositoryTree(
      Path repositoryDir, Path backupRoot, Path... companionConfigs) throws IOException {
    return copyRepositoryTree(repositoryDir, backupRoot, true, companionConfigs);
  }

  /**
   * @param refuseIfLive when true, refuse if {@link #findLiveMarkers(Path)} is non-empty (unless
   *     allow-live property is set)
   */
  public static Result copyRepositoryTree(
      Path repositoryDir, Path backupRoot, boolean refuseIfLive, Path... companionConfigs)
      throws IOException {
    Objects.requireNonNull(repositoryDir, "repositoryDir");
    Objects.requireNonNull(backupRoot, "backupRoot");
    if (!Files.isDirectory(repositoryDir)) {
      throw new IOException(
          "Repository directory does not exist or is not a directory: " + repositoryDir);
    }
    if (refuseIfLive) {
      List<Path> live = findLiveMarkers(repositoryDir);
      if (!live.isEmpty()) {
        String msg =
            "Repository appears live (engine lock markers present): "
                + live
                + ". Stop the instance before offline backup (FR-020). "
                + "Emergency override only: -D"
                + ALLOW_LIVE_BACKUP_PROPERTY
                + "=true";
        if (PSInstallPropertyUtil.isTruthy(System.getProperty(ALLOW_LIVE_BACKUP_PROPERTY))) {
          LOG.warning(msg + " — proceeding due to " + ALLOW_LIVE_BACKUP_PROPERTY);
        } else {
          throw new IOException(msg);
        }
      }
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
   * Whether a file name is a known Derby/H2 engine lock marker (not durable repository data).
   *
   * @param fileName file name only (not a path); may be null
   * @return true for {@code db.lck}, {@code dbex.lck}, {@code *.lck}, {@code *.lock.db}
   */
  public static boolean isLiveMarkerFileName(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return false;
    }
    String name = fileName.toLowerCase(Locale.ROOT);
    return name.equals("db.lck")
        || name.equals("dbex.lck")
        || name.endsWith(".lck")
        || name.endsWith(".lock.db");
  }

  /**
   * Heuristic live-instance detection for offline backup (T088 / FR-020).
   *
   * <p>Looks for common Derby/H2 engine lock marker file names under the repository tree. Presence
   * strongly suggests a running engine; absence does not prove the instance is stopped (docs still
   * require stop-first).
   *
   * @param repositoryDir repository data directory
   * @return list of marker paths found; empty if none
   */
  public static List<Path> findLiveMarkers(Path repositoryDir) throws IOException {
    Objects.requireNonNull(repositoryDir, "repositoryDir");
    List<Path> markers = new ArrayList<>();
    if (!Files.isDirectory(repositoryDir)) {
      return markers;
    }
    Path root = repositoryDir.toAbsolutePath().normalize();
    // No maxDepth cap: nested repository layouts must still surface lock markers (T088).
    try (var walk = Files.walk(root)) {
      walk.filter(Files::isRegularFile)
          .forEach(
              p -> {
                if (isLiveMarkerFileName(p.getFileName().toString())) {
                  markers.add(p);
                }
              });
    }
    return List.copyOf(markers);
  }

  /**
   * Delete engine lock marker files under a repository tree after the instance has been confirmed
   * offline.
   *
   * <p>Stale markers left after an unclean stop can block Derby/H2 open, migration pump, and a
   * later restore of a backup that still contained them. Call only when process/port checks (or
   * equivalent) already show the service is stopped — never as a substitute for stopping a live
   * engine.
   *
   * @param repositoryDir repository data directory
   * @return absolute paths that were removed; empty if none found
   * @throws IOException if a marker could not be deleted
   */
  public static List<Path> clearStaleLiveMarkers(Path repositoryDir) throws IOException {
    Objects.requireNonNull(repositoryDir, "repositoryDir");
    List<Path> found = findLiveMarkers(repositoryDir);
    if (found.isEmpty()) {
      return List.of();
    }
    List<Path> removed = new ArrayList<>(found.size());
    for (Path marker : found) {
      try {
        if (Files.deleteIfExists(marker)) {
          removed.add(marker.toAbsolutePath().normalize());
        }
      } catch (IOException e) {
        throw new IOException(
            "Failed to remove stale engine lock marker (confirm instance is stopped): " + marker,
            e);
      }
    }
    if (!removed.isEmpty()) {
      LOG.warning(
          "Removed stale engine lock markers after offline confirmation (not durable data): "
              + removed);
    }
    return List.copyOf(removed);
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
    // Resolve symlinks on the source root so FOLLOW_LINKS walk paths compare against the real
    // tree (otherwise the first preVisitDirectory is the resolved target and fails startsWith).
    Path sourceAbs = source.toRealPath();
    Path targetAbs = target.toAbsolutePath().normalize();
    Files.createDirectories(targetAbs);
    // FOLLOW_LINKS materialises symlink content for offline backup, but only when the
    // resolved path stays under the source tree (no write outside target via .. segments).
    Files.walkFileTree(
        sourceAbs,
        EnumSet.of(FileVisitOption.FOLLOW_LINKS),
        Integer.MAX_VALUE,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
              throws IOException {
            Path dirAbs = dir.toRealPath();
            if (!dirAbs.startsWith(sourceAbs)) {
              return FileVisitResult.SKIP_SUBTREE;
            }
            Path rel = sourceAbs.relativize(dirAbs);
            Path destDir = targetAbs.resolve(rel).normalize();
            if (!destDir.startsWith(targetAbs)) {
              return FileVisitResult.SKIP_SUBTREE;
            }
            Files.createDirectories(destDir);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            // Never archive engine lock markers — restore of those can block clean startup.
            if (isLiveMarkerFileName(file.getFileName().toString())) {
              return FileVisitResult.CONTINUE;
            }
            Path fileAbs = file.toRealPath();
            if (!fileAbs.startsWith(sourceAbs)) {
              return FileVisitResult.CONTINUE;
            }
            Path rel = sourceAbs.relativize(fileAbs);
            Path dest = targetAbs.resolve(rel).normalize();
            if (!dest.startsWith(targetAbs)) {
              return FileVisitResult.CONTINUE;
            }
            Files.createDirectories(dest.getParent());
            Files.copy(
                fileAbs,
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
