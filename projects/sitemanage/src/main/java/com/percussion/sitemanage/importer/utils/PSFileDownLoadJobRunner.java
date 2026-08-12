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
package com.percussion.sitemanage.importer.utils;

import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.sitemanage.importer.IPSSiteImportLogger;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogEntryType;
import com.percussion.sitemanage.importer.PSSiteImporter;
import com.percussion.sitemanage.importer.helpers.impl.PSImportThemeHelper.LogCategory;
import com.percussion.sitemanage.importer.theme.PSAssetCreator;
import com.percussion.util.PSPurgableTempFile;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.types.PSPair;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.Validate;

/** Executes a file download job, optionally creating an asset. */
public class PSFileDownLoadJobRunner implements Runnable {

  private PSAssetCreator assetCreator = new PSAssetCreator();
  private PSFileDownloadJob job;
  private List<PSPair<Boolean, String>> results = new ArrayList<>();
  private boolean hasCompleted = false;
  private Map<String, Object> requestMap;

  public PSFileDownLoadJobRunner(PSFileDownloadJob job, Map<String, Object> requestMap) {
    this.job = job;
    this.requestMap = requestMap;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof PSFileDownLoadJobRunner) {
      var compareJob = (PSFileDownLoadJobRunner) obj;
      return compareJob.getJob().getFile().equals(this.getJob().getFile())
          && compareJob.getJob().getUrl().equals(this.getJob().getUrl())
          && compareJob.getJob().getCreateAsset().equals(this.getJob().getCreateAsset());
    }
    return false;
  }

  @Override
  public int hashCode() {
    var j = getJob();
    if (j == null) {
      return 0;
    }
    final int prime = 31;
    int result = 1;
    result = prime * result + (j.getFile() == null ? 0 : j.getFile().hashCode());
    result = prime * result + (j.getUrl() == null ? 0 : j.getUrl().hashCode());
    result = prime * result + (j.getCreateAsset() == null ? 0 : j.getCreateAsset().hashCode());
    return result;
  }

  public void setRequestInfo(Map<String, Object> requestInfoMap) {
    if (PSRequestInfo.isInited()) {
      PSRequestInfo.resetRequestInfo();
    }
    PSRequestInfo.initRequestInfo(requestInfoMap);
  }

  @Override
  public void run() {
    setRequestInfo(this.requestMap);
    if (job.getCreateAsset()) {
      results = downloadCreateAsset(job.getUrl(), job.getFile());
    } else {
      results = downloadFile(job.getUrl(), job.getFile());
    }
    hasCompleted = true;
  }

  public List<PSPair<Boolean, String>> getResults() {
    return results;
  }

  public void setResults(List<PSPair<Boolean, String>> results) {
    this.results = results;
  }

  public boolean hasCompleted() {
    return hasCompleted;
  }

  public PSFileDownloadJob getJob() {
    return job;
  }

  public void setJob(PSFileDownloadJob job) {
    this.job = job;
  }

  public List<PSPair<Boolean, String>> downloadFile(String url, String destinationPath) {
    var localResults = new ArrayList<PSPair<Boolean, String>>();
    try {
      var uri = new URI(url);
      var fileUrl = uri.toURL();

      if (url.equals(destinationPath)) {
        localResults.add(
            new PSPair<>(false, "Skipping creation of external resource : " + destinationPath));
        return localResults;
      }
      var file = new File(destinationPath);

      if (doesFileExist(file)) {
        localResults.add(new PSPair<>(true, getWarningMessage(url, destinationPath)));
      }

      if (copyToFile(fileUrl, file)) {
        localResults.add(new PSPair<>(true, getSucessMessage(url, destinationPath)));
      } else {
        localResults.add(new PSPair<>(true, getMissingMessage(url, destinationPath)));
      }
    } catch (Exception e) {
      var destFile = new File(destinationPath);
      localResults.add(new PSPair<>(false, getErrorMessage(url, destFile.getName())));
    }
    return localResults;
  }

  private boolean copyToFile(URL fileUrl, File file) throws IOException {
    boolean returnStatus = false;
    int timeout = PSSiteImporter.getImportTimeout();
    HttpsURLConnection connection = null;
    Exception savedException = null;
    InputStream stream = null;
    try {
      connection = (HttpsURLConnection) fileUrl.openConnection();
      connection.addRequestProperty("User-Agent", "Mozilla");
      connection.connect();
      if (connection.getResponseCode() != HttpURLConnection.HTTP_OK
          && connection.getResponseCode() != HttpURLConnection.HTTP_MOVED_PERM
          && connection.getResponseCode() != HttpURLConnection.HTTP_MOVED_TEMP) {
        returnStatus = false;
      } else {
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        stream = connection.getInputStream();
        FileUtils.copyInputStreamToFile(stream, file);
        returnStatus = true;
      }
    } catch (Exception e) {
      savedException = e;
      throw e;
    } finally {
      if (stream != null) {
        if (savedException != null) {
          try {
            stream.close();
          } catch (Exception e2) {
            savedException.addSuppressed(e2);
          }
        } else {
          stream.close();
        }
      }
      if (connection != null) {
        connection.disconnect();
      }
    }
    return returnStatus;
  }

  /** Downloads a file to a temp file and creates an asset if not already present. */
  public List<PSPair<Boolean, String>> downloadCreateAsset(String url, String destinationPath) {
    var localResults = new ArrayList<PSPair<Boolean, String>>();
    try {
      var uri = new URI(url);
      var fileUrl = uri.toURL();
      boolean assetExist = PSPathUtils.doesItemExist(destinationPath);

      if (assetExist) {
        localResults.add(new PSPair<>(true, getWarningMessage(url, destinationPath)));
      } else {
        var fileExtension = "." + FilenameUtils.getExtension(destinationPath);
        try (var tempImage = new PSPurgableTempFile("tempImage", fileExtension, null)) {
          if (copyToFile(fileUrl, tempImage)) {
            destinationPath = URLDecoder.decode(destinationPath);
            try (InputStream fileInput = new FileInputStream(tempImage)) {
              createAsset(fileInput, destinationPath, this.assetCreator);
            }
            localResults.add(new PSPair<>(true, getSucessMessage(url, destinationPath)));
          } else {
            localResults.add(new PSPair<>(true, getMissingMessage(url, destinationPath)));
          }
        }
      }
      return localResults;
    } catch (Exception e) {
      var destFile = new File(destinationPath);
      localResults.add(new PSPair<>(false, getErrorMessage(url, destFile.getName())));
      return localResults;
    }
  }

  public static synchronized boolean createAsset(
      InputStream fileInput, String destinationPath, PSAssetCreator assetCreator) {
    try {
      if (!PSPathUtils.doesItemExist(destinationPath)) {
        assetCreator.createAssetIfNeeded(fileInput, destinationPath);
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static String getErrorMessage(String url, String destinationPath, Exception ex) {
    return "Failed to download '"
        + url
        + "' to '"
        + destinationPath
        + "'. The underlying error is "
        + ex.getMessage();
  }

  private static String getErrorMessage(String url, String fileName) {
    return fileName + ": Failed to download '" + url + "'";
  }

  private static String getSucessMessage(String url, String destinationPath) {
    return "Successfully downloaded '" + url + "' to '" + destinationPath + "'";
  }

  private static String getWarningMessage(String url, String destinationPath) {
    return "Skip download '" + url + "' to '" + destinationPath + "', as such file already exists.";
  }

  private static String getMissingMessage(String url, String destinationPath) {
    return "Skip download '"
        + url
        + "' to '"
        + destinationPath
        + "', as such file is not downloadable from the server.";
  }

  public static boolean logResults(
      List<PSPair<Boolean, String>> results, IPSSiteImportLogger logger) {
    Validate.notNull(results);
    Validate.notNull(logger);

    boolean success = true;
    if (results.isEmpty() || logger == null) {
      return success;
    }

    var category = LogCategory.DownloadFile.getName();
    for (var result : results) {
      PSLogEntryType entryType = result.getFirst() ? PSLogEntryType.STATUS : PSLogEntryType.ERROR;
      if (!result.getFirst()) {
        success = false;
      }
      logger.appendLogMessage(entryType, category, result.getSecond());
    }
    return success;
  }

  private boolean doesFileExist(File file) {
    return file.exists();
  }
}
