/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards EE11 WAR packaging contract for servlet-utils: filter properties and mock servlet classes
 * must be on the main classpath (not test-only).
 */
class PSServletUtilsClasspathResourcesTest {

  @Test
  @DisplayName("PSInputValidatorFilter.properties is on main classpath")
  void inputValidatorPropertiesPresent() {
    try (InputStream is =
        PSInputValidatorFilter.class.getResourceAsStream("PSInputValidatorFilter.properties")) {
      assertNotNull(
          is,
          "PSInputValidatorFilter.properties must ship under "
              + "src/main/resources/com/percussion/servlet_utils/servlet/");
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  @Test
  @DisplayName("PSMockHttpServletRequest is loadable (used by PSRequest)")
  void mockRequestClassPresent() {
    assertNotNull(PSMockHttpServletRequest.class.getName());
  }

  @Test
  @DisplayName("PSMockHttpServletResponse is loadable (used by callServlet)")
  void mockResponseClassPresent() {
    assertNotNull(PSMockHttpServletResponse.class.getName());
  }
}
