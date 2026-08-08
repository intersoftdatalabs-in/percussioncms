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
package com.percussion.utils.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class PSCopierTest {

  @Test
  public void deepCopyDuplicatesNestedMaps() {
    Map<String, Object> nested = new HashMap<>();
    nested.put("inner", "v");
    Map<String, Object> input = new HashMap<>();
    input.put("plain", "p");
    input.put("child", nested);

    Map<String, Object> copy = PSCopier.deepCopy(input);
    assertEquals(input, copy);
    assertNotSame(input, copy);
    assertNotSame(nested, copy.get("child"));
    assertEquals("v", ((Map<?, ?>) copy.get("child")).get("inner"));

    // Mutating the copy must not affect the original nested map.
    @SuppressWarnings("unchecked")
    Map<String, Object> copyChild = (Map<String, Object>) copy.get("child");
    copyChild.put("inner", "changed");
    assertEquals("v", nested.get("inner"));
  }
}
