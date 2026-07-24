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
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Exclusive migrator lock via {@link FileChannel#tryLock()} (QC-019).
 *
 * <p>Second process attempting the same lock fails with a clear message; does not wait forever.
 * Paths use portable NIO ({@link Path}).
 */
public final class PSMigratorLock implements AutoCloseable {

  /** Default lock file name under install-root installer config. */
  public static final String DEFAULT_LOCK_RELATIVE =
      Path.of("rxconfig", "Installer", "embedded-repository-migration.lock").toString();

  private final Path lockPath;
  private final FileChannel channel;
  private final FileLock lock;

  private PSMigratorLock(Path lockPath, FileChannel channel, FileLock lock) {
    this.lockPath = lockPath;
    this.channel = channel;
    this.lock = lock;
  }

  /**
   * Attempt to acquire an exclusive lock under the install root.
   *
   * @param installRoot CMS or DTS install root (must exist)
   * @return held lock; caller must {@link #close()}
   * @throws IOException if the lock file cannot be created/opened
   * @throws MigratorLockException if another process already holds the lock
   */
  public static PSMigratorLock tryAcquire(Path installRoot)
      throws IOException, MigratorLockException {
    Objects.requireNonNull(installRoot, "installRoot");
    Path lockFile = installRoot.resolve(DEFAULT_LOCK_RELATIVE).normalize();
    Path parent = lockFile.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    FileChannel channel =
        FileChannel.open(
            lockFile,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.READ);
    FileLock lock;
    try {
      lock = channel.tryLock();
    } catch (java.nio.channels.OverlappingFileLockException e) {
      // Same JVM already holds the lock (tryLock is per-process for cross-process only).
      try {
        channel.close();
      } catch (IOException ignored) {
        // best-effort
      }
      throw new MigratorLockException(
          "Another embedded repository migration is already running for install root: "
              + installRoot
              + " (lock file: "
              + lockFile
              + "; overlapping lock in this JVM)",
          e);
    }
    if (lock == null) {
      try {
        channel.close();
      } catch (IOException ignored) {
        // best-effort
      }
      throw new MigratorLockException(
          "Another embedded repository migration is already running for install root: "
              + installRoot
              + " (lock file: "
              + lockFile
              + ")");
    }
    return new PSMigratorLock(lockFile, channel, lock);
  }

  /**
   * @return absolute path of the lock file
   */
  public Path getLockPath() {
    return lockPath;
  }

  @Override
  public void close() throws IOException {
    try {
      if (lock != null && lock.isValid()) {
        lock.release();
      }
    } finally {
      if (channel != null && channel.isOpen()) {
        channel.close();
      }
    }
  }

  /** Thrown when exclusive lock cannot be acquired. */
  public static final class MigratorLockException extends Exception {
    public MigratorLockException(String message) {
      super(message);
    }

    public MigratorLockException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
