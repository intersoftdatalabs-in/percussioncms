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

package com.percussion.tablefactory;

<<<<<<< HEAD
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PSJdbcImportExportHelperTests {

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
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PSJdbcImportExportHelperTests {

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

  @Test
  public void testGetOptions() throws IOException {

<<<<<<< HEAD
    File props = Files.createFile(temporaryFolder.resolve("db.properties")).toFile();
=======
    File props = temporaryFolder.newFile("db.properties");
>>>>>>> development-8.1.x
    props.deleteOnExit();
    FileOutputStream out = new FileOutputStream(props);

    IOUtils.copy(
        this.getClass().getResourceAsStream("/com/percussion/tablefactory/db.properties"), out);

    String args[] = {
      "-dbexport",
      "-dbprops",
      props.getAbsolutePath(),
      "-storagepath",
<<<<<<< HEAD
      temporaryFolder.toAbsolutePath().toString(),
=======
      temporaryFolder.getRoot().getAbsolutePath(),
>>>>>>> development-8.1.x
      "-tablestoskip",
      "PSX_PUBLICATION_DOC,PSX_PUBLICATIONSTATUS,PSX_PUBLICATION_SITE_ITEM,CONTENTSTATUSHISTORY_BAK,PSX_CONTENTCHANGEEVENT_BAK,PSX_SEARCHINDEXQUEUE"
    };
    Map<String, String> options = PSJdbcImportExportHelper.getOptions(args);

    assertNotNull(options);

    assertEquals("-dbexport", options.get("dboption"));
    assertEquals(props.getAbsolutePath(), options.get("-dbprops"));
<<<<<<< HEAD
    assertEquals(temporaryFolder.toAbsolutePath().toString(), options.get("-storagepath"));
=======
    assertEquals(temporaryFolder.getRoot().getAbsolutePath(), options.get("-storagepath"));
>>>>>>> development-8.1.x
    assertEquals(
        "PSX_PUBLICATION_DOC,PSX_PUBLICATIONSTATUS,PSX_PUBLICATION_SITE_ITEM,CONTENTSTATUSHISTORY_BAK,PSX_CONTENTCHANGEEVENT_BAK,PSX_SEARCHINDEXQUEUE",
        options.get("-tablestoskip"));
  }
}
