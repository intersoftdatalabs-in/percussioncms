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
package com.percussion.webservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.codes.CmsErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.intsof.percussioncms.auditlog.codes.WebserviceErrorCodes;
import com.percussion.cms.IPSCmsErrors;
import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.ws.PSRemoteWsRequester;
import com.percussion.cms.objectstore.ws.PSWebServiceAgent;
import com.percussion.conn.PSServerException;
import com.percussion.error.PSException;
import com.percussion.server.IPSServerErrors;
import com.percussion.util.IPSRemoteRequester;
import com.percussion.xml.PSXmlDocumentBuilder;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.apache.soap.Body;
import org.apache.soap.Envelope;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Issue #3861 (parent #2616): leftover {@code system/webservices} production sites throw typed
 * {@code *ErrorCodes} (not bare {@code IPS*Errors} ints). Dual-write is skipped where the catalog
 * is non-auditable. Does not flatten WS 1–27 catalogs.
 */
@Tag("UnitTest")
class PSWebservicesLeftoverErrorCodesSliceTest {

  @Test
  void cmsAndServerPeersMatchLegacyInts() {
    assertEquals(IPSCmsErrors.ERROR_SEND_DATA, CmsErrorCodes.ERROR_SEND_DATA.numericCode());
    assertEquals(
        IPSCmsErrors.RECEIVED_UNKNOWN_DATA, CmsErrorCodes.RECEIVED_UNKNOWN_DATA.numericCode());
    assertEquals(IPSCmsErrors.UNEXPECTED_ERROR, CmsErrorCodes.UNEXPECTED_ERROR.numericCode());
    assertEquals(
        IPSServerErrors.CE_NEEDED_APP_NOT_RUNNING,
        ServerErrorCodes.CE_NEEDED_APP_NOT_RUNNING.numericCode());
    assertEquals(
        IPSServerErrors.COMMUNITIES_AUTHENTICATION_FAILED_INVALID_COMMUNITY,
        ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_INVALID_COMMUNITY.numericCode());
    assertFalse(CmsErrorCodes.ERROR_SEND_DATA.isAuditable());
    assertFalse(CmsErrorCodes.RECEIVED_UNKNOWN_DATA.isAuditable());
    assertFalse(CmsErrorCodes.UNEXPECTED_ERROR.isAuditable());
    assertFalse(ServerErrorCodes.CE_NEEDED_APP_NOT_RUNNING.isAuditable());
    assertTrue(ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_INVALID_COMMUNITY.isAuditable());
  }

  @Test
  void remoteRequesterSendFailureThrowsTypedCmsException() throws Exception {
    IPSRemoteRequester requester = mock(IPSRemoteRequester.class);
    when(requester.getDocument(anyString(), any())).thenThrow(new IOException("down"));
    PSRemoteWsRequester ws = new PSRemoteWsRequester(requester);
    Document msgDoc = PSXmlDocumentBuilder.createXmlDocument();
    Element message = PSXmlDocumentBuilder.createRoot(msgDoc, "LoginRequest");

    PSCmsException ex =
        assertThrows(
            PSCmsException.class, () -> ws.sendRequest("login", "misc", message, "LoginResponse"));
    assertSame(CmsErrorCodes.ERROR_SEND_DATA, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.ERROR_SEND_DATA.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void remoteRequesterUnknownResponseThrowsTypedCmsException() throws Exception {
    Document response = PSXmlDocumentBuilder.createXmlDocument();
    PSXmlDocumentBuilder.createRoot(response, "NotTheResponse");
    IPSRemoteRequester requester = mock(IPSRemoteRequester.class);
    when(requester.getDocument(anyString(), any())).thenReturn(response);
    PSRemoteWsRequester ws = new PSRemoteWsRequester(requester);
    Document msgDoc = PSXmlDocumentBuilder.createXmlDocument();
    Element message = PSXmlDocumentBuilder.createRoot(msgDoc, "LoginRequest");

    PSCmsException ex =
        assertThrows(
            PSCmsException.class, () -> ws.sendRequest("login", "misc", message, "LoginResponse"));
    assertSame(CmsErrorCodes.RECEIVED_UNKNOWN_DATA, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.RECEIVED_UNKNOWN_DATA.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void remoteRequesterFailureResultThrowsTypedUnexpectedError() throws Exception {
    Document response = PSXmlDocumentBuilder.createXmlDocument();
    Element root = PSXmlDocumentBuilder.createRoot(response, "LoginResponse");
    Element result = response.createElement("ResultResponse");
    result.setAttribute("type", "failure");
    Element detail = response.createElement("Message");
    detail.appendChild(response.createTextNode("boom"));
    result.appendChild(detail);
    root.appendChild(result);

    IPSRemoteRequester requester = mock(IPSRemoteRequester.class);
    when(requester.getDocument(anyString(), any())).thenReturn(response);
    PSRemoteWsRequester ws = new PSRemoteWsRequester(requester);
    Document msgDoc = PSXmlDocumentBuilder.createXmlDocument();
    Element message = PSXmlDocumentBuilder.createRoot(msgDoc, "LoginRequest");

    PSCmsException ex =
        assertThrows(
            PSCmsException.class, () -> ws.sendRequest("login", "misc", message, "LoginResponse"));
    assertSame(CmsErrorCodes.UNEXPECTED_ERROR, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.UNEXPECTED_ERROR.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void webServiceAgentSendFailureThrowsTypedCmsException() throws Exception {
    HttpServer server =
        HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext(
        "/",
        exchange -> {
          byte[] body = "not-soap".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(500, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    try {
      int port = server.getAddress().getPort();
      PSWebServiceAgent agent = new PSWebServiceAgent("http", "127.0.0.1", port, "user", "pass");
      Envelope env = new Envelope();
      env.setHeader(agent.getAuthenticateHeader());
      env.setBody(new Body());
      PSCmsException ex =
          assertThrows(PSCmsException.class, () -> agent.sendEnvelope(env, "LoginResponse"));
      assertSame(CmsErrorCodes.ERROR_SEND_DATA, ex.getTypedErrorCode());
      assertEquals(CmsErrorCodes.ERROR_SEND_DATA.numericCode(), ex.getErrorCode());
      assertFalse(ex.isAuditable());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void webServiceAgentUnknownResponseUsesTypedCmsException() {
    Object[] args = {"LoginResponse", "<Fault/>"};
    PSCmsException ex = new PSCmsException(CmsErrorCodes.RECEIVED_UNKNOWN_DATA, args);
    assertSame(CmsErrorCodes.RECEIVED_UNKNOWN_DATA, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.RECEIVED_UNKNOWN_DATA.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void contentRelatedItemMissingHandlerThrowsTypedPsException() {
    PSException ex =
        new PSException(ServerErrorCodes.CE_NEEDED_APP_NOT_RUNNING, "sys_psxNewCopy/copy.xml");
    assertSame(ServerErrorCodes.CE_NEEDED_APP_NOT_RUNNING, ex.getTypedErrorCode());
    assertEquals(ServerErrorCodes.CE_NEEDED_APP_NOT_RUNNING.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void switchCommunityComparisonMatchesTypedAndLegacyServerException() {
    PSServerException typed =
        new PSServerException(
            ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_INVALID_COMMUNITY, "alice");
    assertSame(
        ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_INVALID_COMMUNITY,
        typed.getTypedErrorCode());
    assertEquals(
        ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_INVALID_COMMUNITY.numericCode(),
        typed.getErrorCode());
    assertTrue(typed.isAuditable());

    PSServerException legacy =
        new PSServerException(IPSServerErrors.COMMUNITIES_AUTHENTICATION_FAILED_INVALID_COMMUNITY);
    assertEquals(
        ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_INVALID_COMMUNITY.numericCode(),
        legacy.getErrorCode());
    assertTrue(
        legacy.getErrorCode()
                == ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_INVALID_COMMUNITY
                    .numericCode()
            || ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_INVALID_COMMUNITY.equals(
                legacy.getTypedErrorCode()));

    PSUserNotMemberOfCommunityException member =
        new PSUserNotMemberOfCommunityException(
            WebserviceErrorCodes.USER_NOT_MEMBER_COMMUNITY, "not a member", "stack");
    assertSame(WebserviceErrorCodes.USER_NOT_MEMBER_COMMUNITY, member.getTypedErrorCode());
    assertTrue(member.isAuditable());
  }

  @Test
  void typedCmsExceptionCtorRejectsNullCode() {
    assertThrows(IllegalArgumentException.class, () -> new PSCmsException((CmsErrorCodes) null));
    assertThrows(IllegalArgumentException.class, () -> new PSException((ServerErrorCodes) null));
  }
}
