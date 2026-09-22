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

// REFACTORED: CP-JAVA11

package com.percussion.rest.folders;

import com.percussion.rest.errors.BackendException;
import java.net.URI;

/** Adaptor interface for Folder operations. Sunny Sal: "Adaptor pattern FTW, boss!" */
public interface IFolderAdaptor {

  /** Gets a folder by site, path, and folder name. */
  Folder getFolder(URI baseURI, String site, String path, String folderName)
      throws BackendException;

  /** Updates or creates a folder. */
  Folder updateFolder(URI baseURI, Folder folder) throws BackendException;

  /** Deletes a folder. */
  void deleteFolder(
      URI baseURI, String siteName, String path, String folderName, boolean includeSubFolders)
      throws BackendException;

  /** Gets a folder by id. */
  Folder getFolder(URI baseURI, String id) throws BackendException;

  /** Moves a folder item. */
  void moveFolderItem(URI baseURI, String itemPath, String targetFolderPath)
      throws BackendException;

  /** Moves a folder. */
  void moveFolder(URI baseURI, String folderPath, String targetFolderPath) throws BackendException;

  /** Renames a folder. */
  Folder renameFolder(URI baseURI, String site, String path, String folderName, String newName)
      throws BackendException;

  /**
   * Creates a folder under {@code parentPath} with {@code name} (#4637).
   *
   * <p>Implementations map missing parent to {@link
   * com.percussion.rest.errors.FolderNotFoundException} (HTTP 404), non-admin callers to {@link
   * com.percussion.rest.errors.NotAuthorizedException} (HTTP 403), and name-in-use / invalid
   * destination to HTTP 409.
   */
  Folder createFolder(URI baseURI, String parentPath, String name) throws BackendException;

  /** Copies a folder item. */
  void copyFolderItem(URI baseURI, String itemPath, String targetFolderPath) throws Exception;

  /** Copies a folder. */
  void copyFolder(URI baseURI, String folderPath, String targetFolderPath) throws Exception;

  /** Deletes a folder item. */
  void deleteFolderItem(URI baseURI, String itemPath) throws BackendException;

  /**
   * Restores a recycled folder or item by GUID to its original parent (#4700).
   *
   * <p>Implementations map missing recycled items to {@link
   * com.percussion.rest.errors.FolderNotFoundException} (HTTP 404), non-admin callers to {@link
   * com.percussion.rest.errors.NotAuthorizedException} (HTTP 403), and destination name-in-use to
   * HTTP 409.
   */
  void restoreRecycledItem(URI baseURI, String guid) throws BackendException;

  /**
   * Renames a non-folder item (page, file, or asset). Folder rename stays on {@link
   * #renameFolder}.
   */
  void renameFolderItem(URI baseURI, String itemPath, String newName) throws BackendException;

  /**
   * Loads listing name and display title for a non-folder item (#4701).
   *
   * <p>Missing items map to {@link com.percussion.rest.errors.FolderNotFoundException} (HTTP 404),
   * non-admin callers to {@link com.percussion.rest.errors.NotAuthorizedException} (HTTP 403).
   */
  ItemProperties getItemProperties(URI baseURI, String itemPath) throws BackendException;

  /**
   * Persists listing name and optional display title for a non-folder item (#4701).
   *
   * <p>Blank {@code name} is HTTP 400. Missing items map to 404; non-admin callers to 403; folder
   * selected, name in use, or locked to HTTP 409.
   */
  ItemProperties saveItemProperties(URI baseURI, String itemPath, String name, String displayTitle)
      throws BackendException;
}
