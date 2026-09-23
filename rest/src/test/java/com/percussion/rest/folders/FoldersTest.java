/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.rest.folders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.percussion.rest.MoveFolderItem;
import com.percussion.rest.Status;
import com.percussion.rest.errors.BackendException;
import com.percussion.rest.errors.FolderNotFoundException;
import com.percussion.rest.errors.NotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FoldersTest {

  @Mock IFolderAdaptor adaptor;

  @Mock UriInfo uriInfo;

  @InjectMocks FoldersResource resource;

  @BeforeEach
  void init() {
    org.mockito.Mockito.lenient()
        .when(uriInfo.getBaseUri())
        .thenReturn(UriBuilder.fromUri("http://localhost/api").build());
    resource.setUriInfo(uriInfo);
  }

  @Test
  void moveFolderItem_callsAdaptor() throws Exception {
    MoveFolderItem req = new MoveFolderItem("/a/b", "/a/c");
    Status result = resource.moveFolderItem(req);
    assertEquals("Moved OK", result.getMessage());
    verify(adaptor).moveFolderItem(uriInfo.getBaseUri(), "/a/b", "/a/c");
  }

  @Test
  void moveFolderItem_propagatesBackendException() throws Exception {
    MoveFolderItem req = new MoveFolderItem("/x", "/y");
    doThrow(new BackendException("boom", new Exception("cause")))
        .when(adaptor)
        .moveFolderItem(any(), anyString(), anyString());
    assertThrows(WebApplicationException.class, () -> resource.moveFolderItem(req));
  }

  @Test
  void moveFolderItem_mapsNotAuthorizedToForbidden() throws Exception {
    MoveFolderItem req = new MoveFolderItem("/Assets/src/item", "/Assets/dst");
    doThrow(new NotAuthorizedException())
        .when(adaptor)
        .moveFolderItem(any(), anyString(), anyString());
    NotAuthorizedException thrown =
        assertThrows(NotAuthorizedException.class, () -> resource.moveFolderItem(req));
    assertEquals(jakarta.ws.rs.core.Response.Status.FORBIDDEN, thrown.getStatus());
  }

  @Test
  void moveFolderItem_mapsFolderNotFound() throws Exception {
    MoveFolderItem req = new MoveFolderItem("/Assets/missing", "/Assets/dst");
    doThrow(new FolderNotFoundException())
        .when(adaptor)
        .moveFolderItem(any(), anyString(), anyString());
    assertThrows(FolderNotFoundException.class, () -> resource.moveFolderItem(req));
  }

  @Test
  void moveFolder_callsAdaptor() throws Exception {
    MoveFolderItem req = new MoveFolderItem("/a/b", "/a/c");
    Status result = resource.moveFolder(req);
    assertEquals("Moved OK", result.getMessage());
    verify(adaptor).moveFolderItem(uriInfo.getBaseUri(), "/a/b", "/a/c");
  }

  @Test
  void renameFolderItem_callsAdaptor() throws Exception {
    RenameFolderItemRequest req =
        new RenameFolderItemRequest("/Assets/src/item", "new-name");
    Status result = resource.renameFolderItem(req);
    assertEquals("Renamed OK", result.getMessage());
    verify(adaptor).renameFolderItem(uriInfo.getBaseUri(), "/Assets/src/item", "new-name");
  }

  @Test
  void renameFolderItem_mapsNotAuthorizedToForbidden() throws Exception {
    RenameFolderItemRequest req =
        new RenameFolderItemRequest("/Assets/src/item", "new-name");
    doThrow(new NotAuthorizedException())
        .when(adaptor)
        .renameFolderItem(any(), anyString(), anyString());
    NotAuthorizedException thrown =
        assertThrows(NotAuthorizedException.class, () -> resource.renameFolderItem(req));
    assertEquals(jakarta.ws.rs.core.Response.Status.FORBIDDEN, thrown.getStatus());
  }

  @Test
  void renameFolderItem_mapsFolderNotFound() throws Exception {
    RenameFolderItemRequest req =
        new RenameFolderItemRequest("/Assets/missing", "new-name");
    doThrow(new FolderNotFoundException())
        .when(adaptor)
        .renameFolderItem(any(), anyString(), anyString());
    assertThrows(FolderNotFoundException.class, () -> resource.renameFolderItem(req));
  }

  @Test
  void createFolder_callsAdaptor() throws Exception {
    Folder created = new Folder();
    created.setName("qa4637");
    created.setPath("/Assets");
    when(adaptor.createFolder(any(), eq("/Assets"), eq("qa4637"))).thenReturn(created);
    Folder result = resource.createFolder(new CreateFolderRequest("/Assets", "qa4637"));
    assertSame(created, result);
    verify(adaptor).createFolder(uriInfo.getBaseUri(), "/Assets", "qa4637");
  }

  @Test
  void createFolder_mapsNotAuthorizedToForbidden() throws Exception {
    doThrow(new NotAuthorizedException())
        .when(adaptor)
        .createFolder(any(), anyString(), anyString());
    NotAuthorizedException thrown =
        assertThrows(
            NotAuthorizedException.class,
            () -> resource.createFolder(new CreateFolderRequest("/Assets", "x")));
    assertEquals(jakarta.ws.rs.core.Response.Status.FORBIDDEN, thrown.getStatus());
  }

  @Test
  void createFolder_mapsFolderNotFound() throws Exception {
    doThrow(new FolderNotFoundException())
        .when(adaptor)
        .createFolder(any(), anyString(), anyString());
    assertThrows(
        FolderNotFoundException.class,
        () -> resource.createFolder(new CreateFolderRequest("/Assets/missing", "x")));
  }

  @Test
  void createFolder_mapsConflict() throws Exception {
    doThrow(new WebApplicationException("exists", jakarta.ws.rs.core.Response.Status.CONFLICT))
        .when(adaptor)
        .createFolder(any(), anyString(), anyString());
    WebApplicationException thrown =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createFolder(new CreateFolderRequest("/Assets", "dup")));
    assertEquals(
        jakarta.ws.rs.core.Response.Status.CONFLICT.getStatusCode(),
        thrown.getResponse().getStatus());
  }

  @Test
  void createFolder_rejectsBlankName() {
    WebApplicationException thrown =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createFolder(new CreateFolderRequest("/Assets", "  ")));
    assertEquals(
        jakarta.ws.rs.core.Response.Status.BAD_REQUEST.getStatusCode(),
        thrown.getResponse().getStatus());
  }

  @Test
  void saveItemProperties_callsAdaptor() throws Exception {
    ItemProperties saved = new ItemProperties("/Assets/src/item", "n", "t");
    when(adaptor.saveItemProperties(any(), eq("/Assets/src/item"), eq("n"), eq("t")))
        .thenReturn(saved);
    ItemPropertiesRequest req = new ItemPropertiesRequest("/Assets/src/item", "n", "t");
    ItemProperties result = resource.saveItemProperties(req);
    assertEquals("n", result.getName());
    verify(adaptor).saveItemProperties(uriInfo.getBaseUri(), "/Assets/src/item", "n", "t");
  }

  @Test
  void saveItemProperties_mapsNotAuthorizedToForbidden() throws Exception {
    ItemPropertiesRequest req = new ItemPropertiesRequest("/Assets/src/item", "n", "t");
    doThrow(new NotAuthorizedException())
        .when(adaptor)
        .saveItemProperties(any(), anyString(), anyString(), any());
    NotAuthorizedException thrown =
        assertThrows(NotAuthorizedException.class, () -> resource.saveItemProperties(req));
    assertEquals(jakarta.ws.rs.core.Response.Status.FORBIDDEN, thrown.getStatus());
  }

  @Test
  void saveItemProperties_mapsFolderNotFound() throws Exception {
    ItemPropertiesRequest req = new ItemPropertiesRequest("/Assets/missing", "n", null);
    doThrow(new FolderNotFoundException())
        .when(adaptor)
        .saveItemProperties(any(), anyString(), anyString(), any());
    assertThrows(FolderNotFoundException.class, () -> resource.saveItemProperties(req));
  }

  @Test
  void saveItemProperties_rejectsBlankName() {
    ItemPropertiesRequest req = new ItemPropertiesRequest("/Assets/src/item", "  ", "t");
    WebApplicationException thrown =
        assertThrows(WebApplicationException.class, () -> resource.saveItemProperties(req));
    assertEquals(
        jakarta.ws.rs.core.Response.Status.BAD_REQUEST.getStatusCode(),
        thrown.getResponse().getStatus());
  }

  @Test
  void getItemProperties_callsAdaptor() throws Exception {
    ItemProperties loaded = new ItemProperties("/Assets/src/item", "n", "t");
    when(adaptor.getItemProperties(any(), eq("/Assets/src/item"))).thenReturn(loaded);
    ItemProperties result = resource.getItemProperties("/Assets/src/item");
    assertEquals("n", result.getName());
    verify(adaptor).getItemProperties(uriInfo.getBaseUri(), "/Assets/src/item");
  }

  @Test
  void getItemProperties_rejectsBlankPath() {
    WebApplicationException thrown =
        assertThrows(WebApplicationException.class, () -> resource.getItemProperties("  "));
    assertEquals(
        jakarta.ws.rs.core.Response.Status.BAD_REQUEST.getStatusCode(),
        thrown.getResponse().getStatus());
  }

  @Test
  void renameFolderItem_rejectsBlankName() {
    RenameFolderItemRequest req = new RenameFolderItemRequest("/Assets/src/item", "  ");
    WebApplicationException thrown =
        assertThrows(WebApplicationException.class, () -> resource.renameFolderItem(req));
    assertEquals(jakarta.ws.rs.core.Response.Status.BAD_REQUEST.getStatusCode(), thrown.getResponse().getStatus());
  }

  @Test
  void moveFolder_mapsNotAuthorizedToForbidden() throws Exception {
    MoveFolderItem req = new MoveFolderItem("/Assets/src/folder", "/Assets/dst");
    doThrow(new NotAuthorizedException())
        .when(adaptor)
        .moveFolderItem(any(), anyString(), anyString());
    NotAuthorizedException thrown =
        assertThrows(NotAuthorizedException.class, () -> resource.moveFolder(req));
    assertEquals(jakarta.ws.rs.core.Response.Status.FORBIDDEN, thrown.getStatus());
  }

  @Test
  void moveFolder_mapsFolderNotFound() throws Exception {
    MoveFolderItem req = new MoveFolderItem("/Assets/missing/folder", "/Assets/dst");
    doThrow(new FolderNotFoundException())
        .when(adaptor)
        .moveFolderItem(any(), anyString(), anyString());
    assertThrows(FolderNotFoundException.class, () -> resource.moveFolder(req));
  }

  @Test
  void renameFolder_validatesAndReturnsFolder() throws Exception {
    Folder f = new Folder();
    f.setPath("folder");
    f.setSiteName("site");
    f.setName("newname");
    when(adaptor.renameFolder(
            eq(uriInfo.getBaseUri()), eq("site"), eq(""), eq("folder"), eq("newname")))
        .thenReturn(f);
    Folder returned = resource.renameFolder("site/folder", "newname");
    assertSame(f, returned);
  }

  @Test
  void copyFolderItem_blankDestIsBadRequest() {
    CopyFolderItemRequest req = new CopyFolderItemRequest();
    req.setItemPath("/Assets/src/item");
    req.setTargetFolderPath("  ");
    WebApplicationException thrown =
        assertThrows(WebApplicationException.class, () -> resource.copyFolderItem(req));
    assertEquals(400, thrown.getResponse().getStatus());
  }

  @Test
  void copyFolderItem_successReturnsCopiedOk() throws Exception {
    CopyFolderItemRequest req = new CopyFolderItemRequest();
    req.setItemPath("/Assets/src/item");
    req.setTargetFolderPath("/Assets/dst");
    Status result = resource.copyFolderItem(req);
    assertEquals(200, result.getStatusCode());
    assertEquals("Copied OK", result.getMessage());
    verify(adaptor)
        .copyFolderItem(uriInfo.getBaseUri(), "/Assets/src/item", "/Assets/dst");
  }

  @Test
  void copyFolderItem_mapsNotAuthorizedToForbidden() throws Exception {
    CopyFolderItemRequest req = new CopyFolderItemRequest();
    req.setItemPath("/Assets/src/item");
    req.setTargetFolderPath("/Assets/dst");
    doThrow(new NotAuthorizedException())
        .when(adaptor)
        .copyFolderItem(any(), anyString(), anyString());
    NotAuthorizedException thrown =
        assertThrows(NotAuthorizedException.class, () -> resource.copyFolderItem(req));
    assertEquals(jakarta.ws.rs.core.Response.Status.FORBIDDEN, thrown.getStatus());
  }

  @Test
  void copyFolderItem_mapsFolderNotFound() throws Exception {
    CopyFolderItemRequest req = new CopyFolderItemRequest();
    req.setItemPath("/Assets/missing");
    req.setTargetFolderPath("/Assets/dst");
    doThrow(new FolderNotFoundException())
        .when(adaptor)
        .copyFolderItem(any(), anyString(), anyString());
    FolderNotFoundException thrown =
        assertThrows(FolderNotFoundException.class, () -> resource.copyFolderItem(req));
    assertEquals(jakarta.ws.rs.core.Response.Status.NOT_FOUND, thrown.getStatus());
  }

  @Test
  void copyFolder_successReturnsCopiedOk() throws Exception {
    CopyFolderItemRequest req = new CopyFolderItemRequest();
    req.setItemPath("/Assets/src");
    req.setTargetFolderPath("/Assets/dst");
    Status result = resource.copyFolder(req);
    assertEquals(200, result.getStatusCode());
    assertEquals("Copied OK", result.getMessage());
    verify(adaptor).copyFolder(uriInfo.getBaseUri(), "/Assets/src", "/Assets/dst");
  }

  @Test
  void copyFolder_mapsNotAuthorizedToForbidden() throws Exception {
    CopyFolderItemRequest req = new CopyFolderItemRequest();
    req.setItemPath("/Assets/src");
    req.setTargetFolderPath("/Assets/dst");
    doThrow(new NotAuthorizedException())
        .when(adaptor)
        .copyFolder(any(), anyString(), anyString());
    NotAuthorizedException thrown =
        assertThrows(NotAuthorizedException.class, () -> resource.copyFolder(req));
    assertEquals(jakarta.ws.rs.core.Response.Status.FORBIDDEN, thrown.getStatus());
  }

  @Test
  void copyFolder_mapsFolderNotFound() throws Exception {
    CopyFolderItemRequest req = new CopyFolderItemRequest();
    req.setItemPath("/Assets/missing");
    req.setTargetFolderPath("/Assets/dst");
    doThrow(new FolderNotFoundException())
        .when(adaptor)
        .copyFolder(any(), anyString(), anyString());
    FolderNotFoundException thrown =
        assertThrows(FolderNotFoundException.class, () -> resource.copyFolder(req));
    assertEquals(jakarta.ws.rs.core.Response.Status.NOT_FOUND, thrown.getStatus());
  }

  @Test
  void copyFolder_rethrowsConflict() throws Exception {
    CopyFolderItemRequest req = new CopyFolderItemRequest();
    req.setItemPath("/Assets/src");
    req.setTargetFolderPath("/Assets/src");
    doThrow(new WebApplicationException("conflict", jakarta.ws.rs.core.Response.Status.CONFLICT))
        .when(adaptor)
        .copyFolder(any(), anyString(), anyString());
    WebApplicationException thrown =
        assertThrows(WebApplicationException.class, () -> resource.copyFolder(req));
    assertEquals(409, thrown.getResponse().getStatus());
  }

  @Test
  void copyFolder_mapsDescendantCopyToConflict() throws Exception {
    CopyFolderItemRequest req = new CopyFolderItemRequest();
    req.setItemPath("/Assets");
    req.setTargetFolderPath("/Assets/child");
    doThrow(
            new IllegalStateException(
                "Cannot copy a folder 'Assets' (id=7) to its descendent sub folder 'child'"))
        .when(adaptor)
        .copyFolder(any(), anyString(), anyString());
    WebApplicationException thrown =
        assertThrows(WebApplicationException.class, () -> resource.copyFolder(req));
    assertEquals(409, thrown.getResponse().getStatus());
  }

  @Test
  void copyFolderItem_rethrowsJaxrsNotFound() throws Exception {
    CopyFolderItemRequest req = new CopyFolderItemRequest();
    req.setItemPath("/Assets/src/item");
    req.setTargetFolderPath("/Assets/missing-dst");
    doThrow(new jakarta.ws.rs.NotFoundException("dest"))
        .when(adaptor)
        .copyFolderItem(any(), anyString(), anyString());
    assertThrows(jakarta.ws.rs.NotFoundException.class, () -> resource.copyFolderItem(req));
  }

  @Test
  void restoreRecycledItem_successReturnsOk() throws Exception {
    Status result = resource.restoreRecycledItem("1-101-9");
    assertEquals(200, result.getStatusCode());
    assertEquals("Ok", result.getMessage());
    verify(adaptor).restoreRecycledItem(uriInfo.getBaseUri(), "1-101-9");
  }

  @Test
  void restoreRecycledItem_mapsNotAuthorizedToForbidden() throws Exception {
    doThrow(new NotAuthorizedException())
        .when(adaptor)
        .restoreRecycledItem(any(), anyString());
    NotAuthorizedException thrown =
        assertThrows(
            NotAuthorizedException.class, () -> resource.restoreRecycledItem("1-101-9"));
    assertEquals(jakarta.ws.rs.core.Response.Status.FORBIDDEN, thrown.getStatus());
  }

  @Test
  void restoreRecycledItem_mapsFolderNotFound() throws Exception {
    doThrow(new FolderNotFoundException())
        .when(adaptor)
        .restoreRecycledItem(any(), anyString());
    assertThrows(
        FolderNotFoundException.class, () -> resource.restoreRecycledItem("missing"));
  }

  @Test
  void restoreRecycledItem_mapsConflict() throws Exception {
    doThrow(new WebApplicationException(jakarta.ws.rs.core.Response.Status.CONFLICT))
        .when(adaptor)
        .restoreRecycledItem(any(), anyString());
    WebApplicationException thrown =
        assertThrows(
            WebApplicationException.class, () -> resource.restoreRecycledItem("1-101-9"));
    assertEquals(409, thrown.getResponse().getStatus());
  }

  @Test
  void emptyRecycleBin_successReturnsOk() throws Exception {
    Status result = resource.emptyRecycleBin();
    assertEquals(200, result.getStatusCode());
    assertEquals("Ok", result.getMessage());
    verify(adaptor).emptyRecycleBin(uriInfo.getBaseUri());
  }

  @Test
  void emptyRecycleBin_mapsNotAuthorizedToForbidden() throws Exception {
    doThrow(new NotAuthorizedException()).when(adaptor).emptyRecycleBin(any());
    NotAuthorizedException thrown =
        assertThrows(NotAuthorizedException.class, () -> resource.emptyRecycleBin());
    assertEquals(jakarta.ws.rs.core.Response.Status.FORBIDDEN, thrown.getStatus());
  }

  @Test
  void emptyRecycleBin_mapsFolderNotFound() throws Exception {
    doThrow(new FolderNotFoundException()).when(adaptor).emptyRecycleBin(any());
    assertThrows(FolderNotFoundException.class, () -> resource.emptyRecycleBin());
  }

  @Test
  void emptyRecycleBin_mapsConflict() throws Exception {
    doThrow(new WebApplicationException(jakarta.ws.rs.core.Response.Status.CONFLICT))
        .when(adaptor)
        .emptyRecycleBin(any());
    WebApplicationException thrown =
        assertThrows(WebApplicationException.class, () -> resource.emptyRecycleBin());
    assertEquals(409, thrown.getResponse().getStatus());
  }

  @Test
  void purgeRecycledItem_successReturnsOk() throws Exception {
    Status result = resource.purgeRecycledItem("1-101-9");
    assertEquals(200, result.getStatusCode());
    assertEquals("Ok", result.getMessage());
    verify(adaptor).purgeRecycledItem(uriInfo.getBaseUri(), "1-101-9");
  }

  @Test
  void purgeRecycledItem_mapsNotAuthorizedToForbidden() throws Exception {
    doThrow(new NotAuthorizedException()).when(adaptor).purgeRecycledItem(any(), anyString());
    NotAuthorizedException thrown =
        assertThrows(NotAuthorizedException.class, () -> resource.purgeRecycledItem("1-101-9"));
    assertEquals(jakarta.ws.rs.core.Response.Status.FORBIDDEN, thrown.getStatus());
  }

  @Test
  void purgeRecycledItem_mapsFolderNotFound() throws Exception {
    doThrow(new FolderNotFoundException()).when(adaptor).purgeRecycledItem(any(), anyString());
    assertThrows(FolderNotFoundException.class, () -> resource.purgeRecycledItem("missing"));
  }

  @Test
  void purgeRecycledItem_mapsConflict() throws Exception {
    doThrow(new WebApplicationException(jakarta.ws.rs.core.Response.Status.CONFLICT))
        .when(adaptor)
        .purgeRecycledItem(any(), anyString());
    WebApplicationException thrown =
        assertThrows(WebApplicationException.class, () -> resource.purgeRecycledItem("1-101-9"));
    assertEquals(409, thrown.getResponse().getStatus());
  }

  @Test
  void deleteFolderItem_successReturnsOk() throws Exception {
    Status result = resource.deleteFolderItem("/Assets/src/item");
    assertEquals(200, result.getStatusCode());
    assertEquals("Ok", result.getMessage());
    verify(adaptor).deleteFolderItem(uriInfo.getBaseUri(), "/Assets/src/item");
  }

  @Test
  void deleteFolderItem_mapsNotAuthorizedToForbidden() throws Exception {
    doThrow(new NotAuthorizedException())
        .when(adaptor)
        .deleteFolderItem(any(), anyString());
    NotAuthorizedException thrown =
        assertThrows(
            NotAuthorizedException.class, () -> resource.deleteFolderItem("/Assets/src/item"));
    assertEquals(jakarta.ws.rs.core.Response.Status.FORBIDDEN, thrown.getStatus());
  }

  @Test
  void deleteFolderItem_mapsFolderNotFound() throws Exception {
    doThrow(new FolderNotFoundException())
        .when(adaptor)
        .deleteFolderItem(any(), anyString());
    assertThrows(
        FolderNotFoundException.class, () -> resource.deleteFolderItem("/Assets/missing"));
  }

  @Test
  void deleteFolderItem_mapsConflict() throws Exception {
    doThrow(new WebApplicationException(jakarta.ws.rs.core.Response.Status.CONFLICT))
        .when(adaptor)
        .deleteFolderItem(any(), anyString());
    WebApplicationException thrown =
        assertThrows(
            WebApplicationException.class, () -> resource.deleteFolderItem("/Assets/src/item"));
    assertEquals(409, thrown.getResponse().getStatus());
  }

  @Test
  void renameFolder_backendExceptionWrapped() throws Exception {
    when(adaptor.renameFolder(any(), anyString(), anyString(), anyString(), anyString()))
        .thenThrow(new BackendException("fail", new Exception("cause")));
    assertThrows(WebApplicationException.class, () -> resource.renameFolder("s/f", "n"));
  }
}
