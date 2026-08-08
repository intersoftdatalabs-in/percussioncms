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
package test.percussion.pso.utils;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pso.utils.PSOMutableUrl;
import org.junit.jupiter.api.Test;

/** Behavioral coverage for typed param map iteration in {@link PSOMutableUrl#toString()}. */
public class PSOMutableUrlTest {

  @Test
  void toStringIncludesBaseAndParams() throws Exception {
    PSOMutableUrl url = new PSOMutableUrl("https://example.com/path?a=1&b=2");
    String s = url.toString();
    assertTrue(s.startsWith("https://example.com/path?"));
    assertTrue(s.contains("a=1"));
    assertTrue(s.contains("b=2"));
  }

  @Test
  void setParamUpdatesQueryString() throws Exception {
    PSOMutableUrl url = new PSOMutableUrl("https://example.com/item");
    url.setParam("sys_contentid", "42");
    assertEquals("42", url.getParam("sys_contentid"));
    assertTrue(url.toString().contains("sys_contentid=42"));
  }
}
