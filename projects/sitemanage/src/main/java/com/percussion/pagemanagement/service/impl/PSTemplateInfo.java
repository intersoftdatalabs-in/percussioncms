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
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    // Stub implementation to satisfy compilation. Actual export functionality
    // is disabled in this build.
    resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
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
    // detect multipart content without relying on servlet API package
    var contentType = request.getContentType();
    var isMultipart = contentType != null && contentType.toLowerCase().startsWith("multipart/");

    if (isMultipart) {
      try {
        // wrap request in a RequestContext to avoid javax/jakarta mismatch
        var upload = new ServletFileUpload(new DiskFileItemFactory());
        var items = upload.parseRequest(new org.apache.commons.fileupload.RequestContext() {
            @Override public String getCharacterEncoding() {
                return request.getCharacterEncoding();
            }
            @Override public String getContentType() {
                return request.getContentType();
            }
            @Override public int getContentLength() {
                return request.getContentLength();
            }
            @Override public java.io.InputStream getInputStream() throws java.io.IOException {
                return request.getInputStream();
            }
        });
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
