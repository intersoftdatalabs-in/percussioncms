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
package com.percussion.share.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.pagemanagement.service.IPSPageService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Preview Accept {@code text/html} must not leave {@link PSErrors} untyped (#3809). Browser Accept
 * lists {@code text/html} then {@code application/xml}; HTML wins over XML so the HTML writer is
 * used.
 */
@Tag("UnitTest")
class PSAbstractExceptionMapperHtmlNegotationTest {

  @Test
  @DisplayName("null headers default to JSON")
  void nullHeadersAreJson() {
    assertEquals(
        MediaType.APPLICATION_JSON_TYPE, PSAbstractExceptionMapper.negotiateMediaType(null));
  }

  @Test
  @DisplayName("browser Preview Accept is text/html, not application/xml")
  void browserAcceptIsHtml() {
    HttpHeaders headers = mock(HttpHeaders.class);
    when(headers.getAcceptableMediaTypes())
        .thenReturn(
            List.of(
                MediaType.TEXT_HTML_TYPE,
                MediaType.APPLICATION_XHTML_XML_TYPE,
                MediaType.APPLICATION_XML_TYPE,
                MediaType.WILDCARD_TYPE));
    assertEquals(
        MediaType.TEXT_HTML_TYPE, PSAbstractExceptionMapper.negotiateMediaType(headers));
  }

  @Test
  @DisplayName("JSON Accept stays JSON even when HTML is also listed")
  void jsonWinsOverHtml() {
    HttpHeaders headers = mock(HttpHeaders.class);
    when(headers.getAcceptableMediaTypes())
        .thenReturn(List.of(MediaType.APPLICATION_JSON_TYPE, MediaType.TEXT_HTML_TYPE));
    assertEquals(
        MediaType.APPLICATION_JSON_TYPE, PSAbstractExceptionMapper.negotiateMediaType(headers));
  }

  @Test
  @DisplayName("runtime mapper Preview path: PSErrors entity + text/html")
  void runtimeMapperHtmlType() {
    PSRuntimeExceptionMapper mapper = new PSRuntimeExceptionMapper();
    HttpHeaders headers = mock(HttpHeaders.class);
    when(headers.getAcceptableMediaTypes()).thenReturn(List.of(MediaType.TEXT_HTML_TYPE));
    mapper.setHeaders(headers);

    WebApplicationException wae =
        new WebApplicationException(
            new IPSPageService.PSPageException("Failed to preview page: 16777215-101-551"));
    Response response = mapper.toResponse(wae);
    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());
    assertInstanceOf(String.class, response.getEntity());
    String html = (String) response.getEntity();
    assertTrue(html.contains("<html"));
    assertTrue(html.contains("Failed to preview page: 16777215-101-551"));
    assertTrue(!html.toLowerCase().contains("no message body writer"));
  }
}
