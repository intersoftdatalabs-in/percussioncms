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

<<<<<<< HEAD
import static org.junit.jupiter.api.Assertions.*;
=======
import static org.junit.Assert.*;
>>>>>>> development-8.1.x

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
<<<<<<< HEAD
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
=======
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

public class AbstractMenuControllerTest {

  TestableAbstractMenuController cut;
  Mockery context;

  @Before
  public void setUp() throws Exception {
    context =
        new Mockery() {
          {
            setImposteriser(ClassImposteriser.INSTANCE);
          }
        };
    cut = new TestableAbstractMenuController();
  }

  @SuppressWarnings("unchecked")
  @Test
  public final void testFilterVisibleTemplates() {
    final IPSSite site = context.mock(IPSSite.class);
    final Set<IPSSite> sites = Collections.<IPSSite>singleton(site);

    final IPSAssemblyTemplate t1 = context.mock(IPSAssemblyTemplate.class, "t1");
    final IPSAssemblyTemplate t2 = context.mock(IPSAssemblyTemplate.class, "t2");
    final IPSGuid tg1 = context.mock(IPSGuid.class, "tg1");
    final IPSGuid tg2 = context.mock(IPSGuid.class, "tg2");
    final Set<IPSAssemblyTemplate> temps = new HashSet<IPSAssemblyTemplate>();
    temps.add(t1);
    temps.add(t2);

    final IPSSecurityWs secws = context.mock(IPSSecurityWs.class);
    final IPSAssemblyService asm = context.mock(IPSAssemblyService.class);
    AbstractMenuController.setSecws(secws);
    AbstractMenuController.setAsm(asm);

    cut.setTestCommunityVisibility(true);

    context.checking(
        new Expectations() {
          {
            allowing(t1).getGUID();
            will(returnValue(tg1));
            allowing(t2).getGUID();
            will(returnValue(tg2));
            allowing(t1).getName();
            will(returnValue("t1"));
            allowing(t2).getName();
            will(returnValue("t2"));
            allowing(site).getName();
            will(returnValue("mySite"));
            allowing(site).getAssociatedTemplates();
            will(returnValue(temps));

            one(secws).filterByRuntimeVisibility(with(any(List.class)));
            will(returnValue(Collections.<IPSGuid>singletonList(tg1)));
          }
        });

    List<IPSAssemblyTemplate> results = cut.filterVisibleTemplates(temps, sites);

>>>>>>> development-8.1.x
    assertNotNull(results);
    assertEquals(1, results.size());
    assertEquals(t1, results.get(0));
  }

  @Test
  public final void testIsTemplateOnSite() {
<<<<<<< HEAD
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
=======
    final IPSSite site = context.mock(IPSSite.class);
    final Set<IPSSite> sites = Collections.<IPSSite>singleton(site);

    final IPSAssemblyTemplate t1 = context.mock(IPSAssemblyTemplate.class, "t1");
    final IPSAssemblyTemplate t2 = context.mock(IPSAssemblyTemplate.class, "t2");

    final Set<IPSAssemblyTemplate> temps = new HashSet<IPSAssemblyTemplate>();
    temps.add(t1);

    context.checking(
        new Expectations() {
          {
            atLeast(1).of(site).getAssociatedTemplates();
            will(returnValue(temps));
            allowing(t1).getName();
            will(returnValue("Template1"));
            allowing(site).getName();
            will(returnValue("Site1"));
          }
        });

    boolean res = cut.isTemplateOnSite(t1, sites);
    assertTrue(res);

    context.assertIsSatisfied();

    res = cut.isTemplateOnSite(t2, sites);

>>>>>>> development-8.1.x
    assertFalse(res);
  }

  class TestableAbstractMenuController extends AbstractMenuController {

<<<<<<< HEAD
    /**
     * @see AbstractMenuController#filterVisibleTemplates(Collection, Set)
     */
=======
    /** @see AbstractMenuController#filterVisibleTemplates(Collection, Set) */
>>>>>>> development-8.1.x
    @Override
    public List<IPSAssemblyTemplate> filterVisibleTemplates(
        Collection<IPSAssemblyTemplate> alltemps, Set<IPSSite> sites) {
      return super.filterVisibleTemplates(alltemps, sites);
    }

<<<<<<< HEAD
    /**
     * @see AbstractMenuController#findSitesFromLocations(List)
     */
=======
    /** @see AbstractMenuController#findSitesFromLocations(List) */
>>>>>>> development-8.1.x
    @Override
    public Set<IPSSite> findSitesFromLocations(List<SiteFolderLocation> locations) {
      return super.findSitesFromLocations(locations);
    }

<<<<<<< HEAD
    /**
     * @see AbstractMenuController#isTemplateOnSite(IPSAssemblyTemplate, Set)
     */
=======
    /** @see AbstractMenuController#isTemplateOnSite(IPSAssemblyTemplate, Set) */
>>>>>>> development-8.1.x
    @Override
    public boolean isTemplateOnSite(IPSAssemblyTemplate t, Set<IPSSite> sites) {
      return super.isTemplateOnSite(t, sites);
    }
  }
}
