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

package com.percussion.category.marshaller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.category.data.PSCategory;
import com.percussion.server.PSServer;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression: releasing a {@link java.nio.channels.FileLock} after the channel was closed by
 * try-with-resources threw {@link java.nio.channels.ClosedChannelException}, failing
 * PSSaveAssetsMaintenanceProcess on startup when creating category.xml.
 */
public class PSCategoryMarshallerLockTest {

  @TempDir Path tempDir;

  private File previousRxDir;

  @BeforeEach
  void setRxRoot() {
    previousRxDir = PSServer.getRxDir();
    PSServer.setRxDir(tempDir.toFile());
  }

  @AfterEach
  void restoreRxRoot() {
    if (previousRxDir != null) {
      PSServer.setRxDir(previousRxDir);
    }
  }

  @Test
  void marshalCreatesCategoryFileWithoutClosedChannelException() throws Exception {
    var category = new PSCategory();
    category.setTopLevelNodes(new ArrayList<>());

    var marshaller = new PSCategoryMarshaller();
    marshaller.setCategory(category);

    assertDoesNotThrow(marshaller::marshal);

    Path written = tempDir.resolve("rx_resources").resolve("category").resolve("category.xml");
    assertTrue(Files.isRegularFile(written), "category.xml should be written");
    String xml = Files.readString(written);
    assertTrue(xml.contains("CategoryTree") || xml.contains("category"), xml);
  }

  @Test
  void releaseFileLockIsNoOpWhenLockNull() {
    assertDoesNotThrow(() -> PSCategoryMarshaller.releaseFileLock(null, "category.xml"));
  }
}
