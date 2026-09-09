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

package com.percussion.services.pipeline.binary;

import com.percussion.services.pipeline.PSPipelineIrException;
import com.percussion.services.pipeline.model.PipelineBinaryResourceIr;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Fail-closed path guard for native pipeline binary resources: bundled local fixture token or a
 * portable-safe relative path. Cloud URLs, credentials, and traversal are rejected.
 *
 * <p>The bundled fixture token {@link #BUNDLED_TOKEN} is resolved from the classpath (no live
 * internet).
 */
public final class PSPipelineBinaryPath {

  /** Portable local fixture token. Resolved from classpath, not fetched from the wire. */
  public static final String BUNDLED_TOKEN = "pipeline-binary-fixture";

  /** Filename form of {@link #BUNDLED_TOKEN}. */
  public static final String BUNDLED_FILENAME = "pipeline-binary-fixture.bin";

  static final String BUNDLED_RESOURCE = "pipeline-binary-fixture.bin";

  static final int MAX_PATH_CHARS = 255;

  static final int MAX_CONTENT_TYPE_CHARS = 128;

  private static final Pattern CONTENT_TYPE =
      Pattern.compile("^[A-Za-z0-9][A-Za-z0-9!#$&^_.+-]*/[A-Za-z0-9][A-Za-z0-9!#$&^_.+-]*$");

  private PSPipelineBinaryPath() {}

  /**
   * Validate operator-configured binary fixture path.
   *
   * @return trimmed path, never {@code null}
   */
  public static String requireSafe(String path) throws PSPipelineIrException {
    if (StringUtils.isBlank(path)) {
      throw new PSPipelineIrException("Binary resource path is required");
    }
    String raw = path.trim();
    if (raw.indexOf('\0') >= 0) {
      throw new PSPipelineIrException("Binary resource path must not contain NUL");
    }
    if (raw.length() > MAX_PATH_CHARS) {
      throw new PSPipelineIrException("Binary resource path exceeds length limit");
    }
    String lower = raw.toLowerCase(Locale.ROOT);
    if (raw.contains("://") || lower.startsWith("file:") || looksLikeUrl(raw)) {
      rejectUrl(raw);
    }
    if (raw.indexOf('\\') >= 0) {
      throw new PSPipelineIrException(
          "Binary resource path must use portable relative segments (no backslash)");
    }
    if (raw.startsWith("/") || raw.startsWith("//")) {
      throw new PSPipelineIrException(
          "Binary resource path must be a relative local fixture (no absolute paths)");
    }
    if (hasWindowsDrivePrefix(raw)) {
      throw new PSPipelineIrException(
          "Binary resource path must be a relative local fixture (no drive letters)");
    }
    if (raw.contains("..")) {
      throw new PSPipelineIrException("Binary resource path must not contain path traversal");
    }
    try {
      Path relative = Path.of(raw);
      if (relative.isAbsolute()) {
        throw new PSPipelineIrException(
            "Binary resource path must be a relative local fixture (no absolute paths)");
      }
      for (Path part : relative) {
        String name = part.toString();
        if (name.isBlank() || ".".equals(name) || "..".equals(name)) {
          throw new PSPipelineIrException("Binary resource path must not contain path traversal");
        }
      }
    } catch (InvalidPathException e) {
      throw new PSPipelineIrException("Binary resource path is not a valid local path", e);
    }
    return raw;
  }

  /** True when the path is the bundled classpath fixture token or filename. */
  public static boolean isBundledFixture(String path) {
    if (StringUtils.isBlank(path)) {
      return false;
    }
    String trimmed = path.trim();
    return BUNDLED_TOKEN.equals(trimmed) || BUNDLED_FILENAME.equals(trimmed);
  }

  /**
   * Validate Content-Type for retrieve headers (no CR/LF, no parameters).
   *
   * @return trimmed type, never {@code null}
   */
  public static String requireSafeContentType(String contentType) throws PSPipelineIrException {
    if (StringUtils.isBlank(contentType)) {
      return PipelineBinaryResourceIr.DEFAULT_CONTENT_TYPE;
    }
    String raw = contentType.trim();
    if (raw.indexOf('\0') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\n') >= 0) {
      throw new PSPipelineIrException("Binary resource content type is invalid");
    }
    if (raw.length() > MAX_CONTENT_TYPE_CHARS) {
      throw new PSPipelineIrException("Binary resource content type exceeds length limit");
    }
    if (!CONTENT_TYPE.matcher(raw).matches()) {
      throw new PSPipelineIrException("Binary resource content type is invalid");
    }
    return raw;
  }

  static void rejectUrl(String raw) throws PSPipelineIrException {
    if (raw.contains("@") || credentialUserInfo(raw)) {
      throw new PSPipelineIrException(
          "Binary resource path must not contain userinfo (no credentials in the path)");
    }
    String scheme = schemeOf(raw);
    if (scheme != null) {
      throw new PSPipelineIrException(
          "Binary resource path must be a local fixture (no cloud URLs). Rejected scheme '"
              + scheme
              + "'");
    }
    throw new PSPipelineIrException(
        "Binary resource path must be a local fixture (no cloud URLs). Rejected scheme/host path");
  }

  static boolean looksLikeUrl(String raw) {
    return schemeOf(raw) != null;
  }

  /**
   * RFC 3986 scheme token before {@code :}, or {@code null} when the path is not URI-like.
   * Windows drive letters ({@code C:}) are also scheme-shaped and are rejected as non-local.
   */
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
    return raw.length() >= 2
        && Character.isLetter(raw.charAt(0))
        && raw.charAt(1) == ':';
  }
}
