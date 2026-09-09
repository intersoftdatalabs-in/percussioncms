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

package com.percussion.services.pipeline.xsl;

import com.percussion.services.pipeline.PSPipelineIrException;
import com.percussion.services.pipeline.model.PipelineResultPageIr;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * Fail-closed path guard for native pipeline result-page stylesheets: bundled local fixture token
 * or a portable-safe relative path. Cloud URLs, credentials, and traversal are rejected.
 */
public final class PSPipelineResultPagePath {

  /** Portable local fixture token. Resolved from classpath, not fetched from the wire. */
  public static final String BUNDLED_TOKEN = "pipeline-xsl-result-fixture";

  /** Filename form of {@link #BUNDLED_TOKEN}. */
  public static final String BUNDLED_FILENAME = "pipeline-xsl-result-fixture.xsl";

  static final String BUNDLED_RESOURCE = "pipeline-xsl-result-fixture.xsl";

  static final int MAX_PATH_CHARS = 255;

  static final int MAX_MIME_CHARS = 64;

  static final int MAX_EXTENSION_CHARS = 16;

  private static final Set<String> HTML_EXTENSIONS = Set.of("html", "htm");

  private PSPipelineResultPagePath() {}

  /**
   * Validate operator-configured stylesheet URI (bundled token or relative local path).
   *
   * @return trimmed URI, never {@code null}
   */
  public static String requireSafeStylesheetUri(String uri) throws PSPipelineIrException {
    if (StringUtils.isBlank(uri)) {
      throw new PSPipelineIrException("Result page stylesheet URI is required");
    }
    String raw = uri.trim();
    if (raw.indexOf('\0') >= 0) {
      throw new PSPipelineIrException("Result page stylesheet URI must not contain NUL");
    }
    if (raw.length() > MAX_PATH_CHARS) {
      throw new PSPipelineIrException("Result page stylesheet URI exceeds length limit");
    }
    String lower = raw.toLowerCase(Locale.ROOT);
    if (raw.contains("://") || lower.startsWith("file:") || looksLikeUrl(raw)) {
      rejectUrl(raw);
    }
    if (raw.indexOf('\\') >= 0) {
      throw new PSPipelineIrException(
          "Result page stylesheet URI must use portable relative segments (no backslash)");
    }
    if (raw.startsWith("/") || raw.startsWith("//")) {
      throw new PSPipelineIrException(
          "Result page stylesheet URI must be a relative local fixture (no absolute paths)");
    }
    if (hasWindowsDrivePrefix(raw)) {
      throw new PSPipelineIrException(
          "Result page stylesheet URI must be a relative local fixture (no drive letters)");
    }
    if (raw.contains("..")) {
      throw new PSPipelineIrException("Result page stylesheet URI must not contain path traversal");
    }
    try {
      Path relative = Path.of(raw);
      if (relative.isAbsolute()) {
        throw new PSPipelineIrException(
            "Result page stylesheet URI must be a relative local fixture (no absolute paths)");
      }
      for (Path part : relative) {
        String name = part.toString();
        if (name.isBlank() || ".".equals(name) || "..".equals(name)) {
          throw new PSPipelineIrException(
              "Result page stylesheet URI must not contain path traversal");
        }
      }
    } catch (InvalidPathException e) {
      throw new PSPipelineIrException("Result page stylesheet URI is not a valid local path", e);
    }
    return raw;
  }

  /** True when the URI is the bundled classpath fixture token or filename. */
  public static boolean isBundledFixture(String uri) {
    if (StringUtils.isBlank(uri)) {
      return false;
    }
    String trimmed = uri.trim();
    return BUNDLED_TOKEN.equals(trimmed) || BUNDLED_FILENAME.equals(trimmed);
  }

  /**
   * Normalize request extension to {@code .html} or {@code .htm}.
   *
   * @return dotted extension, never {@code null}
   */
  public static String requireSafeRequestExtension(String extension) throws PSPipelineIrException {
    String raw =
        StringUtils.isBlank(extension)
            ? PipelineResultPageIr.DEFAULT_REQUEST_EXTENSION
            : extension.trim();
    if (raw.indexOf('\0') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\n') >= 0) {
      throw new PSPipelineIrException("Result page request extension is invalid");
    }
    if (raw.length() > MAX_EXTENSION_CHARS) {
      throw new PSPipelineIrException("Result page request extension exceeds length limit");
    }
    String token = raw.startsWith(".") ? raw.substring(1) : raw;
    String lower = token.toLowerCase(Locale.ROOT);
    if (!HTML_EXTENSIONS.contains(lower)) {
      throw new PSPipelineIrException(
          "Result page request extension must be .html or .htm in this slice");
    }
    return "." + lower;
  }

  /**
   * HTML result pages only in this slice ({@code text/html}).
   *
   * @return trimmed MIME type, never {@code null}
   */
  public static String requireSafeMimeType(String mimeType) throws PSPipelineIrException {
    if (StringUtils.isBlank(mimeType)) {
      return PipelineResultPageIr.DEFAULT_MIME_TYPE;
    }
    String raw = mimeType.trim();
    if (raw.indexOf('\0') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\n') >= 0) {
      throw new PSPipelineIrException("Result page mime type is invalid");
    }
    if (raw.length() > MAX_MIME_CHARS) {
      throw new PSPipelineIrException("Result page mime type exceeds length limit");
    }
    if (!PipelineResultPageIr.DEFAULT_MIME_TYPE.equalsIgnoreCase(raw)) {
      throw new PSPipelineIrException("Result page mime type must be text/html in this slice");
    }
    return PipelineResultPageIr.DEFAULT_MIME_TYPE;
  }

  static void rejectUrl(String raw) throws PSPipelineIrException {
    if (raw.contains("@") || credentialUserInfo(raw)) {
      throw new PSPipelineIrException(
          "Result page stylesheet URI must not contain userinfo (no credentials in the path)");
    }
    String scheme = schemeOf(raw);
    if (scheme != null) {
      throw new PSPipelineIrException(
          "Result page stylesheet URI must be a local fixture (no cloud URLs). Rejected scheme '"
              + scheme
              + "'");
    }
    throw new PSPipelineIrException(
        "Result page stylesheet URI must be a local fixture (no cloud URLs). Rejected scheme/host path");
  }

  static boolean looksLikeUrl(String raw) {
    return schemeOf(raw) != null;
  }

  static String schemeOf(String raw) {
    int colon = raw.indexOf(':');
    if (colon <= 0) {
      return null;
    }
    String scheme = raw.substring(0, colon);
    if (scheme.chars().allMatch(ch -> Character.isLetterOrDigit(ch) || ch == '+' || ch == '.' || ch == '-')) {
      return scheme.toLowerCase(Locale.ROOT);
    }
    return null;
  }

  static boolean credentialUserInfo(String raw) {
    int scheme = raw.indexOf("://");
    if (scheme < 0) {
      return raw.contains("@");
    }
    String rest = raw.substring(scheme + 3);
    int slash = rest.indexOf('/');
    String authority = slash >= 0 ? rest.substring(0, slash) : rest;
    return authority.contains("@");
  }

  static boolean hasWindowsDrivePrefix(String raw) {
    return raw.length() >= 2 && Character.isLetter(raw.charAt(0)) && raw.charAt(1) == ':';
  }
}
