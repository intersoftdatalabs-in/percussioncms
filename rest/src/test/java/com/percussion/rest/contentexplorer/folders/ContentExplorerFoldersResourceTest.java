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

package com.percussion.rest.contentexplorer.folders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HTTP-layer tests for {@link ContentExplorerFoldersResource} (#3073). Domain behaviour is covered
 * by sitemanage apibridge unit tests.
 */
@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
class ContentExplorerFoldersResourceTest {

  @Mock private IContentExplorerFolderAdaptor adaptor;

  @Mock private UriInfo uriInfo;

  private ContentExplorerFoldersResource resource;

  @BeforeEach
  void init() {
    resource = new ContentExplorerFoldersResource(adaptor);
    resource.setUriInfo(uriInfo);
    org.mockito.Mockito.lenient()
        .when(uriInfo.getBaseUri())
        .thenReturn(UriBuilder.fromUri("http://localhost/rest").build());
  }

  @Test
  void loadByPathDelegates() {
    RxFolder expected = new RxFolder();
    expected.setName("Folders");
    expected.setPath("//Folders");
    when(adaptor.loadByPath(any(), eq("//Folders"))).thenReturn(expected);

    RxFolder out = resource.loadByPath("//Folders");

    assertEquals("Folders", out.getName());
    verify(adaptor).loadByPath(any(), eq("//Folders"));
  }

  @Test
  void loadByPathNullIs404() {
    when(adaptor.loadByPath(any(), eq("//Folders/missing"))).thenReturn(null);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.loadByPath("//Folders/missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void loadByIdDelegates() {
    RxFolder expected = new RxFolder();
    expected.setId("123");
    when(adaptor.loadById(any(), eq("123"))).thenReturn(expected);

    assertEquals("123", resource.loadById("123").getId());
  }

  @Test
  void loadByIdIllegalArgumentIs400() {
    when(adaptor.loadById(any(), eq("bad")))
        .thenThrow(new IllegalArgumentException("id contains invalid characters"));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.loadById("bad"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void childrenByPathDelegates() {
    RxFolderChildList list = new RxFolderChildList(List.of());
    list.setParentPath("//Folders");
    when(adaptor.findChildrenByPath(any(), eq("//Folders"))).thenReturn(list);

    assertEquals("//Folders", resource.childrenByPath("//Folders").getParentPath());
  }

  @Test
  void addFolderNullBodyIs400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.addFolder(null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void addFolderDelegates() {
    AddFolderRequest body = new AddFolderRequest();
    body.setName("New");
    body.setParentPath("//Folders");
    RxFolder created = new RxFolder();
    created.setName("New");
    when(adaptor.addFolder(any(), eq(body))).thenReturn(created);

    assertEquals("New", resource.addFolder(body).getName());
  }

  @Test
  void saveFolderDelegates() {
    RxFolder body = new RxFolder();
    body.setName("Renamed");
    when(adaptor.saveFolder(any(), eq("10"), eq(body))).thenReturn(body);

    assertEquals("Renamed", resource.saveFolder("10", body).getName());
  }

  @Test
  void deleteFolderDelegates() {
    resource.deleteFolder("10", true);
    verify(adaptor).deleteFolder(any(), eq("10"), eq(true));
  }

  @Test
  void moveChildrenNullBodyIs400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.moveChildren(null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void moveChildrenReturns204() {
    FolderChildrenRequest body = new FolderChildrenRequest();
    body.setSourcePath("//Folders/a");
    body.setTargetPath("//Folders/b");
    assertEquals(204, resource.moveChildren(body).getStatus());
    verify(adaptor).moveChildren(any(), eq(body));
  }

  @Test
  void securityMapsTo403() {
    when(adaptor.loadById(any(), eq("x"))).thenThrow(new SecurityException("denied"));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.loadById("x"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void missingAdaptorIs503() {
    ContentExplorerFoldersResource bare = new ContentExplorerFoldersResource();
    bare.setUriInfo(uriInfo);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.loadById("1"));
    assertEquals(503, ex.getResponse().getStatus());
  }
}
