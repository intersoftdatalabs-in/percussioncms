/*
 * Copyright 2026 Intersoft Data Labs (https://intsof.com)
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intsof.common.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ConfigNamesTest {

  @Test
  void acceptsValidNames() {
    assertEquals(
        "last-install.properties",
        ConfigNames.requireValidSegment("last-install.properties", "file"));
    assertEquals("percussion", ConfigNames.requireValidSegment(" percussion ", "app"));
    assertEquals("my_app-1", ConfigNames.requireValidSegment("my_app-1", "app"));
  }

  @Test
  void rejectsControlAndIllegalChars() {
    assertThrows(
        IllegalArgumentException.class, () -> ConfigNames.requireValidSegment("a\nb", "file"));
    assertThrows(
        IllegalArgumentException.class, () -> ConfigNames.requireValidSegment("a*b", "file"));
    assertThrows(
        IllegalArgumentException.class, () -> ConfigNames.requireValidSegment("a?b", "file"));
  }
}
