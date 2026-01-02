/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.utils.container;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
<<<<<<< HEAD
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PSJettyConnectorsTest {

  @TempDir public Path temporaryFolder;

  @Test
  public void load() throws IOException {
    Path root = temporaryFolder;
=======
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PSJettyConnectorsTest {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void load() throws IOException {
    Path root = temporaryFolder.getRoot().toPath();
>>>>>>> development-8.1.x

    InputStream srcInstallProps =
        PSJettyConnectorsTest.class.getResourceAsStream(
            "/com/percussion/utils/container/jetty/base/etc/installation.properties");
    InputStream srcLoginConf =
        PSJettyConnectorsTest.class.getResourceAsStream(
            "/com/percussion/utils/container/jetty/base/etc/login.conf");
    InputStream srcPercDsXML =
        PSJettyConnectorsTest.class.getResourceAsStream(
            "/com/percussion/utils/container/jetty/base/etc/perc-ds.xml");
    InputStream srcPercDsProperties =
        PSJettyConnectorsTest.class.getResourceAsStream(
            "/com/percussion/utils/container/jetty/base/etc/perc-ds-derby.properties");

<<<<<<< HEAD
    Files.createDirectories(temporaryFolder.resolve("jetty/base/etc"));
    Files.copy(srcInstallProps, root.resolve("jetty/base/etc/installation.properties"));
    Files.copy(srcLoginConf, root.resolve("jetty/base/etc/login.conf"));
    Files.copy(srcPercDsXML, root.resolve("jetty/base/etc/perc-ds.xml"));
    Files.copy(srcPercDsProperties, root.resolve("jetty/base/etc/perc-ds.properties"));

    PSJettyConnectors c = new PSJettyConnectors(root);
    c.load();
    System.out.println(c);
  }

  @Test
  public void save() throws IOException {
    Path root = temporaryFolder;

    InputStream srcInstallProps =
        PSJettyConnectorsTest.class.getResourceAsStream(
            "/com/percussion/utils/container/jetty/base/etc/installation.properties");
    InputStream srcLoginConf =
        PSJettyConnectorsTest.class.getResourceAsStream(
            "/com/percussion/utils/container/jetty/base/etc/login.conf");
    InputStream srcPercDsXML =
        PSJettyConnectorsTest.class.getResourceAsStream(
            "/com/percussion/utils/container/jetty/base/etc/perc-ds.xml");
    InputStream srcPercDsProperties =
        PSJettyConnectorsTest.class.getResourceAsStream(
            "/com/percussion/utils/container/jetty/base/etc/perc-ds-derby.properties");

    Files.createDirectories(temporaryFolder.resolve("jetty/base/etc"));
=======
    temporaryFolder.newFolder("jetty", "base", "etc");

    Files.copy(srcInstallProps, root.resolve("jetty/base/etc/installation.properties"));
    Files.copy(srcLoginConf, root.resolve("jetty/base/etc/login.conf"));
    Files.copy(srcPercDsXML, root.resolve("jetty/base/etc/perc-ds.xml"));
    Files.copy(srcPercDsProperties, root.resolve("jetty/base/etc/perc-ds.properties"));

    PSJettyConnectors c = new PSJettyConnectors(root);
    c.load();
    System.out.println(c);
  }

  @Test
  public void save() throws IOException {
    Path root = temporaryFolder.getRoot().toPath();

    InputStream srcInstallProps =
        PSJettyConnectorsTest.class.getResourceAsStream(
            "/com/percussion/utils/container/jetty/base/etc/installation.properties");
    InputStream srcLoginConf =
        PSJettyConnectorsTest.class.getResourceAsStream(
            "/com/percussion/utils/container/jetty/base/etc/login.conf");
    InputStream srcPercDsXML =
        PSJettyConnectorsTest.class.getResourceAsStream(
            "/com/percussion/utils/container/jetty/base/etc/perc-ds.xml");
    InputStream srcPercDsProperties =
        PSJettyConnectorsTest.class.getResourceAsStream(
            "/com/percussion/utils/container/jetty/base/etc/perc-ds-derby.properties");

    temporaryFolder.newFolder("jetty", "base", "etc");

>>>>>>> development-8.1.x
    Files.copy(srcInstallProps, root.resolve("jetty/base/etc/installation.properties"));
    Files.copy(srcLoginConf, root.resolve("jetty/base/etc/login.conf"));
    Files.copy(srcPercDsXML, root.resolve("jetty/base/etc/perc-ds.xml"));
    Files.copy(srcPercDsProperties, root.resolve("jetty/base/etc/perc-ds.properties"));

    PSJettyConnectors c = new PSJettyConnectors(root);
    // c.setHttpsHost("0.0.0.0");
    System.out.println(c);
    c.save();
  }
}
