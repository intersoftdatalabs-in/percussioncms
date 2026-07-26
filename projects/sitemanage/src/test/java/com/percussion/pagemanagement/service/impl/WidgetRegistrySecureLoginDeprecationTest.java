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

package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-845 / v8.1.7 PR #848: Secure Login widget is listed under the Deprecated group
 * and no longer under Percussion.
 */
class WidgetRegistrySecureLoginDeprecationTest {

  @Test
  void secureLoginIsInDeprecatedGroupOnly() throws Exception {
    String xml;
    try (InputStream in =
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream("com/percussion/pagemanagement/service/impl/WidgetRegistry.xml")) {
      assertNotNull(in, "WidgetRegistry.xml must be on the classpath");
      xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    // Not present as an active Percussion widget name
    assertFalse(
        xml.contains("<widget name=\"Secure Login\" />")
            || xml.contains("<widget name=\"Secure Login\"/>"),
        "Secure Login must not remain in the active Percussion group");
    assertTrue(
        xml.contains("Secure Login (Deprecated)"),
        "Secure Login must appear with Deprecated label");
    int deprecatedIdx = xml.indexOf("<group name=\"Deprecated\">");
    int secureIdx = xml.indexOf("Secure Login (Deprecated)");
    assertTrue(deprecatedIdx >= 0, "Deprecated group must exist");
    assertTrue(
        secureIdx > deprecatedIdx,
        "Secure Login (Deprecated) entry must appear after Deprecated group opens");
  }
}
