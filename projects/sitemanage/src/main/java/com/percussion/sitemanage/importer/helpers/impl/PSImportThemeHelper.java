// REFACTORED: CP-JAVA11
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
package com.percussion.sitemanage.importer.helpers.impl;

import static com.percussion.share.spring.PSSpringWebApplicationContextUtils.getWebApplicationContext;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.sitemanage.data.PSPageContent;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.error.PSSiteImportException;
import com.percussion.sitemanage.importer.theme.IPSFileDownloader;
import com.percussion.sitemanage.importer.theme.PSFileDownloader;
import com.percussion.sitemanage.importer.theme.PSHTMLHeaderImporter;
import com.percussion.sitesummaryservice.service.IPSSiteImportSummaryService;
import com.percussion.theme.service.IPSThemeService;
import com.percussion.utils.types.PSPair;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Helper class that handles the import of site theme files. Sunny Sal says: "A theme without its
 * furniture is just a room!"
 */
@Component("importThemeHelper")
@Lazy
public class PSImportThemeHelper extends PSImportHelper {

  private IPSThemeService themeService;
  private static final Logger log = LogManager.getLogger(PSImportThemeHelper.class);
  private PSHTMLHeaderImporter headerImporter;
  private static final String STATUS_MESSAGE = "importing theme furniture";

  @Autowired
  public PSImportThemeHelper(IPSThemeService themeService) {
    this.themeService = themeService;
  }

  @Override
  public void process(PSPageContent pageContent, PSSiteImportCtx context)
      throws PSSiteImportException {
    startTimer();
    notNull(pageContent);
    notNull(context);

    if (context.isCanceled()) {
      return;
    }
    var summaryService =
        (IPSSiteImportSummaryService)
            getWebApplicationContext().getBean("siteImportSummaryService");
    context.setSummaryService(summaryService);

    var linkPaths = new HashMap<String, String>();
    var scriptPaths = new HashMap<String, String>();
    var resources = new HashMap<String, String>();
    var assets = new HashMap<String, String>();
    var summaryStats =
        new HashMap<IPSSiteImportSummaryService.SiteImportSummaryTypeEnum, Integer>();

    IPSFileDownloader fileDownloader = new PSFileDownloader();

    try {
      var sourceDoc = pageContent.getSourceDocument();
      String baseUrl = null;
      var statusMessagePrefix = context.getStatusMessagePrefix();
      if (statusMessagePrefix != null && statusMessagePrefix.contains("template")) {
        baseUrl = getBaseUrl(context, sourceDoc);
      } else {
        baseUrl = context.getSite().getBaseUrl();
      }
      if (baseUrl.equals("")) {
        baseUrl = context.getSiteUrl();
      }

      var siteName = context.getSite().getName();
      var themeRootDirectory =
          themeService.getThemeRootDirectory(context.getThemeSummary().getName());
      var themeRootUrl = themeService.getThemeRootUrl(context.getThemeSummary().getName());

      headerImporter =
          new PSHTMLHeaderImporter(
              sourceDoc, baseUrl, siteName, themeRootDirectory, themeRootUrl, context.getLogger());

      linkPaths = headerImporter.getLinkPaths();
      removeIfExists(linkPaths);

      scriptPaths = headerImporter.getScriptPaths();
      resources.putAll(scriptPaths);

      resources.putAll(headerImporter.processInlineStyles());
      assets.putAll(headerImporter.processHeaderAndBodyImages());
      assets.putAll(headerImporter.processFlashFiles(context.getSite().getName()));
      resources.putAll(headerImporter.processCssFiles(linkPaths));

      fileDownloader.downloadFiles(resources, context, false);

      var assetResults = fileDownloader.downloadFiles(assets, context, true);
      int assetsCount = (int) assetResults.stream().filter(PSPair::getFirst).count();

      var linkResults = fileDownloader.downloadFiles(linkPaths, context, false);
      int linksCount = (int) linkResults.stream().filter(PSPair::getFirst).count();

      if (linksCount > 0 || assetsCount > 0) {
        summaryStats.put(
            IPSSiteImportSummaryService.SiteImportSummaryTypeEnum.STYLESHEETS, linksCount);
        summaryStats.put(IPSSiteImportSummaryService.SiteImportSummaryTypeEnum.FILES, assetsCount);
        if (context.getSite() != null && context.getSite().getSiteId() != null)
          context
              .getSummaryService()
              .update(context.getSite().getSiteId().intValue(), summaryStats);
        else context.setSummaryStats(summaryStats);
      }
    } catch (Exception e) {
      var msg = "Failed to process jsoup document from url: " + context.getSiteUrl();
      log.warn(msg, e);
    }
    endTimer();
  }

  @Override
  public void rollback(PSPageContent pageContent, PSSiteImportCtx context) {
    // NOOP - this is an optional helper
  }

  /**
   * Helper method to get the base url if it is defined. If it is not present, the site url is used
   * as the base url.
   *
   * @param context The context object containing logger, site data and common information to be
   *     shared among all helpers.
   * @param sourceDoc the source code of the page.
   * @return the base url.
   */
  private String getBaseUrl(PSSiteImportCtx context, Document sourceDoc) {
    Elements bases = sourceDoc.getElementsByTag("base");
    if (bases.size() > 0) {
      String baseUrl = "";
      for (Element b : bases) {
        if (b.hasAttr("href")) {
          baseUrl = b.attr("href");
          break;
        }
      }
      for (Element b : bases) {
        if (b.hasAttr("href")) b.remove();
      }
      return baseUrl;
    } else {
      return context.getSiteUrl();
    }
  }

  /** Categories used by this helper to log content. */
  public enum LogCategory {
    ParseCSS("Parse CSS"),
    ConvertURL("Convert URL"),
    DownloadFile("Download File"),
    ImportHeader("Import Document Header");

    private final String name;

    LogCategory(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  @Override
  public String getHelperMessage() {
    return STATUS_MESSAGE;
  }

  /**
   * Helper method to avoid downloading and processing duplicated css files.
   *
   * @param linkPaths the map of link paths to check.
   */
  private void removeIfExists(Map<String, String> linkPaths) {
    Set<String> cssURLs = new HashSet<>(linkPaths.keySet());
    for (var cssURL : cssURLs) {
      var cssFile = linkPaths.get(cssURL);
      var f = new File(cssFile);
      if (f.exists()) {
        linkPaths.remove(cssURL);
      }
    }
  }
}
