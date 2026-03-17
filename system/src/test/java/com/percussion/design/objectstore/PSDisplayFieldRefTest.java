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

import org.junit.jupiter.api.Test;

/** Unit tests for the <code>PSDisplayFieldRef</code> class. */
public class PSDisplayFieldRefTest {
  /**
   * Tests that the <code>clone()</code> method creates a separate-but-equal instance, including
   * fields defined in the superclass.
   *
   * @throws Exception if the test fails.
   */
  @Test
  public void testClone() throws Exception {
    PSDisplayFieldRef foo = new PSDisplayFieldRef("foo");
    foo.setId(99);
    PSDisplayFieldRef bar = (PSDisplayFieldRef) foo.clone();

    assertEquals(foo, bar);
    assertEquals(99, bar.getId());
    bar.setValueText("bar");
    assertFalse(foo.equals(bar));
  }
}
