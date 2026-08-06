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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PSJndiUtilsTest {

  @Test
  void recognizesCurrentDefaultPasswordFilterExtension() {
    assertTrue(
        PSJndiUtils.isDefaultPasswordFilterExtension(
            "Java/global/percussion/filter/sys_DefaultPasswordFilter"));
  }

  @Test
  void recognizesLegacyDefaultPasswordFilterExtension() {
    assertTrue(
        PSJndiUtils.isDefaultPasswordFilterExtension(
            "Java/global/percussion/filter/defaultPasswordFilter"));
  }

  @Test
  void rejectsNonDefaultPasswordFilterExtensionNames() {
    assertFalse(PSJndiUtils.isDefaultPasswordFilterExtension(null));
    assertFalse(PSJndiUtils.isDefaultPasswordFilterExtension(""));
    assertFalse(
        PSJndiUtils.isDefaultPasswordFilterExtension("Java/global/percussion/filter/custom"));
  }
}
