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

package com.percussion.rest.assets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.percussion.rest.errors.BackendException;
import com.percussion.rest.users.IUserAdaptor;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AssetsTest {

  // QA note for task_1780430034917 (GH#685): verified Flash asset/widget completely removed.
  // Flash.java deleted, no more special flash asset creation (swf now always percFileAsset),
  // perc.flashWidget package dir removed, docs and registry cleaned. See also
  // PSAssetServiceTest for file asset swf test. All references excised per AC.

  @Mock IAssetAdaptor assetAdaptor;

  @Mock IUserAdaptor userAdaptor;

  @Mock UriInfo uriInfo;

  @InjectMocks AssetsResource resource;

  @BeforeEach
  void init() {
    when(uriInfo.getBaseUri()).thenReturn(UriBuilder.fromUri("http://localhost/api").build());
    resource.setUriInfo(uriInfo);
    resource.setAssetAdaptor(assetAdaptor);
    resource.setUserAdaptor(userAdaptor);
  }

  @Test
  void renameAsset_delegatesToAdaptor() throws Exception {
    Asset a = new Asset();
    a.setName("old.png");
    when(assetAdaptor.renameSharedAsset(any(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(a);

    Asset result = resource.renameAsset("Assets/path1/old.png", "new.png");
    assertSame(a, result);
    verify(assetAdaptor)
        .renameSharedAsset(uriInfo.getBaseUri(), "Assets", "path1", "old.png", "new.png");
  }

  @Test
  void renameAsset_backendExceptionWrapped() throws Exception {
    when(assetAdaptor.renameSharedAsset(any(), anyString(), anyString(), anyString(), anyString()))
        .thenThrow(new BackendException("fail", new Exception("cause")));
    assertThrows(
        WebApplicationException.class, () -> resource.renameAsset("Assets/x.png", "y.png"));
  }

  @Test
  void getBinary_usesImageTypeWhenPresent() throws Exception {
    Asset a = new Asset();
    ImageInfo image = new ImageInfo();
    image.setType("image/png");
    a.setImage(image);
    when(assetAdaptor.getBinary(anyString())).thenReturn(out -> {});
    when(assetAdaptor.getSharedAssetByPath(any(), anyString())).thenReturn(a);

    Response r = resource.getBinary("Assets/uploads/banner.png");
    assertEquals("image/png", r.getHeaderString("Content-Type"));
  }

  @Test
  void getBinary_usesThumbnailTypeWhenThumbRequested() throws Exception {
    Asset a = new Asset();
    ImageInfo image = new ImageInfo();
    image.setType("image/png");
    a.setImage(image);
    ImageInfo thumb = new ImageInfo();
    thumb.setType("image/jpeg");
    a.setThumbnail(thumb);
    when(assetAdaptor.getBinary(anyString())).thenReturn(out -> {});
    when(assetAdaptor.getSharedAssetByPath(any(), anyString())).thenReturn(a);

    Response r = resource.getBinary("Assets/thumb_banner.png");
    assertEquals("image/jpeg", r.getHeaderString("Content-Type"));
  }

  @Test
  void getBinary_usesFileTypeWhenFilePresent() throws Exception {
    Asset a = new Asset();
    BinaryFile file = new BinaryFile();
    file.setType("application/pdf");
    a.setFile(file);
    when(assetAdaptor.getBinary(anyString())).thenReturn(out -> {});
    when(assetAdaptor.getSharedAssetByPath(any(), anyString())).thenReturn(a);

    Response r = resource.getBinary("Assets/uploads/report.pdf");
    assertEquals("application/pdf", r.getHeaderString("Content-Type"));
  }
}
