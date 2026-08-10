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
package com.percussion.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class PSBaseHttpUtilsParseQueryTest {

  @Test
  public void multiValuedParamBuildsStringListWithoutUncheckedCast() {
    Map<String, Object> map = PSBaseHttpUtils.parseQueryParamsString("a=1&a=2&a=3", false, false);
    Object values = map.get("a");
    assertInstanceOf(List.class, values);
    @SuppressWarnings("unchecked")
    List<String> list = (List<String>) values;
    assertEquals(List.of("1", "2", "3"), list);
  }

  @Test
  public void singleParamIsPlainString() {
    Map<String, Object> map = PSBaseHttpUtils.parseQueryParamsString("q=hello", false, false);
    assertEquals("hello", map.get("q"));
  }

  @Test
  public void missingValueIsEmptyString() {
    Map<String, Object> map = PSBaseHttpUtils.parseQueryParamsString("flag=", false, false);
    assertEquals("", map.get("flag"));
  }

  @Test
  public void lowerCaseNamesOption() {
    Map<String, Object> map = PSBaseHttpUtils.parseQueryParamsString("Foo=1", true, false);
    assertTrue(map.containsKey("foo"));
    assertEquals("1", map.get("foo"));
  }
}
