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
package com.percussion.sitemanage.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.percussion.fastforward.managednav.IPSNavigationErrors;
import com.percussion.fastforward.managednav.PSNavException;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.IPSDataService;
import com.percussion.sitemanage.data.PSSite;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Create Site maps invalid NavTree seed to HTTP 400 instead of 500 (#3364).
 */
class PSSiteDataRestServiceSaveNavTest {

  @Mock private PSSiteDataService siteDataService;

  private PSSiteDataRestService rest;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    rest = new PSSiteDataRestService(siteDataService);
  }

  @Test
  void saveMapsAlreadyHasNavTreeToBadRequest() throws Exception {
    PSSite site = new PSSite();
    site.setName("QaSite3364");
    PSNavException nav =
        new PSNavException(
            IPSNavigationErrors.NAVIGATION_SERVICE_NAVTREE_CANNOT_BE_ADDED_TO_FOLDER_WITH_NAVTREE);
    when(siteDataService.save(site))
        .thenThrow(new IPSDataService.DataServiceSaveException("Error saving site", nav));

    WebApplicationException thrown =
        assertThrows(WebApplicationException.class, () -> rest.save(site));
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), thrown.getResponse().getStatus());
    assertTrue(thrown.getMessage().contains("already has a NavTree"));
  }

  @Test
  void saveMapsAlreadyHasNavonToBadRequest() throws Exception {
    PSSite site = new PSSite();
    site.setName("QaSite3364");
    PSNavException nav =
        new PSNavException(
            IPSNavigationErrors.NAVIGATION_SERVICE_NAVTREE_CANNOT_BE_ADDED_TO_FOLDER_WITH_NAVON);
    when(siteDataService.save(site))
        .thenThrow(
            new IPSDataService.DataServiceSaveException(
                "Error saving site", new RuntimeException("Error creating site items", nav)));

    WebApplicationException thrown =
        assertThrows(WebApplicationException.class, () -> rest.save(site));
    assertEquals(400, thrown.getResponse().getStatus());
    assertTrue(thrown.getMessage().contains("already has a navigation item"));
  }

  @Test
  void saveKeepsUnexpectedFailuresAsInternalServerError() throws Exception {
    PSSite site = new PSSite();
    site.setName("QaSite3364");
    when(siteDataService.save(site))
        .thenThrow(new IPSDataService.DataServiceSaveException("Error saving site"));

    WebApplicationException thrown =
        assertThrows(WebApplicationException.class, () -> rest.save(site));
    assertEquals(
        Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), thrown.getResponse().getStatus());
  }

  @Test
  void saveReturnsSiteOnSuccess() throws Exception {
    PSSite site = new PSSite();
    site.setName("QaSite3364");
    when(siteDataService.save(site)).thenReturn(site);
    assertSame(site, rest.save(site));
  }

  @Test
  void detectsInvalidNavCreateThroughCauseChain() {
    PSNavException nav =
        new PSNavException(
            IPSNavigationErrors.NAVIGATION_SERVICE_NAVTREE_CANNOT_BE_ADDED_TO_FOLDER_WITH_NAVTREE);
    IPSGenericDao.SaveException save =
        new IPSGenericDao.SaveException("Error saving site", nav);
    IPSDataService.DataServiceSaveException wrapped =
        new IPSDataService.DataServiceSaveException("Error saving object", save);

    assertTrue(PSSiteDataRestService.isInvalidNavTreeCreate(wrapped));
    assertEquals(
        "Cannot add a NavTree to a folder that already has a NavTree.",
        PSSiteDataRestService.invalidNavTreeCreateMessage(wrapped));
    assertFalse(PSSiteDataRestService.isInvalidNavTreeCreate(new RuntimeException("other")));
  }
}
