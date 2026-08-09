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

package com.percussion.HTTPClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Behavioral tests for typed URI scheme maps after rawtypes cleanup (#2460). */
@DisplayName("HTTPClient URI typed scheme maps")
class URIDefaultPortsGenericsTest {

  @Test
  @DisplayName("defaultPort reads typed ConcurrentHashMap entries")
  void defaultPortFromTypedMap() {
    assertEquals(80, URI.defaultPort("http"));
    assertEquals(443, URI.defaultPort("HTTPS"));
    assertEquals(21, URI.defaultPort("ftp"));
  }

  @Test
  @DisplayName("generic and semi-generic scheme flags remain true")
  void schemeSyntaxFlags() {
    assertTrue(URI.usesGenericSyntax("http"));
    assertTrue(URI.usesGenericSyntax("HTTPS"));
    assertTrue(URI.usesSemiGenericSyntax("ldap"));
  }
}
