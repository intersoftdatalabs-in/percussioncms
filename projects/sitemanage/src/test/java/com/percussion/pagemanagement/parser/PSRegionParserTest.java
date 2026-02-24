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
package com.percussion.pagemanagement.parser;

import static com.percussion.pagemanagement.data.PSRegionTreeUtils.getChildRegions;
import static com.percussion.test.TestAssertions.*;

import com.percussion.pagemanagement.data.PSRegion;
import com.percussion.pagemanagement.data.PSRegionCode;
import com.percussion.pagemanagement.data.PSRegionNode;
import com.percussion.pagemanagement.data.PSRegionTreeWriter;
import com.percussion.pagemanagement.parser.IPSRegionParser.IPSRegionParserRegionFactory;
import com.percussion.share.test.PSTestUtils;
import java.io.StringWriter;
import java.util.Collection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PSRegionParserTest {

  PSTemplateRegionFactory factory;
  PSRegionParser<PSRegion, PSRegionCode> parser;
  PSRegionTreeWriter treeWriter;
  StringWriter sw;

  public static class PSTemplateRegionFactory
      implements IPSRegionParserRegionFactory<PSRegion, PSRegionCode> {
    @Override
    public PSRegion createRegion(String regionId) {
      var r = new PSRegion();
      r.setRegionId(regionId);
      return r;
    }

    @Override
    public PSRegion createRootRegion() {
      return new PSRegion();
    }

    @Override
    public PSRegionCode createRegionCode() {
      return new PSRegionCode();
    }
  }

  @BeforeEach
  public void setUp() {
    factory = new PSTemplateRegionFactory();
    parser = new PSRegionParser<>(factory);
    sw = new StringWriter();
    treeWriter = new PSRegionTreeWriter(sw);
  }

  @Test
  public void testGetRegionTree() {
    var html = getHtml("Default.html");
    var regTree = parser.parse(html);
    var regions = regTree.getRegions();
    assertEquals(6L, regions.size());
    assertTrue(regionExists("1", regions.values()));
    assertTrue(regionExists("2", regions.values()));
    assertTrue(regionExists("3", regions.values()));
    for (var id : regions.keySet()) {
      var region = regions.get(id);
      var children = getChildRegions(region);
      if (id.equals("1")) {
        assertEquals(3L, children.size());
        assertTrue(regionExists("1.1", children));
        assertTrue(regionExists("1.3", children));
        assertTrue(regionExists("1.4.1", children));
      } else if (id.equals("2")) {
        assertTrue(children.isEmpty());
      } else if (id.equals("3")) {
        assertTrue(children.isEmpty());
      }
    }
    // log.debug(regTree.getRegions());
  }

  @Test
  public void testHeaderFooterParse() {
    var html = getHtml("TestHeaderFooter.html");
    var regTree = parser.parse(html);
    var children = regTree.getRootNode().getChildren();
    var code = getCode(children.get(0));
    assertNotNull(code);
    assertEquals("#perc_header()", code.getTemplateCode().trim());
    var end = getCode(children.get(children.size() - 1));
    assertEquals("#perc_footer()", end.getTemplateCode().trim());
  }

  @Test
  public void testWrite() {
    log.debug("Write tree");
    var html = getHtml("TestHeaderFooter.html");
    var regTree = parser.parse(html);
    treeWriter.write(regTree.getRootNode());
    var actual = sw.getBuffer().toString();
    assertEquals(html, actual);
  }

  @Test
  public void testRegion() {
    var html = getHtml("Region.html");
    var regTree = parser.parse(html);
    var actual = getChildRegions(regTree.getRootNode()).get(0).getRegionId();
    assertEquals("container", actual);

    actual = getChildRegions(getChildRegions(regTree.getRootNode()).get(0)).get(0).getRegionId();
    assertEquals("header", actual);
  }

  private PSRegionCode getCode(PSRegionNode node) {
    return (PSRegionCode) node;
  }

  private boolean regionExists(String id, Collection<? extends PSRegionNode> nodes) {
    return nodes.stream()
        .filter(PSRegion.class::isInstance)
        .map(PSRegion.class::cast)
        .anyMatch(region -> id.equals(region.getRegionId()));
  }

  private String getHtml(String name) {
    return PSTestUtils.resourceToString(getClass(), name);
  }

  /** The log instance to use for this class, never null. */
  private static final Logger log = LogManager.getLogger(PSRegionParserTest.class);
}
