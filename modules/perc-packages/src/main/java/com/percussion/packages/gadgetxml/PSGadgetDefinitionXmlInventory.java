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

package com.percussion.packages.gadgetxml;

import com.percussion.packages.inventory.PSDefinitionXmlShipPathInventory;
import com.percussion.packages.inventory.PSDefinitionXmlShipPathInventory.Finding;
import com.percussion.packages.inventory.PSDefinitionXmlShipPathInventory.Kind;
import com.percussion.packages.inventory.PSDefinitionXmlShipPathInventory.Report;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * Product package inventory for committed Gadget definition XML under Packages ship paths
 * {@code sys__UserDependency--rxconfig/Gadgets/*.xml} and {@code rxconfig/Gadgets/*.xml} (Phase 5
 * gate G4 / issue #3581, parent #2630, ADR-004).
 *
 * <p>Peer of {@code PSWidgetDefinitionXmlInventory}. Pass condition: zero product Gadget definition
 * XML. Waiver set is empty after perc.Test page dual-ship exit (#3737) (shared Page/Gadget list).
 *
 * <p>Modern {@code gadget-catalog.json} and per-gadget {@code component-package.json} are not
 * definition XML and are ignored. {@code GadgetRegistry.xml} lives outside Packages ship paths
 * (WebUI dual-load residual / #2852) and is not this gate.
 *
 * @see PSDefinitionXmlShipPathInventory
 */
public final class PSGadgetDefinitionXmlInventory {

  /**
   * Package directory names under {@code Packages/} allowed to still commit Gadget definition XML.
   * Empty after #3737. Do not expand without an ADR / residual issue.
   */
  public static final Set<String> WAIVED_PACKAGE_DIRS =
      PSDefinitionXmlShipPathInventory.WAIVED_PACKAGE_DIRS;

  private PSGadgetDefinitionXmlInventory() {
    // utility
  }

  /**
   * Scan every immediate package directory under {@code packagesRoot} for committed Gadget
   * definition XML under recognized ship paths.
   *
   * @param packagesRoot non-null directory of package roots
   * @return report of findings; never null
   * @throws IOException on I/O failure
   */
  public static Report scan(Path packagesRoot) throws IOException {
    return PSDefinitionXmlShipPathInventory.scan(packagesRoot, Kind.GADGET);
  }

  /**
   * Whether the package directory name is on the explicit waiver list (empty after #3737).
   *
   * @param packageDirName package folder name under Packages
   * @return true if waived
   */
  public static boolean isWaivedPackage(String packageDirName) {
    return PSDefinitionXmlShipPathInventory.isWaivedPackage(packageDirName);
  }

  /**
   * Fail-fast assertion used by Surefire / CI: non-waived Gadget definition XML must not reappear.
   *
   * @param packagesRoot Packages tree root
   * @throws IOException on I/O failure
   * @throws IllegalStateException when non-waived Gadget def XML is present
   */
  public static void assertNoNonWaivedGadgetDefinitionXml(Path packagesRoot) throws IOException {
    PSDefinitionXmlShipPathInventory.assertNoNonWaivedDefinitionXml(packagesRoot, Kind.GADGET);
  }

  /**
   * Resolve package-staging Gadgets directory using portable path segments.
   *
   * @param packageDir package root
   * @return {@code sys__UserDependency--rxconfig/Gadgets} (may not exist)
   */
  public static Path resolveGadgetsDir(Path packageDir) {
    return PSDefinitionXmlShipPathInventory.resolvePrimaryShipDir(packageDir, Kind.GADGET);
  }

  /**
   * Resolve install {@code rxconfig/Gadgets} directory using portable path segments.
   *
   * @param packageDir package root
   * @return {@code rxconfig/Gadgets} (may not exist)
   */
  public static Path resolveRxconfigGadgetsDir(Path packageDir) {
    return PSDefinitionXmlShipPathInventory.resolveRxconfigShipDir(packageDir, Kind.GADGET);
  }

  /**
   * CLI entry for local / optional CI use (non-zero exit when non-waived XML is found).
   *
   * <p>Usage: {@code PSGadgetDefinitionXmlInventory <packagesRoot>}
   *
   * @param args first arg = packages root path
   * @throws IOException on I/O failure
   */
  public static void main(String[] args) throws IOException {
    if (args == null || args.length < 1 || args[0] == null || args[0].isBlank()) {
      System.err.println("Usage: PSGadgetDefinitionXmlInventory <packagesRoot>");
      System.err.println(
          "  packagesRoot e.g. modules/perc-packages/src/main/resources/Packages");
      System.exit(2);
      return;
    }
    Path root = Path.of(args[0]).toAbsolutePath().normalize();
    Report report = scan(root);
    System.out.println(
        "Gadget def XML inventory under "
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
      System.err.println("G4 FAIL: non-waived Gadget definition XML present (#3581)");
      System.exit(1);
    }
    System.out.println("G4 PASS: zero non-waived product Gadget definition XML");
  }
}
