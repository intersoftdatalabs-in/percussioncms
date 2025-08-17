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
package test.percussion.pso.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.pso.utils.AbstractTemplateExpander;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.IPSContentPropertyConstants;
import com.percussion.services.contentmgr.IPSNode;
import com.percussion.services.contentmgr.PSContentMgrConfig;
import com.percussion.services.contentmgr.data.PSQueryResult;
import com.percussion.services.contentmgr.data.PSRow;
import com.percussion.services.contentmgr.data.PSRowComparator;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for AbstractTemplateExpander using JUnit 5 and Mockito.
 *
 * @author DavidBenua
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractTemplateExpander Tests")
public class AbstractTemplateExpanderTest {
  private static final Logger log = LogManager.getLogger(AbstractTemplateExpanderTest.class);

  @Mock private IPSGuidManager gmgr;

  @Mock private IPSContentMgr cmgr;

  @Mock private IPSGuid tguid1;

  @Mock private IPSGuid tguid2;

  @Mock private IPSGuid siteGuid;

  @Mock private IPSGuid guid302;

  @Mock private IPSGuid guid303;

  @Mock private IPSGuid folderGuid;

  @Mock private PSComponentSummary sum302;

  @Mock private PSComponentSummary sum303;

  private TestableTemplateExpanderAdaptor cut;
  private List<IPSGuid> templateList;

  /** Setup test fixtures using JUnit 5 and Mockito. */
  @BeforeEach
  void setUp() {
    templateList = new ArrayList<>();

    cut = new TestableTemplateExpanderAdaptor();
    AbstractTemplateExpander.setGmgr(gmgr);
    AbstractTemplateExpander.setCmgr(cmgr);

    // Configure template mocks
    when(tguid1.getType()).thenReturn(PSTypeEnum.TEMPLATE.getOrdinal());
    when(tguid1.getUUID()).thenReturn(1);
    when(tguid2.getType()).thenReturn(PSTypeEnum.TEMPLATE.getOrdinal());
    when(tguid2.getUUID()).thenReturn(2);

    templateList.add(tguid1);
    templateList.add(tguid2);
  }

  @Test
  @DisplayName("Test template expansion")
  void testExpand() {

    var params = Map.of(IPSHtmlParameters.SYS_CONTEXT, "101", "siteid", "301");

    var summaryMap = buildSummaryMapExpectations();
    cut.setNeedsContentNode(false);

    var qr = buildQueryResultExpectations();

    // Mock the site GUID creation
    when(gmgr.makeGuid("301", PSTypeEnum.SITE)).thenReturn(siteGuid);

    try {
      var items = cut.expand(qr, params, summaryMap);
      assertNotNull(items);
      log.debug("items returned " + items.size());
      assertEquals(4, items.size());
      log.info(items);

      verify(gmgr).makeGuid("301", PSTypeEnum.SITE);
    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception caught: " + ex.getMessage());
    }
  }

  @Test
  @Disabled("TODO: Fix Me")
  @DisplayName("Test build node map")
  void testBuildNodeMap() throws RepositoryException {
    var qr = buildQueryResultExpectations();
    var summaryMap = buildSummaryMapExpectations();

    // Create additional mocks for this test
    var node1 = mock(IPSNode.class);
    var guid1 = mock(IPSGuid.class);
    var node2 = mock(IPSNode.class);
    var guid2 = mock(IPSGuid.class);

    List<Node> nodelist = List.of(node1, node2);

    // Configure mocks
    when(cmgr.findItemsByGUID(anyList(), any(PSContentMgrConfig.class))).thenReturn(nodelist);
    when(node1.getGuid()).thenReturn(guid1);
    when(node2.getGuid()).thenReturn(guid2);

    var nodeMap = cut.buildNodeMap(qr, summaryMap);
    assertNotNull(nodeMap);
    log.debug("nodeMap " + nodeMap);
    assertTrue(nodeMap.containsKey(guid1));
    assertTrue(nodeMap.containsKey(guid2));

    verify(cmgr).findItemsByGUID(anyList(), any(PSContentMgrConfig.class));
    verify(node1).getGuid();
    verify(node2).getGuid();
  }

  private Map<Integer, PSComponentSummary> buildSummaryMapExpectations() {
    // Configure summary mocks
    when(sum302.getContentId()).thenReturn(302);
    when(sum303.getContentId()).thenReturn(303);

    return Map.of(
        302, sum302,
        303, sum303);
  }

  private QueryResult buildQueryResultExpectations() {
    final PSRow row1 =
        new PSRow(
            new HashMap<String, Object>() {
              {
                put(IPSContentPropertyConstants.RX_SYS_CONTENTID, "302");
                put(IPSContentPropertyConstants.RX_SYS_REVISION, "2");
                put(IPSContentPropertyConstants.RX_SYS_CONTENTTYPEID, "47");
                put(IPSContentPropertyConstants.RX_SYS_FOLDERID, "201");
              }
            });
    final PSRow row2 =
        new PSRow(
            new HashMap<String, Object>() {
              {
                put(IPSContentPropertyConstants.RX_SYS_CONTENTID, "303");
                put(IPSContentPropertyConstants.RX_SYS_REVISION, "4");
                put(IPSContentPropertyConstants.RX_SYS_CONTENTTYPEID, "48");
                put(IPSContentPropertyConstants.RX_SYS_FOLDERID, "201");
              }
            });

    final PSRowComparator rowcomp =
        new PSRowComparator(
            new ArrayList<PSPair<String, Boolean>>() {
              {
                add(
                    new PSPair<String, Boolean>(
                        IPSContentPropertyConstants.RX_SYS_CONTENTID, true));
              }
            });

    final PSQueryResult qr =
        new PSQueryResult(
            new String[] {
              IPSContentPropertyConstants.RX_SYS_CONTENTID,
              IPSContentPropertyConstants.RX_SYS_REVISION,
              IPSContentPropertyConstants.RX_SYS_CONTENTTYPEID,
              IPSContentPropertyConstants.RX_SYS_FOLDERID
            },
            rowcomp);

    qr.addRow(row1);
    qr.addRow(row2);

    // Mock GUID manager behavior
    when(gmgr.makeGuid(new PSLocator(302, 2))).thenReturn(guid302);
    when(gmgr.makeGuid(new PSLocator(303, 4))).thenReturn(guid303);
    when(gmgr.makeGuid(new PSLocator(201, 0))).thenReturn(folderGuid);

    return qr;
  }

  private class TestableTemplateExpanderAdaptor extends AbstractTemplateExpander {

    @Override
    protected List<IPSGuid> findTemplates(
        IPSGuid itemGuid,
        IPSGuid folderGuid,
        IPSGuid siteGuid,
        int context,
        PSComponentSummary summary,
        Node contentNode,
        Map<String, String> parameters) {
      log.info("Item Guid " + itemGuid);

      return templateList;
    }

    @Override
    public Map<IPSGuid, Node> buildNodeMap(
        QueryResult result, Map<Integer, PSComponentSummary> summaryMap)
        throws RepositoryException {
      return super.buildNodeMap(result, summaryMap);
    }

    @Override
    public boolean isNeedsContentNode() {
      return super.isNeedsContentNode();
    }

    @Override
    public void setNeedsContentNode(boolean needsContentNode) {
      super.setNeedsContentNode(needsContentNode);
    }
  }
}
