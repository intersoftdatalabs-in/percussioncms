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

package com.percussion.utils.io;

<<<<<<< HEAD
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PathUtilsTests {

  @TempDir public Path temporaryFolder;
  private String rxdeploydir;

  @BeforeEach
  public void setup() throws IOException {

    rxdeploydir = System.getProperty("rxdeploydir");
    System.setProperty("rxdeploydir", temporaryFolder.toAbsolutePath().toString());
  }

  @AfterEach
=======
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PathUtilsTests {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private String rxdeploydir;

  @Before
  public void setup() throws IOException {

    rxdeploydir = System.getProperty("rxdeploydir");
    System.setProperty("rxdeploydir", temporaryFolder.getRoot().getAbsolutePath());
  }

  @After
>>>>>>> development-8.1.x
  public void teardown() {
    if (rxdeploydir != null) System.setProperty("rxdeploydir", rxdeploydir);
  }

  public PathUtilsTests() {}

  // TODO: Finish adding various test cases.
  @Test
<<<<<<< HEAD
  @Disabled
=======
  @Ignore
>>>>>>> development-8.1.x
  public void testAutodetect() throws IOException {
    System.setProperty("rxdeploydir", "");
    System.setProperty("user.dir", System.getProperty("user.home"));
    PathUtils.clearRxDir();

    Path p = Paths.get(System.getProperty("user.home"), PathUtils.USER_FOLDER_CHECK_ITEM);
    if (!Files.exists(p)) Files.createDirectory(p);

    assertEquals(
        String.format("%s%s%s", System.getProperty("user.home"), File.separator, ".perc_config"),
        PathUtils.getRxDir(null).getAbsolutePath());

<<<<<<< HEAD
    File dtsBase = temporaryFolder.resolve("Deployment").resolve("Server").toFile();
    File rxconfig = temporaryFolder.resolve("rxconfig").toFile();
    PathUtils.clearRxDir();

    assertEquals(
        temporaryFolder.toAbsolutePath(),
=======
    File dtsBase = temporaryFolder.newFolder("Deployment", "Server");
    File rxconfig = temporaryFolder.newFolder("rxconfig");
    PathUtils.clearRxDir();

    assertEquals(
        temporaryFolder.getRoot().getAbsolutePath(),
>>>>>>> development-8.1.x
        PathUtils.getRxDir(rxconfig.getAbsolutePath()).getAbsolutePath());

    System.setProperty("rxdeploydir", "");
    System.setProperty("user.dir", dtsBase.getAbsolutePath());
    PathUtils.clearRxDir();

    assertEquals(
<<<<<<< HEAD
        temporaryFolder.toAbsolutePath(),
        PathUtils.getRxDir(dtsBase.getAbsolutePath()).getAbsolutePath());

    File jettyBase = temporaryFolder.resolve("jetty").resolve("base").toFile();
=======
        temporaryFolder.getRoot().getAbsolutePath(),
        PathUtils.getRxDir(dtsBase.getAbsolutePath()).getAbsolutePath());

    File jettyBase = temporaryFolder.newFolder("jetty", "base");
>>>>>>> development-8.1.x
    System.setProperty("user.dir", jettyBase.getAbsolutePath());
    PathUtils.clearRxDir();

    assertEquals(
<<<<<<< HEAD
        temporaryFolder.toAbsolutePath(),
        PathUtils.getRxDir(jettyBase.getAbsolutePath()).getAbsolutePath());
  }
=======
        temporaryFolder.getRoot().getAbsolutePath(),
        PathUtils.getRxDir(jettyBase.getAbsolutePath()).getAbsolutePath());
  }

  public void testNoObjectStore() throws IOException {}

  public void testObjectStore() {}

  public void testNoObjectStoreNoRxConfig() {}
>>>>>>> development-8.1.x
}
