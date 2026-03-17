/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.design.objectstore;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import org.apache.commons.lang3.time.FastDateFormat;
import org.junit.jupiter.api.Test;

/** Unit tests for the <code>PSDateLiteralTest</code> class. */
public class PSDateLiteralTest {
  /**
   * Tests that the <code>clone()</code> method creates a separate-but-equal instance, including
   * fields defined in the superclass, and that the copy was deep.
   *
   * @throws Exception if the test fails.
   */
  @Test
  public void testClone() throws Exception {
    Date now = new Date();
    FastDateFormat format = FastDateFormat.getInstance();
    PSDateLiteral foo = new PSDateLiteral(now, format);
    foo.setId(99);
    PSDateLiteral bar = (PSDateLiteral) foo.clone();

    assertEquals(foo, bar);
    assertEquals(99, bar.getId(), "id copied");
    assertEquals(now, bar.getDate(), "m_date copied");

    now.setTime(1); // mutate
    assertEquals(1L, foo.getDate().getTime(), "foo changed");
    assertNotEquals(1L, bar.getDate().getTime(), "bar unchanged");
    assertFalse(foo.equals(bar));
  }
}
