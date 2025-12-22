/*
 * Copyright 1999-2025 Percussion Software, Inc.
// REFACTORED: CP-JAVA11
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

import com.percussion.rest.errors.FolderNotFoundException;
import com.percussion.rest.errors.SiteNotFoundException;
import com.percussion.rest.util.Examples;
import java.net.URI;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class FolderTestAdaptor implements IFolderAdaptor {

  private URI baseURI;

  @Override
  public Folder getFolder(URI baseURI, String id) {
    var folder = Examples.SAMPLE_FOLDER;
    folder.setId(id);
    if ("invalidId".equals(id)) {
      throw new FolderNotFoundException();
    }
    return folder;
  }

  @Override
  public Folder getFolder(URI baseURI, String site, String path, String folderName) {
    var folder = Examples.SAMPLE_FOLDER;
    folder.setName(folderName);
    folder.setPath(path);
    folder.setSiteName(site);

    if ("testNotFound".equals(site)) {
      throw new SiteNotFoundException();
    }
    if (path.contains("testNotFound")) {
      throw new FolderNotFoundException();
    }
    if ("testNotFound".equals(folderName)) {
      throw new FolderNotFoundException();
    }
    return folder;
  }

  @Override
  public Folder updateFolder(URI baseURI, Folder folder) {
    return folder;
  }

  @Override
  public void deleteFolder(
      URI baseURI, String siteName, String path, String folderName, boolean includeSubFolders) {
    if ("testNotFound".equals(folderName)) {
      throw new FolderNotFoundException();
    }
  }

  @Override
  public void moveFolderItem(URI baseURI, String itemPath, String targetFolderPath) {
    // No-op for test adaptor
  }

  @Override
  public void moveFolder(URI baseURI, String folderPath, String targetFolderPath) {
    // No-op for test adaptor
  }

  @Override
  public Folder renameFolder(
      URI baseURI, String site, String path, String folderName, String newName) {
    var f = new Folder();
    f.setSiteName(site);
    f.setPath(path);
    f.setName(newName);
    return f;
  }

  @Override
  public void copyFolderItem(URI baseURI, String itemPath, String targetFolderPath) {
    // No-op for test adaptor
  }

  @Override
  public void copyFolder(URI baseURI, String folderPath, String targetFolderPath) {
    // No-op for test adaptor
  }

  @Override
  public void deleteFolderItem(URI baseURI, String itemPath) {
    // No-op for test adaptor
  }
}
