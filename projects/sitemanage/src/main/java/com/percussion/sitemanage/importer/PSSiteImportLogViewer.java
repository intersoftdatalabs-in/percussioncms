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
package com.percussion.sitemanage.importer;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.security.SecureStringUtils;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogObjectType;
import com.percussion.sitemanage.importer.dao.IPSImportLogDao;
import com.percussion.sitemanage.importer.data.PSImportLogEntry;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servlet that returns the content of a specific template's import log.
 *
 * <p>Final so the constructor may call Spring dependency injection with {@code this} without {@code
 * this-escape}.
 */
@Transactional
public final class PSSiteImportLogViewer extends HttpServlet {

  private static final long serialVersionUID = 1L;
  private static final Logger log = LogManager.getLogger(PSSiteImportLogViewer.class);

  public PSSiteImportLogViewer() {
    PSSpringWebApplicationContextUtils.injectDependencies(this);
  }

  /** Gets the log entry for a specific template id and returns the information as a txt file. */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException {
    try (var out = response.getWriter()) {
      response.setContentType("text/plain");
      String outputMsg = null;
      var templateId = request.getParameter("templateId");
      var siteName = request.getParameter("siteName");

      PSSite site = null;
      List<PSImportLogEntry> logs = null;
      String templateName = "";

      if (!isBlank(templateId)) {
        try {
          var sum = templateService.find(templateId);
          if (sum != null) {
            templateName = sum.getName();
            logs = logDao.findAll(templateId, PSLogObjectType.TEMPLATE.name());
          }
        } catch (PSDataServiceException e) {
          log.error(PSExceptionUtils.getMessageForLog(e));
          log.debug(PSExceptionUtils.getDebugMessageForLog(e));
          outputMsg = "No report log found for this template";
          out.write(outputMsg);
          return;
        }
      }
      if (logs != null && !logs.isEmpty()) {
        if (isBlank(siteName)) {
          try {
            siteName = siteMgr.getItemSites(idMapper.getGuid(templateId)).get(0).getName();
            site = siteDao.find(siteName);
          } catch (PSDataServiceException e) {
            log.error(
                "Couldn't load template: {} Error: {}",
                templateName,
                PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            outputMsg = "No report log found for this template";
            out.write(outputMsg);
            return;
          }
        }

        var templateLogEntry = getLatestLogEntry(logs);

        List<Long> pageLogIds = null;
        if (site != null && templateName.equals(site.getTemplateName())) {
          try {
            var itemIds = folderHelper.findItemIdsByPath(site.getFolderPath());
            pageLogIds = logDao.findLogIdsForObjects(itemIds, PSLogObjectType.PAGE.name());
          } catch (Exception e) {
            log.error(
                "Failed to load page import logs for Site: {}, Error: {}",
                siteName,
                PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
          }
        }

        response.setHeader(
            "Content-Disposition",
            "attachment;filename="
                + SecureStringUtils.stripAllLineBreaks(siteName)
                + "-"
                + SecureStringUtils.stripAllLineBreaks(templateName)
                + "-importlog.txt");

        if (templateLogEntry != null) {
          out.println(templateLogEntry.getLogData());
        }

        if (pageLogIds != null && !pageLogIds.isEmpty()) {
          for (var pageLogId : pageLogIds) {
            var pageLog = logDao.findLogEntryById(pageLogId);
            if (pageLog != null) {
              out.println(pageLog.getLogData());
            }
          }
        }
      } else {
        outputMsg = "No report log found for this template";
        out.write(outputMsg);
      }
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  private PSImportLogEntry getLatestLogEntry(List<PSImportLogEntry> logs) {
    logs.sort((log1, log2) -> log1.getLogEntryDate().compareTo(log2.getLogEntryDate()));
    return logs.get(logs.size() - 1);
  }

  /** Call doGet method. */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
    doGet(req, resp);
  }

  // Spring dependency injection setters/getters
  private static IPSImportLogDao logDao;
  private static IPSTemplateService templateService;
  private static IPSiteDao siteDao;
  private static IPSPageService pageService;
  private static IPSSiteManager siteMgr;
  private static IPSIdMapper idMapper;
  private static IPSFolderHelper folderHelper;

  public static IPSImportLogDao getLogDao() {
    return logDao;
  }

  public static void setLogDao(IPSImportLogDao logDao) {
    PSSiteImportLogViewer.logDao = logDao;
  }

  public static IPSTemplateService getTemplateService() {
    return templateService;
  }

  public static void setTemplateService(IPSTemplateService templateService) {
    PSSiteImportLogViewer.templateService = templateService;
  }

  public static IPSiteDao getSiteDao() {
    return siteDao;
  }

  public static void setSiteDao(IPSiteDao siteDao) {
    PSSiteImportLogViewer.siteDao = siteDao;
  }

  public static IPSPageService getPageService() {
    return pageService;
  }

  public static void setPageService(IPSPageService pageService) {
    PSSiteImportLogViewer.pageService = pageService;
  }

  public static IPSSiteManager getSiteMgr() {
    return siteMgr;
  }

  public static void setSiteMgr(IPSSiteManager siteMgr) {
    PSSiteImportLogViewer.siteMgr = siteMgr;
  }

  public static IPSIdMapper getIdMapper() {
    return idMapper;
  }

  public static void setIdMapper(IPSIdMapper idMapper) {
    PSSiteImportLogViewer.idMapper = idMapper;
  }

  public static void setFolderHelper(IPSFolderHelper folderHelper) {
    PSSiteImportLogViewer.folderHelper = folderHelper;
  }
}
