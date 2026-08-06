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
package com.percussion.security.validation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class for validating file paths against path traversal and ZipSlip attacks (CWE-22:
 * Improper Limitation of a Pathname to a Restricted Directory).
 *
 * <p>Provides methods to safely construct and validate file paths, preventing attackers from using
 * path traversal patterns (e.g., `../../../etc/passwd`) or symbolic links to escape the allowed
 * directory.
 *
 * <p><strong>Threat Model:</strong>
 *
 * <ul>
 *   <li><strong>Path Traversal:</strong> User input like `../../etc/passwd` combined with a base
 *       directory to escape that directory's bounds
 *   <li><strong>ZipSlip:</strong> Extracted zip archives with entries like `../../shell.jsp` that
 *       escape the target extraction directory
 *   <li><strong>Symlink Attacks:</strong> Symbolic links pointing outside allowed directory to
 *       read/write arbitrary files
 * </ul>
 *
 * <p><strong>Key Security Properties:</strong>
 *
 * <ul>
 *   <li>✅ Resolves `..` and `.` components before comparison
 *   <li>✅ Prevents absolute paths in user input (e.g., `/etc/passwd`)
 *   <li>✅ Detects symlinks that escape bounds (when `checkSymlinks=true`)
 *   <li>✅ Validates against real filesystem canonical paths, not string manipulation
 * </ul>
 *
 * <p><strong>Usage Patterns:</strong>
 *
 * <p><b>Pattern 1: User-provided relative filename in a controlled directory</b>
 *
 * <pre>
 * // User uploads file to themes/
 * String userFilename = request.getParameter("filename");  // e.g., "../../etc/passwd"
 * File baseDir = new File("/app/themes");
 * File safeFile = PathValidation.constructSafePath(baseDir, userFilename);
 * // Returns resolved path, throws SecurityException if escape attempted
 * </pre>
 *
 * <p><b>Pattern 2: Extract zip without ZipSlip</b>
 *
 * <pre>
 * ZipFile zip = new ZipFile(uploadedZip);
 * Enumeration&lt;? extends ZipEntry&gt; entries = zip.entries();
 * while (entries.hasMoreElements()) {
 *   String entryName = entries.nextElement().getName();
 *   File target = PathValidation.constructSafePath(extractDir, entryName);
 *   // Safe to extract to target
 * }
 * </pre>
 *
 * <p><b>Pattern 3: Directory access control</b>
 *
 * <pre>
 * File userRequestedDir = new File(System.getProperty("user.dir"), userPath);
 * File safe = PathValidation.validatePathWithinDirectory(userRequestedDir, baseDir);
 * // Throws SecurityException if userRequestedDir not under baseDir
 * </pre>
 *
 * <p>Reference: <a href="https://cwe.mitre.org/data/definitions/22.html">CWE-22: Improper
 * Limitation of a Pathname to a Restricted Directory</a> <br>
 * <a href="https://snyk.io/research/zip-slip-vulnerability/">ZipSlip Vulnerability</a>
 *
 * @author Percussion Security Team
 * @since Java 21
 */
/**
 * Validates file paths to prevent path traversal and other security vulnerabilities. Provides
 * utilities for safely handling file paths and preventing attacks like directory traversal and
 * ZipSlip.
 *
 * @author Percussion Security Team
 */
public class PathValidation {

  /** Private constructor to prevent instantiation. */
  private PathValidation() {}

  private static final Logger log = LogManager.getLogger(PathValidation.class);

  /**
   * Checks if a path should be treated as absolute for security purposes, across all platforms.
   *
   * <p>On Windows, {@link File#isAbsolute()} only returns {@code true} for paths that begin with a
   * drive letter (e.g. {@code C:\}) or a UNC root (e.g. {@code \\server\share}). A string like
   * {@code /etc/passwd} is considered <em>relative</em> by {@link File#isAbsolute()} on Windows
   * even though it is clearly an attempt to address an absolute location. For path-traversal
   * detection we must reject any input that <em>looks</em> absolute regardless of the host OS.
   *
   * <p>Note: the drive-letter branch is only checked on platforms whose default filesystem is
   * case-insensitive (Windows). On Linux/macOS, a colon is a legal filename character so a relative
   * component like {@code C:report} or {@code x:data} is not an attempt at an absolute path and
   * must not be rejected.
   *
   * @param path the path string to inspect
   * @return {@code true} if the path starts with a drive letter, a UNC root, or a leading separator
   */
  private static boolean looksAbsolute(String path) {
    if (path == null || path.isEmpty()) {
      return false;
    }
    if (path.startsWith("/") || path.startsWith("\\")) {
      return true;
    }
    if (isCaseInsensitiveFs()
        && path.length() >= 2
        && Character.isLetter(path.charAt(0))
        && path.charAt(1) == ':') {
      return true;
    }
    return false;
  }

  /**
   * Tests whether {@code child} is the same path as, or is located within, {@code parent}. The
   * comparison is performed using canonical paths and is case-insensitive on platforms where the
   * underlying filesystem is case-insensitive (e.g. Windows).
   *
   * @param child the candidate path
   * @param parent the directory that should contain {@code child}
   * @return {@code true} if {@code child} resolves to {@code parent} or a descendant of it
   * @throws IOException if either path cannot be canonicalized
   */
  private static boolean isWithin(File child, File parent) throws IOException {
    String childCanonical = child.getCanonicalPath();
    String parentCanonical = parent.getCanonicalPath();
    if (isCaseInsensitiveFs()) {
      // Use Locale.ROOT to avoid locale-dependent case folding (e.g. Turkish locale
      // folds 'I' to 'ı' rather than 'i'), which would make containment checks
      // non-deterministic for a security-sensitive decision.
      childCanonical = childCanonical.toLowerCase(Locale.ROOT);
      parentCanonical = parentCanonical.toLowerCase(Locale.ROOT);
    }
    if (childCanonical.equals(parentCanonical)) {
      return true;
    }
    return childCanonical.startsWith(parentCanonical + File.separator);
  }

  /**
   * Returns {@code true} when the host filesystem treats path components as case-insensitive.
   *
   * <p>Windows (NTFS, FAT) and the default macOS filesystems (APFS, HFS+) are case-insensitive,
   * while typical Linux filesystems (ext4, xfs, btrfs) are case-sensitive. We inspect the {@code
   * os.name} system property rather than relying solely on {@link File#separatorChar}, so that
   * macOS is classified correctly and containment checks remain consistent with the underlying
   * filesystem semantics.
   *
   * <p>The result is cached on first read because {@code os.name} cannot change for the lifetime of
   * the JVM.
   */
  private static final boolean CASE_INSENSITIVE_FS = computeCaseInsensitiveFs();

  private static boolean isCaseInsensitiveFs() {
    return CASE_INSENSITIVE_FS;
  }

  private static boolean computeCaseInsensitiveFs() {
    if (File.separatorChar == '\\') {
      return true;
    }
    String os = System.getProperty("os.name", "");
    return os.regionMatches(true, 0, "mac", 0, 3) || os.regionMatches(true, 0, "darwin", 0, 6);
  }

  /**
   * Constructs a safe file path by combining a base directory with a user-supplied relative path,
   * ensuring the resulting path remains within the base directory.
   *
   * <p>This method prevents path traversal attacks (CWE-22) by:
   *
   * <ol>
   *   <li>Rejecting absolute paths in userPath
   *   <li>Resolving `..` and `.` components
   *   <li>Verifying final path is within baseDir
   * </ol>
   *
   * @param baseDir The trusted base directory. Must be a directory.
   * @param userPath The user-supplied relative path component (e.g., filename or relative path).
   *     May contain `/` for subdirectories but cannot use `..` to escape baseDir.
   * @return A File object pointing to the safe path within baseDir
   * @throws SecurityException if:
   *     <ul>
   *       <li>userPath is absolute
   *       <li>The resolved path escapes baseDir (e.g., contains `../..`)
   *       <li>baseDir doesn't exist or is not a directory
   *     </ul>
   *
   * @throws IllegalArgumentException if userPath is null or empty
   *     <p><strong>Example:</strong>
   *     <pre>
   * File baseDir = new File("/app/uploads");
   * File safe = PathValidation.constructSafePath(baseDir, "user-data.txt");
   * // Returns /app/uploads/user-data.txt
   *
   * File safe = PathValidation.constructSafePath(baseDir, "../../etc/passwd");
   * // Throws SecurityException - escape attempted
   * </pre>
   */
  public static File constructSafePath(File baseDir, String userPath) {
    return constructSafePath(baseDir, userPath, false);
  }

  /**
   * Constructs a safe file path with optional symlink checking.
   *
   * @param baseDir The trusted base directory
   * @param userPath The user-supplied relative path
   * @param checkSymlinks If true, additionally rejects any resolved path component that is a
   *     symbolic link whose target escapes {@code baseDir}. The base containment check (line {@link
   *     #isWithin}) always runs and already guarantees the final path is within {@code baseDir};
   *     this flag adds a per-component symlink walk that explicitly identifies and rejects symlinks
   *     pointing outside, suitable for high-security deployments.
   * @return The canonical {@link File} for the safe path within {@code baseDir}. The canonical form
   *     is returned for consistency with {@link #validatePathWithinDirectory(File, File)}; the
   *     underlying security guarantees are unchanged because canonicalization is already used by
   *     the containment check.
   * @throws SecurityException if escape/symlink attack detected
   * @throws IllegalArgumentException if inputs invalid
   */
  public static File constructSafePath(File baseDir, String userPath, boolean checkSymlinks) {
    // Input validation
    if (baseDir == null) {
      throw new IllegalArgumentException("baseDir cannot be null");
    }
    if (userPath == null || userPath.trim().isEmpty()) {
      throw new IllegalArgumentException("userPath cannot be null or empty");
    }
    if (!baseDir.isDirectory()) {
      throw new IllegalArgumentException("baseDir must be an existing directory: " + baseDir);
    }

    // Reject absolute paths in user input. Use a platform-agnostic check so that
    // Unix-style paths like "/etc/passwd" are also rejected on Windows even though
    // File.isAbsolute() considers them relative on that platform.
    if (looksAbsolute(userPath) || new File(userPath).isAbsolute()) {
      log.warn("Path traversal attempt: absolute path in userPath: {}", userPath);
      throw new SecurityException(
          "User-supplied path cannot be absolute: " + userPath + " (CWE-22)");
    }

    try {
      // Get canonical paths (resolves .. and symlinks)
      File combinedPath = new File(baseDir, userPath);

      // Check that resolved path is within baseDir (case-insensitive on Windows)
      if (!isWithin(combinedPath, baseDir)) {
        log.warn(
            "Path traversal attempt: {} -> {} (outside allowed base: {})",
            userPath,
            combinedPath.getCanonicalPath(),
            baseDir.getCanonicalPath());
        throw new SecurityException(
            "Resolved path escapes base directory (CWE-22 path traversal): " + userPath);
      }

      // Additional per-component symlink check if requested. This is distinct from the
      // canonical containment check above: it walks from combinedPath up to baseDir and
      // rejects any existing component that is a symbolic link whose canonical target
      // resolves outside baseDir. The previous implementation duplicated the isWithin
      // containment check and was unreachable.
      if (checkSymlinks) {
        rejectEscapingSymlinks(combinedPath, baseDir, userPath);
      }

      // Return the canonical form for consistency with validatePathWithinDirectory. Callers
      // that need the original "as-supplied" path string can call File.getPath() on the
      // input File instead. Canonicalization was already performed by the containment
      // check above, so the returned File is guaranteed to be normalized and within baseDir.
      return combinedPath.getCanonicalFile();
    } catch (IOException e) {
      log.error("Error validating path: {} + {}: {}", baseDir, userPath, e.getMessage());
      throw new SecurityException("Failed to validate path security: " + e.getMessage(), e);
    }
  }

  /**
   * Walks from {@code resolved} up to (and including) {@code baseDir}, rejecting any existing path
   * component that is a symbolic link whose canonical target escapes {@code baseDir}. This
   * complements {@link #isWithin(File, File)} (which verifies the final canonical path) by
   * providing per-component attribution of the offending symlink.
   *
   * @param resolved the path returned by {@code new File(baseDir, userPath)}
   * @param baseDir the trusted base directory
   * @param userPath the original user-supplied path, used only for diagnostic messages
   * @throws IOException if a canonical path cannot be resolved
   * @throws SecurityException if an escaping symbolic link is encountered
   */
  private static void rejectEscapingSymlinks(File resolved, File baseDir, String userPath)
      throws IOException {
    File baseCanonical = baseDir.getCanonicalFile();
    File cursor = resolved.getAbsoluteFile();
    while (cursor != null) {
      Path cursorPath = cursor.toPath();
      if (Files.exists(cursorPath) && Files.isSymbolicLink(cursorPath)) {
        // Use Files.readSymbolicLink rather than File.getCanonicalFile() to ensure
        // the link target is resolved even on platforms (e.g. Windows) where the
        // legacy File API may not follow symbolic links.
        Path target;
        try {
          target = Files.readSymbolicLink(cursorPath);
        } catch (IOException e) {
          log.warn("Unable to read symlink target: {} for user path: {}", cursorPath, userPath);
          throw new SecurityException("Symlink escapes base directory bounds: " + userPath);
        }
        Path resolvedTarget = target.isAbsolute() ? target : cursorPath.getParent().resolve(target);
        resolvedTarget = resolvedTarget.normalize();
        if (!isWithin(resolvedTarget.toFile(), baseCanonical)) {
          log.warn(
              "Symlink escape attempt: {} -> {} (outside allowed base {})",
              cursor,
              resolvedTarget,
              baseCanonical);
          throw new SecurityException("Symlink escapes base directory bounds: " + userPath);
        }
      }
      if (cursor.equals(baseCanonical) || cursor.getAbsoluteFile().equals(baseCanonical)) {
        return;
      }
      cursor = cursor.getParentFile();
    }
  }

  /**
   * Validates that a given path is within a specified directory.
   *
   * <p>Useful for validating user-requested directory access before performing operations.
   *
   * @param pathToCheck The file/directory to validate
   * @param allowedParentDir The parent directory that should contain pathToCheck
   * @return The pathToCheck if validation succeeds
   * @throws SecurityException if pathToCheck is not within allowedParentDir
   * @throws IllegalArgumentException if inputs invalid
   *     <p><strong>Example:</strong>
   *     <pre>
   * File userDir = new File(System.getProperty("user.dir"), userInput);
   * File safe = PathValidation.validatePathWithinDirectory(userDir, new File("/app/data"));
   * // Throws SecurityException if userDir not under /app/data
   * </pre>
   */
  public static File validatePathWithinDirectory(File pathToCheck, File allowedParentDir) {
    if (pathToCheck == null) {
      throw new IllegalArgumentException("pathToCheck cannot be null");
    }
    if (allowedParentDir == null) {
      throw new IllegalArgumentException("allowedParentDir cannot be null");
    }
    if (!allowedParentDir.isDirectory()) {
      throw new IllegalArgumentException(
          "allowedParentDir must be an existing directory: " + allowedParentDir);
    }

    try {
      if (!isWithin(pathToCheck, allowedParentDir)) {
        log.warn(
            "Path outside allowed directory: {} not in {}",
            pathToCheck.getCanonicalPath(),
            allowedParentDir.getCanonicalPath());
        throw new SecurityException(
            "Path is outside allowed directory: "
                + pathToCheck
                + " (not within "
                + allowedParentDir
                + ")");
      }

      return pathToCheck.getCanonicalFile();
    } catch (IOException e) {
      log.error("Error validating path bounds: {}: {}", pathToCheck, e.getMessage());
      throw new SecurityException("Failed to validate path: " + e.getMessage(), e);
    }
  }

  /**
   * Safely combines multiple path components, preventing directory traversal.
   *
   * <p>Each component is validated before combining, providing defense-in-depth.
   *
   * <p>Components do not need to exist on disk. This method validates that the final composed path
   * cannot escape baseDir, even if intermediate paths don't exist.
   *
   * @param baseDir Starting directory
   * @param pathComponents User-supplied path components (e.g., directory names, filenames)
   * @return Safe combined path
   * @throws SecurityException if any component attempts escape
   *     <p><strong>Example:</strong>
   *     <pre>
   * File result = PathValidation.combineSafePaths(
   *     baseDir,
   *     "themes",            // User theme name
   *     "css",               // Subdirectory
   *     "theme-style.css"    // Filename
   * );
   * // Safely combines all components with validation at each step
   * </pre>
   */
  public static File combineSafePaths(File baseDir, String... pathComponents) {
    if (baseDir == null) {
      throw new IllegalArgumentException("baseDir cannot be null");
    }
    if (!baseDir.isDirectory()) {
      throw new IllegalArgumentException("baseDir must be a directory");
    }

    // Build the combined path from all components
    File current = baseDir;
    for (String component : pathComponents) {
      if (component == null || component.isEmpty()) {
        continue;
      }
      // Validate each component before adding
      // Allow path that may not exist yet by validating canonically

      // Check for escape attempts. Use a platform-agnostic absolute-path check so that
      // Unix-style paths like "/etc/passwd" are rejected on Windows as well.
      if (looksAbsolute(component) || new File(component).isAbsolute()) {
        log.warn("Path traversal attempt: absolute component: {}", component);
        throw new SecurityException("Component cannot be absolute: " + component + " (CWE-22)");
      }

      if (component.contains("..") || component.startsWith(".") && !component.equals(".")) {
        log.warn("Path traversal attempt: escape component: {}", component);
        throw new SecurityException("Component attempts path escape: " + component + " (CWE-22)");
      }

      current = new File(current, component);
    }

    // Validate final combined path is within baseDir (case-insensitive on Windows)
    try {
      if (!isWithin(current, baseDir)) {
        throw new SecurityException(
            "Combined path escapes baseDir bounds: " + current.getCanonicalPath() + " (CWE-22)");
      }
    } catch (IOException e) {
      throw new SecurityException("Cannot validate path: " + e.getMessage(), e);
    }

    // Return the canonical form for consistency with constructSafePath and
    // validatePathWithinDirectory. Components that don't exist yet may not be canonicalizable;
    // in that case return the as-built File. The containment check above already used
    // canonical paths to guarantee safety.
    try {
      return current.getCanonicalFile();
    } catch (IOException e) {
      return current;
    }
  }

  /**
   * Validates that a filename contains only safe characters (no path separators or control
   * characters).
   *
   * <p>Use this for simple filename validation when you only accept single-level filenames (no
   * directory structure).
   *
   * @param filename The filename to validate
   * @return true if filename is safe (no path separators, null bytes, etc.)
   */
  public static boolean isValidFilename(String filename) {
    if (filename == null || filename.isEmpty()) {
      return false;
    }

    // Reject path separators and parent directory references
    if (filename.contains(File.separator) || filename.contains("/") || filename.contains("\\")) {
      return false;
    }
    if (filename.contains("..")) {
      return false;
    }

    // Reject null bytes and other control characters
    for (int i = 0; i < filename.length(); i++) {
      char c = filename.charAt(i);
      if (Character.isISOControl(c) || c == '\0') {
        return false;
      }
    }

    return true;
  }

  /**
   * SecurityException thrown when path validation fails.
   *
   * <p>Indicates a potential path traversal (CWE-22) or ZipSlip attack.
   */
  /** Security exception for path validation failures. */
  public static class SecurityException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new security exception.
     *
     * @param message the exception message
     */
    public SecurityException(String message) {
      super(message);
    }

    /**
     * Creates a new security exception with a cause.
     *
     * @param message the exception message
     * @param cause the underlying cause
     */
    public SecurityException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
