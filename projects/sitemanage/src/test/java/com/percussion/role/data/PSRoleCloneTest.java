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
package com.percussion.role.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Create returns {@link PSRole#clone()} when the new role has no members. */
class PSRoleCloneTest {

  @Test
  void cloneCopiesFieldsWithoutObjectClone() throws CloneNotSupportedException {
    var role = new PSRole();
    role.setName("NightRole");
    role.setDescription("Editors");
    role.setHomepage("Home");
    role.setOldName("Old");
    role.setUsers(List.of("Admin"));

    var copy = role.clone();

    assertNotSame(role, copy);
    assertEquals("NightRole", copy.getName());
    assertEquals("Editors", copy.getDescription());
    assertEquals("Home", copy.getHomepage());
    assertEquals("Old", copy.getOldName());
    assertEquals(List.of("Admin"), copy.getUsers());
  }
}
