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

package com.percussion.role.service.impl;

import static com.percussion.role.service.IPSRoleService.HOMEPAGE_TYPE_DASHBOARD;
import static com.percussion.role.service.IPSRoleService.HOMEPAGE_TYPE_EDITOR;
import static com.percussion.role.service.IPSRoleService.HOMEPAGE_TYPE_HOME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for SPA-first user homepage resolution (Home default landing). */
class PSRoleServiceHomepageTest {

  @Test
  void emptyRolesPreferHome() {
    assertEquals(HOMEPAGE_TYPE_HOME, PSRoleService.resolveUserHomepage(Collections.emptySet()));
    assertEquals(HOMEPAGE_TYPE_HOME, PSRoleService.resolveUserHomepage(null));
  }

  @Test
  void homeWinsOverDashboardAndEditor() {
    assertEquals(
        HOMEPAGE_TYPE_HOME,
        PSRoleService.resolveUserHomepage(
            Set.of(HOMEPAGE_TYPE_HOME, HOMEPAGE_TYPE_DASHBOARD, HOMEPAGE_TYPE_EDITOR)));
  }

  @Test
  void dashboardWhenOnlyDashboardRoles() {
    assertEquals(
        HOMEPAGE_TYPE_DASHBOARD,
        PSRoleService.resolveUserHomepage(Set.of(HOMEPAGE_TYPE_DASHBOARD)));
  }

  @Test
  void editorWhenOnlyEditorRoles() {
    assertEquals(
        HOMEPAGE_TYPE_EDITOR, PSRoleService.resolveUserHomepage(Set.of(HOMEPAGE_TYPE_EDITOR)));
  }

  @Test
  void dashboardBeatsEditorWhenHomeAbsent() {
    assertEquals(
        HOMEPAGE_TYPE_DASHBOARD,
        PSRoleService.resolveUserHomepage(Set.of(HOMEPAGE_TYPE_DASHBOARD, HOMEPAGE_TYPE_EDITOR)));
  }
}
