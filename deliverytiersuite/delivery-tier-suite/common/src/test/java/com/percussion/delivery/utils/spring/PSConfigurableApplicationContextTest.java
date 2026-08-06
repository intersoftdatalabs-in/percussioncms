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
package com.percussion.delivery.utils.spring;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockServletContext;

/**
 * Unit tests for the production DTS Spring {@code contextClass} used by every service {@code
 * web.xml}.
 */
class PSConfigurableApplicationContextTest {

  private static final String CATALINA_BASE = "catalina.base";

  private String previousCatalinaBase;

  @AfterEach
  void restoreCatalinaBase() {
    if (previousCatalinaBase == null) {
      System.clearProperty(CATALINA_BASE);
    } else {
      System.setProperty(CATALINA_BASE, previousCatalinaBase);
    }
  }

  @Test
  void publicNoArgConstructorIsLoadableByClassName() throws Exception {
    // Mirrors Tomcat ContextLoader: Class.forName + reflective construction
    Class<?> clazz =
        Class.forName("com.percussion.delivery.utils.spring.PSConfigurableApplicationContext");
    Object instance = clazz.getDeclaredConstructor().newInstance();
    assertNotNull(instance);
    assertEquals(PSConfigurableApplicationContext.class, instance.getClass());
  }

  @Test
  void defaultsToBeansXmlWhenNoPropertiesAvailable() {
    previousCatalinaBase = System.getProperty(CATALINA_BASE);
    System.clearProperty(CATALINA_BASE);

    PSConfigurableApplicationContext ctx = new PSConfigurableApplicationContext();
    // No servlet context resources → default
    assertArrayEquals(new String[] {"/WEB-INF/beans.xml"}, ctx.getConfigLocations());
  }

  @Test
  void usesWebInfPropertiesWhenPresent() {
    previousCatalinaBase = System.getProperty(CATALINA_BASE);
    System.clearProperty(CATALINA_BASE);

    MockServletContext withProps =
        new MockServletContext() {
          @Override
          public java.io.InputStream getResourceAsStream(String path) {
            if ("/WEB-INF/perc-context.properties".equals(path)) {
              return new java.io.ByteArrayInputStream(
                  "contextLocation=/WEB-INF/beans_mongodb.xml\n".getBytes(StandardCharsets.UTF_8));
            }
            return super.getResourceAsStream(path);
          }
        };

    PSConfigurableApplicationContext ctx = new PSConfigurableApplicationContext();
    ctx.setServletContext(withProps);

    assertArrayEquals(new String[] {"/WEB-INF/beans_mongodb.xml"}, ctx.getConfigLocations());
  }

  @Test
  void catalinaBasePropertiesOverrideWebInf(@TempDir Path tempDir) throws Exception {
    previousCatalinaBase = System.getProperty(CATALINA_BASE);

    Path confPerc = tempDir.resolve("conf").resolve("perc");
    Files.createDirectories(confPerc);
    Files.writeString(
        confPerc.resolve("perc-context.properties"),
        "contextLocation=/WEB-INF/beans_from_catalina.xml\n",
        StandardCharsets.UTF_8);
    System.setProperty(CATALINA_BASE, tempDir.toAbsolutePath().toString());

    MockServletContext withProps =
        new MockServletContext() {
          @Override
          public java.io.InputStream getResourceAsStream(String path) {
            if ("/WEB-INF/perc-context.properties".equals(path)) {
              return new java.io.ByteArrayInputStream(
                  "contextLocation=/WEB-INF/beans_mongodb.xml\n".getBytes(StandardCharsets.UTF_8));
            }
            return super.getResourceAsStream(path);
          }
        };

    PSConfigurableApplicationContext ctx = new PSConfigurableApplicationContext();
    ctx.setServletContext(withProps);

    assertArrayEquals(new String[] {"/WEB-INF/beans_from_catalina.xml"}, ctx.getConfigLocations());
  }
}
