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
package com.percussion.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.percussion.utils.jdbc.PSJdbcUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Installer driver-name keys in {@code RxInstaller.properties} are looked up via {@code
 * ResourceBundle.getString(driver)} from {@link RxLogTables} / {@link InstallUtil} when opening
 * JDBC connections during repository install. Missing keys fail schema install with {@code
 * MissingResourceException: key h2} (matrix smoke / silent H2 install).
 */
@Tag("UnitTest")
class RxInstallerPropertiesDriverKeysTest {

  @Test
  void h2AndPostgresqlDriverKeysResolveToJdbcClasses() {
    assertEquals(PSJdbcUtils.H2_DRIVER_CLASS, RxInstallerProperties.getResources().getString("h2"));
    assertEquals(
        PSJdbcUtils.POSTGRES_DRIVER_CLASS,
        RxInstallerProperties.getResources().getString("postgresql"));
    assertEquals(
        PSJdbcUtils.POSTGRES_DRIVER_CLASS,
        RxInstallerProperties.getResources().getString("postgres"));
    // Legacy keys still present
    assertEquals(
        "org.apache.derby.jdbc.EmbeddedDriver",
        RxInstallerProperties.getResources().getString("derby"));
    assertFalse(RxInstallerProperties.getResources().getString("h2").isBlank());
  }
}
