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
package com.percussion.pubserver.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.percussion.pubserver.IPSPubServerService;
import com.percussion.pubserver.data.PSPublishServerInfo;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.regions.Region;

/**
 * Unit tests for the v2 migration of {@link PSPubServerRestService#getAvailableRegions()}. Verifies
 * the REST endpoint returns region IDs from v2's {@link RegionMetadata#regions()} (default
 * partition) instead of v1's {@code Regions.values()}.
 */
@ExtendWith(MockitoExtension.class)
public class PSPubServerRestServiceTest {

  private PSPubServerRestService service;
  private MockedStatic<Region> regionStatic;

  @BeforeEach
  public void setUp() {
    service = new PSPubServerRestService(null);
  }

  @AfterEach
  public void tearDown() {
    if (regionStatic != null) regionStatic.close();
  }

  @Test
  public void getAvailableRegions_returnsJsonArrayOfRegionIds()
      throws tools.jackson.core.JacksonException {
    List<Region> mockRegions = List.of(Region.US_EAST_1, Region.US_WEST_2, Region.EU_WEST_1);
    regionStatic = mockStatic(Region.class);
    regionStatic.when(Region::regions).thenReturn(mockRegions);

    String json = service.getAvailableRegions();
    assertNotNull(json);
    var arr = tools.jackson.databind.json.JsonMapper.builder().build().readTree(json);
    assertEquals(3, arr.size());
    assertEquals("us-east-1", arr.get(0).asString());
    assertEquals("us-west-2", arr.get(1).asString());
    assertEquals("eu-west-1", arr.get(2).asString());
  }

  @Test
  public void getAvailableRegions_emptyRegions_returnsNull()
      throws tools.jackson.core.JacksonException {
    regionStatic = mockStatic(Region.class);
    regionStatic.when(Region::regions).thenReturn(List.of());

    String json = service.getAvailableRegions();
    assertNull(json);
  }

  @Test
  public void createPubServer_mapsDuplicateNameTo409() throws Exception {
    IPSPubServerService svc = mock(IPSPubServerService.class);
    PSValidationException ve = mock(PSValidationException.class);
    when(ve.getMessage())
        .thenReturn("Cannot create server 'Dup' because a server named 'Dup' already exists.");
    when(svc.createPubServer(eq("1"), eq("Dup"), any())).thenThrow(ve);
    PSPubServerRestService rest = new PSPubServerRestService(svc);
    rest.setUserService(adminUsers());
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> rest.createPubServer("1", "Dup", mock(PSPublishServerInfo.class)));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void createPubServer_forbiddenWhenNotAdminOrDesigner() throws Exception {
    IPSPubServerService svc = mock(IPSPubServerService.class);
    PSPubServerRestService rest = new PSPubServerRestService(svc);
    IPSUserService users = mock(IPSUserService.class);
    PSCurrentUser editor = mock(PSCurrentUser.class);
    when(editor.getName()).thenReturn("Editor");
    when(users.getCurrentUser()).thenReturn(editor);
    when(users.isAdminUser("Editor")).thenReturn(false);
    when(users.isDesignUser("Editor")).thenReturn(false);
    rest.setUserService(users);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> rest.createPubServer("1", "Night", mock(PSPublishServerInfo.class)));
    assertEquals(403, ex.getResponse().getStatus());
  }

  private static IPSUserService adminUsers() throws Exception {
    IPSUserService users = mock(IPSUserService.class);
    PSCurrentUser admin = mock(PSCurrentUser.class);
    when(admin.getName()).thenReturn("Admin");
    when(users.getCurrentUser()).thenReturn(admin);
    when(users.isAdminUser("Admin")).thenReturn(true);
    return users;
  }
}
