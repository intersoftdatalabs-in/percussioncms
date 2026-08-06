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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
  public void listKeywordsDelegatesWithIncludeChoicesTrue() {
    KeywordSummary kw = new KeywordSummary();
    kw.setLabel("Status");
    KeywordChoiceSummary choice = new KeywordChoiceSummary();
    choice.setLabel("Open");
    choice.setValue("open");
    kw.setChoices(List.of(choice));
    when(adaptor.listKeywords(any(), eq(true))).thenReturn(List.of(kw));
    List<KeywordSummary> out = resource.listKeywords(true);
    assertEquals(1, out.size());
    assertEquals("Status", out.get(0).getLabel());
    assertEquals(1, out.get(0).getChoices().size());
    assertEquals("Open", out.get(0).getChoices().get(0).getLabel());
    verify(adaptor).listKeywords(any(), eq(true));
  }

  @Test
  public void listKeywordsDelegatesWithIncludeChoicesFalse() {
    KeywordSummary kw = new KeywordSummary();
    kw.setLabel("Status");
    when(adaptor.listKeywords(any(), eq(false))).thenReturn(List.of(kw));
    List<KeywordSummary> out = resource.listKeywords(false);
    assertEquals(1, out.size());
    assertEquals("Status", out.get(0).getLabel());
    verify(adaptor).listKeywords(any(), eq(false));
  }

  @Test
  public void listKeywordsEmpty() {
    when(adaptor.listKeywords(any(), eq(false))).thenReturn(List.of());
    assertTrue(resource.listKeywords(false).isEmpty());
  }

  @Test
  public void listKeywordsWrapsUnexpectedFailures() {
    RuntimeException cause = new RuntimeException("boom");
    when(adaptor.listKeywords(any(), eq(false))).thenThrow(cause);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listKeywords(false));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(cause, ex.getCause());
  }

  @Test
  public void listKeywordsRethrowsWebApplicationException() {
    WebApplicationException mapped =
        new WebApplicationException("pre-mapped", Response.Status.SERVICE_UNAVAILABLE);
    when(adaptor.listKeywords(any(), eq(false))).thenThrow(mapped);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listKeywords(false));
    assertSame(mapped, ex);
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void getKeywordByValue() {
    KeywordSummary kw = new KeywordSummary();
    kw.setValue("status");
    when(adaptor.getKeyword(any(), eq("status"))).thenReturn(kw);
    assertEquals("status", resource.getKeyword("status").getValue());
  }

  @Test
  public void getKeywordByUuidString() {
    KeywordSummary kw = new KeywordSummary();
    kw.setLabel("ByUuid");
    when(adaptor.getKeyword(any(), eq("0-37-42"))).thenReturn(kw);
    assertEquals("ByUuid", resource.getKeyword("0-37-42").getLabel());
  }

  @Test
  public void getKeywordNotFound() {
    when(adaptor.getKeyword(any(), eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getKeyword("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void getKeywordBlankTreatedAsNotFound() {
    when(adaptor.getKeyword(any(), eq("   "))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getKeyword("   "));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void getKeywordWrapsUnexpectedFailures() {
    RuntimeException cause = new IllegalStateException("Failed to load keyword");
    when(adaptor.getKeyword(any(), eq("boom"))).thenThrow(cause);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getKeyword("boom"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(cause, ex.getCause());
  }

  @Test
  public void getKeywordRethrowsWebApplicationException() {
    WebApplicationException mapped =
        new WebApplicationException("pre-mapped", Response.Status.BAD_GATEWAY);
    when(adaptor.getKeyword(any(), eq("x"))).thenThrow(mapped);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getKeyword("x"));
    assertSame(mapped, ex);
    assertEquals(502, ex.getResponse().getStatus());
  }

  @Test
  public void createKeywordRequiresLabel() {
    when(adaptor.createKeyword(any(), any()))
        .thenThrow(new IllegalArgumentException("label is required"));
    KeywordSummary blank = new KeywordSummary();
    blank.setLabel("   ");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createKeyword(blank));
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
  public void createKeywordWrapsUnexpectedFailures() {
    RuntimeException cause = new IllegalStateException("design ws down");
    when(adaptor.createKeyword(any(), any())).thenThrow(cause);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.createKeyword(new KeywordSummary()));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(cause, ex.getCause());
  }

  @Test
  public void updateKeywordSuccess() {
    KeywordSummary body = new KeywordSummary();
    body.setLabel("Updated");
    KeywordChoiceSummary choice = new KeywordChoiceSummary();
    choice.setLabel("Open");
    choice.setValue("open");
    body.setChoices(List.of(choice));
    KeywordSummary updated = new KeywordSummary();
    updated.setLabel("Updated");
    updated.setChoices(List.of(choice));
    when(adaptor.updateKeyword(any(), eq("9"), any())).thenReturn(updated);
    KeywordSummary out = resource.updateKeyword("9", body);
    assertEquals("Updated", out.getLabel());
    assertEquals(1, out.getChoices().size());
    assertEquals("Open", out.getChoices().get(0).getLabel());
    assertEquals("open", out.getChoices().get(0).getValue());
  }

  @Test
  public void updateKeywordNotFound() {
    when(adaptor.updateKeyword(any(), eq("9"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateKeyword("9", new KeywordSummary()));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void updateKeywordNullBody() {
    when(adaptor.updateKeyword(any(), eq("9"), any()))
        .thenThrow(new IllegalArgumentException("body is required"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.updateKeyword("9", null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void updateKeywordWrapsUnexpectedFailures() {
    RuntimeException cause = new IllegalStateException("Failed to update keyword");
    when(adaptor.updateKeyword(any(), eq("9"), any())).thenThrow(cause);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateKeyword("9", new KeywordSummary()));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(cause, ex.getCause());
  }

  @Test
  public void deleteKeywordNoContent() {
    Response r = resource.deleteKeyword("42");
    assertEquals(204, r.getStatus());
    verify(adaptor).deleteKeyword(any(), eq("42"));
  }

  @Test
  public void deleteKeywordNotFound() {
    doThrow(new KeywordNotFoundException("Keyword not found: 99"))
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
  public void deleteKeywordWrapsUnexpectedFailures() {
    RuntimeException cause = new IllegalStateException("Failed to delete keyword");
    doThrow(cause).when(adaptor).deleteKeyword(any(), eq("7"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteKeyword("7"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(cause, ex.getCause());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnList() {
    KeywordsResource bare = newKeywordsResourceWithoutAdaptor();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.listKeywords(false));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnGet() {
    KeywordsResource bare = newKeywordsResourceWithoutAdaptor();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.getKeyword("status"));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnCreate() {
    KeywordsResource bare = newKeywordsResourceWithoutAdaptor();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.createKeyword(new KeywordSummary()));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnUpdate() {
    KeywordsResource bare = newKeywordsResourceWithoutAdaptor();
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> bare.updateKeyword("9", new KeywordSummary()));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnDelete() {
    KeywordsResource bare = newKeywordsResourceWithoutAdaptor();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.deleteKeyword("9"));
    assertEquals(503, ex.getResponse().getStatus());
  }

  private static KeywordsResource newKeywordsResourceWithoutAdaptor() {
    KeywordsResource bare = new KeywordsResource();
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    bare.setUriInfo(uriInfo);
    return bare;
  }
}
