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
package com.percussion.fastforward.managednav;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.percussion.extension.PSParameterMismatchException;
import com.percussion.server.IPSRequestContext;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Behavioral tests for {@link PSNavAddAttribute} required-parameter validation after real generics
 * cleanup (issue #2034).
 */
class PSNavAddAttributeTest {

  @Test
  void canModifyStyleSheetIsFalse() {
    assertFalse(new PSNavAddAttribute().canModifyStyleSheet());
  }

  @Test
  void processResultDocumentRequiresAttributeName() {
    PSNavAddAttribute exit = new PSNavAddAttribute();
    IPSRequestContext request = mock(IPSRequestContext.class);
    Document doc = mock(Document.class);

    assertThrows(
        PSParameterMismatchException.class,
        () -> exit.processResultDocument(new Object[] {null, "app/query", "1"}, request, doc));
  }

  @Test
  void processResultDocumentRequiresQueryName() {
    PSNavAddAttribute exit = new PSNavAddAttribute();
    IPSRequestContext request = mock(IPSRequestContext.class);
    Document doc = mock(Document.class);

    assertThrows(
        PSParameterMismatchException.class,
        () -> exit.processResultDocument(new Object[] {"attr", null, "1"}, request, doc));
  }

  @Test
  void processResultDocumentRequiresColumnIndex() {
    PSNavAddAttribute exit = new PSNavAddAttribute();
    IPSRequestContext request = mock(IPSRequestContext.class);
    Document doc = mock(Document.class);

    assertThrows(
        PSParameterMismatchException.class,
        () -> exit.processResultDocument(new Object[] {"attr", "app/query", null}, request, doc));
  }

  @Test
  void processResultDocumentRejectsNonNumericColumnIndex() {
    PSNavAddAttribute exit = new PSNavAddAttribute();
    IPSRequestContext request = mock(IPSRequestContext.class);
    Document doc = mock(Document.class);

    assertThrows(
        PSParameterMismatchException.class,
        () ->
            exit.processResultDocument(
                new Object[] {"attr", "app/query", "not-a-number"}, request, doc));
  }
}
