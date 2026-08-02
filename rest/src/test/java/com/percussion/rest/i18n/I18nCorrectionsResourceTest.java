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

package com.percussion.rest.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class I18nCorrectionsResourceTest {

  private I18nCorrectionsAdaptor adaptor;
  private I18nCorrectionsResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(I18nCorrectionsAdaptor.class);
    resource = new I18nCorrectionsResource(adaptor);
  }

  @Test
  public void submitDelegatesToAdaptor() {
    I18nCorrectionSubmission body = new I18nCorrectionSubmission();
    body.setEmail("u@example.com");
    body.setLocale("en-us");
    when(adaptor.submit(any())).thenReturn(I18nCorrectionResult.ok("mid-1"));

    I18nCorrectionResult out = resource.submit(body);
    assertEquals("ok", out.getStatus());
    assertEquals("mid-1", out.getGcmMessageId());
    verify(adaptor).submit(body);
  }

  @Test
  public void nullBodyIs400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.submit(null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void validationMapsTo400() {
    when(adaptor.submit(any())).thenThrow(new IllegalArgumentException("email is required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.submit(new I18nCorrectionSubmission()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void securityMapsTo403() {
    when(adaptor.submit(any())).thenThrow(new SecurityException("not allowed"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.submit(new I18nCorrectionSubmission()));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void notConfiguredMapsTo503() {
    when(adaptor.submit(any())).thenThrow(new IllegalStateException("GCM not configured"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.submit(new I18nCorrectionSubmission()));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void backendFailureMapsTo502WithoutLeakingMessage() {
    when(adaptor.submit(any())).thenThrow(new RuntimeException("token=secret leaked"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.submit(new I18nCorrectionSubmission()));
    assertEquals(502, ex.getResponse().getStatus());
    assertEquals("Failed to submit correction.", ex.getMessage());
  }
}
