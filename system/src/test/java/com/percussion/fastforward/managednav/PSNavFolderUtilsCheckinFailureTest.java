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
package com.percussion.fastforward.managednav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Sample-site percNavon check-in hits stateId 0 (#3672 / #3364). */
class PSNavFolderUtilsCheckinFailureTest {

  @Test
  void detectsStateIdZeroAndTransitionExtension() {
    assertTrue(
        PSNavFolderUtils.isSampleWorkflowCheckinFailure(
            new IllegalArgumentException("stateId must be > 0")));
    assertTrue(
        PSNavFolderUtils.isSampleWorkflowCheckinFailure(
            new PSNavException(
                "Failed to check in navon",
                new RuntimeException(
                    "Java/global/percussion/workflow/sys_wfPerformTransition; boom"))));
    assertFalse(
        PSNavFolderUtils.isSampleWorkflowCheckinFailure(new PSNavException("duplicate navon")));
    assertFalse(PSNavFolderUtils.isSampleWorkflowCheckinFailure(null));
  }

  @Test
  void attachFailureDetectsMissingStateFieldAndNpe() {
    assertTrue(
        PSNavFolderUtils.isSampleWorkflowAttachFailure(
            new IllegalStateException("Field sys_contentstateid not found")));
    assertTrue(
        PSNavFolderUtils.isSampleWorkflowAttachFailure(
            new IllegalArgumentException("Workflow id is not loadable: 0")));
    assertTrue(PSNavFolderUtils.isSampleWorkflowAttachFailure(new NullPointerException()));
    assertFalse(
        PSNavFolderUtils.isSampleWorkflowCheckinFailure(
            new IllegalStateException("Field sys_contentstateid not found")));
    assertFalse(PSNavFolderUtils.isSampleWorkflowAttachFailure(new PSNavException("duplicate")));
  }

  @Test
  void parsePositiveIntRejectsZeroAndJunk() {
    assertEquals(4, PSNavFolderUtils.parsePositiveInt("4", 1));
    assertEquals(7, PSNavFolderUtils.parsePositiveInt(7, 1));
    assertEquals(1, PSNavFolderUtils.parsePositiveInt("0", 1));
    assertEquals(1, PSNavFolderUtils.parsePositiveInt(-3, 1));
    assertEquals(1, PSNavFolderUtils.parsePositiveInt("x", 1));
    assertEquals(1, PSNavFolderUtils.parsePositiveInt(null, 1));
  }
}
