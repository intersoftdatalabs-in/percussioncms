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
package com.percussion.sitemanage.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.codes.WebserviceErrorCodes;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.sitemanage.service.IPSSiteSectionMetaDataService;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.publishing.IPSPublishingWs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * {@link PSSitePublishDao#findSummary(String)} treats typed {@link
 * WebserviceErrorCodes#OBJECT_NOT_FOUND_BY_NAME} as missing (issue #3846).
 */
@Tag("UnitTest")
class PSSitePublishDaoFindSummaryTypedErrorCodeTest {

  private IPSPublishingWs publishWs;
  private PSSitePublishDao dao;

  @BeforeEach
  void setUp() {
    publishWs = mock(IPSPublishingWs.class);
    dao =
        new PSSitePublishDao(
            mock(IPSIdMapper.class),
            publishWs,
            mock(IPSPublisherService.class),
            mock(IPSSiteSectionMetaDataService.class),
            mock(IPSSiteManager.class));
  }

  @Test
  void findSummaryReturnsNullForTypedObjectNotFoundByName() throws Exception {
    PSErrorException missing =
        new PSErrorException(
            WebserviceErrorCodes.OBJECT_NOT_FOUND_BY_NAME, "site gone", "stack");
    when(publishWs.findSite("MissingSite")).thenThrow(missing);

    assertNull(dao.findSummary("MissingSite"));
    assertSame(WebserviceErrorCodes.OBJECT_NOT_FOUND_BY_NAME, missing.getTypedErrorCode());
    assertEquals(
        WebserviceErrorCodes.OBJECT_NOT_FOUND_BY_NAME.numericCode(), missing.getCode());
    assertFalse(missing.isAuditable());
  }

  @Test
  void findSummaryWrapsOtherWebserviceErrors() {
    PSErrorException other =
        new PSErrorException(WebserviceErrorCodes.INVALID_CONTRACT, "bad contract", "stack");
    when(publishWs.findSite("BrokenSite")).thenThrow(other);

    IPSGenericDao.LoadException thrown =
        assertThrows(IPSGenericDao.LoadException.class, () -> dao.findSummary("BrokenSite"));
    assertSame(other, thrown.getCause());
  }
}
