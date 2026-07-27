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

import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.utils.types.PSPair;
import java.util.List;
import java.util.Map;

/**
 * Downloads files from URLs to local paths, returning results for each download. Designed for use
 * in site/theme import operations.
 */
public interface IPSFileDownloader {

  /**
   * Downloads the given files into local files, as specified in the parameter. Returns a list of
   * pairs so the caller can determine if the download was successful or not, together with the
   * message to use in either case.
   *
   * @param urlToPathMap Map where the key is the URL of the file, and the value is the absolute
   *     local path where the file should be saved. Must not be null.
   * @param context PSSiteImportCtx object, must not be null.
   * @param createAsset true if an asset needs to be created. The resource is downloaded to a temp
   *     file, then deleted. false otherwise.
   * @return List of {@code PSPair<Boolean, String>}. For each value, the first element is true if
   *     the download was successful, and false otherwise. The second element is the success message
   *     or an error message.
   */
  List<PSPair<Boolean, String>> downloadFiles(
      Map<String, String> urlToPathMap, PSSiteImportCtx context, boolean createAsset);

  /**
   * Downloads a given file from the given URL. Writes the file to the destination path if it does
   * not exist. Otherwise, logs an informational message.
   *
   * @param url URL where the file is hosted. Assumed not null.
   * @param destinationPath Destination path for the downloaded file. Assumed not null.
   * @return {@code PSPair<Boolean, String>}. The first element is true if the download was
   *     successful, false otherwise. The second element is the success message or an error message.
   */
  PSPair<Boolean, String> downloadFile(String url, String destinationPath);
}
