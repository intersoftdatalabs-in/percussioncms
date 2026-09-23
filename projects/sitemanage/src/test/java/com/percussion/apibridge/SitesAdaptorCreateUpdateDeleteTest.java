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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.sites.Site;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.data.PSSite;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.data.PSSiteProperties;
import com.percussion.sitemanage.service.IPSSiteDataService;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Slice 22 Developer site CUD: Admin create/update/delete via IPSSiteManager. */
@Tag("UnitTest")
class SitesAdaptorCreateUpdateDeleteTest {

  private IPSSiteManager siteManager;
  private SitesAdaptor adaptor;
  private SitesAdaptor denied;

  @BeforeEach
  void setUp() {
    siteManager = mock(IPSSiteManager.class);
    adaptor = new SitesAdaptor(siteManager, () -> true, k -> null, null);
    denied = new SitesAdaptor(siteManager, () -> false, k -> null, null);
  }

  private static Site body(String name, String description, String baseUrl) {
    Site s = new Site();
    s.setName(name);
    s.setDescription(description);
    s.setBaseUrl(baseUrl);
    return s;
  }

  @Test
  void create_savesNewSite() {
    when(siteManager.findSite("NightlySite")).thenReturn(null);
    PSSite created = new PSSite();
    when(siteManager.createSite()).thenReturn(created);

    Site out = adaptor.createSiteFromRequest(body("NightlySite", "  Docs  ", "https://ex.example"));

    assertEquals("NightlySite", out.getName());
    assertEquals("Docs", created.getDescription());
    assertEquals("https://ex.example", created.getBaseUrl());
    verify(siteManager).saveSite(created);
  }

  @Test
  void create_duplicateName_409() {
    when(siteManager.findSite("NightlySite")).thenReturn(new PSSite());
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.createSiteFromRequest(body("NightlySite", null, null)));
    assertEquals(409, ex.getResponse().getStatus());
    verify(siteManager, never()).createSite();
  }

  @Test
  void create_blankName_400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.createSiteFromRequest(body("  ", null, null)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void create_invalidCharacters_400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.createSiteFromRequest(body("bad/name", null, null)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void create_nonAdmin_403() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> denied.createSiteFromRequest(body("NightlySite", null, null)));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void update_descriptionAndBaseUrl() throws PSNotFoundException {
    PSSite existing = new PSSite();
    existing.setName("NightlySite");
    existing.setGUID(new PSGuid(PSTypeEnum.SITE, 42));
    existing.setDescription("old");
    when(siteManager.findSite("NightlySite")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(existing.getGUID())).thenReturn(existing);

    Site req = body("NightlySite", "new desc", "https://new.example");
    Site out = adaptor.updateSite("NightlySite", req);

    assertEquals("new desc", out.getDescription());
    assertEquals("https://new.example", existing.getBaseUrl());
    verify(siteManager).saveSite(existing);
  }

  @Test
  void update_nameMismatch_400() {
    PSSite existing = new PSSite();
    existing.setName("NightlySite");
    existing.setGUID(new PSGuid(PSTypeEnum.SITE, 42));
    when(siteManager.findSite("NightlySite")).thenReturn(existing);

    Site req = body("OtherName", "x", null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.updateSite("NightlySite", req));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void rename_duplicateName_409() {
    PSSite existing = new PSSite();
    existing.setName("NightlySite");
    when(siteManager.findSite("NightlySite")).thenReturn(existing);
    when(siteManager.findSite("OtherSite")).thenReturn(new PSSite());
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.renameSite("NightlySite", "OtherSite"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void rename_blankName_400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.renameSite("NightlySite", " "));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void rename_illegalName_400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.renameSite("NightlySite", "bad/name"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void rename_nonAdmin_403() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> denied.renameSite("NightlySite", "OtherSite"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void rename_persistsViaSiteDataService() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("NightlySite");
    existing.setGUID(new PSGuid(PSTypeEnum.SITE, 7));
    when(siteManager.findSite("NightlySite")).thenReturn(existing);
    when(siteManager.findSite("RenamedSite")).thenReturn(null);
    IPSSiteDataService data = mock(IPSSiteDataService.class);
    PSSiteProperties props = new PSSiteProperties();
    props.setName("NightlySite");
    when(data.getSiteProperties("NightlySite")).thenReturn(props);
    adaptor.setSiteDataService(data);
    PSSite renamed = new PSSite();
    renamed.setName("RenamedSite");
    when(siteManager.findSite("RenamedSite")).thenReturn(null, renamed);

    Site out = adaptor.renameSite("NightlySite", "RenamedSite");

    assertEquals("RenamedSite", out.getName());
    assertEquals("RenamedSite", props.getName().orElse(null));
    verify(data).updateSiteProperties(props);
    verify(siteManager, never()).saveSite(org.mockito.ArgumentMatchers.any(IPSSite.class));
  }

  @Test
  void rename_existingFolder_409() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("NightlySite");
    when(siteManager.findSite("NightlySite")).thenReturn(existing);
    when(siteManager.findSite("TakenFolder")).thenReturn(null);
    IPSSiteDataService data = mock(IPSSiteDataService.class);
    PSSiteProperties props = new PSSiteProperties();
    props.setName("NightlySite");
    when(data.getSiteProperties("NightlySite")).thenReturn(props);
    when(data.updateSiteProperties(props))
        .thenThrow(new PSDataServiceException("Cannot rename site to an existing site folder"));
    adaptor.setSiteDataService(data);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.renameSite("NightlySite", "TakenFolder"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void delete_delegatesToSiteManager() {
    PSSite existing = new PSSite();
    existing.setName("NightlySite");
    when(siteManager.findSite("NightlySite")).thenReturn(existing);

    adaptor.deleteSiteByNameOrId("NightlySite");
    verify(siteManager).deleteSite(existing);
  }

  @Test
  void delete_missing_404() {
    when(siteManager.findSite("Missing")).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.deleteSiteByNameOrId("Missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void delete_nonAdmin_403() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> denied.deleteSiteByNameOrId("NightlySite"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(siteManager, never()).deleteSite(org.mockito.ArgumentMatchers.any(IPSSite.class));
  }
}
