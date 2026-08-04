/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.percussion.security.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.lang3.StringUtils;

/**
 * Centralized path-injection defense (CWE-22 / CWE-23) helper per {@code
 * specs/004-zero-code-scanning-alerts/tasks.md} T043. Use this utility to validate user-supplied
 * paths and filenames before they reach any file I/O call site (FileInputStream, FileOutputStream,
 * Files.*, File constructors, etc.). Code that builds a {@link File} from user input should call
 * {@link #requireSafeFileName} on the untrusted string before composing the path, so a traversal
 * payload like {@code ../../etc/passwd} is rejected before it can be used to read or write outside
 * the intended base directory.
 *
 * <p><strong>Reuse for the 58 java/path-injection alerts under T043.</strong> Per the spec: "prefer
 * a single helper in modules/utils/ shared across call sites." This is that helper.
 *
 * @see <a href="https://owasp.org/www-community/attacks/Path_Traversal">OWASP Path Traversal</a>
 */
public final class PSPathInjectionGuard {

  private PSPathInjectionGuard() {
    // utility class
  }

  /**
   * Returns the input unchanged if it is a safe single-segment file or directory name. Throws
   * {@link IllegalArgumentException} if the value is null, contains a path separator, contains a
   * {@code ..} parent-traversal segment, or contains a NUL byte.
   *
   * <p>This method does NOT verify the file's existence or that the resolved path is within a
   * specific base directory. It only rejects path-traversal patterns. Callers that need to verify
   * "is the resolved path within this base directory" should also use {@link
   * java.nio.file.Path#normalize} and {@link Path#startsWith} on the result.
   *
   * <p><strong>Single-segment contract:</strong> The input must be a single path segment (no {@code
   * /}, no {@code \}); the only characters checked beyond the NUL byte are the traversal markers
   * {@code ..} and {@code .}. Filenames like {@code file..txt} are rejected because they contain
   * the literal substring {@code ..} which the contract treats as a traversal marker. Callers that
   * need to accept multi-segment paths or filenames with literal {@code ..} substrings (unusual)
   * should use {@link #requireUnderBase} (with an explicit base directory) or pre-process the input
   * to remove the {@code ..} substring.
   *
   * @param name a user-supplied file or directory name (single segment, no separators)
   * @return the input unchanged (after validation)
   * @throws IllegalArgumentException if the input fails any of the checks above
   */
  public static String requireSafeFileName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("path must not be null");
    }
    if (name.isEmpty()) {
      throw new IllegalArgumentException("path must not be empty");
    }
    if (name.indexOf('\0') >= 0) {
      throw new IllegalArgumentException(
          "path must not contain a NUL byte: " + describeForError(name));
    }
    // Disallow backslashes on any platform (Windows path separator
    // and Unix-escape sequences both use it).
    if (name.indexOf('\\') >= 0) {
      throw new IllegalArgumentException(
          "path must not contain a backslash: " + describeForError(name));
    }
    // Disallow any path separator embedded in the name. A single
    // segment like "foo" or "foo.txt" is allowed; "a/b" or "/etc/passwd"
    // is not.
    if (name.indexOf('/') >= 0) {
      throw new IllegalArgumentException(
          "path must not contain a forward slash (use a base directory + segment): "
              + describeForError(name));
    }
    // Reject path-segment ".." and "." entries. The ".." substring
    // check rejects things like "file..txt" and "a/../b". The
    // "name equals" check rejects the single-segment "." and ".."
    // entries (the current-dir and parent-dir markers, which
    // would resolve to the base directory itself or above it).
    // The check uses equals() rather than contains() for the "."
    // case because a single "." is a valid character in legitimate
    // filenames like "file.txt" or "archive.tar.gz"; only a
    // segment that IS just "." or ".." is forbidden.
    if (name.contains("..") || ".".equals(name) || "..".equals(name)) {
      throw new IllegalArgumentException(
          "path must not be '.' or '..' segment: " + describeForError(name));
    }
    return name;
  }

  /**
   * Resolves a user-supplied path against a base directory and returns the resulting {@link File},
   * but only if the resolved path is contained within the base directory. Throws {@link
   * IllegalArgumentException} if the resolved path escapes the base directory (path-traversal
   * attack).
   *
   * <p>This is the canonical safe pattern for "build a file path from a user-supplied filename
   * under a known base directory".
   *
   * <p>Example:
   *
   * <pre>{@code
   * File safe = PSPathInjectionGuard.requireUnderBase(
   *     new File("/var/data/themes"), userInput);
   * }</pre>
   *
   * @param baseDir the base directory that the resolved path must be contained within (typically a
   *     server-controlled directory like the themes root or uploads dir)
   * @param userInput a user-supplied path or filename; may be relative (e.g. "subdir/file.txt") or
   *     absolute; may contain normal characters
   * @return a {@link File} whose canonical path is contained within {@code baseDir}'s canonical
   *     path
   * @throws IllegalArgumentException if the resolved path escapes {@code baseDir} (path-traversal
   *     attempt), if the input is null, if {@code baseDir} does not exist, or if the canonical-path
   *     computation fails (e.g. the path is on a non-existent drive)
   */
  public static File requireUnderBase(File baseDir, String userInput) {
    if (baseDir == null) {
      throw new IllegalArgumentException("baseDir must not be null");
    }
    if (userInput == null) {
      throw new IllegalArgumentException("userInput must not be null");
    }
    if (!baseDir.exists() || !baseDir.isDirectory()) {
      throw new IllegalArgumentException("baseDir must exist and be a directory: " + baseDir);
    }
    // Reject NUL bytes early as a fast-path (NUL is the only
    // byte-level special character that can affect downstream file
    // APIs regardless of platform). The canonical-path check below
    // is the authoritative test for traversal attempts (e.g. "..",
    // absolute paths, symlinks). We do NOT do a substring ".."
    // check here because it would reject legitimate filenames like
    // "file..txt" or "archive..tar.gz" that contain a literal ".."
    // substring but resolve within the base directory.
    if (userInput.indexOf('\0') >= 0) {
      throw new IllegalArgumentException(
          "userInput contains a NUL byte: " + describeForError(userInput));
    }
    // Platform-safe resolution: on Windows, `new File(baseDir,
    // absolutePath)` produces a malformed path like
    // `C:\base\C:\absolute\file` because Windows File does NOT
    // discard the base directory when the second arg is absolute.
    // Resolve the input directly when it is already absolute, or
    // compose it under baseDir only when relative.
    File resolved =
        new File(userInput).isAbsolute() ? new File(userInput) : new File(baseDir, userInput);
    String resolvedCanonical;
    String baseCanonical;
    try {
      resolvedCanonical = resolved.getCanonicalPath();
      baseCanonical = baseDir.getCanonicalPath();
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Failed to resolve canonical path for input "
              + describeForError(userInput)
              + ": "
              + e.getMessage(),
          e);
    }
    // Normalize both paths to forward-slash form for the prefix
    // comparison. The canonical paths returned by getCanonicalPath() use
    // the platform separator ("/" on Unix, "\" on Windows); comparing
    // them directly with String.startsWith fails on Windows because
    // "C:\foo\bar" does not start with "C:\foo\" in a literal sense
    // (the trailing "\" is not present). Normalize to "/" first.
    String resolvedNorm = resolvedCanonical.replace('\\', '/');
    String baseNorm = baseCanonical.replace('\\', '/');
    // Append the platform separator to baseNorm so that
    // "/var/data/themesX" is not considered under "/var/data/themes".
    String baseWithSep = baseNorm.endsWith("/") ? baseNorm : baseNorm + "/";
    if (!resolvedNorm.equals(baseNorm) && !resolvedNorm.startsWith(baseWithSep)) {
      throw new IllegalArgumentException(
          "Resolved path '"
              + resolvedCanonical
              + "' is not under base directory '"
              + baseCanonical
              + "' (path-traversal attempt blocked)");
    }
    return new File(resolvedCanonical);
  }

  /**
   * Convenience overload that accepts the base directory as a String. Equivalent to {@code
   * requireUnderBase(new File(baseDir), userInput)}.
   *
   * @param baseDir the base directory that the resolved path must be contained within; see {@link
   *     #requireUnderBase(File, String)} for the full contract.
   * @param userInput a user-supplied path or filename; see {@link #requireUnderBase(File, String)}
   *     for the full contract.
   * @return a {@link File} whose canonical path is contained within {@code baseDir}'s canonical
   *     path.
   * @throws IllegalArgumentException if the resolved path escapes {@code baseDir} (path-traversal
   *     attempt), if either argument is null, or if the canonical-path computation fails.
   */
  public static File requireUnderBase(String baseDir, String userInput) {
    return requireUnderBase(new File(baseDir), userInput);
  }

  /**
   * Convenience overload that builds a {@link Path} from the result of {@link
   * #requireUnderBase(File, String)}.
   *
   * @param baseDir the base directory that the resolved path must be contained within; see {@link
   *     #requireUnderBase(File, String)} for the full contract.
   * @param userInput a user-supplied path or filename; see {@link #requireUnderBase(File, String)}
   *     for the full contract.
   * @return a {@link Path} whose canonical form is contained within {@code baseDir}'s canonical
   *     path.
   * @throws IllegalArgumentException if the resolved path escapes {@code baseDir} (path-traversal
   *     attempt), if either argument is null, or if the canonical-path computation fails.
   */
  public static Path requireUnderBasePath(File baseDir, String userInput) {
    return Paths.get(requireUnderBase(baseDir, userInput).toURI());
  }

  private static String describeForError(String s) {
    if (s == null) return "<null>";
    // Truncate long values to keep error messages bounded.
    String trimmed = s.length() > 80 ? s.substring(0, 77) + "..." : s;
    return "'" + trimmed + "' (len=" + s.length() + ")";
  }

  /**
   * True if {@code value} contains any character that file APIs treat specially and that has no
   * business in a user-supplied filename (NUL byte and path separators only, conservatively).
   * Exposed for callers that need a quick boolean check without the exception-throwing API of
   * {@link #requireSafeFileName}.
   *
   * <p>Note: this does NOT check for the literal ".." substring. Filenames like "file..txt" or
   * "archive..tar.gz" contain ".." but are legitimate; only ".." as a path SEGMENT (delimited by
   * path separators) is a traversal marker. Callers that need segment-aware traversal detection
   * should use {@link #requireSafeFileName} (single-segment) or {@link #requireUnderBase}
   * (multi-segment with canonical-path containment check).
   *
   * @param value the value to check, may be {@code null}.
   * @return {@code true} if the value is non-empty and contains a NUL byte, backslash, or forward
   *     slash; {@code false} otherwise (including when {@code value} is {@code null} or empty).
   */
  public static boolean containsForbiddenCharacters(String value) {
    if (StringUtils.isEmpty(value)) {
      return false;
    }
    return value.indexOf('\0') >= 0 || value.indexOf('\\') >= 0 || value.indexOf('/') >= 0;
  }
}
