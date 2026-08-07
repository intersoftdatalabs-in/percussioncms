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
package com.percussion.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for typed {@link PSWorkflowRoleInfo} role ID/name accessors (issue #2016
 * rawtypes cleanup).
 */
@Tag("UnitTest")
public class PSWorkflowRoleInfoTest {

  @Test
  @DisplayName("role name list getter/setter round-trip via interface")
  void roleNamesRoundTrip() {
    PSWorkflowRoleInfo info = new PSWorkflowRoleInfo();
    List<String> names = Arrays.asList("Author", "Editor");
    IWorkflowRoleInfo asIface = info;
    asIface.setUserActingRoleNames(names);
    assertEquals(names, info.getUserActingRoleNames());
    assertSame(names, info.getUserActingRoleNames());
  }

  @Test
  @DisplayName("role ID list getter/setter round-trip")
  void roleIdsRoundTrip() {
    PSWorkflowRoleInfo info = new PSWorkflowRoleInfo();
    List<Integer> ids = Arrays.asList(1, 2, 7);
    info.setUserActingRoleIDs(ids);
    assertEquals(ids, info.getUserActingRoleIDs());
    assertSame(ids, info.getUserActingRoleIDs());
  }

  @Test
  @DisplayName("null lists are accepted and returned")
  void nullListsAccepted() {
    PSWorkflowRoleInfo info = new PSWorkflowRoleInfo();
    info.setUserActingRoleNames(null);
    info.setUserActingRoleIDs(null);
    assertNull(info.getUserActingRoleNames());
    assertNull(info.getUserActingRoleIDs());
  }

  @Test
  @DisplayName("singleton Admin list used by trash task remains compatible")
  void singletonAdminList() {
    PSWorkflowRoleInfo info = new PSWorkflowRoleInfo();
    info.setUserActingRoleNames(Collections.singletonList("Admin"));
    assertEquals(Collections.singletonList("Admin"), info.getUserActingRoleNames());
  }
}
