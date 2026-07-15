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
        PSAbstractWorkflowExtension.isConcurrencyException(
            new SimulatedLockAcquisitionException("locked")));
    assertTrue(
        PSAbstractWorkflowExtension.isConcurrencyException(
            new SimulatedOptimisticLockException("optimistic")));
    assertTrue(
        PSAbstractWorkflowExtension.isConcurrencyException(
            new SimulatedPessimisticLockException("pessimistic")));
    assertTrue(
        PSAbstractWorkflowExtension.isConcurrencyException(
            new SimulatedConcurrencyFailureException("fail")));
  }

  @Test
  void detectsSpringAndHibernateNamesMissedByOriginalShortList() {
    // org.springframework.dao.CannotAcquireLockException
    assertTrue(
        PSAbstractWorkflowExtension.isConcurrencyException(
            new SimulatedCannotAcquireLockException("cannot acquire")));
    // org.springframework.orm.ObjectOptimisticLockingFailureException
    assertTrue(
        PSAbstractWorkflowExtension.isConcurrencyException(
            new SimulatedObjectOptimisticLockingFailureException("stale")));
    // org.hibernate.StaleObjectStateException / StaleStateException
    assertTrue(
        PSAbstractWorkflowExtension.isConcurrencyException(
            new SimulatedStaleObjectStateException("stale object")));
    assertTrue(
        PSAbstractWorkflowExtension.isConcurrencyException(
            new SimulatedStaleStateException("stale state")));
  }

  @Test
  void nameContainsConcurrencyMarkerCoversFqcnFragments() {
    assertTrue(
        PSAbstractWorkflowExtension.nameContainsConcurrencyMarker(
            "org.springframework.dao.CannotAcquireLockException"));
    assertTrue(
        PSAbstractWorkflowExtension.nameContainsConcurrencyMarker(
            "org.springframework.orm.ObjectOptimisticLockingFailureException"));
    assertTrue(
        PSAbstractWorkflowExtension.nameContainsConcurrencyMarker(
            "org.hibernate.StaleObjectStateException"));
    assertFalse(
        PSAbstractWorkflowExtension.nameContainsConcurrencyMarker("java.lang.IllegalStateException"));
  }

  @Test
  void walksCauseChain() {
    Exception outer = new RuntimeException("wrap", new SimulatedOptimisticLockException("inner"));
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
    var race = new SimulatedOptimisticLockException("race");
    worker.handleError(concurrent, "workflow", race);
    assertEquals(ItemStatus.FAILED, concurrent.status);
    assertSame(race, concurrent.error);

    var hard = new WorkflowItem();
    var boom = new IllegalStateException("boom");
    worker.handleError(hard, "workflow", boom);
    assertEquals(ItemStatus.FAILED, hard.status);
    assertSame(boom, hard.error);
  }

  // Local stand-ins so we do not depend on Hibernate/Spring exception classes on the test
  // classpath. Detection is by class name substring / fragment.
  static final class SimulatedLockAcquisitionException extends RuntimeException {
    SimulatedLockAcquisitionException(String m) {
      super(m);
    }
  }

  static final class SimulatedOptimisticLockException extends RuntimeException {
    SimulatedOptimisticLockException(String m) {
      super(m);
    }
  }

  static final class SimulatedPessimisticLockException extends RuntimeException {
    SimulatedPessimisticLockException(String m) {
      super(m);
    }
  }

  static final class SimulatedConcurrencyFailureException extends RuntimeException {
    SimulatedConcurrencyFailureException(String m) {
      super(m);
    }
  }

  static final class SimulatedCannotAcquireLockException extends RuntimeException {
    SimulatedCannotAcquireLockException(String m) {
      super(m);
    }
  }

  static final class SimulatedObjectOptimisticLockingFailureException extends RuntimeException {
    SimulatedObjectOptimisticLockingFailureException(String m) {
      super(m);
    }
  }

  static final class SimulatedStaleObjectStateException extends RuntimeException {
    SimulatedStaleObjectStateException(String m) {
      super(m);
    }
  }

  static final class SimulatedStaleStateException extends RuntimeException {
    SimulatedStaleStateException(String m) {
      super(m);
    }
  }
}
