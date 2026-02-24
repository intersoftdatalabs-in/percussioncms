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
 * test.percussion.pso.preview PreviewUrlBuilderTest.java
 *
 * @author DavidBenua
 *
 */
package test.percussion.pso.preview;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pso.preview.PreviewUrlBuilder;
import com.percussion.pso.preview.SiteFolderLocation;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PreviewUrlBuilderTest {
  private static final Logger log = LogManager.getLogger(PreviewUrlBuilderTest.class);

  private PreviewUrlBuilder cut;

  @Mock
  private IPSAssemblyTemplate template;
  @Mock
  private IPSSite site;

  @BeforeEach
  public void setUp() throws Exception {
    cut = new PreviewUrlBuilder();
    cut.setDefaultLocationUrl("defaultLocation");
    cut.setMultipleLocationUrl("multipleLocation");
  }

  @Test
  public final void testBuildPreviewUrl() {
    final IPSGuid tempGuid = new PSGuid(123L);
    final IPSAssemblyTemplate template = mock(IPSAssemblyTemplate.class);

    Map<String, Object> urlParams = new HashMap<String, Object>();
    urlParams.put(IPSHtmlParameters.SYS_CONTENTID, "23");
    urlParams.put(IPSHtmlParameters.SYS_REVISION, "1");

    try {
      when(template.getGUID()).thenReturn(tempGuid);
      String result = cut.buildUrl(template, urlParams, null, true);
      assertNotNull(result);
      log.info("url result is " + result);
      assertTrue(result.contains("multiple"));
      verify(template).getGUID();
    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception");
    }
  }

  @Test
  public final void testBuildPreviewUrlAssembly() {
    final IPSGuid tempGuid = new PSLegacyGuid(123L);
    final IPSAssemblyTemplate template = mock(IPSAssemblyTemplate.class);
    final IPSSite site = mock(IPSSite.class);
    SiteFolderLocation location = new SiteFolderLocation();
    location.setSite(site);
    location.setFolderid(457);
    Map<String, Object> urlParams = new HashMap<String, Object>();
    urlParams.put(IPSHtmlParameters.SYS_CONTENTID, "23");
    urlParams.put(IPSHtmlParameters.SYS_REVISION, "1");

    try {
      when(template.getGUID()).thenReturn(tempGuid);
      // preview builder does not use assemblyUrl; no need to stub it
      when(site.getSiteId()).thenReturn(123L);
      cut.setDefaultLocationUrl("/Rhythmyx/foo/bar");
      String result = cut.buildUrl(template, urlParams, location, false);
      assertNotNull(result);
      log.info("url result is " + result);
      assertTrue(result.contains("myx/foo/bar"));
      assertFalse(result.contains("myx/baz/bat"));
      verify(template).getGUID();
      verify(site).getSiteId();
    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception");
    }
  }

  @Test
  public final void testBuildPreviewUrlAssemblyNull() {
    final IPSGuid tempGuid = new PSLegacyGuid(123L);
    final IPSAssemblyTemplate template = mock(IPSAssemblyTemplate.class);
    final IPSSite site = mock(IPSSite.class);

    SiteFolderLocation location = new SiteFolderLocation();
    location.setSite(site);
    // location.setSiteid(123L);
    location.setFolderid(457);
    Map<String, Object> urlParams = new HashMap<String, Object>();
    urlParams.put(IPSHtmlParameters.SYS_CONTENTID, "23");
    urlParams.put(IPSHtmlParameters.SYS_REVISION, "1");

    try {
      when(template.getGUID()).thenReturn(tempGuid);
      // not used by builder
      when(site.getSiteId()).thenReturn(123L);

      cut.setDefaultLocationUrl("/Rhythmyx/foo/bar");
      String result = cut.buildUrl(template, urlParams, location, false);
      assertNotNull(result);
      log.info("url result is " + result);
      assertTrue(result.contains("myx/foo/bar"));

      verify(template).getGUID();
      verify(site).getSiteId();
    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception");
    }
  }
}
