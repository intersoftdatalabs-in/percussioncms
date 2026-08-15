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
    when(uriInfo.getBaseUri()).thenReturn(UriBuilder.fromUri("http://localhost/api").build());
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
  void moveFolder_callsAdaptor() throws Exception {
    MoveFolderItem req = new MoveFolderItem("/a/b", "/a/c");
    Status result = resource.moveFolder(req);
    assertEquals("Moved OK", result.getMessage());
    verify(adaptor).moveFolderItem(uriInfo.getBaseUri(), "/a/b", "/a/c");
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
  void renameFolder_backendExceptionWrapped() throws Exception {
    when(adaptor.renameFolder(any(), anyString(), anyString(), anyString(), anyString()))
        .thenThrow(new BackendException("fail", new Exception("cause")));
    assertThrows(WebApplicationException.class, () -> resource.renameFolder("s/f", "n"));
  }
}
