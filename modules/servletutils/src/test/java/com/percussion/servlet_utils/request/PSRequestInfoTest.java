/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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
package com.percussion.servlet_utils.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.utils.request.PSRequestInfo;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Test request info
 *
 * @author dougrand
 */
@TestMethodOrder(MethodName.class)
public class PSRequestInfoTest {

  /**
   * @param arg0
   */
  public PSRequestInfoTest() {}

  @Test
  public void test10Basic() {
    try {
      PSRequestInfo.setRequestInfo("FOO", "BAR");
      assertTrue(false, "No exception where expected");
    } catch (Exception e) {
      // OK
    }

    PSRequestInfo.initRequestInfo((Map<String, Object>) null);
    String value = (String) PSRequestInfo.getRequestInfo("FOO");
    assertNull(value);

    PSRequestInfo.setRequestInfo("FOO", "YES");
    value = (String) PSRequestInfo.getRequestInfo("FOO");
    assertEquals("YES", value);

    PSRequestInfo.resetRequestInfo();
  }

  @Test
  public void test20Initial() {
    Map<String, Object> initial = new HashMap<String, Object>();
    initial.put("FOO", "BAR");

    PSRequestInfo.initRequestInfo(initial);
    String v = (String) PSRequestInfo.getRequestInfo("FOO");
    assertEquals("BAR", v);
  }
}
