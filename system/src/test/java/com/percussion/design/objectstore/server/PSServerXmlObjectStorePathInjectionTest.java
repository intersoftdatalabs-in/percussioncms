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
package com.percussion.design.objectstore.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.utils.io.PathUtils;
import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Path-injection barrier for application request-root resolution (CodeQL {@code
 * java/path-injection} #2001 / #1988–#2000).
 */
@DisplayName("PSServerXmlObjectStore.getAppRootDir path-injection barrier (CWE-22, #2001)")
class PSServerXmlObjectStorePathInjectionTest {

  @TempDir Path tmp;

  @AfterEach
  void clearThreadRxDir() {
    PathUtils.unsetThreadOnlyRxDir(tmp.toFile());
  }

  @Test
  void getAppRootDir_rejectsParentTraversal() {
    PathUtils.setThreadOnlyRxDir(tmp.toFile());
    assertThrows(
        IllegalArgumentException.class,
        () -> PSServerXmlObjectStore.getAppRootDir(".." + File.separator + "escape"));
  }

  @Test
  void getAppRootDir_resolvesChildUnderRxDir() throws Exception {
    File rx = tmp.toFile();
    PathUtils.setThreadOnlyRxDir(rx);
    File resolved = PSServerXmlObjectStore.getAppRootDir("MyApp");
    assertEquals(new File(rx, "MyApp").getCanonicalFile(), resolved.getCanonicalFile());
    assertTrue(resolved.getCanonicalFile().toPath().startsWith(rx.getCanonicalFile().toPath()));
  }

  @Test
  void getAppRootDir_rejectsBlank() {
    PathUtils.setThreadOnlyRxDir(tmp.toFile());
    assertThrows(IllegalArgumentException.class, () -> PSServerXmlObjectStore.getAppRootDir(""));
    assertThrows(IllegalArgumentException.class, () -> PSServerXmlObjectStore.getAppRootDir(null));
  }
}
