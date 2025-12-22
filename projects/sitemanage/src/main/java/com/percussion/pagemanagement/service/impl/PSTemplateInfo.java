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
package com.percussion.pagemanagement.service.impl;

import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.security.SecureStringUtils;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.share.dao.PSSerializerUtils;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSExtractHTMLException;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Servlet for importing/exporting templates as XML files. GET: Exports a template as XML. POST:
 * Imports a template for a given site.
 *
 * <p>Sunny Sal says: "Template import/export, now with 100% more Java 11!"
 */
public class PSTemplateInfo extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final int DEFAULT_BUFFER_SIZE = 20480; // 20KB
  private static final Logger log = LogManager.getLogger(PSTemplateInfo.class);

  private static IPSTemplateService templateService;

  public PSTemplateInfo() {
    PSSpringWebApplicationContextUtils.injectDependencies(this);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    var templateName = "*";
    var templateId = "";
    var pathInfo = req.getPathInfo();
    PSTemplate templateSelected = null;

    if (pathInfo != null) {
      var path = pathInfo.split("/");
      if (path.length > 2) {
        templateId = path[1];
        templateName = path[2];
      }
    }

    try {
      templateSelected = templateService.exportTemplate(templateId, templateName);
      resp.reset();
      resp.setBufferSize(DEFAULT_BUFFER_SIZE);
      resp.setContentType("text/xml");
      resp.setHeader(
          "Content-Disposition",
          "attachment; filename=\"" + SecureStringUtils.stripAllLineBreaks(templateName) + "\"");
      var ret = PSSerializerUtils.marshal(templateSelected);
      if (ret != null) {
        resp.getWriter().write(ret);
      } else {
        throw new IOException("Unable to export template");
      }
    } catch (Exception ex) {
      log.error(PSExceptionUtils.getMessageForLog(ex));
      log.debug(PSExceptionUtils.getDebugMessageForLog(ex));
      try {
        resp.sendError(500);
      } catch (IOException e) {
        resp.reset();
        resp.setStatus(500);
      }
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String siteId = null;
    var pathInfo = request.getPathInfo();
    if (pathInfo != null) {
      var path = pathInfo.split("/");
      if (path.length > 1) {
        siteId = path[1];
      }
    }

    PSTemplate templateImported = null;
    var isMultipart = ServletFileUpload.isMultipartContent(request);

    if (isMultipart) {
      try {
        var items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
        for (var item : items) {
          if (!item.isFormField()) {
            templateImported = importTemplate(siteId, item);
          }
        }
        if (templateImported != null && templateImported.getName() != null) {
          response.getWriter().print(templateImported.getName());
        } else {
          throw new IOException("Unexpected error while importing template.");
        }
      } catch (Exception e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        try {
          response.sendError(500);
        } catch (IOException e1) {
          response.reset();
          response.setStatus(500);
        }
      }
    }
  }

  /**
   * Imports a template from the uploaded file.
   *
   * @param siteId the site ID, not null
   * @param item the uploaded file item, not null
   * @return the imported template, never null
   * @throws PSExtractHTMLException if extraction fails
   */
  private PSTemplate importTemplate(String siteId, FileItem item) throws PSExtractHTMLException {
    try (var fileInput = item.getInputStream();
        var br = new BufferedReader(new InputStreamReader(fileInput))) {
      var sb = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line).append("\n");
      }
      var validStringXml = sb.toString().trim().replaceFirst("^([\\W]+)<", "<");
      var convertedTemplate = PSSerializerUtils.unmarshal(validStringXml, PSTemplate.class);
      return templateService.importTemplate(convertedTemplate, siteId);
    } catch (PSDataServiceException
        | IPSPathService.PSPathNotFoundServiceException
        | IOException e) {
      throw new PSExtractHTMLException(e);
    }
  }

  public static IPSTemplateService getTemplateService() {
    return templateService;
  }

  public static void setTemplateService(IPSTemplateService templateService) {
    PSTemplateInfo.templateService = templateService;
  }
}
