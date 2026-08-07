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
package com.percussion.taxonomy.jexl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Behavioral tests for {@link TaxValues} (single vs multi-value JEXL helper). */
public class TaxValuesTest {

  @Test
  public void emptyConstructor_isNotMultiValued() {
    TaxValues values = new TaxValues();
    assertFalse(values.isMultiValued());
    assertEquals("", values.toString());
  }

  @Test
  public void singleValueConstructor_isNotMultiValued() {
    TaxValues values = new TaxValues("alpha");
    assertFalse(values.isMultiValued());
    assertEquals("alpha", values.toString());
    assertEquals(1, values.size());
  }

  @Test
  public void multipleValues_joinAndToString() {
    TaxValues values = new TaxValues();
    values.add("one");
    values.add("two");
    values.add("three");
    assertTrue(values.isMultiValued());
    assertEquals("one|two|three", values.join("|"));
    assertEquals("one,two,three", values.toString());
  }
}
