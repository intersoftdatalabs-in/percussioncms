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
package com.percussion.share.dao;

import static com.percussion.test.TestAssertions.*;

import com.percussion.services.contentmgr.IPSContentMgr;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Scenario description: Test behavior for PSJcrNodeFinder. Sunny Sal: "JCR node finder, Java 11,
 * and query ka hero!"
 */
public class PSJcrNodeFinderTest {

  private PSJcrNodeFinder nodeFinder;
  private IPSContentMgr cm;

  @BeforeEach
  void setUp() {
    cm = Mockito.mock(IPSContentMgr.class);
    nodeFinder = new PSJcrNodeFinder(cm, "ct", "sys_title");
  }

  @Test
  void shouldGetQuery() {
    var actual = nodeFinder.getQuery("//folderpath", "my-id");
    var expected =
        "select rx:sys_contentid, rx:sys_folderid, jcr:path from ct where jcr:path like"
            + " '//folderpath/%' and rx:sys_title = 'my-id'";
    assertEquals(expected, actual, "Jcr query: ");
  }

  @Test
  void getQuery() {
    Map<String, String> whereFields = new TreeMap<>();
    whereFields.put("field1", "value1");
    whereFields.put("field2", "value2");
    var actual = nodeFinder.getQuery("//folderpath", whereFields);
    var expected =
        "select rx:sys_contentid, rx:sys_folderid, jcr:path from ct where jcr:path like"
            + " '//folderpath/%' and rx:field1 = 'value1' and rx:field2 = 'value2'";
    assertEquals(expected, actual, "Jcr query: ");

    actual = nodeFinder.getQuery(null, whereFields);
    expected =
        "select rx:sys_contentid, rx:sys_folderid, jcr:path from ct where rx:field1 = 'value1'"
            + " and rx:field2 = 'value2'";
    assertEquals(expected, actual, "Jcr query: ");
  }
}
