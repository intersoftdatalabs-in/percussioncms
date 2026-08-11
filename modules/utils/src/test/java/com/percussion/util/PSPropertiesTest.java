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
package com.percussion.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("UnitTest")
public class PSPropertiesTest {

  @TempDir Path tempDir;

  @Test
  public void fileCtorLoadsProperties() throws Exception {
    Path propsFile = tempDir.resolve("sample.properties");
    Files.writeString(propsFile, "alpha=1\n# comment\nbeta=two\n", StandardCharsets.ISO_8859_1);

    PSProperties props = new PSProperties(propsFile.toString());
    assertEquals("1", props.getProperty("alpha"));
    assertEquals("two", props.getProperty("beta"));
    assertEquals(1, props.getInt("alpha"));
  }

  @Test
  public void loadRecordsCommentLinesInLineData() throws Exception {
    String input = "# keep me\nalpha=1\n";
    PSProperties props = new PSProperties();
    props.load(new ByteArrayInputStream(input.getBytes(StandardCharsets.ISO_8859_1)));
    assertEquals("1", props.getProperty("alpha"));
    // Comment rows are captured in lineData (paired with empty keyData entries)
    assertTrue(props.lineData.stream().anyMatch(l -> l.contains("# keep me")));
    assertTrue(props.keyData.contains("alpha"));
  }

  @Test
  public void putUpdatesExistingWithoutDuplicatingKeyData() {
    PSProperties props = new PSProperties();
    props.put("k", "v1");
    props.put("k", "v2");
    assertEquals("v2", props.getProperty("k"));
    long keyCount = props.keyData.stream().filter("k"::equals).count();
    assertEquals(1, keyCount);
  }
}
