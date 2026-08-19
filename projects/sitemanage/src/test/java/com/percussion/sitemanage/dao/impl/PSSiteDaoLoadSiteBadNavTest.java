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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.codes.SiteManageErrorCodes;
import com.percussion.error.PSException;
import com.percussion.fastforward.managednav.IPSNavigationErrors;
import com.percussion.fastforward.managednav.PSNavException;
import com.percussion.sitemanage.dao.IPSSiteContentDao;
import com.percussion.sitemanage.dao.IPSSitePublishDao;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteSummary;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * {@link PSSiteDao#loadSite(String)} deletes a site whose nav folder is missing, using typed {@link
 * SiteManageErrorCodes} (non-auditable). Avoids cactus / publishing locator.
 */
class PSSiteDaoLoadSiteBadNavTest {

  @Mock private IPSSiteContentDao siteContentDao;
  @Mock private IPSSitePublishDao sitePublishDao;

  private RecordingSiteDao dao;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    dao = new RecordingSiteDao(siteContentDao, sitePublishDao);
  }

  @Test
  void loadSiteDeletesWhenNavFolderMissing() throws Exception {
    PSSiteSummary sum = summary("BrokenSite");
    when(sitePublishDao.findSummary("BrokenSite")).thenReturn(sum);
    when(siteContentDao.getNavTitle(sum))
        .thenThrow(
            new PSNavException(
                IPSNavigationErrors.NAVIGATION_SERVICE_FOLDER_ID_NOT_FOUND_FOR_PATH));

    PSSite result = dao.loadSite("BrokenSite");

    assertNull(result);
    // PSSiteSummary.getId() is the site name (legacy identity used by delete()).
    assertEquals(List.of("BrokenSite"), dao.deletedIds);
  }

  @Test
  void loadSiteRethrowsOtherNavErrorsWithoutDelete() throws Exception {
    PSSiteSummary sum = summary("OtherNav");
    when(sitePublishDao.findSummary("OtherNav")).thenReturn(sum);
    when(siteContentDao.getNavTitle(sum))
        .thenThrow(
            new PSNavException(
                IPSNavigationErrors.NAVIGATION_SERVICE_NAVTREE_CANNOT_BE_ADDED_TO_FOLDER_WITH_NAVTREE));

    PSNavException thrown = assertThrows(PSNavException.class, () -> dao.loadSite("OtherNav"));
    assertEquals(
        IPSNavigationErrors.NAVIGATION_SERVICE_NAVTREE_CANNOT_BE_ADDED_TO_FOLDER_WITH_NAVTREE,
        thrown.getErrorCode());
    assertTrue(dao.deletedIds.isEmpty());
  }

  @Test
  void typedSiteManageExceptionIsNonAuditable() {
    PSException ex =
        new PSException(
            SiteManageErrorCodes.SITE_MANAGE_SERVICE_DELETING_BAD_SITE_RECORD, "BrokenSite");
    assertSame(
        SiteManageErrorCodes.SITE_MANAGE_SERVICE_DELETING_BAD_SITE_RECORD, ex.getTypedErrorCode());
    assertEquals(18252, ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  private static PSSiteSummary summary(String name) {
    PSSiteSummary sum = new PSSiteSummary();
    sum.setName(name);
    return sum;
  }

  static final class RecordingSiteDao extends PSSiteDao {
    final List<String> deletedIds = new ArrayList<>();

    RecordingSiteDao(IPSSiteContentDao content, IPSSitePublishDao publish) {
      super(content, publish);
    }

    @Override
    public void delete(String id) {
      deletedIds.add(id);
    }
  }
}
