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

package com.percussion.deployer.server.dependencies;

/**
 * Pure helpers for item-filter package install. Free of Spring so unit tests run without a CMS
 * context.
 */
final class PSFilterInstallUtils {

  private PSFilterInstallUtils() {}

  /**
   * Whether install may null out {@code @Version} on a managed Hibernate entity before discarding
   * the Java reference.
   *
   * <p>Must remain {@code false}: the entity stays in the persistence context; a dirty null version
   * fails flush and marks the TX rollback-only (UnexpectedRollbackException at commit).
   *
   * @return always {@code false}
   */
  static boolean mayNullVersionOnManagedEntityBeforeDiscard() {
    return false;
  }

  /**
   * Builds a filter install error message including exception type and root cause.
   *
   * @param e failure, not {@code null}
   * @return message for logs / PSDeployException
   */
  static String formatInstallError(Throwable e) {
    if (e == null) {
      throw new IllegalArgumentException("e may not be null");
    }
    Throwable root = e;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    String msg = e.getMessage();
    String rootMsg = root.getMessage();
    return "error occurred while installing filter: "
        + e.getClass().getSimpleName()
        + ": "
        + (msg != null ? msg : "")
        + " (root: "
        + root.getClass().getSimpleName()
        + ": "
        + (rootMsg != null ? rootMsg : "")
        + ")";
  }
}
