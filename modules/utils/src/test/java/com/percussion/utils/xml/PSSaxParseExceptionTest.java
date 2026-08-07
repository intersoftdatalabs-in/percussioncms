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
package com.percussion.utils.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

/** Behavioral coverage for {@link PSSaxParseException} (issue #2016 serial field typing). */
@Tag("UnitTest")
public class PSSaxParseExceptionTest {

  @Test
  @DisplayName("ctor seeds message from first exception and exposes full list")
  void seedsFromFirstAndExposesAll() {
    SAXParseException first = new SAXParseException("first", "pub", "sys", 3, 7);
    SAXParseException second = new SAXParseException("second", null, null, 1, 1);
    List<SAXParseException> src = new ArrayList<>(Arrays.asList(first, second));

    PSSaxParseException wrapped = new PSSaxParseException(src);
    assertEquals("first", wrapped.getMessage());
    assertEquals(3, wrapped.getLineNumber());
    assertEquals(7, wrapped.getColumnNumber());

    Iterator<SAXParseException> it = wrapped.getExceptions();
    assertTrue(it.hasNext());
    assertEquals("first", it.next().getMessage());
    assertTrue(it.hasNext());
    assertEquals("second", it.next().getMessage());

    // defensive copy: mutating the source list must not change the exception
    src.clear();
    assertTrue(wrapped.getExceptions().hasNext());
    assertEquals("first", wrapped.getExceptions().next().getMessage());
  }
}
