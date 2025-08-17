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
package com.percussion.sitemanage.importer.theme;

import static org.apache.commons.lang.Validate.notNull;

import com.percussion.server.PSRequest;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.importer.utils.PSAsyncFileDownload;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.types.PSPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Downloader class that will be used to get files from a URL and create a local copy. */
public class PSFileDownloader implements IPSFileDownloader {

  private static final Set<String> multiThreadSync = new HashSet<>();

  private enum Operation {
    DELETE,
    CHECK
  }

  public static synchronized boolean checkOperation(Operation operation, String item) {
    if (operation == Operation.DELETE) {
      multiThreadSync.remove(item);
      return true;
    }
    var result = !multiThreadSync.contains(item);
    if (result) {
      multiThreadSync.add(item);
    }
    return result;
  }

  @Override
  public List<PSPair<Boolean, String>> downloadFiles(
      Map<String, String> urlToPathMap, PSSiteImportCtx context, boolean createAsset) {
    var downloaded = new ArrayList<PSPair<Boolean, String>>();
    if (!urlToPathMap.isEmpty()) {
      notNull(urlToPathMap);

      var urls = urlToPathMap.keySet();
      final var requestInfoMap = PSRequestInfo.copyRequestInfoMap();
      var request = (PSRequest) requestInfoMap.get(PSRequestInfo.KEY_PSREQUEST);
      requestInfoMap.put(PSRequestInfo.KEY_PSREQUEST, request.cloneRequest());

      var downloader = new PSAsyncFileDownload(requestInfoMap);

      for (var url : urls) {
        var filePath = urlToPathMap.get(url);
        if (checkOperation(Operation.CHECK, url)) {
          downloader.addDownload(filePath, url, createAsset);
        }
      }
      downloader.download();
      downloaded = downloader.getResults();

      for (var url : urls) {
        checkOperation(Operation.DELETE, url);
      }
    }
    return downloaded;
  }

  @Override
  public PSPair<Boolean, String> downloadFile(String url, String destination) {
    var downloads = new HashMap<String, String>();
    downloads.put(url, destination);
    var downloaded = downloadFiles(downloads, null, false);
    return downloaded.get(0);
  }
}
