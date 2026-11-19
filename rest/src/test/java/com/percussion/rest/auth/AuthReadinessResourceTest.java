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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.rest.JacksonContextResolver;
import com.percussion.services.auth.mfa.PSStepUpRequiredException;
import jakarta.ws.rs.WebApplicationException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Tag("UnitTest")
class AuthReadinessResourceTest {

  @Test
  void getReadinessDelegatesAndReturnsReport() {
    IAuthReadinessAdaptor adaptor = mock(IAuthReadinessAdaptor.class);
    AuthReadinessReport expected = new AuthReadinessReport();
    expected.setMfaMode("optional");
    expected.setOverallStatus("PASS");
    when(adaptor.getReadinessReport()).thenReturn(expected);

    AuthReadinessResource resource = new AuthReadinessResource(adaptor);
    assertSame(expected, resource.getReadinessReport());
  }

  @Test
  void mapsForbiddenAndMissingAdaptorOnReadiness() {
    IAuthReadinessAdaptor forbidden = mock(IAuthReadinessAdaptor.class);
    when(forbidden.getReadinessReport()).thenThrow(new SecurityException("Admin role required"));
    AuthReadinessResource resForbidden = new AuthReadinessResource(forbidden);
    WebApplicationException ex403 =
        assertThrows(WebApplicationException.class, resForbidden::getReadinessReport);
    assertEquals(403, ex403.getResponse().getStatus());

    IAuthReadinessAdaptor failing = mock(IAuthReadinessAdaptor.class);
    when(failing.getReadinessReport()).thenThrow(new RuntimeException("boom"));
    WebApplicationException ex500 =
        assertThrows(
            WebApplicationException.class,
            () -> new AuthReadinessResource(failing).getReadinessReport());
    assertEquals(500, ex500.getResponse().getStatus());

    WebApplicationException ex503 =
        assertThrows(
            WebApplicationException.class, () -> new AuthReadinessResource().getReadinessReport());
    assertEquals(503, ex503.getResponse().getStatus());
  }

  @Test
  void sendTestEmailValidatesAndMapsStatuses() {
    IAuthReadinessAdaptor adaptor = mock(IAuthReadinessAdaptor.class);
    AuthMailTestRequest req = new AuthMailTestRequest("admin@example.com");
    AuthMailTestResult ok = new AuthMailTestResult(true, "admin@example.com", "SERVER_PROPERTIES", "OK");
    when(adaptor.sendTestEmail(req)).thenReturn(ok);

    AuthReadinessResource resource = new AuthReadinessResource(adaptor);
    assertSame(ok, resource.sendTestEmail(req));

    WebApplicationException exNull =
        assertThrows(WebApplicationException.class, () -> resource.sendTestEmail(null));
    assertEquals(400, exNull.getResponse().getStatus());

    doThrow(new IllegalArgumentException("bad recipient")).when(adaptor).sendTestEmail(req);
    WebApplicationException ex400 =
        assertThrows(WebApplicationException.class, () -> resource.sendTestEmail(req));
    assertEquals(400, ex400.getResponse().getStatus());

    doThrow(new SecurityException("Admin role required")).when(adaptor).sendTestEmail(req);
    WebApplicationException ex403 =
        assertThrows(WebApplicationException.class, () -> resource.sendTestEmail(req));
    assertEquals(403, ex403.getResponse().getStatus());

    doThrow(new RuntimeException("unexpected")).when(adaptor).sendTestEmail(req);
    WebApplicationException ex500 =
        assertThrows(WebApplicationException.class, () -> resource.sendTestEmail(req));
    assertEquals(500, ex500.getResponse().getStatus());

    WebApplicationException ex503 =
        assertThrows(
            WebApplicationException.class, () -> new AuthReadinessResource().sendTestEmail(req));
    assertEquals(503, ex503.getResponse().getStatus());
  }

  @Test
  void jacksonContextResolverUsesWrapRootValueAndArrayShape() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(AuthReadinessReport.class);
    ObjectMapper treeMapper = JsonMapper.builder().build();

    AuthReadinessReport report = new AuthReadinessReport();
    report.setGeneratedAt(Instant.parse("2026-11-19T00:00:00Z"));
    report.setMfaMode("optional");
    report.setPasswordMinLength(12);
    report.setMailConfigured(true);
    report.setMailSource("SERVER_PROPERTIES");
    report.setOverallStatus("PASS");
    report.setInfraChecks(
        List.of(new AuthInfraCheck("AUTH_MAIL", "Auth Mail", "PASS", "Configured", "")));

    AuthAccountReadinessItem item = new AuthAccountReadinessItem();
    item.setUserName("editor1");
    item.setProviderType("INTERNAL");
    item.setIssues(List.of("MISSING_EMAIL"));
    report.setAccountIssues(List.of(item));

    String json = mapper.writeValueAsString(report);
    JsonNode root = treeMapper.readTree(json);
    assertTrue(root.has("AuthReadinessReport"));
    JsonNode inner = root.get("AuthReadinessReport");
    assertTrue(inner.get("infraChecks").isArray());
    assertTrue(inner.get("accountIssues").isArray());

    AuthMailTestRequest decoded =
        mapper.readValue(
            "{\"AuthMailTestRequest\":{\"recipient\":\"admin@example.com\"}}",
            AuthMailTestRequest.class);
    assertEquals("admin@example.com", decoded.getRecipient());

    String resultJson =
        mapper.writeValueAsString(
            new AuthMailTestResult(true, "admin@example.com", "SERVER_PROPERTIES", "Sent"));
    assertTrue(treeMapper.readTree(resultJson).has("AuthMailTestResult"));
  }

  @Test
  void initiateAdminRecoveryDelegatesAndMapsHttpStatuses() {
    IAuthReadinessAdaptor adaptor = mock(IAuthReadinessAdaptor.class);
    AdminRecoveryRequest req =
        new AdminRecoveryRequest("editor1", "Verified via video call #101", false);
    AdminRecoveryResponse ok =
        new AdminRecoveryResponse(
            true,
            "editor1",
            "RECOVERY_LINK",
            "EMAIL",
            "2026-11-19T03:00:00Z",
            "Sent");
    when(adaptor.initiateAdminRecovery(req)).thenReturn(ok);

    AuthReadinessResource resource = new AuthReadinessResource(adaptor);
    assertSame(ok, resource.initiateAdminRecovery(req));

    WebApplicationException exNull =
        assertThrows(WebApplicationException.class, () -> resource.initiateAdminRecovery(null));
    assertEquals(400, exNull.getResponse().getStatus());

    doThrow(new PSStepUpRequiredException("Step-up required"))
        .when(adaptor)
        .initiateAdminRecovery(req);
    WebApplicationException ex428 =
        assertThrows(WebApplicationException.class, () -> resource.initiateAdminRecovery(req));
    assertEquals(428, ex428.getResponse().getStatus());

    doThrow(new SecurityException("Admin role required")).when(adaptor).initiateAdminRecovery(req);
    WebApplicationException ex403 =
        assertThrows(WebApplicationException.class, () -> resource.initiateAdminRecovery(req));
    assertEquals(403, ex403.getResponse().getStatus());

    doThrow(new IllegalStateException("No verified email")).when(adaptor).initiateAdminRecovery(req);
    WebApplicationException ex409 =
        assertThrows(WebApplicationException.class, () -> resource.initiateAdminRecovery(req));
    assertEquals(409, ex409.getResponse().getStatus());
  }
}
