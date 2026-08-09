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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Documents intentional {@code getDeclaredConstructor()} use for HTTPClient modules (#2460 review).
 *
 * <p>Built-in modules are often package-private with package-private no-arg constructors; {@code
 * Class.getConstructor()} (public-only) would fail for them.
 */
@DisplayName("HTTPConnection module instantiation")
class HTTPConnectionModuleInstantiationTest {

  @Test
  @DisplayName("package-private DefaultModule has no public constructor")
  void packagePrivateModuleHasNoPublicConstructor() {
    assertThrows(NoSuchMethodException.class, () -> DefaultModule.class.getConstructor());
  }

  @Test
  @DisplayName("getDeclaredConstructor instantiates package-private DefaultModule")
  void declaredConstructorInstantiatesPackagePrivateModule() throws Exception {
    HTTPClientModule instance = DefaultModule.class.getDeclaredConstructor().newInstance();
    assertNotNull(instance);
  }

  @Test
  @DisplayName("addModule accepts package-private DefaultModule class")
  void addModuleAcceptsPackagePrivateDefaultModule() {
    HTTPConnection conn = new HTTPConnection("localhost", 80);
    // May already be on the default list; remove first so add can succeed.
    conn.removeModule(DefaultModule.class);
    assertTrue(conn.addModule(DefaultModule.class, 0));
    assertTrue(conn.removeModule(DefaultModule.class));
  }
}
