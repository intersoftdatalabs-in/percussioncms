/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Packaging defaults for #548: new-install embedded repository is H2, not live Derby (QC-013 /
 * T026).
 */
@Tag("UnitTest")
class DefaultEmbeddedH2PackagingTest {

  @Test
  void shippedRxrepositoryDefaultsToH2() throws Exception {
    Path props =
        Path.of("src/main/resources/distribution/rxconfig/Installer/rxrepository.properties");
    String text = Files.readString(props, StandardCharsets.UTF_8);
    assertTrue(text.contains("DB_BACKEND=H2"), text);
    assertTrue(text.contains("DB_DRIVER_NAME=h2"), text);
    assertTrue(text.contains("org.h2.Driver"), text);
    assertFalse(
        text.contains("DB_DRIVER_NAME=derby"),
        "new-install ship props must not default to derby driver");
  }

  @Test
  void installRepositoryDefaultsToH2AndSkipsNetworkServer() throws Exception {
    Path xml = Path.of("src/main/resources/distribution/rxconfig/Installer/installRepository.xml");
    String text = Files.readString(xml, StandardCharsets.UTF_8);
    assertTrue(text.contains("value=\"h2\""), "perc.db.type default must be h2");
    assertFalse(
        text.contains("NetworkServerControl"),
        "distribution installRepository must not start Derby NetworkServer");
    assertFalse(text.contains("port=\"1527\""), "must not wait on Derby DRDA port 1527");
  }
}
