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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.xml;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.error.PSCatalogException;
import java.net.URL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SSRF regression tests for {@link PSDtdTree} (CodeQL {@code java/ssrf}, alert #726, T037
 * residual).
 *
 * <p>Non-file DTD URLs must pass {@code URLValidation} before {@code openConnection}. Malicious
 * targets fail closed as {@link PSCatalogException}.
 */
@DisplayName("PSDtdTree — SSRF (CWE-918) regression tests")
class PSDtdTreeSsrfTest {

  @Test
  @DisplayName("AWS metadata DTD URL is rejected")
  void testAwsMetadataDtdRejected() throws Exception {
    URL malicious = new URL("http://169.254.169.254/latest/meta-data/");
    PSCatalogException ex = assertThrows(PSCatalogException.class, () -> new PSDtdTree(malicious));
    String detail = String.valueOf(ex.getMessage()) + String.valueOf(ex.getCause());
    assertTrue(
        detail.contains("SSRF")
            || detail.contains("reserved")
            || detail.contains("169.254")
            || (ex.getCause() instanceof SecurityException)
            || (ex.getCause() != null && ex.getCause().getCause() instanceof SecurityException),
        "expected SSRF-related failure detail, got: " + detail);
  }

  @Test
  @DisplayName("private IP DTD URL is rejected")
  void testPrivateIpDtdRejected() throws Exception {
    URL malicious = new URL("http://10.0.0.1/evil.dtd");
    assertThrows(PSCatalogException.class, () -> new PSDtdTree(malicious));
  }

  @Test
  @DisplayName("null DTD URL is accepted as empty tree")
  void testNullUrlAccepted() throws Exception {
    // Constructor short-circuits on null without network I/O.
    new PSDtdTree((URL) null);
  }
}
