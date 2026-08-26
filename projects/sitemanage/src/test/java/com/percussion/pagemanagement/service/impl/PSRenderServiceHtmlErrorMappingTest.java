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
package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.pagemanagement.assembler.IPSRenderAssemblyBridge;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSErrors;
import com.percussion.share.web.service.PSErrorsHtmlMessageBodyWriter;
import com.percussion.share.web.service.PSRuntimeExceptionMapper;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Page Management {@code GET /render/page/{id}} error path (#3809): validation / page exceptions
 * become {@link WebApplicationException}, then {@link PSRuntimeExceptionMapper} + HTML writer.
 *
 * <p>Does not stub {@code java.util.Properties} in place of {@code PSProperties}.
 */
@Tag("UnitTest")
class PSRenderServiceHtmlErrorMappingTest {

  private static final String RFF_HOME_ID = "16777215-101-551";

  private IPSRenderAssemblyBridge bridge;
  private PSRenderService service;

  @BeforeEach
  void setUp() {
    bridge = mock(IPSRenderAssemblyBridge.class);
    service =
        new PSRenderService(
            mock(IPSPageService.class),
            bridge,
            mock(IPSTemplateService.class),
            mock(PlatformTransactionManager.class));
  }

  @Test
  @DisplayName("renderPage wraps PSPageException as WebApplicationException")
  void pageExceptionBecomesWae() throws Exception {
    when(bridge.renderPage(eq(RFF_HOME_ID), eq(false), eq(false)))
        .thenThrow(new IPSPageService.PSPageException("Failed to preview page: " + RFF_HOME_ID));
    WebApplicationException wae =
        assertThrows(WebApplicationException.class, () -> service.renderPage(RFF_HOME_ID));
    assertInstanceOf(IPSPageService.PSPageException.class, wae.getCause());
  }

  @Test
  @DisplayName("renderPage wraps PSValidationException as WebApplicationException")
  void validationExceptionBecomesWae() throws Exception {
    when(bridge.renderPage(eq(RFF_HOME_ID), eq(false), eq(false)))
        .thenThrow(new DummyValidationException("Cannot render item because the item does not exist."));
    WebApplicationException wae =
        assertThrows(WebApplicationException.class, () -> service.renderPage(RFF_HOME_ID));
    assertInstanceOf(PSValidationException.class, wae.getCause());
  }

  @Test
  @DisplayName("WAE cause maps to PSErrors text/html that the HTML writer can emit")
  void mappedHtmlHasWriter() throws Exception {
    when(bridge.renderPage(eq(RFF_HOME_ID), eq(false), eq(false)))
        .thenThrow(new IPSPageService.PSPageException("Failed to preview page: " + RFF_HOME_ID));
    WebApplicationException wae =
        assertThrows(WebApplicationException.class, () -> service.renderPage(RFF_HOME_ID));

    PSRuntimeExceptionMapper mapper = new PSRuntimeExceptionMapper();
    HttpHeaders headers = mock(HttpHeaders.class);
    when(headers.getAcceptableMediaTypes()).thenReturn(List.of(MediaType.TEXT_HTML_TYPE));
    mapper.setHeaders(headers);

    Response response = mapper.toResponse(wae);
    assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());
    assertInstanceOf(String.class, response.getEntity());
    String html = (String) response.getEntity();
    assertTrue(html.contains("<html"));
    assertTrue(html.contains(RFF_HOME_ID));
    assertTrue(!html.toLowerCase().contains("no message body writer"));
    assertTrue(
        new PSErrorsHtmlMessageBodyWriter()
            .isWriteable(PSErrors.class, PSErrors.class, null, response.getMediaType()));
  }

  private static final class DummyValidationException extends PSValidationException {
    private static final long serialVersionUID = 1L;

    DummyValidationException(String message) {
      super(message);
    }
  }
}
