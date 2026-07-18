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
package com.percussion.delivery.comments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Open-redirect helpers for {@link PSCommentsRestService} (CodeQL {@code
 * java/unvalidated-url-redirection} #643/#644, T051 review).
 */
@DisplayName("PSCommentsRestService redirect helpers (T051)")
class PSCommentsRestServiceRedirectTest {

  @Test
  void absoluteHttpRefererBecomesRelativePathAndQuery() {
    assertEquals(
        "/page?lastCommentId=1",
        PSCommentsRestService.toRelativeRedirectTarget(
            "https://evil.example/page?lastCommentId=1"));
  }

  @Test
  void relativePathPreserved() {
    assertEquals("/site/page", PSCommentsRestService.toRelativeRedirectTarget("/site/page"));
  }

  @Test
  void protocolRelativeRejected() {
    assertNull(PSCommentsRestService.toRelativeRedirectTarget("//evil.example/phish"));
  }

  @Test
  void sanitizeForLogStripsControlChars() {
    String dirty = "ok\r\nInjected: value";
    String clean = PSCommentsRestService.sanitizeForLog(dirty);
    assertTrue(clean.contains("ok"));
    assertTrue(clean.contains("Injected"));
    // CR/LF are allowed tab/newline/cr class — we strip other Cntrl; CR/LF kept by regex
    // Ensure BEL and similar are stripped
    assertEquals("ab", PSCommentsRestService.sanitizeForLog("a\u0007b"));
  }
}
