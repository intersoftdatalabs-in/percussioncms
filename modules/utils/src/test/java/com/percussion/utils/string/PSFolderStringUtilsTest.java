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

  @Test
  public void testFolderStringUtils() throws IOException {

    File parentA = temporaryFolder.resolve("parentA").toFile();
    File parentB = temporaryFolder.resolve("parentB").toFile();
    File childA = temporaryFolder.resolve("parentA").resolve("childA").toFile();

    assertFalse(SecureStringUtils.isChildOfFilePath(parentA.toPath(), parentB.toPath()));
    assertTrue(SecureStringUtils.isChildOfFilePath(parentA.toPath(), childA.toPath()));
    assertFalse(SecureStringUtils.isChildOfFilePath(parentB.toPath(), childA.toPath()));

    assertTrue(SecureStringUtils.isSameFileAs(parentA.toPath(), parentA.toPath()));
    assertFalse(SecureStringUtils.isSameFileAs(parentA.toPath(), childA.toPath()));
    assertFalse(SecureStringUtils.isSameFileAs(parentA.toPath(), parentB.toPath()));
  }
}
