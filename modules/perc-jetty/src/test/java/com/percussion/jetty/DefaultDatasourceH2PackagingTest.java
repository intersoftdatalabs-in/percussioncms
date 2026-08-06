/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.percussion.jetty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Jetty packaging defaults for #548 H2 embedded repository (QC-013 / T026).
 *
 * <p>Structural tests that guard the shipped {@code perc-ds.properties} and {@code perc.mod}
 * overlays against regressions that would either re-introduce the legacy Derby network server or
 * silently drop the H2 driver as the default embedded repository. Tests resolve their fixture files
 * relative to the module directory so they pass under both single-module and reactor (multi-module)
 * surefire invocations.
 */
@Tag("UnitTest")
class DefaultDatasourceH2PackagingTest {

  @Test
  void percDsPropertiesDefaultToH2() throws Exception {
    Path props = Path.of("src/main/jetty/defaults/etc/perc-ds.properties");
    String text = Files.readString(props, StandardCharsets.UTF_8);
    assertTrue(text.contains("perc.ds.1.driver.name=h2"), text);
    assertTrue(text.contains("org.h2.Driver"), text);
    assertFalse(text.contains("perc.ds.1.driver.name=derby"), text);
  }

  @Test
  void percModDoesNotStartDerbyNetworkServer() throws Exception {
    Path mod = Path.of("src/main/jetty/defaults/modules/perc.mod");
    String text = Files.readString(mod, StandardCharsets.UTF_8);
    assertFalse(text.contains("derby.drda.startNetworkServer"), text);
    assertFalse(text.contains("derby.system.home"), text);
  }
}
