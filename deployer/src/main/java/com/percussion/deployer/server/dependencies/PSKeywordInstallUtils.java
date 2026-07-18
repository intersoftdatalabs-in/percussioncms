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
 * Pure helpers for keyword package install. Kept free of Spring so unit tests can run without a CMS
 * context.
 */
final class PSKeywordInstallUtils {

  private PSKeywordInstallUtils() {}

  /**
   * Whether package install should manually force-bump the Hibernate {@code @Version} before save.
   *
   * <p>Must remain {@code false}: under Hibernate 7, setting {@code version = loaded + 1} then
   * {@code merge} causes optimistic-lock failures and marks the transaction rollback-only, which
   * surfaces as {@code UnexpectedRollbackException} from the deploy service proxy.
   *
   * @return always {@code false}
   */
  static boolean shouldForceHibernateVersionBump() {
    return false;
  }

  /**
   * Builds a keyword install error message that includes the exception type and root cause (Spring
   * rollback wrappers often hide the original failure).
   *
   * @param e failure, not {@code null}
   * @return message for logs / {@code PSDeployException}, never {@code null}
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
    return "error occurred while installing keyword: "
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
