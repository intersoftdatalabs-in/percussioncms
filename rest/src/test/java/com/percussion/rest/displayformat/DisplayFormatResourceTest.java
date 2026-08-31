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

package com.percussion.rest.displayformat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class DisplayFormatResourceTest {

  private IDisplayFormatAdaptor adaptor;
  private DisplayFormatResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IDisplayFormatAdaptor.class);
    resource = new DisplayFormatResource(adaptor);
  }

  @Test
  public void listDisplayFormatsDelegates() throws Exception {
    DisplayFormat f = new DisplayFormat();
    f.setName("Default");
    when(adaptor.findAllDisplayFormats()).thenReturn(List.of(f));

    List<DisplayFormat> out = resource.listDisplayFormats(null, null);
    assertEquals(1, out.size());
    assertEquals("Default", out.get(0).getName());
    assertInstanceOf(DisplayFormatList.class, out);
  }

  @Test
  public void listDisplayFormatsNullSafe() throws Exception {
    when(adaptor.findAllDisplayFormats()).thenReturn(null);
    List<DisplayFormat> out = resource.listDisplayFormats(null, null);
    assertTrue(out.isEmpty());
    assertInstanceOf(DisplayFormatList.class, out);
  }

  @Test
  public void listDisplayFormatsEmptyUsesWrappedListEnvelope() throws Exception {
    when(adaptor.findAllDisplayFormats()).thenReturn(List.of());
    List<DisplayFormat> out = resource.listDisplayFormats(null, null);
    assertTrue(out.isEmpty());
    assertInstanceOf(DisplayFormatList.class, out);
  }

  @Test
  public void listDisplayFormatsFiltersValidForFolder() throws Exception {
    DisplayFormat folderOk = new DisplayFormat();
    folderOk.setName("FolderList");
    folderOk.setValidForFolder(true);
    DisplayFormat notFolder = new DisplayFormat();
    notFolder.setName("SearchOnly");
    notFolder.setValidForFolder(false);
    notFolder.setValidForViewsAndSearches(true);
    when(adaptor.findAllDisplayFormats()).thenReturn(List.of(folderOk, notFolder));

    List<DisplayFormat> out = resource.listDisplayFormats(true, null);
    assertEquals(1, out.size());
    assertEquals("FolderList", out.get(0).getName());
  }

  @Test
  public void listDisplayFormatsFiltersValidForViewsAndSearches() throws Exception {
    DisplayFormat searchOk = new DisplayFormat();
    searchOk.setName("SearchFmt");
    searchOk.setValidForViewsAndSearches(true);
    DisplayFormat noSearch = new DisplayFormat();
    noSearch.setName("FolderOnly");
    noSearch.setValidForViewsAndSearches(false);
    noSearch.setValidForFolder(true);
    when(adaptor.findAllDisplayFormats()).thenReturn(List.of(searchOk, noSearch));

    List<DisplayFormat> out = resource.listDisplayFormats(null, true);
    assertEquals(1, out.size());
    assertEquals("SearchFmt", out.get(0).getName());
  }

  @Test
  public void listDisplayFormatsFiltersCombinedValidForFolderAndViews() throws Exception {
    DisplayFormat both = new DisplayFormat();
    both.setName("Both");
    both.setValidForFolder(true);
    both.setValidForViewsAndSearches(true);
    DisplayFormat folderOnly = new DisplayFormat();
    folderOnly.setName("FolderOnly");
    folderOnly.setValidForFolder(true);
    folderOnly.setValidForViewsAndSearches(false);
    DisplayFormat searchOnly = new DisplayFormat();
    searchOnly.setName("SearchOnly");
    searchOnly.setValidForFolder(false);
    searchOnly.setValidForViewsAndSearches(true);
    DisplayFormat neither = new DisplayFormat();
    neither.setName("Neither");
    neither.setValidForFolder(false);
    neither.setValidForViewsAndSearches(false);
    when(adaptor.findAllDisplayFormats())
        .thenReturn(List.of(both, folderOnly, searchOnly, neither));

    List<DisplayFormat> out = resource.listDisplayFormats(true, true);
    assertEquals(1, out.size());
    assertEquals("Both", out.get(0).getName());
  }

  @Test
  public void getDisplayFormatDelegates() {
    DisplayFormat f = new DisplayFormat();
    f.setName("Default");
    when(adaptor.findDisplayFormatByKey(eq("Default"))).thenReturn(f);

    assertEquals("Default", resource.getDisplayFormat("Default").getName());
    verify(adaptor).findDisplayFormatByKey("Default");
  }

  @Test
  public void getDisplayFormatNotFoundIsGeneric404() {
    when(adaptor.findDisplayFormatByKey(eq("missing"))).thenReturn(null);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getDisplayFormat("missing"));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Display format not found", ex.getMessage());
  }

  @Test
  public void getDisplayFormatWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("down");
    when(adaptor.findDisplayFormatByKey(eq("Default"))).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getDisplayFormat("Default"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void createDisplayFormatClearsIdAndDelegates() {
    DisplayFormat body = new DisplayFormat();
    body.setName("MyFmt");
    body.setDisplayId(9);
    DisplayFormat created = new DisplayFormat();
    created.setName("MyFmt");
    created.setDisplayId(99);
    when(adaptor.createDisplayFormat(any())).thenReturn(created);

    DisplayFormat out = resource.createDisplayFormat(body);

    assertEquals("MyFmt", out.getName());
    assertEquals(0, body.getDisplayId());
    assertNull(body.getGuid());
    verify(adaptor).createDisplayFormat(body);
  }

  @Test
  public void createDisplayFormatBlankNameIs400() {
    when(adaptor.createDisplayFormat(any()))
        .thenThrow(new IllegalArgumentException("name is required"));
    DisplayFormat body = new DisplayFormat();
    body.setName("  ");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createDisplayFormat(body));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void createDisplayFormatDuplicateIs409() {
    when(adaptor.createDisplayFormat(any()))
        .thenThrow(new WebApplicationException("Display format already exists: MyFmt", 409));
    DisplayFormat body = new DisplayFormat();
    body.setName("MyFmt");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createDisplayFormat(body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void createDisplayFormatNonAdminIs403() {
    when(adaptor.createDisplayFormat(any()))
        .thenThrow(new WebApplicationException("Admin role required", Response.Status.FORBIDDEN));
    DisplayFormat body = new DisplayFormat();
    body.setName("MyFmt");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createDisplayFormat(body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void updateDisplayFormatDelegates() {
    DisplayFormat existing = new DisplayFormat();
    existing.setName("MyFmt");
    when(adaptor.findDisplayFormatByKey(eq("MyFmt"))).thenReturn(existing);
    DisplayFormat body = new DisplayFormat();
    body.setLabel("Updated");
    body.setDescription("desc");
    DisplayFormat updated = new DisplayFormat();
    updated.setName("MyFmt");
    updated.setLabel("Updated");
    updated.setDescription("desc");
    when(adaptor.updateDisplayFormat(eq("MyFmt"), any())).thenReturn(updated);

    DisplayFormat out = resource.updateDisplayFormat("MyFmt", body);

    assertEquals("Updated", out.getLabel());
    assertEquals("desc", out.getDescription());
    verify(adaptor).updateDisplayFormat("MyFmt", body);
  }

  @Test
  public void updateDisplayFormatUnknownIs404() {
    when(adaptor.findDisplayFormatByKey(eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateDisplayFormat("missing", new DisplayFormat()));
    assertEquals(404, ex.getResponse().getStatus());
    verify(adaptor, never()).updateDisplayFormat(any(), any());
  }

  @Test
  public void updateDisplayFormatNullBodyIs400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateDisplayFormat("MyFmt", null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void deleteDisplayFormatNoContent() {
    when(adaptor.deleteDisplayFormat(eq("MyFmt"))).thenReturn(true);

    Response r = resource.deleteDisplayFormat("MyFmt");

    assertEquals(204, r.getStatus());
    verify(adaptor).deleteDisplayFormat("MyFmt");
  }

  @Test
  public void deleteDisplayFormatUnknownIs404() {
    when(adaptor.deleteDisplayFormat(eq("missing"))).thenReturn(false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteDisplayFormat("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void deleteDisplayFormatLockConflictIs409() {
    when(adaptor.deleteDisplayFormat(eq("MyFmt")))
        .thenThrow(
            new WebApplicationException(
                "Could not delete display format; design lock required or held by another user",
                409));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteDisplayFormat("MyFmt"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void deleteDisplayFormatNonAdminIs403() {
    when(adaptor.deleteDisplayFormat(eq("MyFmt")))
        .thenThrow(new WebApplicationException("Admin role required", Response.Status.FORBIDDEN));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteDisplayFormat("MyFmt"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void withoutInjectionFailsWithDiagnostic() {
    DisplayFormatResource bare = new DisplayFormatResource();
    WebApplicationException listEx =
        assertThrows(WebApplicationException.class, () -> bare.listDisplayFormats(null, null));
    assertEquals(500, listEx.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, listEx.getCause());

    WebApplicationException getEx =
        assertThrows(WebApplicationException.class, () -> bare.getDisplayFormat("x"));
    assertEquals(500, getEx.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, getEx.getCause());

    WebApplicationException createEx =
        assertThrows(
            WebApplicationException.class, () -> bare.createDisplayFormat(new DisplayFormat()));
    assertEquals(500, createEx.getResponse().getStatus());
  }
}
