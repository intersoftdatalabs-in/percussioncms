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
package com.percussion.pagemanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.pagemanagement.service.impl.PSWidgetUtils;
import com.percussion.pagemanagement.service.impl.PSWidgetUtils.PSWidgetPropertyBlankStringCoercionException;
import com.percussion.pagemanagement.service.impl.PSWidgetUtils.PSWidgetPropertyCoercionException;
import org.junit.jupiter.api.Test;

/** Behavioral tests for typed {@link PSWidgetUtils#coerceProperty}. */
public class PSWidgetUtilsTest {

  @Test
  void coerceNullReturnsNull() {
    assertNull(PSWidgetUtils.coerceProperty("n", null, String.class));
  }

  @Test
  void coerceSameType() {
    assertEquals("hello", PSWidgetUtils.coerceProperty("n", "hello", String.class));
    assertEquals(Integer.valueOf(7), PSWidgetUtils.coerceProperty("n", 7, Integer.class));
  }

  @Test
  void coerceStringToNumberAndBoolean() {
    assertEquals(Integer.valueOf(42), PSWidgetUtils.coerceProperty("n", "42", Integer.class));
    assertEquals(Boolean.TRUE, PSWidgetUtils.coerceProperty("n", "true", Boolean.class));
    assertEquals(Boolean.FALSE, PSWidgetUtils.coerceProperty("n", "false", Boolean.class));
  }

  @Test
  void blankStringThrowsBlankStringException() {
    assertThrows(
        PSWidgetPropertyBlankStringCoercionException.class,
        () -> PSWidgetUtils.coerceProperty("n", "", Integer.class));
  }

  @Test
  void incompatibleTypeThrowsCoercionException() {
    assertThrows(
        PSWidgetPropertyCoercionException.class,
        () -> PSWidgetUtils.coerceProperty("n", new Object(), Integer.class));
  }
}
