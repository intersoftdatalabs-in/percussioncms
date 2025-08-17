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
 * test.percussion.pso.preview AbstractMenuControllerTest.java
 *
 * @author DavidBenua
 *
 */
package test.percussion.pso.preview;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pso.preview.AbstractMenuController;
import com.percussion.pso.preview.SiteFolderLocation;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.security.IPSSecurityWs;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AbstractMenuControllerTest {
  @InjectMocks TestableAbstractMenuController cut = new TestableAbstractMenuController();

  @Mock IPSSecurityWs secws;
  @Mock IPSAssemblyService asm;

  @BeforeEach
  public void setUp() throws Exception {
    AbstractMenuController.setSecws(secws);
    AbstractMenuController.setAsm(asm);
  }

  @Test
  public final void testFilterVisibleTemplates() {
    IPSSite site = Mockito.mock(IPSSite.class);
    Set<IPSSite> sites = Collections.singleton(site);
    IPSAssemblyTemplate t1 = Mockito.mock(IPSAssemblyTemplate.class);
    IPSAssemblyTemplate t2 = Mockito.mock(IPSAssemblyTemplate.class);
    IPSGuid tg1 = Mockito.mock(IPSGuid.class);
    IPSGuid tg2 = Mockito.mock(IPSGuid.class);
    Set<IPSAssemblyTemplate> temps = new HashSet<>();
    temps.add(t1);
    temps.add(t2);

    cut.setTestCommunityVisibility(true);
    Mockito.when(t1.getGUID()).thenReturn(tg1);
    Mockito.when(t2.getGUID()).thenReturn(tg2);
    Mockito.when(t1.getName()).thenReturn("t1");
    Mockito.when(t2.getName()).thenReturn("t2");
    Mockito.when(site.getName()).thenReturn("mySite");
    Mockito.when(site.getAssociatedTemplates()).thenReturn(temps);
    Mockito.when(secws.filterByRuntimeVisibility(Mockito.anyList()))
        .thenReturn(Collections.singletonList(tg1));

    List<IPSAssemblyTemplate> results = cut.filterVisibleTemplates(temps, sites);
    assertNotNull(results);
    assertEquals(1, results.size());
    assertEquals(t1, results.get(0));
  }

  @Test
  public final void testIsTemplateOnSite() {
    IPSSite site = Mockito.mock(IPSSite.class);
    Set<IPSSite> sites = Collections.singleton(site);
    IPSAssemblyTemplate t1 = Mockito.mock(IPSAssemblyTemplate.class);
    IPSAssemblyTemplate t2 = Mockito.mock(IPSAssemblyTemplate.class);
    Set<IPSAssemblyTemplate> temps = new HashSet<>();
    temps.add(t1);
    Mockito.when(site.getAssociatedTemplates()).thenReturn(temps);
    Mockito.when(t1.getName()).thenReturn("Template1");
    Mockito.when(site.getName()).thenReturn("Site1");

    boolean res = cut.isTemplateOnSite(t1, sites);
    assertTrue(res);
    res = cut.isTemplateOnSite(t2, sites);
    assertFalse(res);
  }

  class TestableAbstractMenuController extends AbstractMenuController {

    /**
     * @see AbstractMenuController#filterVisibleTemplates(Collection, Set)
     */
    @Override
    public List<IPSAssemblyTemplate> filterVisibleTemplates(
        Collection<IPSAssemblyTemplate> alltemps, Set<IPSSite> sites) {
      return super.filterVisibleTemplates(alltemps, sites);
    }

    /**
     * @see AbstractMenuController#findSitesFromLocations(List)
     */
    @Override
    public Set<IPSSite> findSitesFromLocations(List<SiteFolderLocation> locations) {
      return super.findSitesFromLocations(locations);
    }

    /**
     * @see AbstractMenuController#isTemplateOnSite(IPSAssemblyTemplate, Set)
     */
    @Override
    public boolean isTemplateOnSite(IPSAssemblyTemplate t, Set<IPSSite> sites) {
      return super.isTemplateOnSite(t, sites);
    }
  }
}
