/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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
package com.percussion.pubserver.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.percussion.pubserver.data.PSPublishServerInfo;
import com.percussion.rx.publisher.IPSRxPublisherService;
import com.percussion.security.PSEncryptor;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentchange.IPSContentChangeService;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.pubserver.IPSDatabasePubServerFilesService;
import com.percussion.services.pubserver.IPSPubServerDao;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.dao.IPSSitePublishDao;
import com.percussion.sitemanage.service.IPSSiteDataService;
import com.percussion.sitemanage.service.IPSSitePublishStatusService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.io.PathUtils;
import com.percussion.utils.service.IPSUtilityService;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Default-flag-only PUT on {@link PSPubServerService#updatePubServer} stores the site default id
 * and does not demote the previous server's publish type.
 */
class PSPubServerServiceDefaultOnlyTest {

  @Test
  void defaultOnlyPutDoesNotDemotePreviousPublishType() throws Exception {
    IPSPubServerDao pubServerDao = mock(IPSPubServerDao.class);
    IPSSiteManager siteMgr = mock(IPSSiteManager.class);
    IPSSiteDataService siteDataService = mock(IPSSiteDataService.class);
    IPSGuidManager guidMgr = mock(IPSGuidManager.class);
    IPSContentChangeService contentChangeService = mock(IPSContentChangeService.class);

    IPSGuid siteGuid = mock(IPSGuid.class);
    IPSGuid serverGuid = mock(IPSGuid.class);
    when(guidMgr.makeGuid("101", PSTypeEnum.SITE)).thenReturn(siteGuid);
    when(guidMgr.makeGuid("9", PSTypeEnum.PUBLISHING_SERVER)).thenReturn(serverGuid);

    PSPubServer previous = namedServer(3L, "prod", "filesystem");
    PSPubServer selected = namedServer(9L, "stage", "filesystem_only");
    when(pubServerDao.loadPubServerModifiable(serverGuid)).thenReturn(selected);

    AtomicReference<Long> defaultId = new AtomicReference<>(3L);
    IPSSite site = mock(IPSSite.class);
    when(site.getGUID()).thenReturn(siteGuid);
    when(site.getName()).thenReturn("Corporate_Investments");
    when(site.isPageBased()).thenReturn(false);
    when(site.getDefaultPubServer()).thenAnswer(inv -> defaultId.get());
    org.mockito.Mockito.doAnswer(
            inv -> {
              defaultId.set(inv.getArgument(0));
              return null;
            })
        .when(site)
        .setDefaultPubServer(any());
    when(siteMgr.findSite(siteGuid)).thenReturn(site);
    when(siteMgr.loadSiteModifiable(siteGuid)).thenReturn(site);

    PSPubServerService service =
        spyService(
            pubServerDao,
            siteMgr,
            siteDataService,
            guidMgr,
            contentChangeService);
    doReturn(previous).when(service).getDefaultPubServer(siteGuid);

    PSPublishServerInfo body = new PSPublishServerInfo();
    body.setIsDefault(true);

    PSPublishServerInfo result = service.updatePubServer("101", "9", body);

    assertTrue(Boolean.TRUE.equals(result.getIsDefault()));
    assertEquals(9L, defaultId.get());
    assertEquals("filesystem", previous.getPublishType());
    assertEquals("filesystem_only", selected.getPublishType());
    verify(siteMgr).saveSite(site);
    verify(pubServerDao, never()).savePubServer(any());
    verify(siteDataService, never())
        .updateServerEditions(any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
    verify(siteDataService, never()).setPublishServerAsDefault(any(), any());
    verifyNoInteractions(contentChangeService);
  }

  @Test
  void alreadyDefaultIsNotAWrite() throws Exception {
    IPSPubServerDao pubServerDao = mock(IPSPubServerDao.class);
    IPSSiteManager siteMgr = mock(IPSSiteManager.class);
    IPSSiteDataService siteDataService = mock(IPSSiteDataService.class);
    IPSGuidManager guidMgr = mock(IPSGuidManager.class);

    IPSGuid siteGuid = mock(IPSGuid.class);
    IPSGuid serverGuid = mock(IPSGuid.class);
    when(guidMgr.makeGuid("101", PSTypeEnum.SITE)).thenReturn(siteGuid);
    when(guidMgr.makeGuid("3", PSTypeEnum.PUBLISHING_SERVER)).thenReturn(serverGuid);

    PSPubServer current = namedServer(3L, "prod", "filesystem");
    when(pubServerDao.loadPubServerModifiable(serverGuid)).thenReturn(current);

    IPSSite site = mock(IPSSite.class);
    when(site.getGUID()).thenReturn(siteGuid);
    when(site.getName()).thenReturn("Corporate_Investments");
    when(site.isPageBased()).thenReturn(false);
    when(site.getDefaultPubServer()).thenReturn(3L);
    when(siteMgr.findSite(siteGuid)).thenReturn(site);

    PSPubServerService service =
        spyService(pubServerDao, siteMgr, siteDataService, guidMgr, mock(IPSContentChangeService.class));
    doReturn(current).when(service).getDefaultPubServer(siteGuid);

    PSPublishServerInfo body = new PSPublishServerInfo();
    body.setIsDefault(true);
    service.updatePubServer("101", "3", body);

    verify(siteMgr, never()).saveSite(any());
    verify(pubServerDao, never()).savePubServer(any());
    assertEquals("filesystem", current.getPublishType());
  }

  @Test
  void falseFlagThrowsBeforeWrite() throws Exception {
    IPSPubServerDao pubServerDao = mock(IPSPubServerDao.class);
    IPSSiteManager siteMgr = mock(IPSSiteManager.class);
    IPSGuidManager guidMgr = mock(IPSGuidManager.class);
    IPSGuid siteGuid = mock(IPSGuid.class);
    IPSGuid serverGuid = mock(IPSGuid.class);
    when(guidMgr.makeGuid("101", PSTypeEnum.SITE)).thenReturn(siteGuid);
    when(guidMgr.makeGuid("9", PSTypeEnum.PUBLISHING_SERVER)).thenReturn(serverGuid);

    PSPubServer selected = namedServer(9L, "stage", "filesystem_only");
    when(pubServerDao.loadPubServerModifiable(serverGuid)).thenReturn(selected);
    IPSSite site = mock(IPSSite.class);
    when(site.getGUID()).thenReturn(siteGuid);
    when(siteMgr.findSite(siteGuid)).thenReturn(site);

    PSPubServerService service =
        spyService(
            pubServerDao,
            siteMgr,
            mock(IPSSiteDataService.class),
            guidMgr,
            mock(IPSContentChangeService.class));
    doReturn(namedServer(3L, "prod", "filesystem")).when(service).getDefaultPubServer(siteGuid);

    PSPublishServerInfo body = new PSPublishServerInfo();
    body.setIsDefault(false);
    assertThrows(
        PSValidationException.class, () -> service.updatePubServer("101", "9", body));
    verify(siteMgr, never()).saveSite(any());
    verify(pubServerDao, never()).savePubServer(any());
  }

  private static PSPubServer namedServer(long id, String name, String publishType) {
    PSPubServer server = new PSPubServer();
    server.setServerId(id);
    server.setName(name);
    server.setPublishType(publishType);
    server.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "Local");
    return server;
  }

  private static PSPubServerService spyService(
      IPSPubServerDao pubServerDao,
      IPSSiteManager siteMgr,
      IPSSiteDataService siteDataService,
      IPSGuidManager guidMgr,
      IPSContentChangeService contentChangeService)
      throws Exception {
    File rxDir = Path.of("tmp", "perc-default-only-rx").toFile();
    try (MockedStatic<PathUtils> paths = mockStatic(PathUtils.class);
        MockedStatic<PSEncryptor> encryptor = mockStatic(PSEncryptor.class)) {
      paths.when(() -> PathUtils.getRxDir(null)).thenReturn(rxDir);
      PSEncryptor instance = mock(PSEncryptor.class);
      encryptor.when(() -> PSEncryptor.getInstance(eq("AES"), anyString())).thenReturn(instance);
      return spy(
          new PSPubServerService(
              pubServerDao,
              siteMgr,
              mock(IPSDatabasePubServerFilesService.class),
              siteDataService,
              mock(IPSSitePublishStatusService.class),
              guidMgr,
              mock(IPSRxPublisherService.class),
              mock(IPSSitePublishDao.class),
              mock(IPSPublisherService.class),
              contentChangeService,
              mock(IPSUtilityService.class)));
    }
  }
}
