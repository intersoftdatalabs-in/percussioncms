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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("UnitTest")
class InteractiveDbConfigCollectorTest {

  @TempDir Path tempDir;

  @Test
  void hasExplicitDbOverrideDetectsDbpropsAndStructured() {
    assertFalse(InteractiveDbConfigCollector.hasExplicitDbOverride(Map.of()));
    assertTrue(
        InteractiveDbConfigCollector.hasExplicitDbOverride(Map.of("dbprops", "/tmp/x.properties")));
    assertTrue(
        InteractiveDbConfigCollector.hasExplicitDbOverride(Map.of("db.type", "mysql")));
    assertTrue(
        InteractiveDbConfigCollector.hasExplicitDbOverride(Map.of("db.host", "h")));
    assertFalse(
        InteractiveDbConfigCollector.hasExplicitDbOverride(Map.of("db.type", "h2")));
  }

  @Test
  void upgradeSkipsPrompts() {
    ScriptedPrompt prompt = new ScriptedPrompt();
    Map<String, String> out =
        InteractiveDbConfigCollector.collect(Map.of(), true, prompt);
    assertTrue(out.isEmpty());
    assertTrue(prompt.outputsAsString().toLowerCase().contains("upgrade"));
  }

  @Test
  void defaultMenuSelectsH2() {
    ScriptedPrompt prompt = new ScriptedPrompt("");
    Map<String, String> out =
        InteractiveDbConfigCollector.collect(new HashMap<>(), false, prompt);
    assertEquals("h2", out.get("db.type"));
  }

  @Test
  void loadPropertiesFileOption() throws Exception {
    Path props = tempDir.resolve("rxrepository.mysql.properties");
    Files.writeString(
        props,
        """
        DB_BACKEND=MYSQL
        DB_DRIVER_NAME=mysql
        DB_DRIVER_CLASS_NAME=org.mariadb.jdbc.Driver
        DB_SERVER=//localhost:3306/percussion
        DB_NAME=percussion
        UID=cms
        PWD=secret
        """,
        StandardCharsets.UTF_8);
    ScriptedPrompt prompt = new ScriptedPrompt("6", props.toString());
    Map<String, String> out =
        InteractiveDbConfigCollector.collect(new HashMap<>(), false, prompt);
    assertEquals(props.toAbsolutePath().normalize().toString(), out.get("dbprops"));
    DbInstallConfigResolver.ResolvedDbConfig cfg =
        DbInstallConfigResolver.resolveDbConfig(out);
    assertEquals("mysql", cfg.systemProperties().get("perc.db.type"));
  }

  private static final class ScriptedPrompt implements InstallPrompt {
    private final Deque<String> answers;
    private final List<String> outputs = new ArrayList<>();

    ScriptedPrompt(String... answers) {
      this.answers = new ArrayDeque<>();
      for (String a : answers) {
        this.answers.addLast(a);
      }
    }

    @Override
    public void print(String message) {
      outputs.add(message == null ? "" : message);
    }

    @Override
    public void println(String message) {
      outputs.add((message == null ? "" : message) + "\n");
    }

    @Override
    public String readLine(String prompt) {
      print(prompt);
      return answers.isEmpty() ? "" : answers.removeFirst();
    }

    @Override
    public String readPassword(String prompt) {
      return readLine(prompt);
    }

    String outputsAsString() {
      StringBuilder sb = new StringBuilder();
      for (String o : outputs) {
        sb.append(o);
      }
      return sb.toString();
    }
  }
}
