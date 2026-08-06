/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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

package com.percussion.utils.container.testutil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Test fixture helper to materialize a minimal Jetty base layout and related resources inside a
 * provided root directory (typically a JUnit 5 @TempDir Path).
 *
 * <p>Creates: root/jetty/base/etc - installation.properties - login.conf - perc-ds.xml -
 * perc-ds.properties (copied from perc-ds-derby.properties resource)
 *
 * <p>Also sets the system property: perc.jetty.base = root/jetty/base
 *
 * <p>Usage in tests: @BeforeEach void init() throws IOException {
 * JettyTestFixtures.materializeJettyBase(temporaryFolder); }
 */
public final class JettyTestFixtures {

  private JettyTestFixtures() {
    // utility
  }

  /**
   * Materialize the minimal Jetty base layout and set perc.jetty.base.
   *
   * @param root root folder for the test (usually @TempDir Path)
   * @return Path to the created "jetty/base" directory
   * @throws IOException on IO errors
   */
  public static Path materializeJettyBase(Path root) throws IOException {
    Path base = root.resolve("jetty").resolve("base");
    Path etc = base.resolve("etc");
    Files.createDirectories(etc);

    copyClasspath(
        "/com/percussion/utils/container/jetty/base/etc/installation.properties",
        etc.resolve("installation.properties"));
    copyClasspath(
        "/com/percussion/utils/container/jetty/base/etc/login.conf", etc.resolve("login.conf"));
    copyClasspath(
        "/com/percussion/utils/container/jetty/base/etc/perc-ds.xml", etc.resolve("perc-ds.xml"));
    // On disk the adapters expect perc-ds.properties; copy from derby properties resource
    copyClasspath(
        "/com/percussion/utils/container/jetty/base/etc/perc-ds-derby.properties",
        etc.resolve("perc-ds.properties"));

    System.setProperty("perc.jetty.base", base.toAbsolutePath().toString());
    return base;
  }

  /**
   * Ensure a Tomcat/DTS directory exists and copy provided server.xml resources for tests. Creates:
   * root/Deployment/Server/conf/server.xml.5.3 root/Staging/Deployment/Server/conf/server.xml.5.3
   *
   * @param root test root path
   * @throws IOException on IO errors
   */
  public static void materializeDtsServerXml(Path root) throws IOException {
    Path prodConf = root.resolve("Deployment").resolve("Server").resolve("conf");
    Path stageConf =
        root.resolve("Staging").resolve("Deployment").resolve("Server").resolve("conf");
    Files.createDirectories(prodConf);
    Files.createDirectories(stageConf);

    copyClasspath(
        "/com/percussion/utils/container/Deployment/Server/conf/server.xml",
        prodConf.resolve("server.xml.5.3"));
    copyClasspath(
        "/com/percussion/utils/container/Staging/Deployment/Server/conf/server.xml",
        stageConf.resolve("server.xml.5.3"));
  }

  /**
   * Ensure legacy JBoss directories exist and copy referenced resources used by tests.
   *
   * <p>Creates (relative to root): AppServer/server/rx/conf/login-config.xml
   * AppServer/server/rx/deploy/jboss-web.deployer/server.xml AppServer/server/rx/deploy/rx-ds.xml
   * AppServer/server/rx/deploy/rxapp.ear/rxapp.war/WEB-INF/config/spring/server-beans.xml
   *
   * @param root test root path
   * @throws IOException on IO errors
   */
  public static void materializeJbossLayout(Path root) throws IOException {
    Path base = root.resolve("AppServer").resolve("server").resolve("rx");
    Path conf = base.resolve("conf");
    Path webDeployer = base.resolve("deploy").resolve("jboss-web.deployer");
    Path earWarSpring =
        base.resolve("deploy")
            .resolve("rxapp.ear")
            .resolve("rxapp.war")
            .resolve("WEB-INF")
            .resolve("config")
            .resolve("spring");

    Files.createDirectories(conf);
    Files.createDirectories(webDeployer);
    Files.createDirectories(earWarSpring);

    copyClasspath(
        "/com/percussion/utils/container/AppServer/server/rx/conf/login-config.xml",
        conf.resolve("login-config.xml"));
    copyClasspath(
        "/com/percussion/utils/container/AppServer/server/rx/deploy/jboss-web.deployer/server.xml",
        webDeployer.resolve("server.xml"));
    copyClasspath(
        "/com/percussion/utils/container/AppServer/server/rx/deploy/rx-ds.xml",
        base.resolve("deploy").resolve("rx-ds.xml"));
    copyClasspath(
        "/com/percussion/utils/container/AppServer/server/rx/deploy/rxapp.ear/rxapp.war/WEB-INF/config/spring/server-beans.xml",
        earWarSpring.resolve("server-beans.xml"));
  }

  private static void copyClasspath(String resourcePath, Path destination) throws IOException {
    Files.createDirectories(destination.getParent());
    try (InputStream in = JettyTestFixtures.class.getResourceAsStream(resourcePath)) {
      if (in == null) {
        throw new IOException("Resource not found on classpath: " + resourcePath);
      }
      Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
