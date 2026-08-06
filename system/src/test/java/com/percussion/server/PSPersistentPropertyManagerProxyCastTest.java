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
package com.percussion.server;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards against casting Spring JDK proxies to {@code PSCmsObjectMgr} concrete class — that fails
 * at runtime during package install post-processing (ClassCastException on $Proxy).
 */
class PSPersistentPropertyManagerProxyCastTest {

  @Test
  @DisplayName("PSPersistentPropertyManager must not cast IPSCmsObjectMgr to PSCmsObjectMgr")
  void noConcreteCastToPsCmsObjectMgr() throws Exception {
    Path src = Paths.get("src/main/java/com/percussion/server/PSPersistentPropertyManager.java");
    if (!Files.isRegularFile(src)) {
      // surefire may run with module-relative or repo-relative cwd
      Path alt =
          Paths.get("system/src/main/java/com/percussion/server/PSPersistentPropertyManager.java");
      if (Files.isRegularFile(alt)) {
        src = alt;
      }
    }
    String text = Files.readString(src, StandardCharsets.UTF_8);
    assertFalse(
        text.contains("impl.PSCmsObjectMgr)"),
        "Do not cast the Spring-proxied IPSCmsObjectMgr bean to PSCmsObjectMgr; "
            + "call methods via IPSCmsObjectMgr only");
  }
}
