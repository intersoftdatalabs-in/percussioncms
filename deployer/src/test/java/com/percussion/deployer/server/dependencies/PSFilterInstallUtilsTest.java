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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PSFilterInstallUtils}.
 *
 * <p>Regression: nulling {@code @Version} on a managed PSItemFilter then discarding the reference
 * left a dirty null-version entity in the session; commit failed with UnexpectedRollbackException
 * during perc.Baseline filter install.
 */
public class PSFilterInstallUtilsTest {

  @Test
  public void testMustNotNullVersionOnManagedEntityBeforeDiscard() {
    assertFalse(
        PSFilterInstallUtils.mayNullVersionOnManagedEntityBeforeDiscard(),
        "Must not null @Version on managed filter entities still in the Hibernate session");
  }

  @Test
  public void testShouldResolveExistingFilterByNameNotOnlyByDependencyId() {
    // Package dep GUID often differs from a prior install; name is the natural id.
    // Install must treat same-name as update (merge), not insert (unique NAME constraint).
    assertTrue(
        PSFilterInstallUtils.shouldResolveExistingByName(),
        "Filter install must resolve existing rows by name for reinstall safety");
  }

  @Test
  public void testDeployServiceMustRollbackOnException() {
    assertTrue(
        PSFilterInstallUtils.deployServiceShouldRollbackOnException(),
        "PSDeployService must use rollbackFor=Exception, not noRollbackFor=Exception");
  }

  @Test
  public void testOnlyFilterMissingErrorCodeMeansFirstInstall() {
    // Kilo review: swallowing all PSFilterException masks DB failures as "not found"
    // and leads to unique NAME constraint / UnexpectedRollbackException.
    assertTrue(
        PSFilterInstallUtils.isFilterMissingErrorCode(
            com.percussion.services.filter.IPSFilterServiceErrors.FILTER_MISSING));
    assertFalse(
        PSFilterInstallUtils.isFilterMissingErrorCode(
            com.percussion.services.filter.IPSFilterServiceErrors.DATABASE));
    assertFalse(
        PSFilterInstallUtils.isFilterMissingErrorCode(
            com.percussion.services.filter.IPSFilterServiceErrors.AUTHTYPE_MISSING));
    assertFalse(PSFilterInstallUtils.isFilterMissingErrorCode(0));
  }

  @Test
  public void testFormatInstallErrorIncludesTypesAndRootCause() {
    Exception root = new IllegalStateException("null version on flush");
    RuntimeException wrapper =
        new RuntimeException(
            "Transaction silently rolled back because it has been marked as rollback-only", root);

    String msg = PSFilterInstallUtils.formatInstallError(wrapper);
    assertTrue(msg.contains("RuntimeException"), msg);
    assertTrue(msg.contains("rollback-only"), msg);
    assertTrue(msg.contains("IllegalStateException"), msg);
    assertTrue(msg.contains("null version"), msg);
  }

  @Test
  public void testFormatInstallErrorRejectsNull() {
    assertThrows(
        IllegalArgumentException.class, () -> PSFilterInstallUtils.formatInstallError(null));
  }
}
