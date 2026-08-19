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

package com.percussion.packages.pagexml;

import com.percussion.packages.inventory.PSDefinitionXmlShipPathInventory;
import com.percussion.packages.inventory.PSDefinitionXmlShipPathInventory.Finding;
import com.percussion.packages.inventory.PSDefinitionXmlShipPathInventory.Kind;
import com.percussion.packages.inventory.PSDefinitionXmlShipPathInventory.Report;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * Product package inventory for committed Page definition XML under Packages ship paths
 * {@code sys__UserDependency--rxconfig/Pages/*.xml} and {@code rxconfig/Pages/*.xml} (Phase 5 gate
 * G4 / issue #3581, parent #2630, ADR-004).
 *
 * <p>Peer of {@code PSWidgetDefinitionXmlInventory}. Pass condition: zero <em>non-waived</em>
 * product Page definition XML. The only explicit waiver is {@code perc.Test}.
 *
 * <p>Modern authoring under {@code pages/&lt;id&gt;/component-package.json} is not definition XML
 * and is ignored. Dual-ship / native {@code *.templateDef} install materialization is a separate
 * surface (not this gate).
 *
 * @see PSDefinitionXmlShipPathInventory
 */
public final class PSPageDefinitionXmlInventory {

  /**
   * Package directory names under {@code Packages/} allowed to still commit Page definition XML.
   * Explicit and minimal — do not expand without an ADR / residual issue.
   */
  public static final Set<String> WAIVED_PACKAGE_DIRS =
      PSDefinitionXmlShipPathInventory.WAIVED_PACKAGE_DIRS;

  private PSPageDefinitionXmlInventory() {
    // utility
  }

  /**
   * Scan every immediate package directory under {@code packagesRoot} for committed Page definition
   * XML under recognized ship paths.
   *
   * @param packagesRoot non-null directory of package roots
   * @return report of findings; never null
   * @throws IOException on I/O failure
   */
  public static Report scan(Path packagesRoot) throws IOException {
    return PSDefinitionXmlShipPathInventory.scan(packagesRoot, Kind.PAGE);
  }

  /**
   * Whether the package directory name is on the explicit waiver list ({@code perc.Test} only).
   *
   * @param packageDirName package folder name under Packages
   * @return true if waived
   */
  public static boolean isWaivedPackage(String packageDirName) {
    return PSDefinitionXmlShipPathInventory.isWaivedPackage(packageDirName);
  }

  /**
   * Fail-fast assertion used by Surefire / CI: non-waived Page definition XML must not reappear.
   *
   * @param packagesRoot Packages tree root
   * @throws IOException on I/O failure
   * @throws IllegalStateException when non-waived Page def XML is present
   */
  public static void assertNoNonWaivedPageDefinitionXml(Path packagesRoot) throws IOException {
    PSDefinitionXmlShipPathInventory.assertNoNonWaivedDefinitionXml(packagesRoot, Kind.PAGE);
  }

  /**
   * Resolve package-staging Pages directory using portable path segments.
   *
   * @param packageDir package root
   * @return {@code sys__UserDependency--rxconfig/Pages} (may not exist)
   */
  public static Path resolvePagesDir(Path packageDir) {
    return PSDefinitionXmlShipPathInventory.resolvePrimaryShipDir(packageDir, Kind.PAGE);
  }

  /**
   * Resolve install {@code rxconfig/Pages} directory using portable path segments.
   *
   * @param packageDir package root
   * @return {@code rxconfig/Pages} (may not exist)
   */
  public static Path resolveRxconfigPagesDir(Path packageDir) {
    return PSDefinitionXmlShipPathInventory.resolveRxconfigShipDir(packageDir, Kind.PAGE);
  }

  /**
   * CLI entry for local / optional CI use (non-zero exit when non-waived XML is found).
   *
   * <p>Usage: {@code PSPageDefinitionXmlInventory <packagesRoot>}
   *
   * @param args first arg = packages root path
   * @throws IOException on I/O failure
   */
  public static void main(String[] args) throws IOException {
    if (args == null || args.length < 1 || args[0] == null || args[0].isBlank()) {
      System.err.println("Usage: PSPageDefinitionXmlInventory <packagesRoot>");
      System.err.println(
          "  packagesRoot e.g. modules/perc-packages/src/main/resources/Packages");
      System.exit(2);
      return;
    }
    Path root = Path.of(args[0]).toAbsolutePath().normalize();
    Report report = scan(root);
    System.out.println(
        "Page def XML inventory under "
            + root
            + ": total="
            + report.all().size()
            + " waived="
            + report.waived().size()
            + " nonWaived="
            + report.nonWaived().size()
            + " waivedPackages="
            + WAIVED_PACKAGE_DIRS);
    for (Finding f : report.all()) {
      System.out.println(
          (f.waived() ? "WAIVED " : "FAIL   ") + f.packageDirName() + " " + f.xmlPath());
    }
    if (!report.isClean()) {
      System.err.println("G4 FAIL: non-waived Page definition XML present (#3581)");
      System.exit(1);
    }
    System.out.println("G4 PASS: zero non-waived product Page definition XML");
  }
}
