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
package com.percussion.distribution.install;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Issue #1800 / #1675: after the OWASP Java Encoder migration, full ESAPI config must not ship in
 * the distribution tree. Guards against re-introducing active Ant packaging of the former ESAPI
 * config directory under {@code rxconfig}, or the source resource tree under {@code
 * perc-security-utils}.
 *
 * <p>Assertions target packaging attributes ({@code mkdir}/{@code copy} dirs), not historical
 * comments that may mention ESAPI.
 */
class EsapiPackagingRemovedTest {

  private static final Path INSTALL_DISTRIBUTION_FILES =
      Path.of("src", "main", "resources", "installDistributionFiles.xml");

  private static final Path INSTALL_XML =
      Path.of("src", "main", "resources", "distribution", "rxconfig", "Installer", "install.xml");

  /** Relative to this module: sibling {@code perc-security-utils} resources. */
  private static final Path ESAPI_RESOURCES =
      Path.of("..", "perc-security-utils", "src", "main", "resources", "esapi");

  /** Active Ant dir attributes that package or install the ESAPI config tree. */
  private static final Pattern ESAPI_DIR_ATTR =
      Pattern.compile(
          "(?i)(?:dir|todir)\\s*=\\s*\"[^\"]*(?:rxconfig[/\\\\]esapi|resources[/\\\\]esapi)[^\"]*\"");

  @Test
  @DisplayName("installDistributionFiles.xml must not copy ESAPI resources into the assembly")
  void assemblyScriptDoesNotPackageEsapi() throws Exception {
    assertTrue(
        Files.isRegularFile(INSTALL_DISTRIBUTION_FILES),
        () -> "missing " + INSTALL_DISTRIBUTION_FILES.toAbsolutePath().normalize());
    String xml = Files.readString(INSTALL_DISTRIBUTION_FILES, StandardCharsets.UTF_8);
    assertFalse(
        ESAPI_DIR_ATTR.matcher(xml).find(),
        "installDistributionFiles.xml must not mkdir/copy ESAPI resource paths");
  }

  @Test
  @DisplayName("install.xml must not install ESAPI resources into the install dir")
  void installScriptDoesNotInstallEsapi() throws Exception {
    assertTrue(
        Files.isRegularFile(INSTALL_XML),
        () -> "missing " + INSTALL_XML.toAbsolutePath().normalize());
    String xml = Files.readString(INSTALL_XML, StandardCharsets.UTF_8);
    assertFalse(
        ESAPI_DIR_ATTR.matcher(xml).find(),
        "install.xml must not mkdir/copy ESAPI resource paths during install/upgrade");
  }

  @Test
  @DisplayName("perc-security-utils must not ship src/main/resources/esapi")
  void securityUtilsDoesNotShipEsapiResources() {
    assertFalse(
        Files.exists(ESAPI_RESOURCES),
        () ->
            "unused ESAPI resource tree must be removed: "
                + ESAPI_RESOURCES.toAbsolutePath().normalize());
  }
}
