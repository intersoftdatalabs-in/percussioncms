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
package com.percussion.services.virtualsite;

import com.percussion.services.sitemgr.IPSSite;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/**
 * Selects the Site filesystem publish target and copies a Virtual Site build tree there.
 *
 * <p>The target is {@link IPSSite#getRoot()} (the Site publishing filesystem location), resolved
 * with portable NIO {@link Path} I/O. Does not invent OS path separators.
 */
public final class PSVirtualSiteFilesystemPublisher {

  /** Build-staging directory that is not copied to the published site. */
  public static final String META_DIR_NAME = "_meta";

  private PSVirtualSiteFilesystemPublisher() {}

  /**
   * Resolve the Site filesystem publish target from {@link IPSSite#getRoot()}.
   *
   * @param site CMS Site, not null
   * @return normalized target path (not required to exist yet)
   * @throws VirtualSiteException when root is blank, unsafe, or overlaps the Virtual source tree
   */
  public static Path selectFilesystemTarget(IPSSite site) throws VirtualSiteException {
    if (site == null) {
      throw new VirtualSiteException("Site is required to select a filesystem publish target.");
    }
    String raw = site.getRoot();
    if (StringUtils.isBlank(raw)) {
      throw new VirtualSiteException(
          "Site filesystem publish root is not configured. Set the Site publishing filesystem"
              + " location (Site root) to a directory, then publish again.");
    }
    Path target;
    try {
      target = Path.of(raw.trim()).normalize();
    } catch (InvalidPathException e) {
      throw new VirtualSiteException(
          "Site filesystem publish root is not a valid path: '" + raw.trim() + "'.", e);
    }
    if (!PSVirtualSiteHelper.isSafeRootPath(target)) {
      throw new VirtualSiteException(
          "Site filesystem publish root must be a non-empty path with no '..' segments after"
              + " normalize. Rejected: '"
              + raw.trim()
              + "'.");
    }
    Optional<Path> source = PSVirtualSiteHelper.rootPath(site);
    if (source.isPresent()) {
      Path src = source.get().normalize();
      if (sameOrNested(src, target)) {
        throw new VirtualSiteException(
            "Site filesystem publish root must be distinct from virtual.rootPath (publishing would"
                + " overwrite or nest inside the Markdown source tree). Configure a dedicated"
                + " publishing directory.");
      }
    }
    return target;
  }

  /**
   * Copy assembled static files from {@code buildOutput} into {@code target}.
   *
   * <p>Skips the {@code _meta} participant-registry directory. Overwrites existing files; does not
   * delete stale files already under the target.
   *
   * @param buildOutput directory written by {@link PSVirtualSiteBuildService}
   * @param target Site filesystem publish root
   * @return copy summary
   * @throws VirtualSiteException when inputs are missing, unsafe, or overlap
   * @throws IOException on filesystem I/O failure
   */
  public static PSVirtualSitePublishCopyResult copyBuildToTarget(Path buildOutput, Path target)
      throws VirtualSiteException, IOException {
    if (buildOutput == null || !Files.isDirectory(buildOutput)) {
      throw new VirtualSiteException(
          "Virtual Site build output is missing or is not a directory: '" + buildOutput + "'.");
    }
    if (target == null || !PSVirtualSiteHelper.isSafeRootPath(target)) {
      throw new VirtualSiteException(
          "Site filesystem publish root is missing or unsafe: '" + target + "'.");
    }
    if (Files.exists(target) && !Files.isDirectory(target)) {
      throw new VirtualSiteException(
          "Site filesystem publish root exists and is not a directory: '" + target + "'.");
    }

    Path srcAbs = buildOutput.toAbsolutePath().normalize();
    Path destAbs = target.toAbsolutePath().normalize();
    if (sameOrNested(srcAbs, destAbs)) {
      throw new VirtualSiteException(
          "Build output and Site filesystem publish root overlap. Use a distinct publish directory"
              + " (not the build staging tree).");
    }

    Files.createDirectories(destAbs); // codeql[java/path-injection]

    int copied = 0;
    try (Stream<Path> walk = Files.walk(srcAbs)) {
      for (Path src : (Iterable<Path>) walk::iterator) {
        Path rel = srcAbs.relativize(src);
        if (isMetaTree(rel)) {
          continue;
        }
        Path dest = destAbs.resolve(rel).normalize(); // codeql[java/path-injection]
        if (!dest.startsWith(destAbs)) {
          throw new VirtualSiteException(
              "Refusing to write outside the Site filesystem publish root: '" + dest + "'.");
        }
        if (Files.isDirectory(src)) {
          Files.createDirectories(dest); // codeql[java/path-injection]
        } else if (Files.isRegularFile(src)) {
          Path parent = dest.getParent();
          if (parent != null) {
            Files.createDirectories(parent); // codeql[java/path-injection]
          }
          Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING); // codeql[java/path-injection]
          copied++;
        }
      }
    }
    return new PSVirtualSitePublishCopyResult(destAbs, copied);
  }

  /**
   * Copy assembled static files and return only the file count.
   *
   * <p>Callers that must not mention {@link PSVirtualSitePublishCopyResult} in method signatures
   * (Spring lookup-method resolution / {@code sitesAdaptor} bean load) should use this entry.
   *
   * @param buildOutput directory written by {@link PSVirtualSiteBuildService}
   * @param target Site filesystem publish root
   * @return number of regular files written
   * @throws VirtualSiteException when inputs are missing, unsafe, or overlap
   * @throws IOException on filesystem I/O failure
   */
  public static int copyBuildFileCountToTarget(Path buildOutput, Path target)
      throws VirtualSiteException, IOException {
    return copyBuildToTarget(buildOutput, target).filesCopied();
  }

  static boolean isMetaTree(Path relative) {
    if (relative == null || relative.getNameCount() < 1) {
      return false;
    }
    Path first = relative.getName(0);
    return first != null && META_DIR_NAME.equals(first.toString());
  }

  /**
   * True when the two paths are equal after normalize, or either is a child of the other.
   *
   * <p>Does not call {@code toAbsolutePath()} so relative configs do not depend on process cwd.
   */
  static boolean sameOrNested(Path a, Path b) {
    if (a == null || b == null) {
      return false;
    }
    Path left = a.normalize();
    Path right = b.normalize();
    if (left.equals(right)) {
      return true;
    }
    try {
      return left.startsWith(right) || right.startsWith(left);
    } catch (RuntimeException e) {
      return false;
    }
  }
}
