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
 * test.percussion.pso.preview ActionPreviewControllerTest.java
 *
 * @author DavidBenua
 *
 */
package test.percussion.pso.preview;

<<<<<<< HEAD
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
=======
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
>>>>>>> development-8.1.x

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.pso.jexl.PSOObjectFinder;
import com.percussion.pso.preview.ActionPreviewController;
import com.percussion.pso.preview.SiteFolderFinder;
import com.percussion.pso.preview.SiteFolderLocation;
import com.percussion.pso.preview.UrlBuilder;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
// ...existing code...
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.security.IPSSecurityWs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
<<<<<<< HEAD
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

@ExtendWith(MockitoExtension.class)
public class ActionPreviewControllerTest {
  private static final Logger log = LogManager.getLogger(ActionPreviewControllerTest.class);

  @InjectMocks private ActionPreviewController cut = new ActionPreviewController();

  @Mock private SiteFolderFinder finder;
  @Mock private PSOObjectFinder objectFinder;
  @Mock private UrlBuilder urlbuilder;
  @Mock private IPSAssemblyService asm;
  @Mock private IPSSecurityWs secws;

  @BeforeEach
  public void setUp() throws Exception {
    cut.setSiteFolderFinder(finder);
    cut.setViewName("myView");
    cut.setTestCommunityVisibility(false);
    ActionPreviewController.setObjectFinder(objectFinder);
    ActionPreviewController.setAsm(asm);
    ActionPreviewController.setSecws(secws);
    cut.setUrlBuilder(urlbuilder);
  }

  @Test
  @Disabled("Test is failing") // TODO: Fix me
=======
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

public class ActionPreviewControllerTest {
  private static final Logger log = LogManager.getLogger(ActionPreviewControllerTest.class);

  private ActionPreviewController cut;
  private Mockery context;
  private SiteFolderFinder finder;
  private PSOObjectFinder objectFinder;
  private UrlBuilder urlbuilder;
  private IPSAssemblyService asm;
  private IPSSecurityWs secws;

  @Before
  public void setUp() throws Exception {
    context =
        new Mockery() {
          {
            setImposteriser(ClassImposteriser.INSTANCE);
          }
        };
    cut = new ActionPreviewController();
    finder = context.mock(SiteFolderFinder.class);
    cut.setSiteFolderFinder(finder);
    cut.setViewName("myView");
    cut.setTestCommunityVisibility(false);
    objectFinder = context.mock(PSOObjectFinder.class);
    ActionPreviewController.setObjectFinder(objectFinder);

    asm = context.mock(IPSAssemblyService.class);
    ActionPreviewController.setAsm(asm);

    secws = context.mock(IPSSecurityWs.class);
    ActionPreviewController.setSecws(secws);

    urlbuilder = context.mock(UrlBuilder.class);
    cut.setUrlBuilder(urlbuilder);
  }

  @SuppressWarnings("unchecked")
  @Test
  @Ignore("Test is failing") // TODO: Fix me
>>>>>>> development-8.1.x
  public final void testHandleRequestInternalHttpServletRequestHttpServletResponse() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    try {
<<<<<<< HEAD
      PSComponentSummary summary = Mockito.mock(PSComponentSummary.class);
      List<SiteFolderLocation> locations = new ArrayList<>();
      IPSSite site = Mockito.mock(IPSSite.class);
      IPSAssemblyTemplate template = Mockito.mock(IPSAssemblyTemplate.class);

      SiteFolderLocation location1 = new SiteFolderLocation();
      location1.setFolderid(2);
      location1.setFolderPath("//Sites/foo/bar/baz");
      location1.setSite(site);
      locations.add(location1);
      request.setMethod("GET");
      request.addPreferredLocale(new Locale("en", "us"));
=======
      final PSComponentSummary summary = context.mock(PSComponentSummary.class);
      final List<SiteFolderLocation> locations = new ArrayList<SiteFolderLocation>();
      final IPSSite site = context.mock(IPSSite.class);
      final IPSAssemblyTemplate template = context.mock(IPSAssemblyTemplate.class);

      SiteFolderLocation location1 =
          new SiteFolderLocation() {
            {
              setFolderid(2);
              setFolderPath("//Sites/foo/bar/baz");
              setSite(site);
            }
          };
      locations.add(location1);
      request.setMethod("GET");

      request.addPreferredLocale(new Locale("en", "us"));

>>>>>>> development-8.1.x
      request.addParameter(IPSHtmlParameters.SYS_CONTENTID, "1");
      request.addParameter(IPSHtmlParameters.SYS_REVISION, "1");
      request.addParameter(IPSHtmlParameters.SYS_FOLDERID, "2");

<<<<<<< HEAD
      Mockito.when(finder.findSiteFolderLocations("1", "2", "")).thenReturn(locations);
      Mockito.when(objectFinder.getComponentSummaryById("1")).thenReturn(summary);
      Mockito.when(summary.getContentTypeGUID()).thenReturn(new PSGuid(3L));
      Mockito.when(asm.findTemplatesByContentType(Mockito.any(IPSGuid.class)))
          .thenReturn(Collections.singletonList(template));
      Mockito.when(site.getAssociatedTemplates()).thenReturn(Collections.singleton(template));
      Mockito.when(template.getName()).thenReturn("myTemplate");
      Mockito.when(template.getLabel()).thenReturn("My Template");
      Mockito.when(template.getOutputFormat()).thenReturn(IPSAssemblyTemplate.OutputFormat.Page);
      Mockito.when(site.getName()).thenReturn("mySite");
      Mockito.when(
              urlbuilder.buildUrl(
                  Mockito.any(IPSAssemblyTemplate.class),
                  Mockito.any(Map.class),
                  Mockito.any(SiteFolderLocation.class),
                  Mockito.any(Boolean.class)))
          .thenReturn("http://localhost/foo/bar/baz");
=======
      context.checking(
          new Expectations() {
            {
              one(finder).findSiteFolderLocations("1", "2", "");
              will(returnValue(locations));
              one(objectFinder).getComponentSummaryById("1");
              will(returnValue(summary));
              one(summary).getContentTypeGUID();
              will(returnValue(new PSLegacyGuid(3, 4)));
              one(asm).findTemplatesByContentType(with(any(IPSGuid.class)));
              will(returnValue(Collections.<IPSAssemblyTemplate>singletonList(template)));
              one(site).getAssociatedTemplates();
              will(returnValue(Collections.<IPSAssemblyTemplate>singleton(template)));
              allowing(template).getName();
              will(returnValue("myTemplate"));
              allowing(template).getLabel();
              will(returnValue("My Template"));
              one(template).getOutputFormat();
              will(returnValue(IPSAssemblyTemplate.OutputFormat.Page));
              allowing(site).getName();
              will(returnValue("mySite"));
              one(urlbuilder)
                  .buildUrl(
                      with(any(IPSAssemblyTemplate.class)),
                      with(any(Map.class)),
                      with(any(SiteFolderLocation.class)),
                      with(any(Boolean.class)));
              will(returnValue("http://localhost/foo/bar/baz"));
            }
          });
>>>>>>> development-8.1.x

      ModelAndView mav = cut.handleRequest(request, response);
      assertNotNull(mav);
      assertEquals("myView", mav.getViewName());

      log.info("Mav is " + mav);
<<<<<<< HEAD
=======
      context.assertIsSatisfied();
>>>>>>> development-8.1.x
    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception caught");
    }
  }
}
