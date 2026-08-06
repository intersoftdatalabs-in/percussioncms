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
package com.percussion.preinstall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Regression for #2192 / #1750: Phase-1 {@code demo-sites} options must drive ANT {@code
 * -Dinstall.demo.sites=…} even when the preinstall JVM has no matching system property.
 *
 * <p>Slice-1 evidence (#2191) showed {@code cms.demo-sites=true} in last-install while ANT received
 * {@code install.demo.sites=false} because {@code Main.execJar} only read {@link System}
 * properties.
 */
@Tag("UnitTest")
class MainDemoSitesAntFlagTest {

  private String prevDemoSitesSysProp;

  @BeforeEach
  void captureSysProp() {
    prevDemoSitesSysProp =
        System.getProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY);
  }

  @AfterEach
  void restoreSysProp() {
    if (prevDemoSitesSysProp == null) {
      System.clearProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY);
    } else {
      System.setProperty(
          DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY, prevDemoSitesSysProp);
    }
  }

  @Test
  void optionsDemoSitesTrueYieldsTrueAntArgWithoutJvmSystemProperty() {
    System.clearProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY);

    Map<String, String> options = new HashMap<>();
    options.put(DbInstallConfigResolver.DEMO_SITES_KEY, "true");

    assertTrue(Main.resolveDemoSitesForAnt(options));
    assertEquals(
        "-D" + DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY + "=true",
        Main.demoSitesAntSystemPropertyArg(options));
  }

  @Test
  void optionsDemoSitesFalseYieldsFalseAntArg() {
    System.clearProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY);

    Map<String, String> options = Map.of(DbInstallConfigResolver.DEMO_SITES_KEY, "false");

    assertFalse(Main.resolveDemoSitesForAnt(options));
    assertEquals(
        "-D" + DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY + "=false",
        Main.demoSitesAntSystemPropertyArg(options));
  }

  @Test
  void emptyOptionsWithoutSysPropDefaultsFalse() {
    System.clearProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY);

    assertFalse(Main.resolveDemoSitesForAnt(Map.of()));
    assertFalse(Main.resolveDemoSitesForAnt(null));
    assertEquals(
        "-D" + DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY + "=false",
        Main.demoSitesAntSystemPropertyArg(null));
  }

  @Test
  void systemPropertyAloneStillHonoredWhenOptionsOmitKey() {
    System.setProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY, "true");

    assertTrue(Main.resolveDemoSitesForAnt(Map.of()));
    assertEquals(
        "-D" + DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY + "=true",
        Main.demoSitesAntSystemPropertyArg(Map.of()));
  }

  @Test
  void optionsKeyWinsOverConflictingSystemProperty() {
    // parseDemoSitesFlag prefers CLI/options over System property when both set.
    System.setProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY, "true");
    Map<String, String> options = Map.of(DbInstallConfigResolver.DEMO_SITES_KEY, "false");

    assertFalse(Main.resolveDemoSitesForAnt(options));
    assertEquals(
        "-D" + DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY + "=false",
        Main.demoSitesAntSystemPropertyArg(options));
  }

  @Test
  void truthyAliasesFromWizardOptionsPropagate() {
    System.clearProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY);

    assertTrue(
        Main.resolveDemoSitesForAnt(
            Map.of(DbInstallConfigResolver.DEMO_SITES_KEY, "yes")));
    assertTrue(
        Main.resolveDemoSitesForAnt(Map.of(DbInstallConfigResolver.DEMO_SITES_KEY, "Y")));
    assertTrue(
        Main.resolveDemoSitesForAnt(Map.of(DbInstallConfigResolver.DEMO_SITES_KEY, "1")));
  }

  @Test
  void installDemoSitesAliasInOptionsMapPropagates() {
    System.clearProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY);

    // parseDemoSitesFlag also accepts the ANT property name as an options key.
    assertTrue(
        Main.resolveDemoSitesForAnt(
            Map.of(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY, "true")));
    assertEquals(
        "-D" + DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY + "=true",
        Main.demoSitesAntSystemPropertyArg(
            Map.of(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY, "true")));
  }
}
