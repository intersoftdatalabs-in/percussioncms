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
package com.percussion.security.validation;

import java.io.File;
import java.io.IOException;
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
   * @param path the path string to inspect
   * @return {@code true} if the path starts with a drive letter, a UNC root, or a leading
   *     separator
   */
  private static boolean looksAbsolute(String path) {
    if (path == null || path.isEmpty()) {
      return false;
    }
    if (path.startsWith("/") || path.startsWith("\\")) {
      return true;
    }
    if (path.length() >= 2 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':') {
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
      childCanonical = childCanonical.toLowerCase();
      parentCanonical = parentCanonical.toLowerCase();
    }
    if (childCanonical.equals(parentCanonical)) {
      return true;
    }
    return childCanonical.startsWith(parentCanonical + File.separator);
  }

  /**
   * Returns {@code true} when the host filesystem treats path components as case-insensitive.
   *
   * <p>Currently this is hard-coded to {@code true} on Windows. On other platforms the JDK reports
   * the underlying {@code FileSystem} as case-sensitive, so we keep the default case-sensitive
   * behaviour for those systems.
   */
  private static boolean isCaseInsensitiveFs() {
    return File.separatorChar == '\\';
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
   * @param checkSymlinks If true, detects symlinks that escape baseDir. Slower but more secure for
   *     high-security environments.
   * @return Safe File object within baseDir
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

      // Additional symlink check if requested (for high-security deployments)
      if (checkSymlinks && combinedPath.exists() && !isWithin(combinedPath, baseDir)) {
        log.warn(
            "Symlink escape attempt: {} -> {} (outside allowed base)",
            combinedPath.getAbsolutePath(),
            combinedPath.getCanonicalPath());
        throw new SecurityException("Symlink escapes base directory bounds: " + userPath);
      }

      // Return the path as constructed (not canonicalized) so that callers see the same
      // path-string form as the supplied baseDir. The security check above already used
      // canonical paths to guarantee the result stays within baseDir.
      return combinedPath;
    } catch (IOException e) {
      log.error("Error validating path: {} + {}: {}", baseDir, userPath, e.getMessage());
      throw new SecurityException("Failed to validate path security: " + e.getMessage(), e);
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
            "Combined path escapes baseDir bounds: "
                + current.getCanonicalPath()
                + " (CWE-22)");
      }
    } catch (IOException e) {
      throw new SecurityException("Cannot validate path: " + e.getMessage(), e);
    }

    return current;
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
