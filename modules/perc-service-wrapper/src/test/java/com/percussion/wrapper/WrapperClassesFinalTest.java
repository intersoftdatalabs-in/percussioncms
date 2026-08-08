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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Behavioral coverage for DTS/Jetty start wrappers: {@code final} classes so constructors do not
 * need {@code this-escape} suppressions (issue #2025).
 */
public class WrapperClassesFinalTest {

  @TempDir File tempDir;

  @Test
  @DisplayName("DtsStartWrapper is final so StartWrapper methods cannot be overridden by a subclass")
  void dtsStartWrapperIsFinal() {
    assertTrue(Modifier.isFinal(DtsStartWrapper.class.getModifiers()));
  }

  @Test
  @DisplayName("JettyStartWrapper is final so StartWrapper methods cannot be overridden by a subclass")
  void jettyStartWrapperIsFinal() {
    assertTrue(Modifier.isFinal(JettyStartWrapper.class.getModifiers()));
  }

  @Test
  @DisplayName("DtsStartWrapper without perc-catalina.properties is inactive and not installed")
  void dtsWrapperInactiveWhenPropertiesMissing() {
    DtsStartWrapper wrapper =
        new DtsStartWrapper("Production DTS", tempDir, new String[] {"--extra"});
    assertFalse(wrapper.isActive());
    assertTrue(wrapper.getName().contains("DTS"));
  }

  @Test
  @DisplayName("DtsStartWrapper active when perc-catalina.properties present")
  void dtsWrapperActiveWhenPropertiesPresent() throws Exception {
    File confPerc = new File(tempDir, "conf" + File.separator + "perc");
    assertTrue(confPerc.mkdirs());
    File props = new File(confPerc, "perc-catalina.properties");
    Files.writeString(
        props.toPath(),
        "http.port=9980\nshutdown.port=5010\nshutdown.key=SHUTDOWN\n",
        StandardCharsets.UTF_8);

    DtsStartWrapper wrapper = new DtsStartWrapper("Production DTS", tempDir, null);
    assertTrue(wrapper.isActive());
    assertTrue(wrapper.getPort() == 9980);
    assertTrue(wrapper.getShutdownPort() == 5010);
    assertTrue(wrapper.getStartCmd() != null && wrapper.getStartCmd().length > 0);
  }

  @Test
  @DisplayName("JettyStartWrapper without start.jar is not installed")
  void jettyWrapperNotInstalledWithoutStartJar() {
    JettyStartWrapper wrapper = new JettyStartWrapper("Jetty", tempDir, new String[] {});
    assertFalse(wrapper.isActive());
  }
}
