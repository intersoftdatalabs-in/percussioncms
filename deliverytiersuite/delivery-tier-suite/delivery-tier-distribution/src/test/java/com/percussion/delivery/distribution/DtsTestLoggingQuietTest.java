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
package com.percussion.delivery.distribution;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * DTS unit tests must not flood the Maven reactor with Hibernate SQL. Test
 * resources use {@code log4j2-test.xml} (Log4j2 auto-load) and {@code
 * hibernate.show_sql=false}.
 */
class DtsTestLoggingQuietTest {

  private static final Path DTS_ROOT = Path.of("..");

  private static final List<String> SERVICE_MODULES =
      List.of(
          "comments",
          "common",
          "feeds",
          "forms",
          "membership",
          "metadata",
          "polls",
          "secure-membership",
          "p13n-ds");

  @Test
  void serviceModulesShipLog4j2TestXml() {
    for (String module : SERVICE_MODULES) {
      Path xml = DTS_ROOT.resolve(module).resolve("src").resolve("test").resolve("resources")
          .resolve("log4j2-test.xml");
      assertTrue(Files.isRegularFile(xml), "missing " + xml.toAbsolutePath());
    }
  }

  @Test
  void testResourcesDoNotEnableHibernateShowSql() throws Exception {
    List<String> offenders = new ArrayList<>();
    List<String> modules = new ArrayList<>(SERVICE_MODULES);
    modules.add("delivery-tier-distribution");
    for (String module : modules) {
      Path testRoot = DTS_ROOT.resolve(module).resolve("src").resolve("test");
      if (!Files.isDirectory(testRoot)) {
        continue;
      }
      try (Stream<Path> walk = Files.walk(testRoot)) {
        walk.filter(Files::isRegularFile)
            .filter(
                p -> {
                  String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                  return n.endsWith(".xml") || n.endsWith(".properties");
                })
            .forEach(
                p -> {
                  try {
                    String text = Files.readString(p, StandardCharsets.UTF_8);
                    if (text.contains("hibernate.show_sql")
                        && text.matches("(?s).*hibernate\\.show_sql[\"'>\\s=]+true.*")) {
                      offenders.add(p.toString());
                    }
                  } catch (Exception e) {
                    throw new RuntimeException(p.toString(), e);
                  }
                });
      }
    }
    assertTrue(
        offenders.isEmpty(),
        "DTS test resources must set hibernate.show_sql=false; found true in:\n"
            + String.join("\n", offenders));
  }

  @Test
  void parentSurefireRedirectsTestOutputToFile() throws Exception {
    Path pom = DTS_ROOT.resolve("pom.xml");
    assertTrue(Files.isRegularFile(pom), pom.toAbsolutePath().toString());
    String xml = Files.readString(pom, StandardCharsets.UTF_8);
    assertTrue(
        xml.contains("<redirectTestOutputToFile>true</redirectTestOutputToFile>"),
        "delivery-tier-suite pom surefire must redirect test stdout away from the reactor log");
  }

  @Test
  void log4j2TesterXmlIsNotUsed() {
    for (String module : SERVICE_MODULES) {
      Path leftover =
          DTS_ROOT
              .resolve(module)
              .resolve("src")
              .resolve("test")
              .resolve("resources")
              .resolve("log4j2-tester.xml");
      assertFalse(
          Files.isRegularFile(leftover),
          "rename log4j2-tester.xml to log4j2-test.xml (Log4j2 auto-load name): " + leftover);
    }
  }
}
