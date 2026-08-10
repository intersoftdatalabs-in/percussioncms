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

package com.percussion.HTTPClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Enumeration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Behavioral tests for typed CIHashtable after rawtypes cleanup (#2460). */
@DisplayName("HTTPClient CIHashtable generics")
class CIHashtableGenericsTest {

  @Test
  @DisplayName("keys are case-insensitive and original case is preserved in keys()")
  void caseInsensitiveKeysPreserveOriginalCase() {
    CIHashtable table = new CIHashtable();
    table.put("Content-Type", "text/html");

    assertEquals("text/html", table.get("content-type"));
    assertEquals("text/html", table.get("CONTENT-TYPE"));
    assertTrue(table.containsKey("CoNtEnT-TyPe"));

    Enumeration<Object> keys = table.keys();
    assertTrue(keys.hasMoreElements());
    assertEquals("Content-Type", keys.nextElement());
    assertFalse(keys.hasMoreElements());
  }
}
