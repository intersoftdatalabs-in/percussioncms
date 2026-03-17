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

package test.percussion.pso.demandpreview.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.percussion.pso.demandpreview.exception.SiteLookUpException;
import com.percussion.pso.demandpreview.service.SiteEditionConfig;
import com.percussion.pso.demandpreview.service.SiteEditionHolder;
import com.percussion.pso.demandpreview.service.SiteEditionLookUpService;
import com.percussion.pso.demandpreview.service.impl.SiteEditionLookUpServiceImpl;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.utils.guid.IPSGuid;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SiteTemplateLookUpServiceImplTest {
  private static final Logger log = LogManager.getLogger(SiteTemplateLookUpServiceImplTest.class);

  @Mock IPSSiteManager siteManager;
  @Mock IPSPublisherService publisherService;
  @Mock IPSAssemblyService asm;
  @Mock SiteEditionLookUpService siteEditionLookUpService;
  @Mock IPSGuidManager guidManager;

  TestLookUpService lookUp;

  @BeforeEach
  public void setUp() throws Exception {
    lookUp = new TestLookUpService();
    lookUp.setSiteManager(siteManager);
    lookUp.setPubisherService(publisherService);
    lookUp.setGuidManager(guidManager);
    lookUp.setAsm(asm);
  }

  @Test
  @Disabled
  // TODO: Fix testLookUpSiteEdition
  public void testLookUpSiteEdition() throws SiteLookUpException {
    Map<String, SiteEditionConfig> siteLookUpMap = new HashMap<>();
    SiteEditionConfig sConfig = new SiteEditionConfig();
    sConfig.setSiteName("psoSite");
    sConfig.setEditionName("psoEdition");
    sConfig.setAssemblyContext(1);
    sConfig.setContextURLRootVar("http://test/");
    siteLookUpMap.put("psoSite", sConfig);
    lookUp.setSiteLookUpMap(siteLookUpMap);

    final String siteId = "234";
    final SiteEditionHolder stHolder = new SiteEditionHolder();
    final IPSSite site = mock(IPSSite.class);
    final IPSSite pSite = mock(IPSSite.class);
    final IPSEdition edition = mock(IPSEdition.class);
    final IPSGuid siteGuid = mock(IPSGuid.class);
    final IPSAssemblyTemplate template = mock(IPSAssemblyTemplate.class);
    stHolder.setSite(pSite);
    stHolder.setEdition(edition);

    when(guidManager.makeGuid(Integer.parseInt(siteId), PSTypeEnum.SITE)).thenReturn(siteGuid);
    when(siteManager.loadSite(siteGuid)).thenReturn(site);
    when(site.getName()).thenReturn("psoSite");
    when(publisherService.findEditionByName("psoEdition")).thenReturn(edition);
    when(siteManager.loadSite("psoSite")).thenReturn(pSite);

    SiteEditionHolder holder = lookUp.LookUpSiteEdition(siteId);
    assertNotNull(holder);
    assertSame(holder.getClass(), stHolder.getClass());
    verify(guidManager).makeGuid(Integer.parseInt(siteId), PSTypeEnum.SITE);
  }

  @Test
  public void testLookupWithWrongSite() throws SiteLookUpException {
    Map<String, SiteEditionConfig> siteLookUpMap = new HashMap<String, SiteEditionConfig>();
    SiteEditionConfig sConfig = new SiteEditionConfig();
    sConfig.setSiteName("psoSite");
    sConfig.setEditionName("psoTemplate");
    siteLookUpMap.put("siteName", sConfig);
    lookUp.setSiteLookUpMap(siteLookUpMap);

    final String siteId = "234";
    final SiteEditionHolder stHolder = new SiteEditionHolder();
    final IPSSite site = mock(IPSSite.class);
    final IPSSite pSite = mock(IPSSite.class);
    final IPSEdition edition = mock(IPSEdition.class);
    final IPSGuid siteGuid = mock(IPSGuid.class);
    stHolder.setSite(pSite);
    stHolder.setEdition(edition);

    when(guidManager.makeGuid(142, PSTypeEnum.SITE)).thenReturn(siteGuid);
    when(siteManager.loadSite(siteGuid)).thenThrow(new PSNotFoundException("Site Not Found"));

    try {
      lookUp.LookUpSiteEdition("142");
      fail("Test with wrong site name failed");
    } catch (PSNotFoundException | SiteLookUpException ex) {
      log.info("Got expected exception {}", ex.getMessage());
    }
    verify(guidManager).makeGuid(142, PSTypeEnum.SITE);
  }

  @Test
  public void testLookWithNoSiteName() throws SiteLookUpException {
    Map<String, SiteEditionConfig> siteLookUpMap = new HashMap<String, SiteEditionConfig>();
    SiteEditionConfig sConfig = new SiteEditionConfig();
    sConfig.setSiteName("");
    sConfig.setEditionName("psoTemplate");
    siteLookUpMap.put("site", sConfig);
    lookUp.setSiteLookUpMap(siteLookUpMap);

    final String siteId = "234";
    final SiteEditionHolder stHolder = new SiteEditionHolder();
    final IPSSite site = mock(IPSSite.class);
    final IPSSite pSite = mock(IPSSite.class);
    final IPSEdition edition = mock(IPSEdition.class);
    final IPSGuid siteGuid = mock(IPSGuid.class);
    stHolder.setSite(pSite);
    stHolder.setEdition(edition);

    when(guidManager.makeGuid(Integer.parseInt(siteId), PSTypeEnum.SITE)).thenReturn(siteGuid);
    when(siteManager.loadSite(siteGuid)).thenReturn(site);
    when(site.getName()).thenReturn("siteName");

    try {
      lookUp.LookUpSiteEdition(siteId);
      fail("Test with empty site name failed");
    } catch (PSNotFoundException | SiteLookUpException ex) {
      log.error("Error looking up site info", ex);
    }
    verify(guidManager).makeGuid(Integer.parseInt(siteId), PSTypeEnum.SITE);
  }

  @Test
  public void testLookWithNoSiteEdition() throws SiteLookUpException {
    Map<String, SiteEditionConfig> siteLookUpMap = new HashMap<String, SiteEditionConfig>();
    SiteEditionConfig sConfig = new SiteEditionConfig();
    sConfig.setSiteName("psoSite");
    sConfig.setEditionName("");
    siteLookUpMap.put("site", sConfig);
    lookUp.setSiteLookUpMap(siteLookUpMap);

    final String siteId = "234";
    final SiteEditionHolder stHolder = new SiteEditionHolder();
    final IPSSite site = mock(IPSSite.class);
    final IPSSite pSite = mock(IPSSite.class);
    final IPSEdition edition = mock(IPSEdition.class);
    final IPSGuid siteGuid = mock(IPSGuid.class);
    stHolder.setSite(pSite);
    stHolder.setEdition(edition);

    when(guidManager.makeGuid(Integer.parseInt(siteId), PSTypeEnum.SITE)).thenReturn(siteGuid);
    when(siteManager.loadSite(siteGuid)).thenReturn(site);
    when(site.getName()).thenReturn("siteName");

    try {
      lookUp.LookUpSiteEdition(siteId);
      fail("Test with empty edition Name failed");
    } catch (PSNotFoundException | SiteLookUpException ex) {
      log.error("Error looking up site info", ex);
    }
    verify(guidManager).makeGuid(Integer.parseInt(siteId), PSTypeEnum.SITE);
  }

  private class TestLookUpService extends SiteEditionLookUpServiceImpl {
    @Override
    public void setSiteManager(IPSSiteManager siteManager) {
      super.setSiteManager(siteManager);
    }

    @Override
    public void setPubisherService(IPSPublisherService publisherService) {
      super.setPubisherService(publisherService);
    }

    @Override
    public void setGuidManager(IPSGuidManager guidManager) {
      super.setGuidManager(guidManager);
    }

    /**
     * @see SiteEditionLookUpServiceImpl#setAsm(IPSAssemblyService)
     */
    @Override
    public void setAsm(IPSAssemblyService asm) {
      super.setAsm(asm);
    }
  }
}
