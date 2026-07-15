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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Regression for GH-849 / v8.1.7 PR #853: concurrency / lock exceptions during publish workflow
 * must be recognized so they can be demoted from ERROR to INFO logging.
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
  void walksCauseChain() {
    Exception outer = new RuntimeException("wrap", new SimulatedOptimisticLockException("inner"));
    assertTrue(PSAbstractWorkflowExtension.isConcurrencyException(outer));
  }

  @Test
  void rejectsUnrelatedExceptions() {
    assertFalse(PSAbstractWorkflowExtension.isConcurrencyException(null));
    assertFalse(PSAbstractWorkflowExtension.isConcurrencyException(new IllegalStateException("x")));
  }

  // Local stand-ins so we do not depend on Hibernate/Spring exception classes on the test
  // classpath. Detection is by class name substring.
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
}
