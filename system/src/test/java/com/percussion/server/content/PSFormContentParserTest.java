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
package com.percussion.server.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.server.PSRequestParsingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for typed form-content parameter maps after #3213 Xlint cleanup.
 */
class PSFormContentParserTest {

  @Test
  @DisplayName("parseParameterString rejects a null parameter map")
  void parseParameterStringRejectsNullMap() {
    assertThrows(
        IllegalArgumentException.class, () -> PSFormContentParser.parseParameterString(null, "a=b"));
  }

  @Test
  @DisplayName("parseParameterString rejects a null parameter string")
  void parseParameterStringRejectsNullString() {
    Map<String, Object> params = new HashMap<>();
    assertThrows(
        IllegalArgumentException.class,
        () -> PSFormContentParser.parseParameterString(params, null));
  }

  @Test
  @DisplayName("parseParameterString stores typed name/value pairs")
  void parseParameterStringStoresPairs() throws PSRequestParsingException {
    Map<String, Object> params = new HashMap<>();
    PSFormContentParser.parseParameterString(params, "alpha=one&beta=two");
    assertEquals("one", params.get("alpha"));
    assertEquals("two", params.get("beta"));
  }

  @Test
  @DisplayName("duplicate names accumulate into a typed value list")
  void parseParameterStringDuplicateNamesAccumulate() throws PSRequestParsingException {
    Map<String, Object> params = new HashMap<>();
    PSFormContentParser.parseParameterString(params, "tag=a&tag=b");
    Object value = params.get("tag");
    assertInstanceOf(ArrayList.class, value);
    @SuppressWarnings("unchecked")
    List<Object> values = (List<Object>) value;
    assertEquals(2, values.size());
    assertEquals("a", values.get(0));
    assertEquals("b", values.get(1));
  }

  @Test
  @DisplayName("XHTML ampersand entities are converted before splitting params")
  void parseParameterStringConvertsAmpEntities() throws PSRequestParsingException {
    Map<String, Object> params = new HashMap<>();
    PSFormContentParser.parseParameterString(params, "first=1&amp;second=2");
    assertEquals("1", params.get("first"));
    assertEquals("2", params.get("second"));
  }

  @Test
  @DisplayName("empty parameter string leaves the map empty")
  void parseParameterStringEmptyIsNoOp() throws PSRequestParsingException {
    Map<String, Object> params = new HashMap<>();
    PSFormContentParser.parseParameterString(params, "");
    assertTrue(params.isEmpty());
  }
}
