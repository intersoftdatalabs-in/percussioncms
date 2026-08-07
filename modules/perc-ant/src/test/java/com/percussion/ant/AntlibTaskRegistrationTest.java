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
package com.percussion.ant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Guards installer silent-path taskdefs shipped via {@code com/percussion/ant/antlib.xml}.
 *
 * <p>Matrix / {@code perc-devctl qa-up} H2 installs load tasks only through this antlib resource.
 * A task class present on the classpath but missing from antlib fails with {@code failed to create
 * task or type &lt;Name&gt;} mid-install (see #2065 golden smoke / #548 repository password).
 */
@Tag("UnitTest")
public class AntlibTaskRegistrationTest {

  private static final String ANTLIB = "com/percussion/ant/antlib.xml";

  @Test
  void antlibRegistersGenerateRepositoryPassword() throws Exception {
    String xml;
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(ANTLIB)) {
      assertNotNull(in, "classpath must contain " + ANTLIB);
      xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    assertTrue(
        xml.contains("name=\"PSGenerateRepositoryPassword\""),
        "antlib must register PSGenerateRepositoryPassword for silent H2 install");
    assertTrue(
        xml.contains("com.percussion.ant.install.PSGenerateRepositoryPassword"),
        "antlib classname must match install package");
  }

  @Test
  void antlibRegistersStripSampleLocales() throws Exception {
    String xml;
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(ANTLIB)) {
      assertNotNull(in, "classpath must contain " + ANTLIB);
      xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    assertTrue(
        xml.contains("name=\"PSStripSampleLocales\""),
        "antlib must register PSStripSampleLocales for demo-site seed strip (#2303)");
    assertTrue(
        xml.contains("com.percussion.ant.install.PSStripSampleLocales"),
        "antlib classname must match install package");
  }
}
