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
package test.percussion.pso.imageedit.services;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.pso.imageedit.services.AbstractTemplateExpanderAdaptor;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.IPSContentPropertyConstants;
import com.percussion.services.contentmgr.IPSNode;
import com.percussion.services.contentmgr.PSContentMgrConfig;
import com.percussion.services.contentmgr.data.PSQueryResult;
import com.percussion.services.contentmgr.data.PSRow;
import com.percussion.services.contentmgr.data.PSRowComparator;
import com.percussion.services.contentmgr.impl.jsrdata.PSRowIterator;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * // REFACTORED: CP-JAVA11
 *
 * @author DavidBenua
 */
@ExtendWith(MockitoExtension.class)
@Disabled
public class AbstractTemplateExpanderAdaptorTest {
  private static final Logger log = LogManager.getLogger(AbstractTemplateExpanderAdaptorTest.class);

  @Mock IPSGuidManager gmgr;
  @Mock IPSContentMgr cmgr;
  @InjectMocks TestableTemplateExpanderAdaptor cut;
  List<IPSGuid> templateList;

  @BeforeEach
  public void setUp() {
    templateList = new ArrayList<>();
    AbstractTemplateExpanderAdaptor.setGmgr(gmgr);
    AbstractTemplateExpanderAdaptor.setCmgr(cmgr);
    var tguid1 = Mockito.mock(IPSGuid.class);
    var tguid2 = Mockito.mock(IPSGuid.class);
    Mockito.when(tguid1.getType()).thenReturn(PSTypeEnum.TEMPLATE.getOrdinal());
    Mockito.when(tguid1.getUUID()).thenReturn(1);
    Mockito.when(tguid2.getType()).thenReturn(PSTypeEnum.TEMPLATE.getOrdinal());
    Mockito.when(tguid2.getUUID()).thenReturn(2);
    templateList.add(tguid1);
    templateList.add(tguid2);
    cut = new TestableTemplateExpanderAdaptor();
  }

  @Test
  public void testExpand() throws Exception {
    var params = new HashMap<String, String>();
    params.put(IPSHtmlParameters.SYS_CONTEXT, "101");
    params.put("siteid", "301");
    var summaryMap = buildSummaryMapExpectations();
    cut.setNeedsContentNode(false);
    var qr = buildQueryResultExpectations();
    var siteGuid = Mockito.mock(IPSGuid.class);
    Mockito.when(gmgr.makeGuid("301", PSTypeEnum.SITE)).thenReturn(siteGuid);
    var items = cut.expand(qr, params, summaryMap);
    assertNotNull(items);
    log.debug("items returned " + items.size());
    assertEquals(4, items.size());
    log.info(items);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testBuildNodeMap() throws Exception {
    var qr = buildQueryResultExpectations();
    var summaryMap = buildSummaryMapExpectations();
    var node1 = Mockito.mock(IPSNode.class);
    var guid1 = Mockito.mock(IPSGuid.class);
    var node2 = Mockito.mock(IPSNode.class);
    var guid2 = Mockito.mock(IPSGuid.class);
    var nodelist = new ArrayList<Node>();
    nodelist.add(Mockito.mock(Node.class));
    nodelist.add(Mockito.mock(Node.class));
    Mockito.when(cmgr.findItemsByGUID(Mockito.anyList(), Mockito.any(PSContentMgrConfig.class)))
        .thenReturn(nodelist);
    Mockito.when(node1.getGuid()).thenReturn(guid1);
    Mockito.when(node2.getGuid()).thenReturn(guid2);
    var nodeMap = cut.buildNodeMap(qr, summaryMap);
    assertNotNull(nodeMap);
    log.debug("nodeMap " + nodeMap);
    assertTrue(nodeMap.containsKey(guid1));
    assertTrue(nodeMap.containsKey(guid2));
  }

  private Map<Integer, PSComponentSummary> buildSummaryMapExpectations() {
    var sum302 = Mockito.mock(PSComponentSummary.class);
    var sum303 = Mockito.mock(PSComponentSummary.class);
    Mockito.when(sum302.getContentId()).thenReturn(302);
    Mockito.when(sum303.getContentId()).thenReturn(303);
    var summaryMap = new HashMap<Integer, PSComponentSummary>();
    summaryMap.put(302, sum302);
    summaryMap.put(303, sum303);
    return summaryMap;
  }

  private QueryResult buildQueryResultExpectations() {
    var row1 =
        new PSRow(
            new HashMap<String, Object>() {
              {
                put(IPSContentPropertyConstants.RX_SYS_CONTENTID, "302");
                put(IPSContentPropertyConstants.RX_SYS_REVISION, "2");
                put(IPSContentPropertyConstants.RX_SYS_CONTENTTYPEID, "47");
                put(IPSContentPropertyConstants.RX_SYS_FOLDERID, "201");
              }
            });
    var row2 =
        new PSRow(
            new HashMap<String, Object>() {
              {
                put(IPSContentPropertyConstants.RX_SYS_CONTENTID, "303");
                put(IPSContentPropertyConstants.RX_SYS_REVISION, "4");
                put(IPSContentPropertyConstants.RX_SYS_CONTENTTYPEID, "48");
                put(IPSContentPropertyConstants.RX_SYS_FOLDERID, "201");
              }
            });
    var rows =
        new PSRowIterator(
            new ArrayList<PSRow>() {
              {
                add(row1);
                add(row2);
              }
            });
    var guid302 = Mockito.mock(IPSGuid.class);
    var guid303 = Mockito.mock(IPSGuid.class);
    var folderGuid = Mockito.mock(IPSGuid.class);
    var rowcomp =
        new PSRowComparator(
            new ArrayList<PSPair<String, Boolean>>() {
              {
                add(new PSPair<>(IPSContentPropertyConstants.RX_SYS_CONTENTID, true));
              }
            });
    var qr =
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
    Mockito.when(gmgr.makeGuid(new PSLocator(302, 2))).thenReturn(guid302);
    Mockito.when(gmgr.makeGuid(new PSLocator(303, 4))).thenReturn(guid303);
    Mockito.when(gmgr.makeGuid(new PSLocator(201, 0))).thenReturn(folderGuid);
    return qr;
  }

  private class TestableTemplateExpanderAdaptor extends AbstractTemplateExpanderAdaptor {
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
