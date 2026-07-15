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

package com.percussion.itemmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.itemmanagement.service.impl.PSAbstractWorkflowExtension.WorkflowItem;
import com.percussion.itemmanagement.service.impl.PSAbstractWorkflowExtension.WorkflowItem.ItemStatus;
import com.percussion.sitemanage.task.impl.PSWorkflowEditionTask;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-849 / v8.1.7 PR #853: concurrency / lock exceptions during publish workflow
 * must be recognized so they can be demoted from ERROR to INFO logging.
 *
 * <p>Post-edition date-update swallow/rethrow is covered by {@code
 * PSWorkflowEditionTaskConcurrencyTest} (same package as the task).
 */
class PSAbstractWorkflowExtensionConcurrencyTest {

  @Test
  void detectsLockAndOptimisticConcurrencyByClassName() {
    assertTrue(
        PSAbstractWorkflowExtension.isConcurrencyException(new LockAcquisitionException("locked")));
    assertTrue(
        PSAbstractWorkflowExtension.isConcurrencyException(new OptimisticLockException("optimistic")));
    assertTrue(
        PSAbstractWorkflowExtension.isConcurrencyException(
            new PessimisticLockException("pessimistic")));
    assertTrue(
        PSAbstractWorkflowExtension.isConcurrencyException(
            new ConcurrencyFailureException("fail")));
  }

  @Test
  void detectsSpringAndHibernateNamesMissedByOriginalShortList() {
    assertTrue(
        PSAbstractWorkflowExtension.isConcurrencyException(
            new CannotAcquireLockException("cannot acquire")));
    assertTrue(
        PSAbstractWorkflowExtension.isConcurrencyException(
            new ObjectOptimisticLockingFailureException("stale")));
    assertTrue(
        PSAbstractWorkflowExtension.isConcurrencyException(
            new StaleObjectStateException("stale object")));
    assertTrue(
        PSAbstractWorkflowExtension.isConcurrencyException(new StaleStateException("stale state")));
  }

  @Test
  void simpleNameEqualityCoversFqcnAndRejectsEmbeddedFragments() {
    assertTrue(
        PSAbstractWorkflowExtension.isConcurrencyExceptionSimpleName(
            "org.springframework.dao.CannotAcquireLockException"));
    assertTrue(
        PSAbstractWorkflowExtension.isConcurrencyExceptionSimpleName(
            "org.springframework.orm.ObjectOptimisticLockingFailureException"));
    assertTrue(
        PSAbstractWorkflowExtension.isConcurrencyExceptionSimpleName(
            "org.hibernate.StaleObjectStateException"));
    assertFalse(
        PSAbstractWorkflowExtension.isConcurrencyExceptionSimpleName(
            "java.lang.IllegalStateException"));
    // Substring false-positives must not demote
    assertFalse(
        PSAbstractWorkflowExtension.isConcurrencyExceptionSimpleName(
            "NotAnOptimisticLockException"));
    assertFalse(
        PSAbstractWorkflowExtension.isConcurrencyExceptionSimpleName(
            "OptimisticLockExceptionHandler"));
  }

  @Test
  void walksCauseChain() {
    Exception outer = new RuntimeException("wrap", new OptimisticLockException("inner"));
    assertTrue(PSAbstractWorkflowExtension.isConcurrencyException(outer));
  }

  @Test
  void rejectsUnrelatedExceptions() {
    assertFalse(PSAbstractWorkflowExtension.isConcurrencyException(null));
    assertFalse(PSAbstractWorkflowExtension.isConcurrencyException(new IllegalStateException("x")));
  }

  @Test
  void handleErrorMarksFailedForConcurrencyAndNonConcurrency() {
    var task = new PSWorkflowEditionTask();
    var worker = task.getWorker(Map.of());
    var concurrent = new WorkflowItem();
    var race = new OptimisticLockException("race");
    worker.handleError(concurrent, "workflow", race);
    assertEquals(ItemStatus.FAILED, concurrent.status);
    assertSame(race, concurrent.error);

    var hard = new WorkflowItem();
    var boom = new IllegalStateException("boom");
    worker.handleError(hard, "workflow", boom);
    assertEquals(ItemStatus.FAILED, hard.status);
    assertSame(boom, hard.error);
  }

  /**
   * Stand-ins use the same simple names as Spring/Hibernate/JPA types so detection can use exact
   * simple-name equality without putting those libraries on the test compile classpath.
   */
  static final class LockAcquisitionException extends RuntimeException {
    LockAcquisitionException(String m) {
      super(m);
    }
  }

  static final class OptimisticLockException extends RuntimeException {
    OptimisticLockException(String m) {
      super(m);
    }
  }

  static final class PessimisticLockException extends RuntimeException {
    PessimisticLockException(String m) {
      super(m);
    }
  }

  static final class ConcurrencyFailureException extends RuntimeException {
    ConcurrencyFailureException(String m) {
      super(m);
    }
  }

  static final class CannotAcquireLockException extends RuntimeException {
    CannotAcquireLockException(String m) {
      super(m);
    }
  }

  static final class ObjectOptimisticLockingFailureException extends RuntimeException {
    ObjectOptimisticLockingFailureException(String m) {
      super(m);
    }
  }

  static final class StaleObjectStateException extends RuntimeException {
    StaleObjectStateException(String m) {
      super(m);
    }
  }

  static final class StaleStateException extends RuntimeException {
    StaleStateException(String m) {
      super(m);
    }
  }
}
