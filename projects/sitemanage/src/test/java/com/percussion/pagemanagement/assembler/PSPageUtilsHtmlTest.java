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
package com.percussion.pagemanagement.assembler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Behavioral unit tests for {@link PSPageUtils#html(Object, String, Object)}.
 *
 * <p>Covers graceful default handling for null/empty item or fields and Map-path missing property
 * defaults. Ported product fix from v8.1.7 PR #716 (Bugfix/8.1.7 release testing).
 */
class PSPageUtilsHtmlTest {

  private PSPageUtils utils;

  @BeforeEach
  void setUp() {
    utils = new PSPageUtils();
  }

  @Test
  void nullItemWithDefaultReturnsEscapedDefault() {
    assertEquals("fallback", utils.html(null, "title", "fallback"));
  }

  @Test
  void nullItemWithNullDefaultReturnsEmptyString() {
    assertEquals("", utils.html(null, "title", null));
  }

  @Test
  void nullFieldsWithDefaultReturnsEscapedDefault() {
    Map<String, String> item = Map.of("title", "Hello");
    assertEquals("fallback", utils.html(item, null, "fallback"));
  }

  @Test
  void emptyFieldsWithDefaultReturnsEscapedDefault() {
    Map<String, String> item = Map.of("title", "Hello");
    assertEquals("fallback", utils.html(item, "", "fallback"));
    assertEquals("fallback", utils.html(item, "   ", "fallback"));
  }

  @Test
  void emptyFieldsWithNullDefaultReturnsEmptyString() {
    Map<String, String> item = Map.of("title", "Hello");
    assertEquals("", utils.html(item, "", null));
  }

  @Test
  void nullItemAndFieldsDoNotThrow() {
    assertDoesNotThrow(() -> utils.html(null, null, "x"));
    assertDoesNotThrow(() -> utils.html(null, null, null));
    assertDoesNotThrow(() -> utils.html(null, "", "x"));
  }

  @Test
  void mapMissingPropertyWithDefaultReturnsEscapedDefault() {
    Map<String, String> item = new HashMap<>();
    item.put("other", "value");
    assertEquals("default-val", utils.html(item, "missing", "default-val"));
  }

  @Test
  void mapMissingPropertyWithDefaultEscapesHtml() {
    Map<String, String> item = Map.of("other", "value");
    assertEquals("&lt;b&gt;safe&lt;/b&gt;", utils.html(item, "missing", "<b>safe</b>"));
  }

  @Test
  void mapPresentPropertyReturnsEscapedValue() {
    Map<String, String> item = Map.of("title", "Hello & World");
    assertEquals("Hello &amp; World", utils.html(item, "title", "unused-default"));
  }

  @Test
  void mapMissingPropertyWithNullDefaultReturnsEmptyWithoutThrowing() {
    // handleNoFieldFound throws RepositoryException; outer catch returns ""
    Map<String, String> item = Map.of("other", "value");
    assertEquals("", assertDoesNotThrow(() -> utils.html(item, "missing", null)));
  }

  @Test
  void mapCommaSeparatedFieldsUsesFirstPresent() {
    Map<String, String> item = Map.of("b", "second");
    assertEquals("second", utils.html(item, "a,b,c", "default"));
  }

  @Test
  void twoArgHtmlDelegatesAndHandlesNullItem() {
    // two-arg form uses null default → empty string for null item
    assertEquals("", utils.html(null, "title"));
  }
}
