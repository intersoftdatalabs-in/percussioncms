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
package com.percussion.wrapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.nio.file.Files;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Behavioral coverage for {@link MainProxy}: construction without raw {@code Class} usage and
 * graceful failure when the start jar is not a real Jetty start jar (issue #2025).
 */
public class MainProxyTest {

  @TempDir File tempDir;

  @Test
  @DisplayName("constructor with non-jetty jar does not throw")
  void constructorWithBogusJarDoesNotThrow() throws Exception {
    File jar = new File(tempDir, "not-start.jar");
    Files.writeString(jar.toPath(), "not-a-jar");
    assertDoesNotThrow(() -> new MainProxy(jar));
  }

  @Test
  @DisplayName("processCommandLine is safe when start jar is not a real Jetty archive")
  void processCommandLineSafeWithBogusJar() throws Exception {
    // Main may still resolve from the parent classloader (jetty-start test dependency).
    // Either a StartArgsProxy or null is acceptable; the call must not throw.
    File jar = new File(tempDir, "empty.jar");
    Files.writeString(jar.toPath(), "x");
    MainProxy proxy = new MainProxy(jar);
    assertDoesNotThrow(() -> proxy.processCommandLine(new String[] {"--version"}));
  }
}
