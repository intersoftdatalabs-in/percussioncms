/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.pathmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.service.IPSPathService.PSPathNotFoundServiceException;
import com.percussion.pathmanagement.service.impl.PSSitePathItemService;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import com.percussion.share.service.IPSDataService.DataServiceNotFoundException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.service.IPSSiteDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Regression for GH-879 / v8.1.7 PR #886: orphaned site-path pages surface a short, friendly
 * message instead of internal "contact Customer Success" wording.
 */
class PSSitePathItemServiceOrphanedPageMessageTest {

  private static final String FRIENDLY_MSG =
      "Oops. We're sorry. The requested page is no longer available.";

  @Mock private IPSSiteDataService siteDataService;

  private TestableSitePathItemService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new TestableSitePathItemService(siteDataService);
  }

  @Test
  void orphanedPagePathUsesFriendlyMessage() throws Exception {
    when(siteDataService.find(anyString())).thenThrow(new DataServiceLoadException("site missing"));
    when(siteDataService.findByPath(anyString()))
        .thenThrow(new DataServiceNotFoundException("path missing"));

    var ex =
        assertThrows(
            PSPathNotFoundServiceException.class, () -> service.exposeFindItem("/site1/b/c/"));
    assertEquals(FRIENDLY_MSG, ex.getMessage());
    assertTrue(
        !ex.getMessage().contains("Customer Success"),
        "must not expose internal support escalation wording");
  }

  /**
   * Exposes protected {@link PSSitePathItemService#findItem(String)} for unit testing without a
   * full Spring context.
   */
  static final class TestableSitePathItemService extends PSSitePathItemService {
    TestableSitePathItemService(IPSSiteDataService siteDataService) {
      super(siteDataService, null, null, null, null, null, null, null, null, null, null, null);
    }

    PSPathItem exposeFindItem(String path)
        throws PSPathNotFoundServiceException,
            DataServiceNotFoundException,
            PSValidationException,
            DataServiceLoadException {
      return findItem(path);
    }
  }
}

