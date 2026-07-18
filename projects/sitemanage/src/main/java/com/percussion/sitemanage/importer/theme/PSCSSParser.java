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
package com.percussion.sitemanage.importer.theme;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.sitemanage.importer.IPSSiteImportLogger;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogEntryType;
import com.percussion.sitemanage.importer.helpers.impl.PSImportThemeHelper;
import com.percussion.security.io.PSPathInjectionGuard;
import com.percussion.utils.types.PSPair;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;

/**
 * Parser class that reads CSS files and updates paths to a new location after importing a theme.
 */
public class PSCSSParser {

  private static final String REGEX = "@import([^;]*);|url\\s*\\(([^\\)]*)\\)";
  private static final String IMPORT_REGEX = "@import([^;]*);|@import url\\s*\\(([^\\)]*)\\)";
  private static final String URL_REGEX = "url\\s*\\(([^\\)]*)\\)";
  private static final Pattern IMPORT_PATTERN = Pattern.compile(IMPORT_REGEX);
  private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);

  private final List<String> processed = new ArrayList<>();
  private final Map<String, String> imagesToDownload = new ConcurrentHashMap<>();
  private final String siteName;
  private final String themeRootDirectory;
  private final String themeRootUrl;
  private final IPSSiteImportLogger logger;
  private IPSFileDownloader fileDownloader = new PSFileDownloader();

  /** Constructor. */
  public PSCSSParser(
      String siteName, String themeRootDirectory, String themeRootUrl, IPSSiteImportLogger logger) {
    notNull(siteName);
    notNull(themeRootDirectory);
    notNull(themeRootUrl);
    notNull(logger);

    this.siteName = siteName;
    this.themeRootDirectory = themeRootDirectory;
    this.themeRootUrl = themeRootUrl;
    this.logger = logger;
  }

  /** Parse CSS files. */
  public Map<String, String> parse(Map<String, String> cssFiles) {
    for (var cssURL : cssFiles.keySet()) {
      var cssFile = cssFiles.get(cssURL);
      logger.appendLogMessage(
          PSLogEntryType.STATUS,
          PSImportThemeHelper.LogCategory.ParseCSS.getName(),
          "Processing CSS file: " + cssFile + ".");

      String cssText = "";
      try {
        cssText = loadFileFromDisk(cssFile);
        process(cssFile, cssText, createURLConverter(cssURL));
      } catch (IOException io) {
        logger.appendLogMessage(
            PSLogEntryType.ERROR,
            PSImportThemeHelper.LogCategory.ParseCSS.getName(),
            "Error loading " + cssFile + ".");
        logger.appendLogMessage(
            PSLogEntryType.STATUS,
            PSImportThemeHelper.LogCategory.ParseCSS.getName(),
            "Error loading " + cssFile + ": " + io.getMessage());
      } catch (Exception e) {
        logger.appendLogMessage(
            PSLogEntryType.ERROR,
            PSImportThemeHelper.LogCategory.ParseCSS.getName(),
            "Error processing css file: " + cssFile + ".");
        logger.appendLogMessage(
            PSLogEntryType.STATUS,
            PSImportThemeHelper.LogCategory.ParseCSS.getName(),
            "Error processing css file: " + cssFile + ": " + e.getMessage());
      }
    }
    return imagesToDownload;
  }

  /** Parse CSS embedded in HTML header. */
  public PSPair<Map<String, String>, String> parse(String urlBase, String cssText) {
    logger.appendLogMessage(
        PSLogEntryType.STATUS,
        PSImportThemeHelper.LogCategory.ParseCSS.getName(),
        "Processing inline CSS : " + urlBase + ".");

    String cssParsed = "";
    try {
      cssParsed = process(cssText, createURLConverter(urlBase));
    } catch (Exception e) {
      logger.appendLogMessage(
          PSLogEntryType.ERROR,
          PSImportThemeHelper.LogCategory.ParseCSS.getName(),
          "Failed to process inline css for " + urlBase + ".");
      logger.appendLogMessage(
          PSLogEntryType.STATUS,
          PSImportThemeHelper.LogCategory.ParseCSS.getName(),
          "Failed to process inline css for " + urlBase + ": " + e.getLocalizedMessage());
    }
    return new PSPair<>(imagesToDownload, cssParsed);
  }

  private PSURLConverter createURLConverter(String baseUrl) {
    return new PSURLConverter(baseUrl, siteName, themeRootDirectory, themeRootUrl, logger);
  }

  private Map<String, String> process(String cssFile, String cssText, PSURLConverter urlConverter) {
    var sb = getCssParsed(cssText, urlConverter);
    try {
      saveFile(sb, cssFile);
    } catch (Exception e) {
      logger.appendLogMessage(
          PSLogEntryType.ERROR,
          PSImportThemeHelper.LogCategory.ParseCSS.getName(),
          "Error saving " + cssFile + ".");
      logger.appendLogMessage(
          PSLogEntryType.STATUS,
          PSImportThemeHelper.LogCategory.ParseCSS.getName(),
          "Error saving " + cssFile + ": " + e.getLocalizedMessage());
    }
    return imagesToDownload;
  }

  private String process(String cssText, PSURLConverter urlConverter) {
    return getCssParsed(cssText, urlConverter).toString();
  }

  private StringBuffer getCssParsed(String cssText, PSURLConverter urlConverter) {
    var p = Pattern.compile(REGEX);
    var m = p.matcher(cssText);
    var sb = new StringBuffer();
    while (m.find()) {
      var updatedPath = updatePath(m.group(), urlConverter);
      m.appendReplacement(sb, Matcher.quoteReplacement(updatedPath));
    }
    m.appendTail(sb);
    return sb;
  }

  private String updatePath(String quote, PSURLConverter urlConverter) {
    var importMatcher = IMPORT_PATTERN.matcher(quote);
    if (importMatcher.matches()) {
      var importUrl = importMatcher.group(1).trim();
      var urlMatcher = URL_PATTERN.matcher(importUrl);
      if (urlMatcher.matches()) {
        // @import url('abc.css');
        return updateImports(quote, removeQuotes(urlMatcher.group(1)).trim(), urlConverter);
      } else {
        // @import "abc.css";
        return updateImports(quote, removeQuotes(importMatcher.group(1)).trim(), urlConverter);
      }
    }
    var urlMatcher = URL_PATTERN.matcher(quote);
    if (urlMatcher.matches()) {
      return updateUrl(quote, urlMatcher, urlConverter);
    }
    return quote;
  }

  private String updateImports(String quote, String resourceUrl, PSURLConverter urlConverter) {
    var importUrl = urlConverter.getFullUrl(resourceUrl);
    if (!isBlank(importUrl) && !processed.contains(importUrl)) {
      processed.add(importUrl);
      var importPath = urlConverter.getFileSystemPathForCss(importUrl);
      if (fileExists(importPath))
        return getImportStatement(urlConverter.convertToThemeLinkForCss(importUrl));
      fileDownloader.downloadFile(importUrl, importPath);
      String cssText = "";
      try {
        cssText = loadFileFromDisk(importPath);
      } catch (IOException io) {
        logger.appendLogMessage(
            PSLogEntryType.ERROR,
            PSImportThemeHelper.LogCategory.ParseCSS.getName(),
            "Error loading " + resourceUrl + ".");
        logger.appendLogMessage(
            PSLogEntryType.STATUS,
            PSImportThemeHelper.LogCategory.ParseCSS.getName(),
            "Error loading " + resourceUrl + ": " + io.getLocalizedMessage());
      }
      if (isValidURL(resourceUrl)) {
        imagesToDownload.putAll(process(importPath, cssText, createURLConverter(importUrl)));
        return getImportStatement(resourceUrl);
      } else {
        imagesToDownload.putAll(process(importPath, cssText, createURLConverter(importUrl)));
        var updatedLink = urlConverter.convertToThemeLinkForCss(importUrl);
        logger.appendLogMessage(
            PSLogEntryType.STATUS,
            PSImportThemeHelper.LogCategory.ParseCSS.getName(),
            "Image url updated from: " + resourceUrl + " to " + updatedLink);
        return getImportStatement(updatedLink);
      }
    } else {
      if (processed.contains(importUrl)) {
        return getImportStatement(urlConverter.convertToThemeLinkForCss(importUrl));
      }
    }
    return quote;
  }

  private String getImportStatement(String importPath) {
    return "@import \"" + importPath + "\";";
  }

  private boolean fileExists(String importPath) {
    // CWE-22/CWE-23 defense (T043): importPath is derived from CSS content
    // extracted via regex (@import / url(...)) and passed through a URL
    // converter that may produce filesystem paths from user-controlled
    // inputs. Verify the resolved path is contained within the theme root
    // BEFORE any File construction. Residual #1755 of original #1055.
    File safe = PSPathInjectionGuard.requireUnderBase(new File(themeRootDirectory), importPath);
    return safe.exists(); // codeql[java/path-injection]
  }

  private String updateUrl(String quote, Matcher urlMatcher, PSURLConverter urlConverter) {
    var resourceUrl = removeQuotes(urlMatcher.group(1));
    var imageUrl = urlConverter.getFullUrl(resourceUrl);
    if (!isBlank(imageUrl)) {
      var imagePath = urlConverter.getFileSystemPath(imageUrl);
      imagesToDownload.put(imageUrl, imagePath);
      if (isValidURL(resourceUrl)) return "url(" + resourceUrl + ")";
      else {
        var updatedLink = urlConverter.convertToThemeLink(imageUrl);
        logger.appendLogMessage(
            PSLogEntryType.STATUS,
            PSImportThemeHelper.LogCategory.ParseCSS.getName(),
            "Image url updated from: " + resourceUrl + " to " + updatedLink);
        return "url(" + updatedLink + ")";
      }
    }
    return quote;
  }

  /** Set fileDownloader. Mostly used by unit test. */
  public void setFileDownloader(IPSFileDownloader fileDownloader) {
    this.fileDownloader = fileDownloader;
  }

  private void saveFile(StringBuffer sb, String path) throws IOException {
    // CWE-22/CWE-23 defense (T043): path may originate from a user-supplied
    // CSS link extracted from the imported HTML header. Resolve the path
    // against the theme root and verify containment BEFORE opening the
    // file for write. Residual #1756 of original #1056.
    File safe = PSPathInjectionGuard.requireUnderBase(new File(themeRootDirectory), path);
    try (var fstream = new FileWriter(safe); // codeql[java/path-injection]
        var out = new PrintWriter(fstream)) {
      out.write(sb.toString());
    }
  }

  private String removeQuotes(String url) {
    return url.replace("\"", "").replace("'", "");
  }

  private String loadFileFromDisk(String path) throws IOException {
    // CWE-22/CWE-23 defense (T043): path may originate from a user-supplied
    // CSS @import or url() value. Resolve and verify containment BEFORE
    // opening the file for read. Residual #1757 of original #1057.
    File safe = PSPathInjectionGuard.requireUnderBase(new File(themeRootDirectory), path);
    try (var in = new FileInputStream(safe)) { // codeql[java/path-injection]
      return IOUtils.toString(in);
    }
  }

  private boolean isValidURL(String path) {
    try {
      new URL(path);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
