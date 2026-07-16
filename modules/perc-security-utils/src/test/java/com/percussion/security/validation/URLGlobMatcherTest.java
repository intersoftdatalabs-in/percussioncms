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
package com.percussion.security.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("URLGlobMatcher")
class URLGlobMatcherTest {

  @Test
  @DisplayName("normalize lowercases scheme and host, omits default port")
  void testNormalize() throws Exception {
    URL u = new URL("HTTPS://Example.COM/Path?q=1");
    assertEquals("https://example.com/Path?q=1", URLGlobMatcher.normalize(u));
  }

  @Test
  @DisplayName("normalize includes explicit port")
  void testNormalizePort() throws Exception {
    URL u = new URL("http://hr.internal:8080/api");
    assertEquals("http://hr.internal:8080/api", URLGlobMatcher.normalize(u));
  }

  @Test
  @DisplayName("glob matches path prefix")
  void testGlobPath() throws Exception {
    String n = URLGlobMatcher.normalize(new URL("https://api.example.com/v1/x"));
    assertTrue(URLGlobMatcher.matches("https://api.example.com/v1/*", n));
  }

  @Test
  @DisplayName("glob path mismatch fails")
  void testGlobPathMismatch() throws Exception {
    String n = URLGlobMatcher.normalize(new URL("https://api.example.com/v2/x"));
    assertFalse(URLGlobMatcher.matches("https://api.example.com/v1/*", n));
  }

  @Test
  @DisplayName("lone star never matches")
  void testLoneStar() {
    assertFalse(URLGlobMatcher.matches("*", "https://evil.com/"));
  }
}
