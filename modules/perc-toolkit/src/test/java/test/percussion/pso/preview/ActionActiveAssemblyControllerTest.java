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
/*
 * test.percussion.pso.preview ActionActiveAssemblyControllerTest.java
 *
 * @author DavidBenua
 *
 */
package test.percussion.pso.preview;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.error.PSException;
import com.percussion.pso.jexl.PSOObjectFinder;
import com.percussion.pso.preview.AbstractMenuController;
import com.percussion.pso.preview.ActionActiveAssemblyController;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.legacy.IPSCmsContentSummaries;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.utils.guid.IPSGuid;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ActionActiveAssemblyControllerTest {
  private static final org.apache.logging.log4j.Logger log =
      LogManager.getLogger(ActionActiveAssemblyControllerTest.class);

  @Mock IPSAssemblyService asm;
  @Mock IPSCmsContentSummaries sumsvc;
  @Mock PSOObjectFinder finder;

  TestableActiveAssemblyController cut;

  @BeforeEach
  public void setUp() {
    cut = new TestableActiveAssemblyController();
    AbstractMenuController.setAsm(asm);
    AbstractMenuController.setObjectFinder(finder);
    cut.setTestCommunityVisibility(false);
  }

  @Test
  @Disabled("Test is failing") // TODO: Fix me
  public void testFindVisibleTemplates() {
    IPSSite site = Mockito.mock(IPSSite.class);
    IPSAssemblyTemplate t1 = Mockito.mock(IPSAssemblyTemplate.class);
    IPSAssemblyTemplate t2 = Mockito.mock(IPSAssemblyTemplate.class);
    PSComponentSummary summ = Mockito.mock(PSComponentSummary.class);
    List<IPSAssemblyTemplate> templates = List.of(t1, t2);
    IPSGuid ctype = new PSGuid(123L);

    try {
      Mockito.when(asm.findTemplatesByContentType(Mockito.any(IPSGuid.class)))
          .thenReturn(templates);
      Mockito.when(finder.getComponentSummaryById("2")).thenReturn(summ);
      Mockito.when(summ.getContentTypeGUID()).thenReturn(ctype);
      Mockito.when(site.getAssociatedTemplates()).thenReturn(Collections.singleton(t2));
      Mockito.when(site.getName()).thenReturn("mySite");
      Mockito.when(t1.getName()).thenReturn("t1");
      Mockito.when(t1.getOutputFormat()).thenReturn(IPSAssemblyTemplate.OutputFormat.Page);
      Mockito.when(t2.getName()).thenReturn("t2");
      Mockito.when(t2.getOutputFormat()).thenReturn(IPSAssemblyTemplate.OutputFormat.Page);
      Mockito.when(t2.getActiveAssemblyType()).thenReturn(IPSAssemblyTemplate.AAType.Normal);

      List<IPSAssemblyTemplate> results =
          cut.findVisibleTemplates("2", Collections.singleton(site));
      assertNotNull(results);
      assertEquals(1, results.size());
    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception");
    }
  }

  private static class TestableActiveAssemblyController extends ActionActiveAssemblyController {
    @Override
    public List<IPSAssemblyTemplate> findVisibleTemplates(String contentid, Set<IPSSite> sites)
        throws PSException, PSAssemblyException {
      return super.findVisibleTemplates(contentid, sites);
    }
  }
}
