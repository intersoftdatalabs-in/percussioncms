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

import static com.percussion.share.dao.PSFolderPathUtils.concatPath;
import static com.percussion.share.dao.PSFolderPathUtils.pathSeparator;
import static com.percussion.share.spring.PSSpringWebApplicationContextUtils.getWebApplicationContext;
import static com.percussion.sitemanage.service.IPSSiteSectionMetaDataService.PAGE_CATALOG;
import static com.percussion.sitemanage.service.IPSSiteSectionMetaDataService.SECTION_SYSTEM_FOLDER_NAME;

import com.intsof.percussioncms.auditlog.codes.HttpErrorCodes;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.service.IPSPageCatalogService;
import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.queue.IPSPageImportQueue;
import com.percussion.queue.impl.PSSiteQueue;
import com.percussion.services.assembly.impl.PSReplacementFilter;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.data.PSPageContent;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.error.PSSiteImportException;
import com.percussion.sitemanage.importer.IPSConnectivity;
import com.percussion.sitemanage.importer.IPSSiteImportLogger;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogEntryType;
import com.percussion.sitemanage.importer.PSLink;
import com.percussion.sitemanage.importer.PSSiteImporter;
import com.percussion.sitemanage.importer.theme.PSFileDownloader;
import com.percussion.sitemanage.importer.theme.PSURLConverter;
import com.percussion.sitemanage.importer.utils.PSHtmlRetriever;
import com.percussion.sitemanage.importer.utils.PSLinkExtractor;
import com.percussion.sitesummaryservice.service.IPSSiteImportSummaryService;
import com.percussion.theme.service.IPSThemeService;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component("linkExtractionHelper")
@Lazy
public class PSLinkExtractionHelper extends PSImportHelper {

  private static final String HREF = "href";
  private static final String PERC_MANAGED_ATTR = "perc-managed";
  private static final String CATALOG_FOLDERS =
      pathSeparator() + concatPath(SECTION_SYSTEM_FOLDER_NAME, PAGE_CATALOG);

  /** The default connectivity implementation. A wrapper around Jsoup. */
  protected class PSLinkExtractionHelperConnectivity implements IPSConnectivity {
    Connection miConn;

    public PSLinkExtractionHelperConnectivity(
        String url, boolean ignoreContentType, boolean followRedirects, String userAgent) {
      miConn =
          PSSiteImporter.buildJsoupConnection(url, ignoreContentType, followRedirects, userAgent);
    }

    public Document get() throws IOException {
      PSSiteImporter.URLConnectionProperties properties = null;
      try {
        properties = PSSiteImporter.overrideConnectionProperties();
        return miConn.get();
      } finally {
        PSSiteImporter.restoreConnectionProperties(properties);
      }
    }

    public int getResponseStatusCode() {
      return miConn.response().statusCode();
    }

    public String getResponseUrl() {
      return miConn.response().url().toString();
    }
  }

  public static final String ASSETS_DIR_PREFIX = "/Assets/uploads/";
  public static final String ASSETS_DIR_SUFFIX = "/import/";
  private static final String STATUS_MESSAGE = "extracting links";

  private transient IPSPageCatalogService pageCatalogService;
  private IPSThemeService themeService;
  private IPSPageImportQueue importQueue;
  private IPSIdMapper idMapper;
  private IPSiteDao siteDao;
  private IPSPageImportQueue pageImportQueue;
  private IPSPageDao pageDao;
  private IPSItemWorkflowService itemWorkflowService;
  private IPSFolderHelper folderHelper;

  @Autowired
  public PSLinkExtractionHelper(
      final IPSPageCatalogService pageCatalogService,
      IPSThemeService themeService,
      IPSPageDao pageDao,
      IPSItemWorkflowService itemWorkflowService,
      IPSFolderHelper folderHelper) {
    super();
    this.pageCatalogService = pageCatalogService;
    this.themeService = themeService;
    this.pageDao = pageDao;
    this.itemWorkflowService = itemWorkflowService;
    this.folderHelper = folderHelper;
  }

  protected IPSConnectivity getConnectivity(
      String url, boolean ignoreContentType, boolean followRedirects, String userAgent) {
    return new PSLinkExtractionHelperConnectivity(
        url, ignoreContentType, followRedirects, userAgent);
  }

  public PSSiteQueue getSiteQueue(PSSiteImportCtx context) {
    if (pageImportQueue == null) {
      pageImportQueue = (IPSPageImportQueue) getWebApplicationContext().getBean("pageImportQueue");
    }
    return pageImportQueue.getPageIds(context);
  }

  @Override
  public void process(final PSPageContent pageContent, final PSSiteImportCtx context)
      throws PSSiteImportException, IPSGenericDao.SaveException {
    startTimer();
    final IPSSiteImportLogger log = context.getLogger();

    var themeRootDirectory = getThemeRootDirectory(context);
    var themeRootUrl = getThemeRootUrl(context);
    var site = context.getSite().orElseThrow(() -> new IllegalStateException("Site required"));
    var siteName = site.getName();
    var siteQueue = getSiteQueue(context);
    final var links =
        PSLinkExtractor.getLinksForDocument(
            pageContent.getSourceDocument(), log, siteQueue, site.getBaseUrl().orElse(""));
    final var imageLinks =
        PSLinkExtractor.getImagesForDocument(pageContent.getSourceDocument(), log);

    var summaryStats =
        new HashMap<IPSSiteImportSummaryService.SiteImportSummaryTypeEnum, Integer>();
    imageLinks.forEach(imageLink -> imageLink.getElement().attr(PERC_MANAGED_ATTR, "true"));

    int catalogedCount = 0;
    var filesForDownload = new HashMap<String, String>();
    var downloader = new PSFileDownloader();

    for (var link : links) {
      var resolvedUrlTarget = link.getAbsoluteLink();
      try {
        if (!siteQueue.hasLinkBeenProcessed(resolvedUrlTarget)) {
          var linkForCache =
              PSLink.createLinkWithoutElementReference(
                  link.getLinkPath(),
                  link.getLinkText(),
                  link.getAbsoluteLink(),
                  link.getPageName());
          siteQueue.setProcessedLink(resolvedUrlTarget, linkForCache);

          var conn =
              getConnectivity(
                  link.getAbsoluteLink(), false, true, context.getUserAgent().orElse(""));
          var ret = new PSHtmlRetriever(conn);
          var doc = ret.getHtmlDocument();
          if (doc != null) {
            if (link.getLinkText() != null
                && link.getLinkText().equals(PSLinkExtractor.QUERY_STRING_LINK_TEXT_TOKEN)) {
              link.setLinkText(doc.title());
            }
            resolvedUrlTarget = conn.getResponseUrl();
            var pathToTargetItem = getPathForTargetItem(siteName, link);
            link.getElement().attr(HREF, pathToTargetItem);
            link.getElement().attr(PERC_MANAGED_ATTR, "true");
            if (link.getAbsoluteLink().startsWith(context.getSiteUrl().orElse(""))) {
              catalogPage(context, log, link, conn.getResponseStatusCode(), resolvedUrlTarget);
              catalogedCount++;
            }
          } else {
            var urlConverter =
                getURLConverter(context, log, themeRootDirectory, themeRootUrl, siteName);
            var remoteUrl = getRemoteUrlConverted(link, urlConverter);
            var fullThemePath =
                getCmsFolderPathForImageAssetsSiteName(siteName, urlConverter, remoteUrl);
            filesForDownload.put(resolvedUrlTarget, fullThemePath);

            if (link.getLinkText() != null
                && link.getLinkText().equals(PSLinkExtractor.QUERY_STRING_LINK_TEXT_TOKEN)) {
              link.setLinkText(link.getPageName());
            }
            link.setLinkPath(fullThemePath);
            link.getElement().attr(HREF, fullThemePath);
            link.getElement().attr(PERC_MANAGED_ATTR, "true");
          }
        } else {
          var pathToTargetItem = getPathForTargetItem(siteName, link);
          link.getElement().attr(HREF, pathToTargetItem);
          link.getElement().attr(PERC_MANAGED_ATTR, "true");
        }
      } catch (IOException | PSDataServiceException e) {
        log.appendLogMessage(
            PSLogEntryType.ERROR,
            "Link Extractor",
            link.getAbsoluteLink() + " could not be retrieved.");
      }
    }
    downloader.downloadFiles(filesForDownload, context, true);

    if (catalogedCount > 0 || !links.isEmpty()) {
      summaryStats.put(IPSSiteImportSummaryService.SiteImportSummaryTypeEnum.PAGES, catalogedCount);
      summaryStats.put(
          IPSSiteImportSummaryService.SiteImportSummaryTypeEnum.INTERNALLINKS, links.size());
      if (context.getSummaryService().isPresent() && site.getSiteId().isPresent())
        context.getSummaryService().get().update(site.getSiteId().get().intValue(), summaryStats);
    }
    log.appendLogMessage(
        PSLogEntryType.STATUS, "Link Extractor", "Finished cataloging links for Site: " + siteName);
    endTimer();
  }

  protected String getThemeRootUrl(final PSSiteImportCtx context) {
    return themeService.getThemeRootUrl(
        context.getThemeSummary().map(ts -> ts.getName()).orElse(""));
  }

  protected String getThemeRootDirectory(final PSSiteImportCtx context) {
    return themeService.getThemeRootDirectory(
        context.getThemeSummary().map(ts -> ts.getName()).orElse(""));
  }

  protected PSURLConverter getURLConverter(
      final PSSiteImportCtx context,
      final IPSSiteImportLogger log,
      String themeRootDirectory,
      String themeRootUrl,
      String siteName) {
    return new PSURLConverter(
        context.getSiteUrl().orElse(""), siteName, themeRootDirectory, themeRootUrl, log);
  }

  protected String getCmsFolderPathForImageAssetsSiteName(
      String siteName, PSURLConverter urlConverter, String remoteUrl) throws MalformedURLException {
    return urlConverter.getCmsFolderPathForImageAsset(remoteUrl, siteName);
  }

  protected String getRemoteUrlConverted(PSLink link, PSURLConverter urlConverter) {
    return urlConverter.getFullUrl(link.getAbsoluteLink());
  }

  protected String getPathForTargetItem(String siteName, PSLink link)
      throws PSDataServiceException {
    var pathToTargetItem = getFinderPathForTargetItem(siteName, link.getRelativePathWithFileName());
    if (!PSPathUtils.doesItemExist(pathToTargetItem)) {
      pathToTargetItem = getCatalogedItemPath(siteName, link.getLinkPath(), link.getPageName());
    }
    return pathToTargetItem;
  }

  protected String getCatalogedItemPath(String siteName, String folderPath, String pageName)
      throws PSDataServiceException {
    var site = getSiteDao().findSummary(siteName);
    if (site == null) {
      throw new PSDataServiceException(
          "Unable to find cataloged pages, the specified site was not found: " + siteName);
    }
    var catalogRoot = getCatalogFolderPath(site);
    var fullFolderPath = concatPath(catalogRoot, folderPath);
    return concatPath(fullFolderPath, pageName);
  }

  protected String getFinderPathForTargetItem(String siteName, String targetPathUrl) {
    var targetPath = targetPathUrl.startsWith("/") ? targetPathUrl : "/" + targetPathUrl;
    return PSPathUtils.SITES_FINDER_ROOT + "/" + siteName + targetPath;
  }

  private String getCatalogFolderPath(PSSiteSummary site) {
    return concatPath(site.getFolderPath(), CATALOG_FOLDERS);
  }

  private boolean catalogPage(
      final PSSiteImportCtx context,
      final IPSSiteImportLogger log,
      PSLink link,
      int responseStatusCode,
      String resolvedUrlTarget) {
    // unwrap site context for reuse
    PSSite site = context.getSite().orElseThrow(() -> new IllegalStateException("Site required"));
    String siteName = site.getName();
    boolean isCataloged = false;
    if (responseStatusCode == HttpErrorCodes.HTTP_OK.numericCode()) {
      try {
        if (context.isCanceled()) {
          return false;
        }
        var linkUrlWithoutAnchor = resolvedUrlTarget;
        var potentialUrlAnchor = PSReplacementFilter.getAnchor(linkUrlWithoutAnchor);
        if (potentialUrlAnchor != null && !potentialUrlAnchor.isEmpty())
          linkUrlWithoutAnchor = linkUrlWithoutAnchor.replace(potentialUrlAnchor, "");

        var linkPathWithoutAnchor = link.getLinkPath();
        var potentialPathAnchor = PSReplacementFilter.getAnchor(linkPathWithoutAnchor);
        if (potentialPathAnchor != null && !potentialPathAnchor.isEmpty())
          linkPathWithoutAnchor = linkPathWithoutAnchor.replace(potentialPathAnchor, "");

        evaluateForIndexPage(context, linkPathWithoutAnchor);

        var page =
            pageCatalogService.addCatalogPage(
                siteName,
                link.getPageName(),
                link.getLinkText(),
                linkPathWithoutAnchor,
                linkUrlWithoutAnchor);
        if (page != null) {
          isCataloged = true;
          if (context.isCanceled()) {
            return true;
          }
          var id = ((PSLegacyGuid) getIdMapper().getGuid(page.getId())).getContentId();
          getImportQueue()
              .addCatalogedPageIds(
                  site, context.getUserAgent().orElse(null), Collections.singletonList(id));
        }
      } catch (Exception e) {
        log.appendLogMessage(
            PSLogEntryType.ERROR,
            "Link Extractor",
            "Failed to catalog page: " + link.getAbsoluteLink());
        log.appendLogMessage(
            PSLogEntryType.STATUS,
            "Link Extractor",
            "Failed to catalog page: "
                + link.getAbsoluteLink()
                + ", error was: "
                + e.getLocalizedMessage());
      }
    }
    return isCataloged;
  }

  protected void evaluateForIndexPage(final PSSiteImportCtx context, String linkPathWithoutAnchor)
      throws Exception {
    if (pageCatalogService.pageWithFolderPathExists(
        pageCatalogService.getFullFolderPath(
            linkPathWithoutAnchor, (PSSiteSummary) context.getSite().orElseThrow()))) {
      var pageForMove =
          pageDao.findPageByPath(
              pageCatalogService.getFullFolderPath(
                  linkPathWithoutAnchor, (PSSiteSummary) context.getSite().orElseThrow()));
      itemWorkflowService.checkOut(pageForMove.getId());
      var folderForMove = concatPath(pageForMove.getFolderPath(), pageForMove.getName());
      pageForMove.setName("index-" + pageForMove.getName());
      pageDao.save(pageForMove);
      itemWorkflowService.checkIn(pageForMove.getId());

      var fullPath = concatPath(pageForMove.getFolderPath(), pageForMove.getName());
      var newPageFolderPath = concatPath(folderForMove, pageForMove.getName());
      if (!PSPathUtils.doesItemExist(folderForMove)) folderHelper.createFolder(folderForMove);
      folderHelper.moveItem(folderForMove, fullPath, false);

      pageForMove = pageDao.findPageByPath(newPageFolderPath);
      itemWorkflowService.checkOut(pageForMove.getId());
      pageForMove.setName("index.html");
      pageDao.save(pageForMove);
      itemWorkflowService.checkIn(pageForMove.getId());
    }
  }

  private IPSIdMapper getIdMapper() {
    if (idMapper == null) {
      idMapper = (IPSIdMapper) getWebApplicationContext().getBean("sys_idMapper");
    }
    return idMapper;
  }

  private IPSPageImportQueue getImportQueue() {
    if (importQueue == null) {
      importQueue = (IPSPageImportQueue) getWebApplicationContext().getBean("pageImportQueue");
    }
    return importQueue;
  }

  private IPSiteDao getSiteDao() {
    if (siteDao == null) {
      siteDao = (IPSiteDao) getWebApplicationContext().getBean("siteDao");
    }
    return siteDao;
  }

  @Override
  public void rollback(final PSPageContent pageContent, final PSSiteImportCtx context) {
    // NOOP - this is an optional helper
  }

  @Override
  public String getHelperMessage() {
    return STATUS_MESSAGE;
  }

  public void setPageCatalogService(final IPSPageCatalogService pageCatalogService) {
    this.pageCatalogService = pageCatalogService;
  }
}
