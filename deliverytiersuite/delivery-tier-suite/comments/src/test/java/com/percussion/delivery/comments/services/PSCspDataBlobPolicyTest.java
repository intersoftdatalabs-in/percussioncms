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

package com.percussion.delivery.comments.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-818 / v8.1.7 PRs #825 and #846: DTS CSP must allow {@code data:} and {@code
 * blob:} image URIs so embedded/base64 and blob-backed images render under Content-Security-Policy.
 */
class PSCspDataBlobPolicyTest {

  @Test
  void testResourcesPercSecurityPropertiesAllowDataAndBlobImgSrc() throws Exception {
    Properties props = new Properties();
    try (InputStream in =
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream("perc-security.properties")) {
      assertNotNull(in, "classpath:perc-security.properties must exist for comments tests");
      props.load(in);
    }

    String csp = props.getProperty("contentSecurityPolicy");
    assertNotNull(csp, "contentSecurityPolicy property must be present");
    assertTrue(csp.contains("data:"), "CSP must allow data: image URIs, was: " + csp);
    assertTrue(csp.contains("blob:"), "CSP must allow blob: image URIs, was: " + csp);
    assertTrue(
        csp.contains("img-src"),
        "CSP should declare img-src (or equivalent) for image URI schemes, was: " + csp);
  }
}
