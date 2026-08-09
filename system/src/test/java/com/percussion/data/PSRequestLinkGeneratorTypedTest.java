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
package com.percussion.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSRequestLink;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Smoke tests for {@link PSRequestLinkGenerator} after typing parameter/extractor lists (helpers
 * remain protected and require full app wiring for integration coverage).
 */
@Tag("UnitTest")
class PSRequestLinkGeneratorTypedTest {

  @Test
  void linkTypeStringCoversQueryInsertUpdateDelete() throws Exception {
    Method m = PSRequestLinkGenerator.class.getDeclaredMethod("getLinkTypeString", int.class);
    m.setAccessible(true);
    assertEquals("query", ((String) m.invoke(null, PSRequestLink.RL_TYPE_QUERY)).toLowerCase());
    String insert = (String) m.invoke(null, PSRequestLink.RL_TYPE_INSERT);
    String update = (String) m.invoke(null, PSRequestLink.RL_TYPE_UPDATE);
    String delete = (String) m.invoke(null, PSRequestLink.RL_TYPE_DELETE);
    assertTrue(insert != null && !insert.isEmpty());
    assertTrue(update != null && !update.isEmpty());
    assertTrue(delete != null && !delete.isEmpty());
  }
}
