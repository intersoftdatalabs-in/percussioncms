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

import com.percussion.rest.Status;
import com.percussion.rest.errors.BackendException;
import com.percussion.share.service.exception.PSDataServiceException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.core.StreamingOutput;

/** Asset adaptor interface for shared asset operations. */
public interface IAssetAdaptor {

  /** Fetches a collection of server-shared assets based on the path and/or type. */
  Collection<Asset> getSharedAssets(URI baseURI, String path, String type)
      throws BackendException, PSDataServiceException;

  /** Fetches a single asset by its ID. */
  Asset getSharedAsset(URI baseURI, String id) throws BackendException, PSDataServiceException;

  /** Fetches a single asset by its path. Can handle if filename starts with thumb_. */
  Asset getSharedAssetByPath(URI baseURI, String path);

  /** Deletes a single shared asset by its ID. */
  Status deleteSharedAsset(String id) throws BackendException, PSDataServiceException;

  /** Deletes a single shared asset by its path. */
  Status deleteSharedAssetByPath(String path) throws BackendException;

  /** Creates or updates a shared asset based on its path. */
  Asset createOrUpdateSharedAsset(URI baseURI, String path, Asset asset) throws BackendException;

  /** Update an asset directly given its ID. */
  Asset updateSharedAsset(URI baseURI, String id, Asset asset) throws BackendException;

  /** Creates a new asset at the given path. */
  Asset createSharedAsset(URI baseURI, String path, Asset asset) throws BackendException;

  /** Updates an asset with a binary. */
  Asset uploadBinary(
      URI baseURI,
      String path,
      String assetType,
      InputStream inputStream,
      String uploadFilename,
      String fileMimeType,
      boolean forceCheckOut)
      throws BackendException;

  /** Streams an asset binary to an output stream. */
  StreamingOutput getBinary(String path) throws BackendException;

  /** Renames the shared Asset. */
  Asset renameSharedAsset(URI baseURI, String site, String folder, String name, String newName)
      throws BackendException;

  // --- Reports ---

  List<String> nonADACompliantImagesReport(URI baseUri) throws BackendException;

  List<String> nonADACompliantFilesReport(URI baseUri) throws BackendException;

  List<String> allImagesReport(URI baseUri) throws BackendException;

  List<String> allFilesReport(URI baseUri) throws BackendException;

  // --- Bulk update ---

  Status bulkupdateNonADACompliantImages(URI baseUri, InputStream inputStream);

  Status bulkupdateNonADACompliantFiles(URI baseUri, InputStream inputStream);

  Status bulkupdateImageAssets(URI baseUri, InputStream inputStream);

  Status bulkupdateFileAssets(URI baseUri, InputStream inputStream);

  // --- Workflow ---

  int approveAllAssets(URI baseUri, String folder) throws BackendException;

  int archiveAllAsets(URI baseUri, String folder) throws BackendException;

  int submitForReviewAllAsets(URI baseUri, String folder) throws BackendException;

  // --- Import/Preview ---

  List<String> previewAssetImport(
      URI baseUri,
      String osFolder,
      String assetFolder,
      boolean replace,
      boolean onlyIfDifferent,
      boolean autoApprove)
      throws BackendException;

  void assetImport(
      URI baseUri,
      String osFolder,
      String assetFolder,
      boolean replace,
      boolean onlyIfDifferent,
      boolean autoApprove);
}
