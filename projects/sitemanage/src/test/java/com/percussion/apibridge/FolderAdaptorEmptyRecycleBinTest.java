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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.recycle.data.PSEmptyRecycleResult;
import com.percussion.recycle.service.IPSEmptyRecycleService;
import com.percussion.recycle.service.IPSEmptyRecycleService.PSEmptyRecycleNotAuthorizedException;
import com.percussion.recycle.service.IPSEmptyRecycleService.PSEmptyRecycleNotFoundException;
import com.percussion.rest.errors.FolderNotFoundException;
import com.percussion.rest.errors.NotAuthorizedException;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.sitemanage.service.IPSSiteDataService;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import jakarta.ws.rs.WebApplicationException;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Empty the Explorer recycle bin via {@code POST /rest/folders/recycle/empty} (#4762). */
@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class FolderAdaptorEmptyRecycleBinTest {

  @Mock private IPSPathService pathService;
  @Mock private IPSFolderHelper folderHelper;
  @Mock private IPSUserService userService;
  @Mock private IPSSiteDataService siteDataService;
  @Mock private IPSEmptyRecycleService emptyRecycleService;

  private FolderAdaptor adaptor;
  private final URI base = URI.create("http://localhost/rest");

  @BeforeEach
  void setUp() throws Exception {
    adaptor =
        new FolderAdaptor(
            pathService,
            folderHelper,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            userService,
            null,
            siteDataService,
            null);
    adaptor.setEmptyRecycleService(emptyRecycleService);
    PSCurrentUser admin = new PSCurrentUser();
    admin.setName("admin1");
    when(userService.getCurrentUser()).thenReturn(admin);
    when(userService.isAdminUser("admin1")).thenReturn(true);
  }

  @Test
  void emptyRecycleBinSucceedsWhenServiceReportsClean() throws Exception {
    PSEmptyRecycleResult result = new PSEmptyRecycleResult();
    result.setPurgedItemCount(2);
    when(emptyRecycleService.emptyRecyclingBin()).thenReturn(result);

    assertDoesNotThrow(() -> adaptor.emptyRecycleBin(base));
    verify(emptyRecycleService).emptyRecyclingBin();
  }

  @Test
  void emptyRecycleBinMapsUndeletedToConflict() throws Exception {
    PSEmptyRecycleResult result = new PSEmptyRecycleResult();
    result.setUndeletedCount(1);
    result.setErrors(List.of("locked"));
    when(emptyRecycleService.emptyRecyclingBin()).thenReturn(result);

    WebApplicationException thrown =
        assertThrows(WebApplicationException.class, () -> adaptor.emptyRecycleBin(base));
    assertEquals(409, thrown.getResponse().getStatus());
  }

  @Test
  void emptyRecycleBinMapsNotAuthorized() throws Exception {
    when(emptyRecycleService.emptyRecyclingBin())
        .thenThrow(new PSEmptyRecycleNotAuthorizedException("no"));

    assertThrows(NotAuthorizedException.class, () -> adaptor.emptyRecycleBin(base));
  }

  @Test
  void emptyRecycleBinMapsMissingRoot() throws Exception {
    when(emptyRecycleService.emptyRecyclingBin())
        .thenThrow(new PSEmptyRecycleNotFoundException("missing", new IllegalStateException("x")));

    assertThrows(FolderNotFoundException.class, () -> adaptor.emptyRecycleBin(base));
  }
}
