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
 * Unit tests for {@link PSKeywordInstallUtils} (keyword package install helpers).
 *
 * <p>Regression: forced {@code @Version} bump before merge caused Hibernate 7 optimistic-lock
 * failures and {@code UnexpectedRollbackException} during perc.openGraphWidget / perc.nav install.
 */
public class PSKeywordInstallUtilsTest {

  @Test
  public void testMustNotForceHibernateVersionBump() {
    assertFalse(
        PSKeywordInstallUtils.shouldForceHibernateVersionBump(),
        "Package install must not force-bump @Version before merge under Hibernate 7");
  }

  @Test
  public void testFormatInstallErrorIncludesTypesAndRootCause() {
    Exception root = new IllegalStateException("stale entity");
    RuntimeException wrapper =
        new RuntimeException(
            "Transaction silently rolled back because it has been marked as rollback-only", root);

    String msg = PSKeywordInstallUtils.formatInstallError(wrapper);
    assertTrue(msg.contains("RuntimeException"), msg);
    assertTrue(msg.contains("rollback-only"), msg);
    assertTrue(msg.contains("IllegalStateException"), msg);
    assertTrue(msg.contains("stale entity"), msg);
  }

  @Test
  public void testFormatInstallErrorRejectsNull() {
    assertThrows(
        IllegalArgumentException.class, () -> PSKeywordInstallUtils.formatInstallError(null));
  }
}
