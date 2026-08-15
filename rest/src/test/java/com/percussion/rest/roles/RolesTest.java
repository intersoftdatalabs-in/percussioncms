/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.rest.roles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.percussion.rest.MainTest;
import org.junit.jupiter.api.Test;

public class RolesTest extends MainTest {

  @Test
  public void testNeverNull() {
    var r = new Role();
    assertNull(r.getDescription(), "Unset wire scalars are nullable");
    assertNull(r.getName(), "Unset wire scalars are nullable");
    assertNull(r.getHomePage(), "Unset wire scalars are nullable");
    assertNotNull(r.getUsers(), "Should never be null");
  }
}
