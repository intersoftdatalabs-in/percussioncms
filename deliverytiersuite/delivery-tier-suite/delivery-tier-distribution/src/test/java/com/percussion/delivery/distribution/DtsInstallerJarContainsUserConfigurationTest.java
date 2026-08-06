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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.delivery.distribution;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/**
 * Regression guard for issue #1825.
 *
 * <p>{@code InstallerUserSettings} opens {@code com.intsof.common.utilities.UserConfiguration} (and
 * {@code AppConfigurationFolder}) when the DTS interactive wizard starts. Those classes live in
 * {@code com.intsof.common:utilities}, which is a compile dependency but is not on a thin {@code
 * java -jar} classpath unless unpack-staged into {@code project.build.outputDirectory} (see {@code
 * unpack-userconfiguration} in this module's pom).
 *
 * <p>Without the unpack, the installer dies immediately with {@code NoClassDefFoundError:
 * com/intsof/common/utilities/UserConfiguration}. This test asserts the shipping jar contains the
 * required classes. No-op when run outside Maven (no build artifact on disk). The antrun {@code
 * verify-pathvalidation-shaded} gate also asserts the same entries at verify-time.
 */
class DtsInstallerJarContainsUserConfigurationTest {

  private static final String USER_CONFIGURATION_CLASS =
      "com/intsof/common/utilities/UserConfiguration.class";
  private static final String APP_CONFIGURATION_FOLDER_CLASS =
      "com/intsof/common/utilities/AppConfigurationFolder.class";

  @Test
  void shippingJarBundlesUserConfigurationClasses() {
    Path jar = Path.of("target", "delivery-tier-distribution.jar");
    if (!Files.isRegularFile(jar)) {
      return;
    }

    Set<String> entries = new HashSet<>();
    try (ZipFile zip = new ZipFile(jar.toFile())) {
      zip.stream().map(java.util.zip.ZipEntry::getName).forEach(entries::add);
    } catch (IOException io) {
      fail("Could not read " + jar + ": " + io.getMessage());
    }

    assertTrue(
        entries.contains(USER_CONFIGURATION_CLASS),
        () ->
            "Expected "
                + USER_CONFIGURATION_CLASS
                + " in "
                + jar
                + ". InstallerUserSettings requires it at wizard start (GH-1825). Check"
                + " maven-dependency-plugin execution unpack-userconfiguration stages"
                + " com.intsof.common:utilities into project.build.outputDirectory.");
    assertTrue(
        entries.contains(APP_CONFIGURATION_FOLDER_CLASS),
        () ->
            "Expected "
                + APP_CONFIGURATION_FOLDER_CLASS
                + " in "
                + jar
                + ". InstallerUserSettings uses AppConfigurationFolder with"
                + " UserConfiguration (GH-1825). Check unpack-userconfiguration.");
  }
}
