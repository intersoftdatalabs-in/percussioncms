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
package com.percussion.servlet_utils.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for {@link PSMockHttpServletRequest}, including the two-arg convenience
 * constructor that must seed method and URI without invoking overridable setters (issue #2021 /
 * this-escape remediation).
 */
public class PSMockHttpServletRequestTest {

  @Test
  @DisplayName("two-arg constructor seeds method and request URI")
  void twoArgConstructorSeedsMethodAndUri() {
    PSMockHttpServletRequest request = new PSMockHttpServletRequest("POST", "/Rhythmyx/content");
    assertEquals("POST", request.getMethod());
    assertEquals("/Rhythmyx/content", request.getRequestURI());
  }

  @Test
  @DisplayName("two-arg constructor defaults null method to GET and null URI to empty")
  void twoArgConstructorNullDefaults() {
    PSMockHttpServletRequest request = new PSMockHttpServletRequest(null, null);
    assertEquals("GET", request.getMethod());
    assertEquals("", request.getRequestURI());
  }

  @Test
  @DisplayName("setRequestURI still normalizes null to empty after construction")
  void setRequestURINullBecomesEmpty() {
    PSMockHttpServletRequest request = new PSMockHttpServletRequest("GET", "/path");
    request.setRequestURI(null);
    assertEquals("", request.getRequestURI());
  }
}
