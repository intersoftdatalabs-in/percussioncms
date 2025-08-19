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
import com.percussion.share.dao.PSSerializerUtils;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Servlet for importing/exporting templates as XML files. GET: Exports a template as XML. POST:
 * Imports a template for a given site. Sunny Sal says: "Template import/export, now with 100% more
 * Java 11!"
 */
public class PSTemplateServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final int DEFAULT_BUFFER_SIZE = 20480; // 20KB

  private IPSTemplateService templateService;
  private static final Logger log = LogManager.getLogger("PSTemplateInfo");

  public PSTemplateServlet() {
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
      resp.setHeader("Content-Disposition", "attachment; filename=\"" + templateName + "\"");
      resp.getWriter().write(PSSerializerUtils.marshal(templateSelected));
    } catch (Exception ex) {
      throw new ServletException("Failed to find Template with name = " + templateName, ex);
    }
  }

  @Override
  @SuppressWarnings({"unchecked"})
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
          throw new ServletException(
              "The file you attempted to import is not a CM1 template XML file. Choose a valid CM1"
                  + " template XML file for upload");
        }
      } catch (PSExtractHTMLException caE) {
        handleExtractionError(caE, response);
      } catch (Exception e) {
        throw new ServletException(
            "The file you attempted to import is not a CM1 template XML file. Choose a valid CM1"
                + " template XML file for upload",
            e);
      }
    }
  }

  /**
   * Handles extraction errors.
   *
   * @param e the extraction error / exception, assumed not <code>null</code>.
   * @param response the HTTP response, assumed not <code>null</code>.
   * @throws IOException if there is an error occurs during set error and response on the HTTP
   *     response object.
   */
  private void handleExtractionError(PSExtractHTMLException e, HttpServletResponse response)
      throws IOException {
    var errorMsg = e.getMessage();

    if (StringUtils.isBlank(errorMsg) && e.getCause() != null) {
      errorMsg = e.getCause().getMessage();
    } else if (StringUtils.isNotBlank(errorMsg) && e.getCause() != null) {
      errorMsg = errorMsg + " The underlying error is: " + e.getCause().getMessage();
    }
    log.error(errorMsg);

    if (log.isDebugEnabled()) {
      if (e.getCause() != null) log.error("Got extraction error.", e.getCause());
      else log.error("Got extraction error.", e);
    }
    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMsg);
  }

  /**
   * Create the template from the uploaded file.
   *
   * @param siteId the id of the site, assumed not <code>null</code>.
   * @param item the file item, assumed not <code>null</code>. The input stream of this item will be
   *     closed by this method.
   * @return the newly created template, never <code>null</code>.
   * @throws IOException
   * @throws PSExtractHTMLException if fail to create template due to error on extracting content
   */
  private PSTemplate importTemplate(String siteId, FileItem item)
      throws IOException, PSExtractHTMLException {
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
    } catch (Exception e) {
      var msg =
          "The file you attempted to import is not a Page template XML file. Choose a valid CM1"
              + " template XML file for upload";
      var cause = e.getCause();
      if (cause != null && StringUtils.isNotBlank(cause.getLocalizedMessage())) {
        msg = cause.getMessage();
      } else if (StringUtils.isNotBlank(e.getLocalizedMessage())) {
        msg = e.getMessage();
      }
      log.error("Error getting the content from file: {}", msg);
      throw new PSExtractHTMLException(msg, e);
    }
  }

  public IPSTemplateService getTemplateService() {
    return templateService;
  }

  public void setTemplateService(IPSTemplateService templateService) {
    this.templateService = templateService;
  }
}
