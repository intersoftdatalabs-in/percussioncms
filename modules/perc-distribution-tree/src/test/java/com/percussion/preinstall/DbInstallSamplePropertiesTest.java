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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class DbInstallSamplePropertiesTest {

  @Test
  void samplePropertyFilesExistInDistributionTree() {
    Path samples = Path.of("src/main/resources/distribution/rxconfig/Installer/samples");
    assertTrue(
        Files.isDirectory(samples), "samples directory missing: " + samples.toAbsolutePath());
    assertTrue(Files.isRegularFile(samples.resolve("rxrepository.mysql.properties")));
    assertTrue(Files.isRegularFile(samples.resolve("rxrepository.sqlserver.properties")));
    assertTrue(Files.isRegularFile(samples.resolve("rxrepository.oracle.properties")));
  }
}
