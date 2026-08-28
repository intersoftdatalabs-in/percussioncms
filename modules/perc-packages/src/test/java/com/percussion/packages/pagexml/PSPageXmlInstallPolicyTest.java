/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Page install-mode precedence: sysprop &gt; package-local &gt; native default (issue #3949 /
 * parent #2630). Dual-ship is explicit opt-in only.
 *
 * <p>Cross-platform: path construction uses {@link Path#resolve(String)} / {@link Files}.
 */
class PSPageXmlInstallPolicyTest {

  @TempDir Path tempDir;

  @AfterEach
  void clearSysProps() {
    System.clearProperty(PSPageXmlInstallPolicy.SYS_PROP_INSTALL_MODE);
    System.clearProperty(PSPageXmlInstallPolicy.SYS_PROP_DUAL_SHIP);
  }

  @Test
  void defaultMode_isNative() {
    assertEquals(PSPageXmlInstallMode.NATIVE, PSPageXmlInstallPolicy.DEFAULT_MODE);
  }

  @Test
  void resolve_nullPackageDir_isNative() {
    assertEquals(PSPageXmlInstallMode.NATIVE, PSPageXmlInstallPolicy.resolve(null));
    assertTrue(PSPageXmlInstallPolicy.isNativeInstallEnabled(null));
    assertFalse(PSPageXmlInstallPolicy.isDualShipEnabled(null));
  }

  @Test
  void resolve_unconfiguredPackageDir_isNative() throws Exception {
    Path pkg = tempDir.resolve("pkg-unconfigured");
    Files.createDirectories(pkg);
    assertEquals(PSPageXmlInstallMode.NATIVE, PSPageXmlInstallPolicy.resolve(pkg));
    assertTrue(PSPageXmlInstallPolicy.isNativeInstallEnabled(pkg));
    assertFalse(PSPageXmlInstallPolicy.isDualShipEnabled(pkg));
  }

  @Test
  void resolve_propsFileWithoutModeKey_isNative() throws Exception {
    Path pkg = tempDir.resolve("pkg-empty-props");
    Files.createDirectories(pkg);
    Files.writeString(
        pkg.resolve(PSPageXmlInstallPolicy.PACKAGE_INSTALL_PROPS),
        "# no page.installMode key\n",
        StandardCharsets.UTF_8);
    assertEquals(PSPageXmlInstallMode.NATIVE, PSPageXmlInstallPolicy.resolve(pkg));
  }

  @Test
  void resolve_packageLocalDualShip_whenNoSysProp() throws Exception {
    Path pkg = writeInstallMode("pkg-dual", "dual-ship");
    assertEquals(PSPageXmlInstallMode.DUAL_SHIP, PSPageXmlInstallPolicy.resolve(pkg));
    assertTrue(PSPageXmlInstallPolicy.isDualShipEnabled(pkg));
    assertFalse(PSPageXmlInstallPolicy.isNativeInstallEnabled(pkg));
  }

  @Test
  void resolve_packageLocalNative_whenNoSysProp() throws Exception {
    Path pkg = writeInstallMode("pkg-native", "native");
    assertEquals(PSPageXmlInstallMode.NATIVE, PSPageXmlInstallPolicy.resolve(pkg));
  }

  @Test
  void resolve_sysPropInstallMode_overridesPackageLocalNative() throws Exception {
    Path pkg = writeInstallMode("pkg-override-native", "native");
    System.setProperty(PSPageXmlInstallPolicy.SYS_PROP_INSTALL_MODE, "dual-ship");
    assertEquals(PSPageXmlInstallMode.DUAL_SHIP, PSPageXmlInstallPolicy.resolve(pkg));
  }

  @Test
  void resolve_sysPropInstallMode_overridesPackageLocalDualShip() throws Exception {
    Path pkg = writeInstallMode("pkg-override-dual", "dual-ship");
    System.setProperty(PSPageXmlInstallPolicy.SYS_PROP_INSTALL_MODE, "native");
    assertEquals(PSPageXmlInstallMode.NATIVE, PSPageXmlInstallPolicy.resolve(pkg));
  }

  @Test
  void resolve_precedence_sysPropThenPackageLocalThenNativeDefault() throws Exception {
    Path unconfigured = tempDir.resolve("pkg-default");
    Files.createDirectories(unconfigured);
    Path localDual = writeInstallMode("pkg-local-dual", "dual-ship");

    assertEquals(
        PSPageXmlInstallMode.NATIVE,
        PSPageXmlInstallPolicy.resolve(unconfigured),
        "no sysprop, no package-local → native default");
    assertEquals(
        PSPageXmlInstallMode.DUAL_SHIP,
        PSPageXmlInstallPolicy.resolve(localDual),
        "no sysprop, package-local dual-ship → dual-ship");

    System.setProperty(PSPageXmlInstallPolicy.SYS_PROP_INSTALL_MODE, "native");
    assertEquals(
        PSPageXmlInstallMode.NATIVE,
        PSPageXmlInstallPolicy.resolve(localDual),
        "sysprop native overrides package-local dual-ship");

    System.setProperty(PSPageXmlInstallPolicy.SYS_PROP_INSTALL_MODE, "dual-ship");
    assertEquals(
        PSPageXmlInstallMode.DUAL_SHIP,
        PSPageXmlInstallPolicy.resolve(unconfigured),
        "sysprop dual-ship overrides native default");
  }

  @Test
  void resolve_dualShipFalseSysProp_forcesNativeOverPackageLocalDualShip() throws Exception {
    Path pkg = writeInstallMode("pkg-falsey", "dual-ship");
    System.setProperty(PSPageXmlInstallPolicy.SYS_PROP_DUAL_SHIP, "false");
    assertEquals(PSPageXmlInstallMode.NATIVE, PSPageXmlInstallPolicy.resolve(pkg));
  }

  @Test
  void parseMode_unknown_throws() {
    IllegalArgumentException err =
        assertThrows(IllegalArgumentException.class, () -> PSPageXmlInstallPolicy.parseMode("bogus"));
    assertTrue(err.getMessage().contains("bogus"));
    assertTrue(err.getMessage().contains("native"));
  }

  private Path writeInstallMode(String dirName, String mode) throws Exception {
    Path pkg = tempDir.resolve(dirName);
    Files.createDirectories(pkg);
    Files.writeString(
        pkg.resolve(PSPageXmlInstallPolicy.PACKAGE_INSTALL_PROPS),
        PSPageXmlInstallPolicy.PROP_PAGE_INSTALL_MODE + "=" + mode + "\n",
        StandardCharsets.UTF_8);
    return pkg;
  }
}
