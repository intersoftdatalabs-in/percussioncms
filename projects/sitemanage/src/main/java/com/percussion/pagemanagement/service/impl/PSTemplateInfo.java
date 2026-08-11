// REFACTORED: CP-JAVA11
package com.percussion.pagemanagement.service.impl;

import com.percussion.error.PSExceptionUtils;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.share.dao.PSSerializerUtils;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSExtractHTMLException;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Servlet for importing/exporting templates as XML files. GET: Exports a template as XML. POST:
 * Imports a template for a given site.
 *
 * <p>Final so the constructor may call Spring dependency injection with {@code this} without {@code
 * this-escape}.
 */
public final class PSTemplateInfo extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static final int DEFAULT_BUFFER_SIZE = 20480; // 20KB.

  private static final Logger log = LogManager.getLogger(PSTemplateInfo.class);

  private static IPSTemplateService templateService;

  public PSTemplateInfo() {
    PSSpringWebApplicationContextUtils.injectDependencies(this);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String siteId = null;
    // Get the site Id from the path
    String pathInfo = req.getPathInfo();
    if (pathInfo != null) {
      String[] path = pathInfo.split("/");
      if (path.length > 1) {
        siteId = path[1];
      }
    }

    PSTemplate templateImported = null;
    boolean isMultipart =
        req.getContentType() != null && req.getContentType().startsWith("multipart/form-data");

    if (isMultipart) {
      try {
        // Use Jakarta Servlet native multipart support (no need for Commons FileUpload)
        for (Part part : req.getParts()) {
          if (!part.getName().startsWith("form")) {
            templateImported = importTemplate(siteId, part);
            break;
          }
        }
        if (templateImported != null && templateImported.getName() != null) {
          resp.getWriter().print(templateImported.getName());
        } else {
          throw new IOException("Unexpected error while importing template.");
        }
      } catch (Exception e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        try {
          resp.sendError(500);
        } catch (IOException e1) {
          resp.reset();
          resp.setStatus(500);
        }
      }
    }
  }

  /**
   * Imports a template from the uploaded file.
   *
   * @param siteId the site ID, not null
   * @param part the uploaded file part, not null
   * @return the imported template, never null
   * @throws PSExtractHTMLException if extraction fails
   */
  private PSTemplate importTemplate(String siteId, Part part) throws PSExtractHTMLException {
    try (var fileInput = part.getInputStream();
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
