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
package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH#706: EMS Event List widget package, InstallPackages entries, and CMS EMS REST
 * proxy must not ship in 8.2.
 */
class EmsEventListRemovalTest {

  private static final Path EMS_PACKAGE =
      Path.of("modules/perc-packages/src/main/resources/Packages/perc.widget.emseventlist");

  private static final Path EMS_REST_PROXY =
      Path.of(
          "projects/sitemanage/src/main/java/com/percussion/integrations/ems/rest/PSEmsRestService.java");

  private static final Path DTS_INTEGRATIONS_MODULE =
      Path.of("deliverytiersuite/delivery-tier-suite/integrations");

  private static final List<Path> INSTALL_PACKAGE_LISTS =
      List.of(
          Path.of("system/config/PackageInstaller/InstallPackages.new.xml"),
          Path.of("system/config/PackageInstaller/InstallPackages.dev.xml"),
          Path.of("system/config/PackageInstaller/InstallPackages.upgrade.xml"),
          Path.of(
              "modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/InstallPackages.xml"),
          Path.of(
              "modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/InstallPackages.upgrade.xml"));

  @Test
  void emsPackageAndProxyAreGone() {
    Path root = resolveRepoRoot();
    assertFalse(
        Files.isDirectory(root.resolve(EMS_PACKAGE)),
        "perc.widget.emseventlist package directory must be deleted (GH#706)");
    assertFalse(
        Files.isRegularFile(root.resolve(EMS_REST_PROXY)),
        "PSEmsRestService CMS proxy must be deleted (GH#706)");
    assertFalse(
        Files.isDirectory(root.resolve(DTS_INTEGRATIONS_MODULE)),
        "DTS integrations module (EMS-only) must be deleted (GH#706)");
  }

  @Test
  void installPackagesNoLongerListEmsEventList() throws Exception {
    Path root = resolveRepoRoot();
    for (Path rel : INSTALL_PACKAGE_LISTS) {
      Path list = root.resolve(rel);
      assertTrue(Files.isRegularFile(list), "missing InstallPackages list: " + rel);
      String xml = Files.readString(list, StandardCharsets.UTF_8);
      assertFalse(
          xml.contains("perc.widget.emseventlist"),
          "InstallPackages must not list perc.widget.emseventlist: " + rel);
    }
  }

  @Test
  void deliveryServersNoLongerListPercIntegrations() throws Exception {
    Path root = resolveRepoRoot();
    for (String rel :
        List.of(
            "system/config/delivery-servers.xml",
            "system/config/delivery-servers.xml.dev",
            "system/config/delivery-servers.xml.qa",
            // Test fixtures also scrubbed in this PR — keep covered so they cannot regress
            "projects/sitemanage/src/test/resources/deliveries/FailingToLoadTest.xml",
            "projects/sitemanage/src/test/java/com/percussion/delivery/service/impl/FailingToLoadTest.xml")) {
      Path cfg = root.resolve(rel);
      assertTrue(Files.isRegularFile(cfg), "missing delivery-servers config: " + rel);
      String xml = Files.readString(cfg, StandardCharsets.UTF_8);
      assertFalse(
          xml.contains("perc-integrations"),
          "delivery-servers must not list perc-integrations (GH#706): " + rel);
    }
  }

  /**
   * Walks up from the process working directory until a monorepo marker is found ({@code
   * modules/perc-packages} next to root {@code pom.xml}), so the test is robust when run from repo
   * root, module basedir, or nested CI paths.
   */
  private static Path resolveRepoRoot() {
    Path dir = Path.of("").toAbsolutePath().normalize();
    while (dir != null) {
      if (Files.isDirectory(dir.resolve("modules/perc-packages"))
          && Files.isRegularFile(dir.resolve("pom.xml"))) {
        return dir;
      }
      Path parent = dir.getParent();
      if (parent == null || parent.equals(dir)) {
        break;
      }
      dir = parent;
    }
    fail("could not resolve monorepo root from " + Path.of("").toAbsolutePath().normalize());
    return Path.of("").toAbsolutePath().normalize();
  }
}
