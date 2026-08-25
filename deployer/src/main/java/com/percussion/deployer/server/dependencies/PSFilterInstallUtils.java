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

package com.percussion.deployer.server.dependencies;

import com.intsof.percussioncms.auditlog.codes.FilterServiceErrorCodes;

/**
 * Pure helpers for item-filter package install. Free of Spring so unit tests run without a CMS
 * context. Public so {@code PSDeployService} can share the FILTER_MISSING policy without
 * duplicating the error-code check.
 */
public final class PSFilterInstallUtils {

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
  public static boolean mayNullVersionOnManagedEntityBeforeDiscard() {
    return false;
  }

  /**
   * Whether package install should treat an existing filter with the same name as an update even
   * when the package dependency id (GUID) does not match.
   *
   * <p>Must remain {@code true}: {@code PSItemFilter.name} is a unique natural id. Inserting a
   * second row with the same name fails only at flush and surfaces as UnexpectedRollbackException.
   *
   * @return always {@code true}
   */
  public static boolean shouldResolveExistingByName() {
    return true;
  }

  /**
   * Deploy service must roll back on checked deploy failures (not {@code noRollbackFor =
   * Exception}). Nested Hibernate {@code RuntimeException}s mark the TX rollback-only; attempting
   * commit then only reports {@code UnexpectedRollbackException}.
   *
   * @return always {@code true}
   */
  public static boolean deployServiceShouldRollbackOnException() {
    return true;
  }

  /**
   * Whether a filter-service error code means "no row with that name" (safe to insert).
   *
   * <p>Only {@link com.percussion.services.filter.IPSFilterServiceErrors#FILTER_MISSING} qualifies.
   * All other codes must propagate so package install does not treat DB failures as a first install
   * and then hit the unique NAME constraint.
   *
   * @param errorCode {@link com.percussion.services.filter.PSFilterException#getErrorCode()}
   * @return {@code true} only for FILTER_MISSING
   */
  public static boolean isFilterMissingErrorCode(int errorCode) {
    return errorCode == FilterServiceErrorCodes.FILTER_MISSING.numericCode();
  }

  /**
   * Builds a filter install error message including exception type and root cause.
   *
   * @param e failure, not {@code null}
   * @return message for logs / PSDeployException
   */
  public static String formatInstallError(Throwable e) {
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
