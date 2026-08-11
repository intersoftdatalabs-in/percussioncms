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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Documents pagination query-param encoding after rawtypes cleanup (#2760).
 *
 * <p>Null values intentionally encode as empty ({@code key=}), matching the
 * pre-generics ternary — not {@code encodeQuery(null)} which would throw.
 */
@Tag("UnitTest")
class PSPagedRequestLinkGeneratorTypedTest {

  @Test
  void nullValueEncodesAsEmptyNotThrow() {
    StringBuilder buf = new StringBuilder();
    PSPagedRequestLinkGenerator.appendEncodedQueryParam(buf, "folderid", null);
    assertEquals("folderid=", buf.toString());
  }

  @Test
  void nonNullValueIsQueryEncoded() {
    StringBuilder buf = new StringBuilder();
    PSPagedRequestLinkGenerator.appendEncodedQueryParam(buf, "q", "a b");
    // Space is percent-encoded by PSURLEncoder.encodeQuery
    assertEquals("q=a+b", buf.toString());
  }

  @Test
  void nonStringParameterValueStillClassCastsLikeLegacyCast() {
    // Mimic historical (String) params.get(key) on a non-String entry.
    Object raw = Integer.valueOf(42);
    assertThrows(ClassCastException.class, () -> {
      @SuppressWarnings("unused")
      String val = (String) raw;
    });
  }
}
