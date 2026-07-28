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
package com.percussion.ant.install;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Guards #548 installer classpath: Derby→H2 migration uses TableFactory {@code Class.forName} on
 * the perc-ant system classloader ({@code java -jar perc-ant-*.jar}). Both engines must resolve
 * without relying on the Ant {@code ant.deps} child loader (jetty JDBC lib).
 */
class PSMigrationEmbeddedDriversClasspathTest {

  @Test
  void h2DriverLoadable() {
    Class<?> driver =
        assertDoesNotThrow(
            () -> Class.forName("org.h2.Driver"),
            "org.h2.Driver must be a compile dependency of perc-ant so the shaded installer JAR can"
                + " import into H2 during upgrade (field failure: Unable to connect to database"
                + " server: org.h2.Driver after Derby export)");
    assertNotNull(driver);
  }

  @Test
  void derbyAutoloadedDriverLoadable() {
    // Derby 10.17+ primary entry; legacy EmbeddedDriver may be absent
    Class<?> driver =
        assertDoesNotThrow(
            () -> Class.forName("org.apache.derby.iapi.jdbc.AutoloadedDriver"),
            "Derby AutoloadedDriver must remain on perc-ant for TableFactory export from product"
                + " managed Derby repositories");
    assertNotNull(driver);
  }
}
