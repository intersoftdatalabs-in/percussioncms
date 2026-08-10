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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed HTTPClient utility APIs after rawtypes cleanup (#2460).
 */
@DisplayName("HTTPClient Util generics")
class UtilGenericsTest {

  @Test
  @DisplayName("parseHeader returns typed HttpHeaderElement vector")
  void parseHeaderReturnsTypedElements() throws Exception {
    Vector<HttpHeaderElement> elems = Util.parseHeader("gzip, deflate;q=0.5");
    assertNotNull(elems);
    assertEquals(2, elems.size());
    assertEquals("gzip", elems.firstElement().getName());
    assertNull(elems.firstElement().getValue());

    HttpHeaderElement deflate = elems.elementAt(1);
    assertEquals("deflate", deflate.getName());
    assertEquals(1, deflate.getParams().length);
    assertEquals("q", deflate.getParams()[0].getName());
    assertEquals("0.5", deflate.getParams()[0].getValue());
  }

  @Test
  @DisplayName("getElement is case-insensitive on typed vector")
  void getElementCaseInsensitive() throws Exception {
    Vector<HttpHeaderElement> elems = Util.parseHeader("Identity, Chunked");
    assertNotNull(Util.getElement(elems, "identity"));
    assertNotNull(Util.getElement(elems, "CHUNKED"));
    assertNull(Util.getElement(elems, "gzip"));
  }

  @Test
  @DisplayName("assembleHeader round-trips parsed header elements")
  void assembleHeaderRoundTrip() throws Exception {
    Vector<HttpHeaderElement> elems = Util.parseHeader("gzip, deflate");
    String assembled = Util.assembleHeader(elems);
    assertTrue(assembled.toLowerCase().contains("gzip"));
    assertTrue(assembled.toLowerCase().contains("deflate"));

    Vector<HttpHeaderElement> again = Util.parseHeader(assembled);
    assertEquals(elems.size(), again.size());
  }

  @Test
  @DisplayName("getList creates and reuses typed context maps")
  void getListCreatesAndReuses() {
    ConcurrentHashMap<Object, ConcurrentHashMap<String, Integer>> outer = new ConcurrentHashMap<>();
    Object ctx = new Object();

    ConcurrentHashMap<String, Integer> first = Util.getList(outer, ctx);
    assertNotNull(first);
    first.put("a", 1);

    ConcurrentHashMap<String, Integer> second = Util.getList(outer, ctx);
    assertSame(first, second);
    assertEquals(1, second.get("a"));
  }
}
