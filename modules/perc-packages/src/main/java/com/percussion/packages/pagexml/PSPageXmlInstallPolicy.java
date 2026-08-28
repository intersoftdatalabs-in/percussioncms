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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * Resolves page-template install mode for a package root (ADR-004 / issues #2786 + #2806 + #3949).
 *
 * <p>Precedence (highest first):
 *
 * <ol>
 *   <li>System property {@value #SYS_PROP_INSTALL_MODE} — {@code native} or {@code dual-ship}
 *   <li>System property {@value #SYS_PROP_DUAL_SHIP} — {@code false}/{@code 0}/{@code off} forces
 *       native when modern pages exist (dual-ship generation off)
 *   <li>Package-local {@value #PACKAGE_INSTALL_PROPS} key {@value #PROP_PAGE_INSTALL_MODE}
 *   <li>Default: {@link PSPageXmlInstallMode#NATIVE} (issue #3949 / parent #2630). Dual-ship is
 *       explicit opt-in only via sysprop or package-local {@code dual-ship}.
 * </ol>
 *
 * <p>Aligns with {@link com.percussion.packages.shim.PSLegacyDefinitionXmlShim} policy: modern
 * authoring is preferred; dual-ship is a time-boxed install bridge, not permanent product
 * authoring.
 */
public final class PSPageXmlInstallPolicy {

  /**
   * Default when no sysprop and no package-local {@value #PROP_PAGE_INSTALL_MODE} is set.
   * Native as of #3949; dual-ship remains available as an explicit override.
   */
  public static final PSPageXmlInstallMode DEFAULT_MODE = PSPageXmlInstallMode.NATIVE;

  /**
   * System property forcing install mode: {@code native} or {@code dual-ship} (case-insensitive).
   */
  public static final String SYS_PROP_INSTALL_MODE = "perc.packages.page.installMode";

  /**
   * System property: when {@code false}/{@code 0}/{@code off}, dual-ship root templateDef generation
   * is disabled (native archive staging used when modern pages are present).
   */
  public static final String SYS_PROP_DUAL_SHIP = "perc.packages.dualShip.pageTemplateDefs";

  /** Package-local properties file at package root. */
  public static final String PACKAGE_INSTALL_PROPS = "package-install.properties";

  /** Property key: {@code native} or {@code dual-ship}. */
  public static final String PROP_PAGE_INSTALL_MODE = "page.installMode";

  private PSPageXmlInstallPolicy() {
    // utility
  }

  /**
   * Resolve install mode for a package directory (or staging copy). Does not require modern pages
   * to be present — callers decide whether to act on modern sources.
   *
   * @param packageDir package source or staging root; may be null → {@link #DEFAULT_MODE} (native)
   * @return non-null mode
   */
  public static PSPageXmlInstallMode resolve(Path packageDir) {
    String sysMode = System.getProperty(SYS_PROP_INSTALL_MODE);
    if (sysMode != null && !sysMode.isBlank()) {
      return parseMode(sysMode.trim());
    }

    String dualShipProp = System.getProperty(SYS_PROP_DUAL_SHIP);
    if (dualShipProp != null && isFalsey(dualShipProp)) {
      return PSPageXmlInstallMode.NATIVE;
    }

    if (packageDir != null) {
      try {
        Properties props = loadPackageInstallProps(packageDir);
        if (props != null) {
          String mode = props.getProperty(PROP_PAGE_INSTALL_MODE);
          if (mode != null && !mode.isBlank()) {
            return parseMode(mode.trim());
          }
        }
      } catch (IOException e) {
        throw new IllegalStateException(
            "Failed reading " + PACKAGE_INSTALL_PROPS + " under " + packageDir + ": " + e.getMessage(),
            e);
      }
    }

    return DEFAULT_MODE;
  }

  /** Whether dual-ship root {@code *.templateDef} materialization should run for this package. */
  public static boolean isDualShipEnabled(Path packageDir) {
    return resolve(packageDir) == PSPageXmlInstallMode.DUAL_SHIP;
  }

  /** Whether native archive staging (no dual-ship root files) is selected. */
  public static boolean isNativeInstallEnabled(Path packageDir) {
    return resolve(packageDir) == PSPageXmlInstallMode.NATIVE;
  }

  static Properties loadPackageInstallProps(Path packageDir) throws IOException {
    Objects.requireNonNull(packageDir, "packageDir");
    Path propsFile = packageDir.resolve(PACKAGE_INSTALL_PROPS);
    if (!Files.isRegularFile(propsFile)) {
      return null;
    }
    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(propsFile)) {
      props.load(in);
    }
    return props;
  }

  static PSPageXmlInstallMode parseMode(String raw) {
    String v = raw.toLowerCase(Locale.ROOT).replace('_', '-').trim();
    return switch (v) {
      case "native", "native-install", "archive" -> PSPageXmlInstallMode.NATIVE;
      case "dual-ship", "dualship", "dual" -> PSPageXmlInstallMode.DUAL_SHIP;
      default -> throw new IllegalArgumentException(
          "Unknown page install mode '"
              + raw
              + "'. Expected 'native' or 'dual-ship' ("
              + SYS_PROP_INSTALL_MODE
              + " / "
              + PROP_PAGE_INSTALL_MODE
              + ").");
    };
  }

  private static boolean isFalsey(String raw) {
    String v = raw.trim().toLowerCase(Locale.ROOT);
    return "false".equals(v) || "0".equals(v) || "off".equals(v) || "no".equals(v);
  }
}
