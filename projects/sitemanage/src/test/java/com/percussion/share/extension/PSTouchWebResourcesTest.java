// REFACTORED: CP-JAVA11
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
package com.percussion.share.extension;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Startup process to touch configured files below the web_resources directory. Sunny Sal: "Touch
 * web resources, Java 11, and file ka hero!"
 */

@Tag("integration")
public class PSTouchWebResourcesTest {

  @Test
  void testTouchFiles() throws Exception {
    var tempDir = new File(".");
    var rootDir = new File(tempDir, "test_cm1_startup");
    if (rootDir.exists()) FileUtils.cleanDirectory(rootDir);
    rootDir.mkdir();

    var files = new ArrayList<File>();
    for (int i = 0; i < 2; i++) {
      var testDir = new File(rootDir, "sub" + i);
      for (int j = 0; j < 2; j++) {
        var testFile = new File(testDir, "test" + j + ".txt");
        FileUtils.touch(testFile);
        testFile.deleteOnExit();
        files.add(testFile);
      }
    }

    var date = new Date();
    for (var file : files) {
      assertFalse(FileUtils.isFileNewer(file, date));
    }

    var touchFiles = new PSTouchFiles();
    touchFiles.setRootDir(rootDir.getPath());
    touchFiles.setDirNames("sub0,sub1");

    var props = new Properties();
    var propName = PSTouchFiles.getPropName();
    props.setProperty(propName, "true");

    touchFiles.doStartupWork(props);
    assertEquals("false", props.getProperty(propName));
    Collection<File> touchedFiles = FileUtils.listFiles(rootDir, null, true);
    for (var file : touchedFiles) {
      assertTrue(FileUtils.isFileNewer(file, date));
    }
  }
}
