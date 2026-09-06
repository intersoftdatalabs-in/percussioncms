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

package com.percussion.apibridge;

import com.percussion.rest.fileexplorer.FileExplorerEntry;
import com.percussion.rest.fileexplorer.FileExplorerRoot;
import com.percussion.rest.fileexplorer.IFileExplorerAdaptor;
import com.percussion.security.io.PSPathInjectionGuard;
import com.percussion.server.PSServer;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * File Explorer browse (Workbench §12.1) over operator-configured allow-listed roots.
 *
 * <p>Distinct from SY-05 application CMS/resource files and SY-02 {@code /serverconfigs}. Client
 * relative paths are validated with NIO {@link Path} before any directory listing. Traversal,
 * absolute/drive/UNC, and non-allow-listed roots never walk the filesystem.
 */
@PSSiteManageBean
@Lazy
public class FileExplorerAdaptor implements IFileExplorerAdaptor {

  private static final Logger log = LogManager.getLogger(FileExplorerAdaptor.class);

  static final String ADMIN_REQUIRED = "Admin role required to browse File Explorer";
  static final String INVALID_PATH = "Invalid path";
  static final String INVALID_ROOT = "Invalid root";

  /** server.properties key: {@code id=/abs/or/relative;id2=other}. */
  public static final String PROP_ALLOW_LISTED_ROOTS = "fileExplorer.allowListedRoots";

  private static final Pattern SAFE_ROOT_ID = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,63}");

  private final Supplier<Map<String, ConfiguredRoot>> catalog;
  private final BooleanSupplier adminChecker;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  public FileExplorerAdaptor() {
    this(FileExplorerAdaptor::loadRootsFromServerProperties, null);
  }

  /** Package-visible for tests. */
  FileExplorerAdaptor(
      Supplier<Map<String, ConfiguredRoot>> catalog, BooleanSupplier adminChecker) {
    this.catalog = catalog;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
  }

  @Override
  public List<FileExplorerRoot> listRoots() {
    requireAdmin();
    List<FileExplorerRoot> out = new ArrayList<>();
    for (ConfiguredRoot root : catalog.get().values()) {
      FileExplorerRoot dto = new FileExplorerRoot();
      dto.setId(root.id());
      dto.setDisplayName(root.displayName());
      dto.setExists(isExistingDirectory(root.directory()));
      out.add(dto);
    }
    return out;
  }

  @Override
  public List<FileExplorerEntry> listChildren(String rootId, String relativePath) {
    requireAdmin();
    if (!isSafeRootId(rootId)) {
      throw new IllegalArgumentException(INVALID_ROOT);
    }
    ConfiguredRoot root = catalog.get().get(rootId);
    if (root == null) {
      return null;
    }
    String safeRel = normalizeSafeRelativePath(relativePath);
    if (safeRel == null) {
      throw new IllegalArgumentException(INVALID_PATH);
    }
    Path resolved = resolveUnderRoot(root.directory(), safeRel);
    if (resolved == null) {
      throw new IllegalArgumentException(INVALID_PATH);
    }
    if (!Files.isDirectory(resolved)) {
      return null;
    }
    return listImmediateChildren(root.directory(), resolved, safeRel);
  }

  private List<FileExplorerEntry> listImmediateChildren(
      Path rootDir, Path directory, String parentRel) {
    List<FileExplorerEntry> out = new ArrayList<>();
    Path rootAbs = rootDir.toAbsolutePath().normalize();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
      for (Path child : stream) {
        if (child == null) {
          continue;
        }
        if (Files.isSymbolicLink(child)) {
          log.debug("Skipping symbolic link in File Explorer listing: {}", child.getFileName());
          continue;
        }
        Path childAbs = child.toAbsolutePath().normalize();
        if (!isUnderRoot(rootAbs, childAbs)) {
          continue;
        }
        String name = child.getFileName() != null ? child.getFileName().toString() : null;
        if (name == null || normalizeSafeRelativePath(name) == null) {
          continue;
        }
        String rel =
            parentRel == null || parentRel.isEmpty() ? name : parentRel + "/" + name;
        FileExplorerEntry entry = new FileExplorerEntry();
        entry.setName(name);
        entry.setRelativePath(rel);
        boolean dir = Files.isDirectory(childAbs);
        entry.setDirectory(dir);
        if (!dir && Files.isRegularFile(childAbs)) {
          try {
            entry.setSize(Files.size(childAbs));
          } catch (IOException e) {
            log.debug("Unable to read size for File Explorer child: {}", e.toString());
          }
        }
        out.add(entry);
      }
    } catch (IOException e) {
      log.warn("Failed to list File Explorer directory: {}", e.toString());
      throw new WebApplicationException(
          "Failed to list directory", e, Response.Status.INTERNAL_SERVER_ERROR);
    }
    out.sort(
        Comparator.comparing(
                (FileExplorerEntry e) -> Boolean.TRUE.equals(e.getDirectory()) ? 0 : 1)
            .thenComparing(
                FileExplorerEntry::getName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    return out;
  }

  private static boolean isExistingDirectory(Path directory) {
    if (directory == null) {
      return false;
    }
    try {
      return Files.isDirectory(directory);
    } catch (RuntimeException e) {
      return false;
    }
  }

  private void requireAdmin() {
    boolean allowed;
    try {
      allowed = adminChecker.getAsBoolean();
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      log.error("Admin check failed unexpectedly", e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
    if (!allowed) {
      throw new WebApplicationException(ADMIN_REQUIRED, Response.Status.FORBIDDEN);
    }
  }

  boolean isCurrentUserAdmin() {
    if (userService == null) {
      return false;
    }
    try {
      PSCurrentUser current = userService.getCurrentUser();
      if (current == null || StringUtils.isBlank(current.getName())) {
        return false;
      }
      return userService.isAdminUser(current.getName());
    } catch (PSDataServiceException e) {
      log.debug("Unable to resolve current user for Admin check: {}", e.getMessage());
      return false;
    }
  }

  static Map<String, ConfiguredRoot> loadRootsFromServerProperties() {
    return parseAllowListedRoots(PSServer.getProperty(PROP_ALLOW_LISTED_ROOTS), rxDirFromServer());
  }

  static Path rxDirFromServer() {
    File rx = PSServer.getRxDir();
    return rx == null ? null : rx.toPath();
  }

  /**
   * Parse {@code id=path;id2=path2}. Relative paths resolve against the CMS install root. Malformed
   * or unsafe entries are skipped (fail closed) — never walk them.
   *
   * <p>{@code ;} is the reserved entry delimiter and must not appear in a configured path. A path
   * that contains {@code ;} is split and the leftover token is skipped.
   */
  static Map<String, ConfiguredRoot> parseAllowListedRoots(String spec, Path rxDir) {
    Map<String, ConfiguredRoot> out = new LinkedHashMap<>();
    if (spec == null || spec.isBlank()) {
      return out;
    }
    String[] entries = spec.split(";");
    for (String entry : entries) {
      if (entry == null) {
        continue;
      }
      String trimmed = entry.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      int eq = trimmed.indexOf('=');
      if (eq <= 0 || eq >= trimmed.length() - 1) {
        continue;
      }
      String id = trimmed.substring(0, eq).trim();
      String pathText = trimmed.substring(eq + 1).trim();
      if (!isSafeRootId(id) || pathText.isEmpty()) {
        continue;
      }
      Path dir = resolveConfiguredPath(pathText, rxDir);
      if (dir == null) {
        continue;
      }
      out.putIfAbsent(id, new ConfiguredRoot(id, id, dir));
    }
    return out;
  }

  /**
   * Operator-trusted configured path. May be absolute (including a drive letter). Rejects {@code
   * ..} in the configured text so a properties typo cannot escape the install tree via relative
   * resolve.
   */
  static Path resolveConfiguredPath(String pathText, Path rxDir) {
    if (pathText == null || pathText.isBlank() || pathText.indexOf('\0') >= 0) {
      return null;
    }
    if (pathText.contains("..")) {
      return null;
    }
    Path p;
    try {
      p = Path.of(pathText);
    } catch (InvalidPathException e) {
      return null;
    }
    if (!p.isAbsolute()) {
      if (rxDir == null) {
        return null;
      }
      p = rxDir.resolve(p);
    }
    return p.toAbsolutePath().normalize();
  }

  static boolean isSafeRootId(String id) {
    return id != null && SAFE_ROOT_ID.matcher(id).matches();
  }

  /**
   * Normalize a client relative path under a root. Blank means the root itself ({@code ""}).
   * Rejects {@code ..}, {@code .}, absolute, drive-letter, UNC, NUL, empty segments, and {@code :}
   * in a segment (cross-platform: {@code :} is a drive-letter marker on Windows even though it is
   * legal in Linux filenames). Returns {@code null} when unsafe — callers must not echo the raw
   * input.
   *
   * <p>Segments are validated <em>before</em> {@link Path#normalize()} so {@code a/../b} cannot
   * collapse into an apparently safe leaf.
   */
  static String normalizeSafeRelativePath(String relativePath) {
    if (relativePath == null || relativePath.isBlank()) {
      return "";
    }
    if (relativePath.indexOf('\0') >= 0) {
      return null;
    }
    String unified = relativePath.trim().replace('\\', '/');
    if (unified.startsWith("/") || unified.startsWith("~")) {
      return null;
    }
    if (unified.length() >= 2 && unified.charAt(1) == ':') {
      return null;
    }
    if (unified.startsWith("//")) {
      return null;
    }
    String[] rawSegments = unified.split("/", -1);
    if (rawSegments.length == 0) {
      return null;
    }
    StringBuilder apiPath = new StringBuilder();
    for (int i = 0; i < rawSegments.length; i++) {
      String segment = rawSegments[i];
      if (segment == null || segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
        return null;
      }
      if (segment.indexOf(':') >= 0) {
        return null;
      }
      try {
        PSPathInjectionGuard.requireSafeFileName(segment);
      } catch (IllegalArgumentException e) {
        return null;
      }
      if (i > 0) {
        apiPath.append('/');
      }
      apiPath.append(segment);
    }
    try {
      if (Path.of(apiPath.toString()).isAbsolute()) {
        return null;
      }
    } catch (RuntimeException e) {
      return null;
    }
    return apiPath.toString();
  }

  /**
   * Resolve a already-validated relative API path under {@code root}. Returns {@code null} if the
   * NIO result would escape the root.
   */
  static Path resolveUnderRoot(Path root, String safeRelativeApiPath) {
    if (root == null) {
      return null;
    }
    Path rootAbs = root.toAbsolutePath().normalize();
    Path resolved = rootAbs;
    if (safeRelativeApiPath != null && !safeRelativeApiPath.isEmpty()) {
      for (String segment : safeRelativeApiPath.split("/")) {
        resolved = resolved.resolve(segment);
      }
    }
    resolved = resolved.normalize();
    if (!isUnderRoot(rootAbs, resolved)) {
      return null;
    }
    return resolved;
  }

  static boolean isUnderRoot(Path rootAbs, Path candidateAbs) {
    if (rootAbs == null || candidateAbs == null) {
      return false;
    }
    Path r = rootAbs.toAbsolutePath().normalize();
    Path c = candidateAbs.toAbsolutePath().normalize();
    for (Path p = c; p != null; p = p.getParent()) {
      if (p.equals(r)) {
        return true;
      }
    }
    return false;
  }

  record ConfiguredRoot(String id, String displayName, Path directory) {}
}
