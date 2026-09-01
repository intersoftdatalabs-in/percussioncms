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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.Guid;
import com.percussion.rest.GuidList;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.security.IPSSecurityDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class CommunityAdaptorCreateDeleteTest {

  private IPSSecurityDesignWs securityDesignWs;
  private CommunityAdaptor adaptor;

  @BeforeEach
  void setUp() throws Exception {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
    securityDesignWs = mock(IPSSecurityDesignWs.class);
    adaptor = new CommunityAdaptor();
    Field field = CommunityAdaptor.class.getDeclaredField("securityDesignWs");
    field.setAccessible(true);
    field.set(adaptor, securityDesignWs);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfo.resetRequestInfo();
  }

  @Test
  void duplicateNameIs409AndDoesNotSave() {
    when(securityDesignWs.createCommunities(anyList(), eq("test-session"), eq("Admin")))
        .thenThrow(
            new IllegalArgumentException(
                "The name 'Default' for type 'COMMUNITY_DEF' already exists."));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.createCommunities(List.of("Default")));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("already exists"));
    verify(securityDesignWs, never()).saveCommunities(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void blankNameStaysIllegalArgument() {
    when(securityDesignWs.createCommunities(anyList(), eq("test-session"), eq("Admin")))
        .thenThrow(new IllegalArgumentException("name cannot be null or empty"));
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createCommunities(List.of(" ")));
    assertTrue(ex.getMessage().toLowerCase().contains("empty"));
  }

  @Test
  void deleteInUseWithoutIgnoreIs409() {
    doThrow(new PSErrorsException())
        .when(securityDesignWs)
        .deleteCommunities(anyList(), eq(false), eq("test-session"), eq("Admin"));
    GuidList ids = new GuidList();
    Guid g = new Guid();
    g.setStringValue("0-13-10");
    ids.add(g);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteCommunities(ids, false));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void isAlreadyExistsFailureReadsMessage() {
    assertTrue(
        CommunityAdaptor.isAlreadyExistsFailure(
            new IllegalArgumentException("The name 'QA' for type 'COMMUNITY_DEF' already exists.")));
    assertFalse(
        CommunityAdaptor.isAlreadyExistsFailure(
            new IllegalArgumentException("name cannot be null or empty")));
    assertFalse(
        CommunityAdaptor.isAlreadyExistsFailure(
            new RuntimeException("The name 'QA' for type 'COMMUNITY_DEF' already exists.")));
    assertFalse(
        CommunityAdaptor.isAlreadyExistsFailure(
            new RuntimeException(
                "Save failed: already exists",
                new IllegalArgumentException(
                    "The name 'QA' for type 'COMMUNITY_DEF' already exists."))));
    assertFalse(
        CommunityAdaptor.isAlreadyExistsFailure(
            new IllegalArgumentException(
                "Save failed: already exists then continued with another error")));
  }
}
