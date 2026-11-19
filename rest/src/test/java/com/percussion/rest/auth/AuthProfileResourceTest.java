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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.rest.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.rest.JacksonContextResolver;
import com.percussion.services.auth.ratelimit.PSAuthRateLimitException;
import jakarta.ws.rs.WebApplicationException;
import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Tag("UnitTest")
class AuthProfileResourceTest {

  @Test
  void delegatesEmailOperationsAndMapsErrorStatuses() {
    IAuthProfileAdaptor adaptor = mock(IAuthProfileAdaptor.class);
    ProfileEmailStatus status = new ProfileEmailStatus();
    status.setUserName("editor1");
    status.setEmail("editor1@example.com");
    status.setEmailVerified(true);
    when(adaptor.getMyEmailStatus()).thenReturn(status);

    AuthProfileResource resource = new AuthProfileResource(adaptor);
    assertSame(status, resource.getMyEmailStatus());

    ProfileEmailChangeRequest req = new ProfileEmailChangeRequest("new@example.com");
    ProfileEmailActionResult actionResult =
        new ProfileEmailActionResult(
            true, false, "editor1@example.com", "new@example.com", true, "Sent");
    when(adaptor.requestMyEmailChange(req)).thenReturn(actionResult);
    assertSame(actionResult, resource.requestMyEmailChange(req));

    when(adaptor.resendMyEmailVerification()).thenReturn(actionResult);
    assertSame(actionResult, resource.resendMyEmailVerification());

    assertEquals(
        400,
        assertThrows(WebApplicationException.class, () -> resource.requestMyEmailChange(null))
            .getResponse()
            .getStatus());

    doThrow(new IllegalArgumentException("bad email")).when(adaptor).requestMyEmailChange(req);
    assertEquals(
        400,
        assertThrows(WebApplicationException.class, () -> resource.requestMyEmailChange(req))
            .getResponse()
            .getStatus());

    doThrow(new PSAuthRateLimitException()).when(adaptor).resendMyEmailVerification();
    assertEquals(
        429,
        assertThrows(WebApplicationException.class, resource::resendMyEmailVerification)
            .getResponse()
            .getStatus());

    doThrow(new IllegalStateException("smtp missing")).when(adaptor).resendMyEmailVerification();
    assertEquals(
        503,
        assertThrows(WebApplicationException.class, resource::resendMyEmailVerification)
            .getResponse()
            .getStatus());

    doReturn(actionResult).when(adaptor).resendMyEmailVerification();
    assertSame(actionResult, resource.resendMyEmailVerification());

    assertEquals(
        503,
        assertThrows(WebApplicationException.class, () -> new AuthProfileResource().getMyEmailStatus())
            .getResponse()
            .getStatus());
  }

  @Test
  void jacksonContextResolverWrapsProfileDtosWithRootName() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(ProfileEmailStatus.class);
    ObjectMapper treeMapper = JsonMapper.builder().build();

    ProfileEmailStatus status = new ProfileEmailStatus();
    status.setUserName("editor1");
    status.setEmail("old@example.com");
    status.setEmailVerified(true);
    status.setEmailVerifiedAt(Instant.parse("2026-11-18T12:00:00Z"));
    status.setPendingEmail("new@example.com");
    status.setEmailEditable(true);
    status.setEmailRequired(true);
    status.setProviderType("INTERNAL");

    String json = mapper.writeValueAsString(status);
    JsonNode root = treeMapper.readTree(json);
    assertTrue(root.has("ProfileEmailStatus"));
    JsonNode inner = root.get("ProfileEmailStatus");
    assertEquals("editor1", inner.get("userName").asString());
    assertEquals("new@example.com", inner.get("pendingEmail").asString());
    assertTrue(inner.get("emailVerified").asBoolean());

    ProfileEmailChangeRequest decoded =
        mapper.readValue(
            "{\"ProfileEmailChangeRequest\":{\"email\":\"next@example.com\"}}",
            ProfileEmailChangeRequest.class);
    assertEquals("next@example.com", decoded.getEmail());

    String actionJson =
        mapper.writeValueAsString(
            new ProfileEmailActionResult(true, false, "old@example.com", "next@example.com", true, "Sent"));
    JsonNode actionRoot = treeMapper.readTree(actionJson);
    assertTrue(actionRoot.has("ProfileEmailActionResult"));
    assertFalse(actionRoot.get("ProfileEmailActionResult").get("appliedImmediately").asBoolean());
  }
}
