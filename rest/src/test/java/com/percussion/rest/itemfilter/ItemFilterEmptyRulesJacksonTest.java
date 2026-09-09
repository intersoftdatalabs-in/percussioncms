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

package com.percussion.rest.itemfilter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.JacksonContextResolver;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Empty {@code rules: []} must bind as an empty list (clear), not null (omit). */
public class ItemFilterEmptyRulesJacksonTest {

  @Test
  void emptyRulesArrayDeserializesToEmptyListNotNull() {
    ObjectMapper m = new JacksonContextResolver().getContext(ItemFilter.class);
    ItemFilter f = m.readValue("{\"ItemFilter\":{\"name\":\"x\",\"rules\":[]}}", ItemFilter.class);
    assertNotNull(f.getRules(), "rules should not be null for []");
    assertTrue(f.getRules().isEmpty());
  }

  @Test
  void omittedRulesStayNull() {
    ObjectMapper m = new JacksonContextResolver().getContext(ItemFilter.class);
    ItemFilter f = m.readValue("{\"ItemFilter\":{\"name\":\"x\"}}", ItemFilter.class);
    assertNull(f.getRules());
  }
}
