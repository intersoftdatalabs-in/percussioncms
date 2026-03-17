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
package com.percussion.contentmigration.rules;

import static com.percussion.test.TestAssertions.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.File;
import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for PSIdMatchingMigrationRule. */
public class PSIdMatchingMigrationRuleTest {

  private Document sourceDoc;
  private Document targetDoc;
  private PSIdMatchingMigrationRule matchingMigrationRule;

  @BeforeEach
  void setup() throws IOException {
    sourceDoc =
        Jsoup.parse(new File("src/test/resources/contentmigration/sourceDoc.html"), "UTF-8");
    targetDoc =
        Jsoup.parse(new File("src/test/resources/contentmigration/targetDoc.html"), "UTF-8");
  }

  @Test
  void testMatchingMigrationRule() {
    matchingMigrationRule = new PSIdMatchingMigrationRule();
    var content = matchingMigrationRule.findMatchingContent("1", sourceDoc, targetDoc);
    Elements elems = targetDoc.select("#perc-content");
    Element elem = elems.get(0);
    assertNotNull(content);
    assertEquals(elem.html(), content);
  }

  @Test
  void testNotMatchingMigrationRule() {
    matchingMigrationRule = new PSIdMatchingMigrationRule();
    var content = matchingMigrationRule.findMatchingContent("2", sourceDoc, targetDoc);
    Elements elems = targetDoc.select("#perc-content");
    Element elem = elems.get(0);
    assertNull(content);
    assertNotEquals(elem.html(), content);
  }
}
