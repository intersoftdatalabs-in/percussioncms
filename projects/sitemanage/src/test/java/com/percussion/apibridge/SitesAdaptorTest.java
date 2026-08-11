/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.sites.Site;
import com.percussion.rest.sites.VirtualSiteProperties;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSPublishingContext;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.data.PSSite;
import com.percussion.services.virtualsite.PSVirtualSiteHelper;
import com.percussion.utils.guid.IPSGuid;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// uses PSVirtualSiteHelper.putProperty (no GuidManager) for property bag setup

@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class SitesAdaptorTest {

  @Mock private IPSSiteManager siteManager;

  private SitesAdaptor adaptor;
  private IPSGuid previewCtx;
  private IPSGuid siteGuid;

  @BeforeEach
  void setUp() {
    adaptor = new SitesAdaptor(siteManager);
    previewCtx = new PSGuid(PSTypeEnum.CONTEXT, 1);
    siteGuid = new PSGuid(PSTypeEnum.SITE, 100);
  }

  @Test
  void readVirtual_mapsHelperProperties() {
    PSSite site = new PSSite();
    site.setName("Help");
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, "C:/docs/product-docs");
    put(site, PSVirtualSiteHelper.PROP_CONFIG_FILE, "custom.yaml");
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "product-docs");

    VirtualSiteProperties v = SitesAdaptor.readVirtual(site);
    assertEquals("git-filesystem", v.getSourceKind().orElse(null));
    assertEquals("C:/docs/product-docs", v.getRootPath().orElse(null));
    assertEquals("custom.yaml", v.getConfigFile().orElse(null));
    assertEquals("product-docs", v.getSiteKey().orElse(null));
    assertTrue(Boolean.TRUE.equals(v.getVirtual()));
  }

  @Test
  void readVirtual_repositoryIsNotVirtual() {
    PSSite site = new PSSite();
    site.setName("Corp");
    VirtualSiteProperties v = SitesAdaptor.readVirtual(site);
    assertFalse(Boolean.TRUE.equals(v.getVirtual()));
    assertTrue(v.getSourceKind().isEmpty());
  }

  @Test
  void findByName_returnsDetailWithVirtual() {
    PSSite site = new PSSite();
    site.setName("Help");
    site.setDescription("docs");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, "C:/docs");
    when(siteManager.findSite("Help")).thenReturn(site);

    Site out = adaptor.findByName("Help");
    assertNotNull(out);
    assertEquals("Help", out.getName().orElse(null));
    assertTrue(out.getVirtual().isPresent());
    assertTrue(Boolean.TRUE.equals(out.getVirtual().get().getVirtual()));
  }

  @Test
  void getVirtualSiteProperties_404WhenMissing() {
    when(siteManager.findSite("missing")).thenReturn(null);
    // guid parse may throw or return null find
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.getVirtualSiteProperties("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void updateVirtualSiteProperties_persistsAndValidates() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("Help");
    existing.setGUID(siteGuid);

    PSSite modifiable = new PSSite();
    modifiable.setName("Help");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("Help")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("git-filesystem");
    body.setRootPath("C:/docs/product-docs");
    body.setConfigFile("_config.yaml");
    body.setSiteKey("product-docs");

    VirtualSiteProperties out = adaptor.updateVirtualSiteProperties("Help", body);
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    assertEquals("git-filesystem", out.getSourceKind().orElse(null));
    assertEquals("C:/docs/product-docs", out.getRootPath().orElse(null));

    ArgumentCaptor<PSSite> saved = ArgumentCaptor.forClass(PSSite.class);
    verify(siteManager).saveSite(saved.capture());
    assertTrue(PSVirtualSiteHelper.isVirtual(saved.getValue()));
    assertEquals(
        "product-docs",
        PSVirtualSiteHelper.findProperty(saved.getValue(), PSVirtualSiteHelper.PROP_SITE_KEY)
            .orElse(null));
  }

  @Test
  void updateVirtualSiteProperties_rejectsUnknownSourceKind() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("Help");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("Help");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("Help")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("sql-adapter");
    body.setRootPath("C:/docs");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateVirtualSiteProperties("Help", body));
    assertEquals(400, ex.getResponse().getStatus());
    verify(siteManager, never()).saveSite(any());
  }

  @Test
  void updateVirtualSiteProperties_requiresRootWhenVirtual() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("Help");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("Help");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("Help")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("git-filesystem");
    // rootPath missing

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateVirtualSiteProperties("Help", body));
    assertEquals(400, ex.getResponse().getStatus());
    verify(siteManager, never()).saveSite(any());
  }

  @Test
  void updateVirtualSiteProperties_clearToRepository() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("Help");
    existing.setGUID(siteGuid);

    PSSite modifiable = new PSSite();
    modifiable.setName("Help");
    modifiable.setGUID(siteGuid);
    put(modifiable, PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem");
    put(modifiable, PSVirtualSiteHelper.PROP_ROOT_PATH, "C:/docs");

    when(siteManager.findSite("Help")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("repository");

    VirtualSiteProperties out = adaptor.updateVirtualSiteProperties("Help", body);
    assertFalse(Boolean.TRUE.equals(out.getVirtual()));
    verify(siteManager).saveSite(eq(modifiable));
    assertFalse(PSVirtualSiteHelper.isVirtual(modifiable));
  }

  @Test
  void resolvePropertyContext_reusesExisting() throws Exception {
    PSSite site = new PSSite();
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem");
    IPSGuid ctx = adaptor.resolvePropertyContext(site);
    assertEquals(previewCtx, ctx);
    verify(siteManager, never()).loadContext(any(String.class));
  }

  @Test
  void resolvePropertyContext_fallsBackToFirstWhenNoPreview() throws Exception {
    PSSite site = new PSSite();
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT))
        .thenThrow(new PSNotFoundException("no preview"));
    IPSPublishingContext other = mock(IPSPublishingContext.class);
    IPSGuid otherId = new PSGuid(PSTypeEnum.CONTEXT, 9);
    when(other.getGUID()).thenReturn(otherId);
    when(siteManager.findAllContexts()).thenReturn(List.of(other));

    assertEquals(otherId, adaptor.resolvePropertyContext(site));
  }

  private void put(PSSite site, String name, String value) {
    PSVirtualSiteHelper.putProperty(site, previewCtx, name, value);
  }
}
