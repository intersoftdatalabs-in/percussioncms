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

package com.percussion.rest.keywords;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class KeywordsResourceCrudTest {

  private IKeywordsAdaptor adaptor;
  private KeywordsResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IKeywordsAdaptor.class);
    resource = new KeywordsResource(adaptor);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    resource.setUriInfo(uriInfo);
  }

  @Test
  public void listKeywordsDelegates() {
    KeywordSummary kw = new KeywordSummary();
    kw.setLabel("Status");
    when(adaptor.listKeywords(any(), eq(true))).thenReturn(List.of(kw));
    List<KeywordSummary> out = resource.listKeywords(true);
    assertEquals(1, out.size());
    assertEquals("Status", out.get(0).getLabel());
  }

  @Test
  public void getKeywordByValue() {
    KeywordSummary kw = new KeywordSummary();
    kw.setValue("status");
    when(adaptor.getKeyword(any(), eq("status"))).thenReturn(kw);
    assertEquals("status", resource.getKeyword("status").getValue());
  }

  @Test
  public void getKeywordNotFound() {
    when(adaptor.getKeyword(any(), eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getKeyword("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void createKeywordRequiresLabel() {
    when(adaptor.createKeyword(any(), any()))
        .thenThrow(new IllegalArgumentException("label is required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.createKeyword(new KeywordSummary()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void createKeywordSuccess() {
    KeywordSummary body = new KeywordSummary();
    body.setLabel("New");
    KeywordSummary created = new KeywordSummary();
    created.setLabel("New");
    when(adaptor.createKeyword(any(), any())).thenReturn(created);
    assertEquals("New", resource.createKeyword(body).getLabel());
  }

  @Test
  public void updateKeywordNotFound() {
    when(adaptor.updateKeyword(any(), eq("9"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateKeyword("9", new KeywordSummary()));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void deleteKeywordNoContent() {
    Response r = resource.deleteKeyword("42");
    assertEquals(204, r.getStatus());
    verify(adaptor).deleteKeyword(any(), eq("42"));
  }

  @Test
  public void deleteKeywordNotFound() {
    doThrow(new IllegalArgumentException("Keyword not found: 99"))
        .when(adaptor)
        .deleteKeyword(any(), eq("99"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteKeyword("99"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void deleteKeywordInvalidId() {
    doThrow(new IllegalArgumentException("Invalid keyword id: xyz"))
        .when(adaptor)
        .deleteKeyword(any(), eq("xyz"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteKeyword("xyz"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void getKeywordBlankTreatedAsNotFound() {
    when(adaptor.getKeyword(any(), isNull())).thenReturn(null);
    when(adaptor.getKeyword(any(), eq("   "))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getKeyword("   "));
    assertEquals(404, ex.getResponse().getStatus());
  }
}
