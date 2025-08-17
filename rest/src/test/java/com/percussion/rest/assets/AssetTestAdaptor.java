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
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.core.StreamingOutput;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class AssetTestAdaptor implements IAssetAdaptor {

  @Override
  public Collection<Asset> getSharedAssets(URI baseURI, String path, String type) {
    return null;
  }

  @Override
  public Asset getSharedAsset(URI baseURI, String id) {
    return null;
  }

  @Override
  public Asset getSharedAssetByPath(URI baseURI, String path) {
    return null;
  }

  @Override
  public Status deleteSharedAsset(String id) {
    return null;
  }

  @Override
  public Status deleteSharedAssetByPath(String path) {
    return null;
  }

  @Override
  public Asset createOrUpdateSharedAsset(URI baseURI, String path, Asset asset) {
    return null;
  }

  @Override
  public Asset updateSharedAsset(URI baseURI, String id, Asset asset) {
    return null;
  }

  @Override
  public Asset createSharedAsset(URI baseURI, String path, Asset asset) {
    return null;
  }

  @Override
  public Asset uploadBinary(
      URI baseURI,
      String path,
      String assetType,
      InputStream inputStream,
      String uploadFilename,
      String fileMimeType,
      boolean forceCheckOut) {
    return null;
  }

  @Override
  public StreamingOutput getBinary(String path) {
    return null;
  }

  @Override
  public Asset renameSharedAsset(
      URI baseURI, String site, String folder, String name, String newName) {
    var a = new Asset();
    a.setFolderPath(folder);
    a.setName(newName);
    return a;
  }

  @Override
  public List<String> nonADACompliantImagesReport(URI baseUri) {
    return null;
  }

  @Override
  public List<String> nonADACompliantFilesReport(URI baseUri) {
    return null;
  }

  @Override
  public List<String> allImagesReport(URI baseUri) {
    return null;
  }

  @Override
  public List<String> allFilesReport(URI baseUri) {
    return null;
  }

  @Override
  public Status bulkupdateNonADACompliantImages(URI baseUri, InputStream inputStream) {
    return new Status();
  }

  @Override
  public Status bulkupdateNonADACompliantFiles(URI baseUri, InputStream inputStream) {
    return new Status();
  }

  @Override
  public Status bulkupdateImageAssets(URI baseUri, InputStream inputStream) {
    return new Status();
  }

  @Override
  public Status bulkupdateFileAssets(URI baseUri, InputStream inputStream) {
    return new Status();
  }

  @Override
  public int approveAllAssets(URI baseUri, String folder) {
    return 0;
  }

  @Override
  public List<String> previewAssetImport(
      URI baseUri,
      String osFolder,
      String assetFolder,
      boolean replace,
      boolean onlyIfDifferent,
      boolean autoApprove) {
    return null;
  }

  @Override
  public void assetImport(
      URI baseUri,
      String osFolder,
      String assetFolder,
      boolean replace,
      boolean onlyIfDifferent,
      boolean autoApprove) {
    // No-op for test adaptor
  }

  @Override
  public int archiveAllAsets(URI baseUri, String folder) {
    return 0;
  }

  @Override
  public int submitForReviewAllAsets(URI baseUri, String folder) {
    return 0;
  }
}
