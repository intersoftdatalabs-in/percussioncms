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
package com.percussion.pagemanagement.data;

import static com.percussion.share.test.PSDataObjectTestUtils.assertEqualsMethod;
import static com.percussion.share.test.PSDataObjectTestUtils.assertXmlSerialization;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.share.dao.PSSerializerUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class PSPageRegionBranchesSerializationTest {

  @Test
  public void testSerialization() throws Exception {
    var branches = new PSRegionBranches();
    var item = new PSWidgetItem();
    item.setName("JB");
    branches.setRegionWidgets("test", asList(item));
    var s = PSSerializerUtils.marshal(branches);
    var unmarshal = PSSerializerUtils.unmarshal(s, PSRegionBranches.class);
    assertNotNull(unmarshal);
    assertFalse(unmarshal.getRegionWidgetAssociations().isEmpty());
    log.debug(s);
  }

  @Test
  public void testSetRegionWidgets() {
    var branches = new PSRegionBranches();
    var rid = "rid";
    var wi = new PSWidgetItem();
    wi.setDefinitionId("BLAH");

    branches.setRegionWidgets(rid, asList(wi));

    wi = new PSWidgetItem();
    wi.setDefinitionId("STUFF");

    branches.setRegionWidgets(rid, asList(wi));
    assertEquals(1, branches.getRegionWidgetAssociations().size());
  }

  @Test
  public void testPageSerialization() throws Exception {
    var branches = new PSRegionBranches();
    testSetRegionWidgets();
    var page = new PSPage();
    page.setId("1000");
    page.setFolderPath("//folderpath");
    page.setName("Page Name");
    page.setTemplateId("2000");
    page.setLinkTitle("dummy");

    var overrideRegion = new PSRegion();
    overrideRegion.setRegionId("templateRegion");

    var code = new PSRegionCode();
    code.setTemplateCode("#region('' '' '' '' '')");
    var pageSubRegion = new PSRegion();

    pageSubRegion.setRegionId("rid");
    List<PSRegionNode> regionNodes = new ArrayList<>();
    regionNodes.add(code);
    pageSubRegion.setChildren(regionNodes);

    overrideRegion.setChildren(Arrays.<PSRegionNode>asList(pageSubRegion));

    List<PSRegion> pageRegions = asList(overrideRegion);
    branches.setRegions(pageRegions);
    page.setRegionBranches(branches);
    var s = PSSerializerUtils.marshal(page);
    assertXmlSerialization(page);
    assertEqualsMethod(page);

    log.debug("\n" + s);
  }

  /** The log instance to use for this class, never null. */
  private static final Logger log =
      LogManager.getLogger(PSPageRegionBranchesSerializationTest.class);
}
