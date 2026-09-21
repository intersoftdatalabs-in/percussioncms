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

package com.percussion.rest.applicationfiles;

import com.percussion.rest.ObjectLockSummary;
import java.util.List;

/**
 * Adaptor for XML application CMS/resource files (SY-05). Path-safe list/get/put under a catalog
 * application root — not {@code /serverconfigs} (SY-02).
 */
public interface IApplicationFileAdaptor {

  /**
   * List relative file paths under the named application root.
   *
   * @return summaries (no content); {@code null} if the application is unknown / unsafe
   */
  List<ApplicationFileSummary> listFiles(String appName);

  /**
   * Load one file by application name and relative path under that app root.
   *
   * <p>Non-UTF-8 bodies are reported as {@code binary=true} with no {@code content} (the raw bytes
   * are served by {@link #getFileBytes} so they are never mangled by JSON text decoding).
   *
   * @return detail with content, or {@code null} if app/path unknown or unsafe
   */
  ApplicationFileSummary getFile(String appName, String relativePath);

  /**
   * Read the raw bytes of one file verbatim (binary-safe download).
   *
   * @return raw file bytes, or {@code null} if app/path unknown
   * @throws IllegalArgumentException when the relative path is unsafe (parent traversal, absolute,
   *     drive-letter, NUL forms) — the resource maps this to HTTP 400
   */
  byte[] getFileBytes(String appName, String relativePath);

  /**
   * Admin. Replace UTF-8 text content of an existing file, or <strong>create</strong> a new file
   * when the relative path does not yet exist under the application root. A directory occupying
   * the path is a conflict (HTTP 409), not a silent no-op.
   *
   * @return updated detail, or {@code null} if the application is unknown or the path is unsafe
   */
  ApplicationFileSummary putFile(String appName, String relativePath, ApplicationFileSummary body);

  /**
   * Admin. Replace the raw bytes of an existing or new file under the application root
   * (binary-safe round-trip). Requires a held design lock — same semantics as {@link #putFile}.
   *
   * @return updated detail ({@code binary=true} when the new body is not valid UTF-8 text), or
   *     {@code null} if app/path unknown
   * @throws IllegalArgumentException when the relative path is unsafe or the body is null
   */
  ApplicationFileSummary putFileBytes(String appName, String relativePath, byte[] bytes);

  /**
   * Admin. Create a folder at a relative path under the application root.
   *
   * @return folder summary, or {@code null} if the application is unknown
   */
  ApplicationFileSummary createFolder(String appName, String relativePath);

  /**
   * Admin. Delete a relative file or folder (folders are removed recursively).
   *
   * @return {@code Boolean.TRUE} when deleted, or {@code null} if the application or path is unknown
   */
  Boolean deletePath(String appName, String relativePath);

  /**
   * Admin. Rename or move a relative file or folder under the application root.
   *
   * @return destination summary, or {@code null} if the application or source path is unknown
   */
  ApplicationFileSummary movePath(String appName, String fromPath, String toPath);

  /**
   * Admin. Acquire a design-session lock for one relative file (object-store application file
   * lock). Re-lock by the same session user extends the lock. Does not steal another user's lock.
   *
   * @return lock summary, or {@code null} if the application or path is unknown / unsafe
   */
  ObjectLockSummary lockFile(String appName, String relativePath);

  /**
   * Admin. Release a design-session lock owned by the current user/session.
   *
   * @return {@code Boolean.TRUE} when released, or {@code null} if the application or path is
   *     unknown / unsafe
   */
  Boolean unlockFile(String appName, String relativePath);
}
