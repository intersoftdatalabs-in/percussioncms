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
package com.percussion.patch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.assetmanagement.service.impl.PSAssetService;
import com.percussion.category.marshaller.PSCategoryMarshaller;
import com.percussion.category.marshaller.PSCategoryUnMarshaller;
import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSExtensionCall;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.install.InstallUtil;
import com.percussion.install.PSLogger;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.impl.PSItemWorkflowService;
import com.percussion.linkmanagement.service.IPSManagedLinkService;
import com.percussion.linkmanagement.service.impl.PSManagedLinkService;
import com.percussion.maintenance.service.IPSMaintenanceManager;
import com.percussion.maintenance.service.IPSMaintenanceProcess;
import com.percussion.maintenance.service.impl.PSMaintenanceManager;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.impl.PSPageService;
import com.percussion.search.PSSearchIndexEventQueue;
import com.percussion.security.PSSecurityProvider;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.PSRequest;
import com.percussion.server.PSServer;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.share.dao.impl.PSIdMapper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.tablefactory.PSJdbcDbmsDef;
import com.percussion.tablefactory.PSJdbcTableFactoryException;
import com.percussion.tablefactory.install.RxLogTables;
import com.percussion.util.PSSqlHelper;
import com.percussion.utils.PSJsoupPreserver;
import com.percussion.utils.io.PathUtils;
import com.percussion.utils.jdbc.PSJdbcUtils;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.timing.PSTimer;
import com.percussion.utils.types.PSPair;
import com.percussion.webservices.PSWebserviceUtils;
import com.percussion.webservices.system.PSSystemWsLocator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import tools.jackson.databind.json.JsonMapper;

/**
 * Maintenance task to save all the assets related to managed links. Sunny Sal says: "Saving assets,
 * one link at a time!"
 *
 * @author robertjohansen
 */
public class PSSaveAssetsMaintenanceProcess
    implements Runnable, IPSMaintenanceProcess, IPSNotificationListener {

  private static final Logger log = LogManager.getLogger(PSSaveAssetsMaintenanceProcess.class);
  static final String MAINT_PROC_NAME = PSSaveAssetsMaintenanceProcess.class.getName();
  private IPSMaintenanceManager maintenanceManager;
  private IPSItemWorkflowService itemWorkflowService;
  private IPSAssetService assetService;
  private IPSManagedLinkService managedLinkService;
  private IPSNotificationService notificationService;
  private IPSPageService pageService;
  private IPSIdMapper idMapper;
  private Connection conn = null;
  private Set<ItemWrapper> assetListSet;
  private Set<ItemWrapper> qualifiedPages;
  private PSJdbcDbmsDef dbmsDef;
  private final String processLinksBase =
      "upgrade" + File.separator + "processedlinks" + File.separator;
  private final String savedLinksBase = processLinksBase + "savedlinks" + File.separator;

  private final String assetsLogFilePath = processLinksBase + "Assets.json";
  private final String pagesLogFilePath = processLinksBase + "Pages.json";
  private final String assetsReadFilePath = savedLinksBase + "Assets.json";
  private final String pagesReadFilePath = savedLinksBase + "Pages.json";

  private boolean coreStarted = false;
  private boolean indexStarted = false;
  private boolean packageStarted = false;
  private volatile boolean hasRun = false;

  public PSSaveAssetsMaintenanceProcess(
      PSMaintenanceManager maintenanceManager,
      PSAssetService assetService,
      PSItemWorkflowService itemWorkflowService,
      PSManagedLinkService managedLinkService,
      PSIdMapper idMapper,
      PSPageService pageService) {
    this.maintenanceManager = maintenanceManager;
    this.assetService = assetService;
    this.itemWorkflowService = itemWorkflowService;
    this.managedLinkService = managedLinkService;
    this.idMapper = idMapper;
    this.pageService = pageService;
    assetListSet = new HashSet<>();
  }

  /** Constructor for unit testing purposes. */
  public PSSaveAssetsMaintenanceProcess(IPSMaintenanceManager maintenanceManager) {
    this.maintenanceManager = maintenanceManager;
    assetListSet = new HashSet<>();
  }

  private void notifyComplete() {
    if (notificationService != null) {
      notificationService.notifyEvent(
          new PSNotificationEvent(EventType.SAVE_ASSETS_PROCESS_COMPLETE, null));
    }
  }

  private void completeMaintWork() {
    if (maintenanceManager != null) {
      maintenanceManager.workCompleted(this);
    }
  }

  private void failMaintWork() {
    if (maintenanceManager != null) {
      maintenanceManager.workFailed(this);
    }
  }

  private void startMaintWork() {
    if (maintenanceManager != null) {
      maintenanceManager.startingWork(this);
    }
  }

  /** Create a connection to the database. */
  public Connection getConnection() {
    try {
      var repprops =
          PSJdbcDbmsDef.loadRxRepositoryProperties(PSServer.getRxDir().getAbsolutePath());
      dbmsDef = new PSJdbcDbmsDef(repprops);
      var connection = RxLogTables.createConnection(repprops);
      log.debug("Connection Made: {}", connection);
      return connection;
    } catch (Exception e) {
      log.warn(e.getMessage(), e);
      return null;
    }
  }

  /** Close the connection to the database. */
  public boolean closeConnection() {
    if (conn != null) {
      try {
        conn.close();
      } catch (SQLException e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        log.warn(PSExceptionUtils.getMessageForLog(e));
        return false;
      }
      conn = null;
    } else {
      log.warn("Connection already closed");
    }
    return true;
  }

  /** Execute a SQL statement against the connection and return the result set. */
  public ResultSet executeSqlStatement(Statement stat, String sqlStat) {
    ResultSet result = null;
    if (conn == null) {
      log.warn("Connection Object not available to execute against");
      return result;
    }
    try {
      result = stat.executeQuery(sqlStat);
    } catch (Exception e) {
      log.error("executeSqlStatement : {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
    return result;
  }

  /** Loads assets from file or database. */
  public Set<ItemWrapper> loadAssets() {
    var logFile = new File(PathUtils.getRxDir(null), assetsLogFilePath);
    var readFile = new File(PathUtils.getRxDir(null), assetsReadFilePath);
    if (readFile.exists()) {
      loadFailedAssetsFromFile(readFile);
    } else if (!logFile.exists()) {
      loadAssetsFromDB();
    } else {
      log.info("Found previous assets file not processing assets.");
      assetListSet = new HashSet<>();
    }
    return assetListSet;
  }

  /**
   * Loads content IDs from database for types that have managed link fields into the asset list.
   */
  public void loadAssetsFromDB() {
    var defMgr = PSItemDefManager.getInstance();
    var allContentTypes = defMgr.getContentTypeNames(-1);
    for (var contentType : allContentTypes) {
      if ("percPage".equalsIgnoreCase(contentType)) {
        continue;
      }
      if (!getManagedLinkFields(contentType).isEmpty()) {
        try {
          var typeId = defMgr.contentTypeNameToId(contentType);
          addTypeAssets(typeId, contentType);
        } catch (PSInvalidContentTypeException e) {
          log.error("Cannot load content type with name {}", contentType);
        }
      }
    }
  }

  private void addTypeAssets(long contentTypeId, String typeName) {
    conn = getConnection();
    Statement rawSelectStat = null;
    ResultSet idresult = null;
    try {
      var CONTENTSTATUS = PSSqlHelper.qualifyTableName("CONTENTSTATUS");
      var typeIdSelect =
          "SELECT CONTENTID FROM " + CONTENTSTATUS + " WHERE CONTENTTYPEID = " + contentTypeId;
      rawSelectStat = conn.createStatement();
      idresult = executeSqlStatement(rawSelectStat, typeIdSelect);
      if (idresult != null) {
        addAssets(getAssetFromResult(idresult, "CONTENTID"));
      }
    } catch (Exception e) {
      log.error("Exception loading assets for type {}", typeName);
    } finally {
      try {
        if (idresult != null) idresult.close();
      } catch (Exception e) {
      }
      try {
        if (rawSelectStat != null) rawSelectStat.close();
      } catch (Exception e) {
      }
      try {
        if (conn != null) conn.close();
      } catch (Exception e) {
      }
    }
    log.info("Finished Loading Assets for type {}", typeName);
  }

  public void loadFailedAssetsFromFile(File f) {
    assetListSet = new HashSet<>();
    var objectMapper = JsonMapper.builder().build();
    try {
      addAssets(
          (Set<ItemWrapper>)
              objectMapper.readValue(
                  f,
                  objectMapper
                      .getTypeFactory()
                      .constructCollectionType(Set.class, ItemWrapper.class)));
      assetListSet.removeIf(
          asset ->
              asset.getStatus() == ItemWrapper.STATUS.SUCCESS
                  || asset.getStatus() == ItemWrapper.STATUS.NOTQUALIFIED);
    } catch (Exception e) {
      log.error("Error Reading Log File : {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  private void loadPagesFromDB() throws SQLException {
    conn = getConnection();
    Statement stat = null;
    ResultSet resultSet = null;
    try {
      var TABLE =
          PSSqlHelper.qualifyTableName(
              "CT_PAGE", getDBDef().getDataBase(), getDBDef().getSchema(), getDBDef().getDriver());
      var stmt =
          "SELECT C.CONTENTID, C.PAGE_SUMMARY FROM "
              + "(SELECT MAX(A.REVISIONID) AS REVISIONID, A.CONTENTID FROM "
              + TABLE
              + " A GROUP BY A.CONTENTID) AS B INNER JOIN "
              + TABLE
              + " C ON "
              + "B.CONTENTID = C.CONTENTID AND B.REVISIONID = C.REVISIONID AND "
              + "C.PAGE_SUMMARY IS NOT NULL";
      stat = conn.createStatement();
      resultSet = executeSqlStatement(stat, stmt);
      qualifiedPages = getQualifiedPages(resultSet);
    } finally {
      try {
        if (resultSet != null) resultSet.close();
      } catch (Exception e) {
      }
      try {
        if (stat != null) stat.close();
      } catch (Exception e) {
      }
      closeConnection();
    }
  }

  private Set<ItemWrapper> getQualifiedPages(ResultSet result) {
    var list = new HashSet<ItemWrapper>();
    try {
      while (result.next()) {
        var id = result.getInt("CONTENTID");
        var sum = result.getString("PAGE_SUMMARY");
        var doc = Jsoup.parseBodyFragment(PSJsoupPreserver.formatPreserveTagsForJSoupParse(sum));
        var anchors =
            doc.select(
                    IPSManagedLinkService.A_HREF
                        + ":not(a["
                        + IPSManagedLinkService.PERC_LINKID_ATTR
                        + "])")
                .select(":not([sys_dependentid])");
        var imgs =
            doc.select(
                    IPSManagedLinkService.IMG_SRC
                        + ":not(img["
                        + IPSManagedLinkService.PERC_LINKID_ATTR
                        + "])")
                .select(":not([sys_dependentid])");
        if ((anchors.size() > 0 || imgs.size() > 0) && qualifyLinkPaths(anchors, imgs)) {
          var page = new ItemWrapper(id, ItemWrapper.STATUS.UNPROCESSED);
          list.add(page);
        }
      }
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
    return list;
  }

  public Set<ItemWrapper> loadAssets(String tableName, String colName) throws SQLException {
    var TABLE =
        PSSqlHelper.qualifyTableName(
            tableName, getDBDef().getDataBase(), getDBDef().getSchema(), getDBDef().getDriver());
    var statement = "SELECT " + TABLE + "." + colName + " FROM " + TABLE;
    var stat = conn.createStatement();
    var result = executeSqlStatement(stat, statement);
    addAssets(getAssetFromResult(result, colName));
    return assetListSet;
  }

  public boolean qualifyAsset(PSAsset asset) {
    var qualified = false;
    PSPair<Boolean, String> prResult;
    var managedFields = getManagedLinkFields(asset.getType());
    for (var field : managedFields) {
      if (asset.getFields().get(field) != null) {
        var assetText = asset.getFields().get(field).toString();
        var newAssetText =
            assetText.replace("<!-- morelink -->", "<span class=\"perc-blog-more-link\"></span>");
        if (!assetText.equals(newAssetText)) {
          qualified = true;
        }
        prResult = processLinks(assetText);
        if (prResult.getFirst()) {
          asset.getFields().put(field, prResult.getSecond());
        }
        if (!qualified) {
          qualified = prResult.getFirst();
        }
      }
    }
    return qualified;
  }

  public List<String> getManagedLinkFields(String type) {
    var managedFields = new ArrayList<String>();
    var defMgr = PSItemDefManager.getInstance();
    try {
      var itemDef = defMgr.getItemDef(type, -1);
      for (var field : itemDef.getFieldSet().getAllFields()) {
        if (isManagedLinkField(field)) {
          managedFields.add(field.getSubmitName());
        }
      }
    } catch (PSInvalidContentTypeException e) {
      throw new IllegalArgumentException("Cannot get type definition " + type, e);
    }
    return managedFields;
  }

  private boolean isManagedLinkField(PSField field) {
    var managedLinkField = false;
    var inputTranslation = field.getInputTranslation();
    if (inputTranslation != null) {
      var translations = inputTranslation.getTranslations();
      if (translations.size() > 0) {
        for (int i = 0; i < translations.size(); i++) {
          var extCall = (PSExtensionCall) translations.get(i);
          if (extCall != null
              && ("sys_manageLinksConverter".equals(extCall.getName())
                  || "sys_manageLinksOnUpdate".equals(extCall.getName()))) {
            managedLinkField = true;
            break;
          }
        }
      }
    }
    return managedLinkField;
  }

  private PSPair<Boolean, String> processLinks(String source) {
    var hasUnmanagedLinks = true;
    source = PSJsoupPreserver.formatPreserveTagsForJSoupParse(source);
    var doc = Jsoup.parseBodyFragment(source);
    var anchors =
        doc.select(
            IPSManagedLinkService.A_HREF
                + ":not(a["
                + IPSManagedLinkService.PERC_LINKID_ATTR
                + "])");
    var imgs =
        doc.select(
            IPSManagedLinkService.IMG_SRC
                + ":not(img["
                + IPSManagedLinkService.PERC_LINKID_ATTR
                + "])");
    // get all anchor links with an href attr and target="_blank" but without the rel attr.
    // A_HREF is already "a[href]", so append attribute filters only (not another "a[...]").
    // Fix ported from v8.1.7 PR #716.
    var targetAnchors =
        doc.select(
            IPSManagedLinkService.A_HREF
                + "[target=\"_blank\"]"
                + ":not([rel=\"noopener noreferrer\"])");
    if (anchors.isEmpty() && imgs.isEmpty() && targetAnchors.isEmpty()) {
      hasUnmanagedLinks = false;
    } else {
      hasUnmanagedLinks = qualifyLinkPaths(anchors, imgs) || !targetAnchors.isEmpty();
    }
    return new PSPair<>(hasUnmanagedLinks, doc.body().html());
  }

  private boolean qualifyLinkPaths(Elements anchors, Elements imgs) {
    for (var anchor : anchors) {
      var sysDependant = anchor.attr("sys_dependentid");
      if (StringUtils.isEmpty(sysDependant)) {
        var path = anchor.attr("href");
        if ((path.startsWith("/Sites/")
            || path.startsWith("/Assets/")
            || path.startsWith("//Sites/")
            || path.startsWith("//Assets/"))) {
          return true;
        }
      }
    }
    for (var img : imgs) {
      var sysDependant = img.attr("sys_dependentid");
      if (StringUtils.isEmpty(sysDependant)) {
        var path = img.attr("src");
        if ((path.startsWith("/Sites/")
            || path.startsWith("/Assets/")
            || path.startsWith("//Sites/")
            || path.startsWith("//Assets/"))) {
          return true;
        }
      }
    }
    return false;
  }

  public PSAsset checkOutAndLoadAsset(int id) throws Exception {
    PSAsset asset = null;
    var locator = new PSLocator(id, -1);
    var guid = idMapper.getString(locator);
    asset = assetService.load(guid);
    if (qualifyAsset(asset)) {
      if (itemWorkflowService.isCheckedOutToSomeoneElse(guid)) {
        itemWorkflowService.forceCheckOut(guid);
      } else {
        itemWorkflowService.checkOut(guid);
      }
      var assetNew = assetService.load(guid);
      assetNew.setFields(asset.getFields());
      asset = assetNew;
    } else {
      asset = null;
    }
    return asset;
  }

  public void saveAsset(PSAsset asset) throws Exception {
    try {
      assetService.save(asset);
    } finally {
      itemWorkflowService.checkIn(asset.getId());
    }
  }

  public Set<ItemWrapper> getAssetFromResult(ResultSet result, String colName) {
    var list = new HashSet<ItemWrapper>();
    try {
      while (result.next()) {
        var id = result.getInt(colName);
        var asset = new ItemWrapper(id, ItemWrapper.STATUS.UNPROCESSED);
        list.add(asset);
      }
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
    return list;
  }

  public PSJdbcDbmsDef getDBDef() {
    return dbmsDef;
  }

  private void addAssets(Set<ItemWrapper> assets) {
    assetListSet.addAll(assets);
  }

  public Set<ItemWrapper> getAssetListSet() {
    return assetListSet;
  }

  public void logAssets() throws IOException {
    var file = new File(PathUtils.getRxDir(null), processLinksBase);
    if (!file.exists()) {
      file.mkdirs();
    }
    log.info("Logging Assets to {}", assetsLogFilePath);
    var objectMapper = JsonMapper.builder().build();
    objectMapper.writeValue(new File(PathUtils.getRxDir(null), assetsLogFilePath), assetListSet);
  }

  private void logPages() throws IOException {
    var file = new File(PathUtils.getRxDir(null), processLinksBase);
    if (!file.exists()) {
      file.mkdirs();
    }
    log.info("Logging Pages to {}", pagesLogFilePath);
    var objectMapper = JsonMapper.builder().build();
    objectMapper.writeValue(new File(PathUtils.getRxDir(null), pagesLogFilePath), qualifiedPages);
  }

  public void processAssets() {
    try {
      log.info("Started asset processing.");
      loadAssets();
      if (!assetListSet.isEmpty()) {
        logAssets();
      }
      int assetCount = 0;
      for (var assetW : assetListSet) {
        try {
          var asset = checkOutAndLoadAsset(assetW.getId());
          if (asset == null) {
            assetW.setProcess(ItemWrapper.STATUS.NOTQUALIFIED);
          } else {
            saveAsset(asset);
            assetW.setProcess(ItemWrapper.STATUS.SUCCESS);
          }
        } catch (Exception e) {
          log.error(
              "Failed to process asset with id: {}  due to : {}",
              assetW.getId(),
              PSExceptionUtils.getMessageForLog(e));
          log.debug(PSExceptionUtils.getDebugMessageForLog(e));
          assetW.setProcess(ItemWrapper.STATUS.FAIL);
        }
        assetCount += 1;
        if (assetCount % 250 == 0) {
          log.info("Processed {} assets out of {}", assetCount, assetListSet.size());
          try {
            logAssets();
          } catch (Exception e) {
            log.warn("Trouble logging assets.", e);
          }
        }
      }
    } catch (Exception e) {
      log.error("Could not run asset fix: {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
    if (!assetListSet.isEmpty()) {
      try {
        logAssets();
      } catch (Exception e) {
        log.error("Failed to complete logging of ids.", e);
      }
    }
    log.info("Completed Asset Fix.");
  }

  @Override
  public String getProcessId() {
    return MAINT_PROC_NAME;
  }

  @Override
  public void run() {
    try {
      var timer = new PSTimer(log);
      var req = PSRequest.getContextForRequest();
      PSRequestInfo.initRequestInfo((Map<String, Object>) null);
      PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_PSREQUEST, req);
      PSWebserviceUtils.setUserName(PSSecurityProvider.INTERNAL_USER_NAME);

      var sysSvc = PSSystemWsLocator.getSystemWebservice();
      sysSvc.switchCommunity("Default");
      log.info(
          "Started processing pages and assets - this may take a while depending on your content."
              + " We suggest a relaxing cup of tea while you wait.");

      checkDuplicateColumn();

      processAssets();
      processPages();

      if (PSCategoryUnMarshaller.createCategoryFileIfNotExisting() == null) {
        var marshaller = new PSCategoryMarshaller();
        marshaller.setCategory(PSCategoryUnMarshaller.getEmptyCategory());
        marshaller.marshal();
      }

      notifyComplete();
      completeMaintWork();
      log.info("Completed processing of pages and assets. Hope you enjoyed your cup of tea.");

      timer.logElapsed("Asset Fix Time elapsed: ");
    } catch (Exception e) {
      log.error(
          "Failed to complete Save Assets Process. To try again either delete"
              + " RXRoot/logs/Assets.json to start over or, Copy RXRoot/logs/Assets.json to"
              + " RXRoot/logs/saveassets/Assets.json to try from point of failure.",
          e);
      failMaintWork();
    }
  }

  private void processPages() {
    log.info("Started pages processing.");
    try {
      loadPages();
      if (!qualifiedPages.isEmpty()) {
        forceSavePages();
        logPages();
      }
    } catch (SQLException e) {
      log.error(
          "Failed to load pages, processing of pages for fixing the summary links will not be"
              + " completed",
          e);
    } catch (Exception e) {
      log.error(
          "Failed to load pages, processing of pages for fixing the summary links will not be"
              + " completed",
          e);
    }
    log.info("Completed pages processing.");
  }

  private void loadPages() throws SQLException, IOException {
    var logFile = new File(pagesLogFilePath);
    var readFile = new File(pagesReadFilePath);
    if (readFile.exists()) {
      loadPagesFromFile(readFile);
    } else if (logFile.exists()) {
      qualifiedPages = new HashSet<>();
      log.info("Found previously processed pages log file, skipping them in this run.");
    } else {
      loadPagesFromDB();
      logPages();
    }
  }

  private void loadPagesFromFile(File readFile) {
    qualifiedPages = new HashSet<>();
    var objectMapper = JsonMapper.builder().build();
    try {
      qualifiedPages.addAll(
          (Set<ItemWrapper>)
              objectMapper.readValue(
                  readFile,
                  objectMapper
                      .getTypeFactory()
                      .constructCollectionType(Set.class, ItemWrapper.class)));
      var it = qualifiedPages.iterator();
      while (it.hasNext()) {
        var page = it.next();
        if (page.getStatus().equals(ItemWrapper.STATUS.SUCCESS)
            || page.getStatus().equals(ItemWrapper.STATUS.NOTQUALIFIED)) {
          it.remove();
        }
      }
    } catch (Exception e) {
      log.error("Error Reading Pages Log File : {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  private void forceSavePages() {
    for (var qpage : qualifiedPages) {
      var locator = new PSLocator(qpage.getId(), -1);
      var guid = idMapper.getString(locator);
      var failed = false;
      try {
        itemWorkflowService.forceCheckOut(guid);
        pageService.save(pageService.load(guid));
      } catch (Exception e) {
        failed = true;
        qpage.setProcess(ItemWrapper.STATUS.FAIL);
        log.error("Failed to load and save the page with ID {}", guid);
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
      try {
        itemWorkflowService.checkIn(guid);
        if (!failed) {
          qpage.setProcess(ItemWrapper.STATUS.SUCCESS);
        }
      } catch (Exception e) {
        log.error("Failed to check in the page after processing with ID:{}", guid);
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
    }
  }

  /** Wrapper of asset so that we can track status and id and serialize as json objects. */
  public static class ItemWrapper {
    private Integer id;
    private STATUS status = STATUS.UNPROCESSED;

    private enum STATUS {
      UNPROCESSED,
      FAIL,
      SUCCESS,
      NOTQUALIFIED
    }

    @JsonCreator
    public ItemWrapper(@JsonProperty("id") Integer id, @JsonProperty("status") STATUS status) {
      this.id = id;
      this.status = status;
    }

    public Integer getId() {
      return id;
    }

    public void setId(Integer id) {
      this.id = id;
    }

    public STATUS getStatus() {
      return status;
    }

    public void setProcess(STATUS status) {
      this.status = status;
    }
  }

  public void setNotificationService(IPSNotificationService notificationService) {
    notificationService.addListener(EventType.CORE_SERVER_POST_INIT, this);
    notificationService.addListener(EventType.SEARCH_INDEX_STATUS_CHANGE, this);
    notificationService.addListener(EventType.STARTUP_PKG_INSTALL_COMPLETE, this);
    this.notificationService = notificationService;
  }

  @Override
  public void notifyEvent(PSNotificationEvent notification) {
    if (hasRun) {
      return;
    }
    if (EventType.CORE_SERVER_POST_INIT == notification.getType()) {
      startMaintWork();
      coreStarted = true;
    }
    if (EventType.STARTUP_PKG_INSTALL_COMPLETE == notification.getType()) {
      packageStarted = true;
    }
    if (EventType.SEARCH_INDEX_STATUS_CHANGE == notification.getType()) {
      var indexQueue = PSSearchIndexEventQueue.getInstance();
      if ("Running".equals(indexQueue.getStatus())) {
        indexStarted = true;
      }
    }
    if (coreStarted && indexStarted && packageStarted) {
      hasRun = true;
      var thread = new Thread(this);
      thread.setDaemon(true);
      thread.start();
    }
  }

  /**
   * Legacy dual-column cleanup for {@code CT_PERCFILEASSET}: when both {@code ITEM_FILE_ATTACHMENT}
   * and {@code ITEM_FILE_ATTACHMENTX} exist (or only the old name remains), rename the old column
   * onto the canonical name.
   *
   * <p><b>Must not</b> drop {@code ITEM_FILE_ATTACHMENT} solely because {@code COUNT(... IS NOT
   * NULL) == 0} — that is true on empty / fresh installs and previously deleted the only real
   * column, breaking {@code psx_cepercFileAsset} with {@code no such column ITEM_FILE_ATTACHMENT}.
   */
  public void checkDuplicateColumn() {
    var qualifyingTableName = PSFileAssetColumnMigration.TABLE;
    var columnNew = PSFileAssetColumnMigration.COLUMN_NEW;
    var columnOld = PSFileAssetColumnMigration.COLUMN_OLD;
    var baseConfigDir = PSServer.getBaseConfigDir();
    log.info(baseConfigDir);
    var rootDir = PSServer.getRxDir().getAbsolutePath();
    if (baseConfigDir.contains("jetty")) {
      rootDir = baseConfigDir.substring(0, baseConfigDir.lastIndexOf("jetty") - 1);
    }
    var propFile =
        java.nio.file.Path.of(rootDir, "rxconfig", "Installer", "rxrepository.properties").toFile();
    log.info(propFile.getAbsolutePath());
    if (!(propFile.exists() && propFile.isFile())) {
      log.error("Unable to connect to the repository datasource file: {}", propFile);
      return;
    }
    try (var in = new FileInputStream(propFile)) {
      var props = new Properties();
      props.load(in);
      var dbmsDef = new PSJdbcDbmsDef(props);
      if (!"".equals(rootDir)) {
        InstallUtil.setRootDir(rootDir);
      }
      var pw = dbmsDef.getPassword();
      var driver = dbmsDef.getDriver();
      var server = dbmsDef.getServer();
      var database = dbmsDef.getDataBase();
      var uid = dbmsDef.getUserId();
      PSLogger.logInfo(
          "Driver : "
              + driver
              + " Server : "
              + server
              + " Database : "
              + database
              + " uid : "
              + uid);
      try (var conn = InstallUtil.createConnection(driver, server, database, uid, pw)) {
        var finalTableName =
            PSSqlHelper.qualifyTableName(
                qualifyingTableName.trim(),
                dbmsDef.getDataBase(),
                dbmsDef.getSchema(),
                dbmsDef.getDriver());
        var schema = dbmsDef.getSchema();
        var hasNew = columnExists(conn, schema, qualifyingTableName, columnNew);
        var hasOld = columnExists(conn, schema, qualifyingTableName, columnOld);
        if (!PSFileAssetColumnMigration.shouldMigrate(hasNew, hasOld)) {
          PSLogger.logInfo(
              "CT_PERCFILEASSET attachment columns OK (new="
                  + hasNew
                  + ", old="
                  + hasOld
                  + "); skip legacy rename.");
          return;
        }
        int nonNullNewCount = 0;
        if (hasNew) {
          var sqlSelect =
              String.format(
                  "SELECT COUNT(*) FROM %s WHERE %s IS NOT NULL ", finalTableName, columnNew);
          PSLogger.logInfo("Executing select statement : " + sqlSelect);
          try (var stmtSelect = conn.createStatement();
              var rs = stmtSelect.executeQuery(sqlSelect)) {
            if (rs.next()) {
              nonNullNewCount = rs.getInt(1);
            }
          }
        }
        try (var stmtAlter = conn.createStatement()) {
          if (PSFileAssetColumnMigration.shouldDropEmptyNewColumn(
              hasNew, hasOld, nonNullNewCount)) {
            var sqlAlterDropColumn =
                String.format("ALTER TABLE %s DROP COLUMN %s ", finalTableName, columnNew);
            PSLogger.logInfo("Dropping empty dual column: " + sqlAlterDropColumn);
            stmtAlter.executeUpdate(sqlAlterDropColumn);
          }
          var sqlAlterChangeName =
              String.format(
                  "ALTER TABLE %s RENAME COLUMN %s TO %s ", finalTableName, columnOld, columnNew);
          if (driver.equalsIgnoreCase(PSJdbcUtils.MYSQL_DRIVER)) {
            sqlAlterChangeName =
                String.format(
                    "ALTER TABLE %s CHANGE %s %s LONGBLOB NULL",
                    finalTableName, columnOld, columnNew);
          } else if (driver.equalsIgnoreCase(PSJdbcUtils.JTDS_DRIVER)
              || driver.equalsIgnoreCase(PSJdbcUtils.MICROSOFT_DRIVER)
              || driver.equalsIgnoreCase(PSJdbcUtils.SPRINTA)) {
            sqlAlterChangeName =
                String.format(
                    "sp_rename '%s.%s', '%s', 'COLUMN' ", finalTableName, columnOld, columnNew);
          } else if (driver.equalsIgnoreCase(PSJdbcUtils.DERBY_DRIVER)) {
            sqlAlterChangeName =
                String.format("RENAME COLUMN %s.%s TO %s ", finalTableName, columnOld, columnNew);
          }
          PSLogger.logInfo("Renaming legacy attachment column: " + sqlAlterChangeName);
          stmtAlter.executeUpdate(sqlAlterChangeName);
        } catch (Exception e) {
          handleException(e);
        }
      } catch (Exception ex) {
        handleException(ex);
      }
    } catch (PSJdbcTableFactoryException | IOException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  /**
   * Whether a column exists on the table (case-insensitive match on name).
   *
   * @param conn open connection
   * @param schema DB schema, may be null
   * @param table un-qualified table name
   * @param column column name
   * @return true if metadata reports the column
   */
  static boolean columnExists(Connection conn, String schema, String table, String column)
      throws SQLException {
    var md = conn.getMetaData();
    // Derby and others often store unquoted identifiers upper-case
    try (var rs = md.getColumns(conn.getCatalog(), schema, table, null)) {
      while (rs.next()) {
        var name = rs.getString("COLUMN_NAME");
        if (name != null && name.equalsIgnoreCase(column)) {
          return true;
        }
      }
    }
    // Retry with upper-case table name if first pass found nothing (driver quirks)
    try (var rs =
        md.getColumns(
            conn.getCatalog(),
            schema != null ? schema.toUpperCase() : null,
            table.toUpperCase(),
            null)) {
      while (rs.next()) {
        var name = rs.getString("COLUMN_NAME");
        if (name != null && name.equalsIgnoreCase(column)) {
          return true;
        }
      }
    }
    return false;
  }

  public void handleException(Exception ex) {
    if (ex.getMessage().contains("ORA-00942") || ex.getMessage().contains("does not exist")) {
      PSLogger.logWarn(ex.getMessage());
    }
  }
}
