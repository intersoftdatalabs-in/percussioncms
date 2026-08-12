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

import java.net.URI;
import java.util.List;

/**
 * Adaptor for content-explorer folder operations (#3073 / parent #3054).
 *
 * <p>HTTP lives on {@link ContentExplorerFoldersResource}; production impl is sitemanage apibridge
 * wrapping classic {@code IPSContentWs} folder methods (same domain path as SOAP content folder
 * ops).
 *
 * <p><strong>Out of scope:</strong> Explorer WebUI dual-run / client switch (#3074); CM1 site
 * section semantics on {@code FoldersResource}.
 */
public interface IContentExplorerFolderAdaptor {

  /** Load folder by fully qualified RX path (normalized in adaptor). */
  RxFolder loadByPath(URI baseUri, String path);

  /** Load folder by guid / content id string. */
  RxFolder loadById(URI baseUri, String id);

  /** Direct children (items + folders) by parent id. */
  RxFolderChildList findChildrenById(URI baseUri, String id);

  /** Direct children by RX path ({@code /} returns root Folders + Sites). */
  RxFolderChildList findChildrenByPath(URI baseUri, String path);

  /** Folder-only direct children by parent id. */
  RxFolderChildList findChildFoldersById(URI baseUri, String id);

  /** Folder-only direct children by RX path. */
  RxFolderChildList findChildFoldersByPath(URI baseUri, String path);

  /** Create a single folder under an existing parent. */
  RxFolder addFolder(URI baseUri, AddFolderRequest request);

  /** Create missing segments of a fully qualified path. */
  List<RxFolder> addFolderTree(URI baseUri, AddFolderTreeRequest request);

  /**
   * Save an existing folder (name, description, community, locale, properties). Loads by id from
   * path param; body supplies fields to apply.
   */
  RxFolder saveFolder(URI baseUri, String id, RxFolder folder);

  /** Move children from source to target folder. */
  void moveChildren(URI baseUri, FolderChildrenRequest request);

  /** Add children to a parent folder. */
  void addChildren(URI baseUri, FolderChildrenRequest request);

  /** Remove children from a parent folder (optional purge). */
  void removeChildren(URI baseUri, FolderChildrenRequest request);

  /** Delete folder recursively; optional purge of child items. */
  void deleteFolder(URI baseUri, String id, boolean purgeItems);
}
