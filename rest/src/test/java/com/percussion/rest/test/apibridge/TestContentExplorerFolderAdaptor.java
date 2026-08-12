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

package com.percussion.rest.test.apibridge;

import com.percussion.rest.contentexplorer.folders.AddFolderRequest;
import com.percussion.rest.contentexplorer.folders.AddFolderTreeRequest;
import com.percussion.rest.contentexplorer.folders.FolderChildrenRequest;
import com.percussion.rest.contentexplorer.folders.IContentExplorerFolderAdaptor;
import com.percussion.rest.contentexplorer.folders.RxFolder;
import com.percussion.rest.contentexplorer.folders.RxFolderChildList;
import java.net.URI;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Spring test stub for {@link IContentExplorerFolderAdaptor}. Required for ApplicationContext load
 * after constructor injection on {@code ContentExplorerFoldersResource}.
 */
@Component
@Lazy
public class TestContentExplorerFolderAdaptor implements IContentExplorerFolderAdaptor {

  @Override
  public RxFolder loadByPath(URI baseUri, String path) {
    RxFolder f = new RxFolder();
    f.setPath(path);
    f.setName("stub");
    f.setId("0");
    return f;
  }

  @Override
  public RxFolder loadById(URI baseUri, String id) {
    RxFolder f = new RxFolder();
    f.setId(id);
    f.setName("stub");
    return f;
  }

  @Override
  public RxFolderChildList findChildrenById(URI baseUri, String id) {
    RxFolderChildList list = new RxFolderChildList(List.of());
    list.setParentId(id);
    return list;
  }

  @Override
  public RxFolderChildList findChildrenByPath(URI baseUri, String path) {
    RxFolderChildList list = new RxFolderChildList(List.of());
    list.setParentPath(path);
    return list;
  }

  @Override
  public RxFolderChildList findChildFoldersById(URI baseUri, String id) {
    return findChildrenById(baseUri, id);
  }

  @Override
  public RxFolderChildList findChildFoldersByPath(URI baseUri, String path) {
    return findChildrenByPath(baseUri, path);
  }

  @Override
  public RxFolder addFolder(URI baseUri, AddFolderRequest request) {
    RxFolder f = new RxFolder();
    f.setName(request != null ? request.getName() : null);
    return f;
  }

  @Override
  public List<RxFolder> addFolderTree(URI baseUri, AddFolderTreeRequest request) {
    return List.of();
  }

  @Override
  public RxFolder saveFolder(URI baseUri, String id, RxFolder folder) {
    if (folder != null) {
      folder.setId(id);
    }
    return folder;
  }

  @Override
  public void moveChildren(URI baseUri, FolderChildrenRequest request) {
    // no-op
  }

  @Override
  public void addChildren(URI baseUri, FolderChildrenRequest request) {
    // no-op
  }

  @Override
  public void removeChildren(URI baseUri, FolderChildrenRequest request) {
    // no-op
  }

  @Override
  public void deleteFolder(URI baseUri, String id, boolean purgeItems) {
    // no-op
  }
}
