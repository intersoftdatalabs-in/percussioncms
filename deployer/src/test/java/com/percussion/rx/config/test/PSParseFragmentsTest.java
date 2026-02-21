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
package com.percussion.rx.config.test;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.rx.config.impl.PSConfigDefGenerator;
import com.percussion.utils.tools.PSParseFragments;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class PSParseFragmentsTest {
  @Test
  public void testAll() throws Exception {
    PSConfigDefGenerator gen = PSConfigDefGenerator.getInstance();
    String content = gen.getFragementFileContents();

    Map<String, String> frags = PSParseFragments.parseContent(content);
    assertTrue(frags.size() > 1, "Must have more than one fragments");

    assertTrue(frags.get("XMLHEAD") != null, "Must have XMLHEAD");
    assertTrue(frags.get("SLOT") != null, "Must have SLOT");
  }

  @Test
  public void testParse() throws Exception {
    String text = "1st\n2nd line\r\n3rd line";
    String[] lines = PSParseFragments.splitByNewlines(text);
    for (String line : lines) {
      char ch = line.charAt(line.length() - 1);
      assertTrue(ch != '\r' && ch != '\n');
    }
    assertEquals("1st", lines[0]);
    assertEquals("2nd line", lines[1]);
    assertEquals("3rd line", lines[2]);
  }
}
