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

package com.percussion.sitemanage.task.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;

import com.percussion.services.legacy.IPSCmsObjectMgr;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Regression for GH-849 / v8.1.7 PR #853: post-edition date updates must swallow concurrency
 * exceptions (INFO demotion) without failing the job, while other exceptions still propagate.
 */
@ExtendWith(MockitoExtension.class)
class PSWorkflowEditionTaskConcurrencyTest {

  @Mock private IPSCmsObjectMgr cmsObjectManager;

  @Test
  void updateContentDatesSwallowsConcurrencyButRethrowsOthers() throws Exception {
    var task = new PSWorkflowEditionTask();
    task.setCmsObjectManager(cmsObjectManager);

    doThrow(new CannotAcquireLockException("locked"))
        .when(cmsObjectManager)
        .setPostDate(anyCollection());

    // Must not throw — concurrency is demoted and swallowed
    task.updateContentDatesAfterWorkflow(42L, Set.of(1), Set.of());

    doThrow(new IllegalStateException("hard failure"))
        .when(cmsObjectManager)
        .setPostDate(anyCollection());

    try {
      task.updateContentDatesAfterWorkflow(43L, Set.of(2), Set.of());
      fail("expected non-concurrency exception to propagate");
    } catch (IllegalStateException expected) {
      assertEquals("hard failure", expected.getMessage());
    }
  }

  /** Simple name must equal {@code CannotAcquireLockException} for exact-name detection. */
  static final class CannotAcquireLockException extends RuntimeException {
    CannotAcquireLockException(String m) {
      super(m);
    }
  }
}
