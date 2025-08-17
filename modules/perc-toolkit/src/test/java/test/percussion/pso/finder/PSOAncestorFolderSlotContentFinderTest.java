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
package test.percussion.pso.finder;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.pso.finder.PSOAncestorFolderSlotContentFinder;
import com.percussion.pso.jexl.PSOFolderTools;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.impl.finder.PSBaseSlotContentFinder.SlotItem;
import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.content.IPSContentWs;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Scenario description:
 *
 * @author adamgent, Nov 25, 2008 // REFACTORED: CP-JAVA11
 */
@ExtendWith(MockitoExtension.class)
public class PSOAncestorFolderSlotContentFinderTest {
  @Mock IPSContentWs contentWs;
  @Mock IPSGuidManager guidManager;
  @Mock IPSAssemblyItem assemblyItem;
  @Mock IPSTemplateSlot slot;
  @Mock IPSGuid a;
  @Mock IPSGuid b;
  @Mock IPSGuid c;
  @InjectMocks PSOAncestorFolderSlotContentFinder finder;
  StubFolderTools folderTools;
  Map<String, Object> params;

  protected static class StubFolderTools extends PSOFolderTools {
    String expectedFolderPath = "//path";

    public String getExpectedFolderPath() {
      return expectedFolderPath;
    }

    public void setExpectedFolderPath(String expectedFolderPath) {
      this.expectedFolderPath = expectedFolderPath;
    }

    @Override
    public String getParentFolderPath(IPSAssemblyItem assemblyItem)
        throws PSErrorResultsException, PSExtensionProcessingException, PSErrorException {
      return getExpectedFolderPath();
    }
  }

  @BeforeEach
  public void setUp() {
    folderTools = new StubFolderTools();
    finder = new PSOAncestorFolderSlotContentFinder(contentWs, folderTools);
    params = new HashMap<>();
    Mockito.when(slot.getFinderArguments()).thenReturn(new HashMap<>());
  }

  @Test
  public void shouldGetSlotItemOfContentTypeInParentFolder() throws Exception {
    params.put(PSOAncestorFolderSlotContentFinder.PARAM_CONTENTTYPE, "generic");
    final String path = "//a/b/c";
    folderTools.setExpectedFolderPath(path);
    Mockito.when(contentWs.findPathIds(path)).thenReturn(asList(a, b, c));
    Mockito.when(contentWs.findFolderChildren(c, false)).thenReturn(asList());
    PSItemSummary sumYes = new PSItemSummary(1, 1, "yes", 300, "generic", false);
    PSItemSummary sumNo = new PSItemSummary(2, 1, "yes", 302, "blah", false);
    Mockito.when(contentWs.findFolderChildren(b, false)).thenReturn(asList(sumNo, sumYes));
    // Should not call findFolderChildren(a, false)
    Set<SlotItem> slotItems = finder.getSlotItems(assemblyItem, slot, params);
    assertEquals(1, slotItems.size());
  }

  @Test
  public void shouldGetZeroSlotItemsFromAncestorFoldersIfNoItemOfDesiredTypeExistInAncestorFolders()
      throws Exception {
    params.put(PSOAncestorFolderSlotContentFinder.PARAM_CONTENTTYPE, "generic");
    final String path = "//a/b/c";
    folderTools.setExpectedFolderPath(path);
    Mockito.when(contentWs.findPathIds(path)).thenReturn(asList(a, b, c));
    Mockito.when(contentWs.findFolderChildren(c, false)).thenReturn(asList());
    PSItemSummary sumNotGeneric = new PSItemSummary(1, 1, "yes", 300, "NOT_GENERIC", false);
    PSItemSummary sumNo = new PSItemSummary(2, 1, "yes", 302, "blah", false);
    Mockito.when(contentWs.findFolderChildren(b, false)).thenReturn(asList(sumNo, sumNotGeneric));
    Mockito.when(contentWs.findFolderChildren(a, false)).thenReturn(asList());
    Set<SlotItem> slotItems = finder.getSlotItems(assemblyItem, slot, params);
    assertEquals(0, slotItems.size());
  }
}
