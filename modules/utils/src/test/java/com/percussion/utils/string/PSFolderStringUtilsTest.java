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

package com.percussion.utils.string;

<<<<<<< HEAD
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.security.SecureStringUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PSFolderStringUtilsTest {

  @TempDir public Path temporaryFolder;
=======
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.percussion.security.SecureStringUtils;
import java.io.File;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PSFolderStringUtilsTest {

  @Rule public TemporaryFolder temporaryFolder = TemporaryFolder.builder().build();
>>>>>>> development-8.1.x

  @Test
  public void testFolderStringUtils() throws IOException {

<<<<<<< HEAD
    File parentA = temporaryFolder.resolve("parentA").toFile();
    File parentB = temporaryFolder.resolve("parentB").toFile();
    File childA = temporaryFolder.resolve("parentA").resolve("childA").toFile();
=======
    File parentA = temporaryFolder.newFolder("parentA");
    File parentB = temporaryFolder.newFolder("parentB");
    File childA = temporaryFolder.newFolder("parentA", "childA");
>>>>>>> development-8.1.x

    assertFalse(SecureStringUtils.isChildOfFilePath(parentA.toPath(), parentB.toPath()));
    assertTrue(SecureStringUtils.isChildOfFilePath(parentA.toPath(), childA.toPath()));
    assertFalse(SecureStringUtils.isChildOfFilePath(parentB.toPath(), childA.toPath()));

    assertTrue(SecureStringUtils.isSameFileAs(parentA.toPath(), parentA.toPath()));
    assertFalse(SecureStringUtils.isSameFileAs(parentA.toPath(), childA.toPath()));
    assertFalse(SecureStringUtils.isSameFileAs(parentA.toPath(), parentB.toPath()));
  }
}
