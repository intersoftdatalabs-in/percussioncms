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
package com.percussion.webservices;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Guard: folder / sentinel workflow ids must not call {@code loadWorkflow}
 * (GH-3330 — no 500 {@code Failed to load workflow with id '-1'}).
 */
public class PSWebserviceUtilsGetWorkflowTest {

  @Test
  public void sentinelIdsAreNotLoadable() {
    assertFalse(PSWebserviceUtils.isLoadableWorkflowId(-1));
    assertFalse(PSWebserviceUtils.isLoadableWorkflowId(0));
    assertTrue(PSWebserviceUtils.isLoadableWorkflowId(1));
    assertTrue(PSWebserviceUtils.isLoadableWorkflowId(6));
  }

  @Test
  public void getWorkflowRejectsSentinelWithoutLocatorLoad() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> PSWebserviceUtils.getWorkflow(-1));
    assertTrue(ex.getMessage().contains("-1"));
    assertThrows(IllegalArgumentException.class, () -> PSWebserviceUtils.getWorkflow(0));
  }
}
