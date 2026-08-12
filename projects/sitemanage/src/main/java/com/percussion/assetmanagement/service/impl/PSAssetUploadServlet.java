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

package com.percussion.assetmanagement.service.impl;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.security.SecureStringUtils;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.share.service.exception.PSExtractHTMLException;
import com.percussion.sitemanage.importer.theme.PSAssetCreator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.PrintWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

/**
 * Servlet responsible for uploading a file and creating an asset from it. It will check-in the
 * asset after creation.
 */
@WebServlet("/assetUploadServlet")
@MultipartConfig
public class PSAssetUploadServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  /** Utility method to get file name from HTTP header content-disposition. */
  private String getFileName(Part part) {
    var contentDisp = part.getHeader("content-disposition");
    logger.debug("content-disposition header={}", contentDisp);
    var tokens = contentDisp.split(";");
    for (var token : tokens) {
      if (token.trim().startsWith("filename")) {
        return token.substring(token.indexOf("=") + 2, token.length() - 1);
      }
    }
    return "";
  }

  // TODO: Remove me @SuppressFBWarnings("XSS_SERVLET")
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    var folderpath = request.getParameter("folder");
    if (folderpath == null) folderpath = "/Assets/uploads/";
    var assetType = request.getParameter("assetType");
    if (assetType == null) assetType = "file";
    var selector = request.getParameter("cssSelector");
    var includeElement = request.getParameter("includeElement");
    var includeOuterHtml = includeElement != null && includeElement.equals("outerhtml");
    var approve = request.getParameter("approveOnUpload");
    var approveOnUpload = approve != null && approve.equalsIgnoreCase("true");

    String fileName = null;
    try {
      PSAsset newAsset = null;
      for (var part : request.getParts()) {
        fileName = SecureStringUtils.sanitizeFileName(getFileName(part));
        if (!StringUtils.isEmpty(fileName)) {
          newAsset =
              assetCreator.createAsset(
                  folderpath,
                  PSAssetCreator.getAssetType(assetType),
                  part.getInputStream(),
                  fileName,
                  selector,
                  includeOuterHtml,
                  approveOnUpload);
        }
      }
      if (newAsset != null) {
        var jsonObject = new JSONObject();
        jsonObject.put("result", newAsset.getName());
        try (PrintWriter out = response.getWriter()) {
          response.setContentType("application/json");
          response.setCharacterEncoding("UTF-8");
          out.print(jsonObject.toString());
          out.flush();
        }
      } else {
        safeWriteErrorResponse(response, fileName, "No valid file was provided for upload.", 400);
      }
    } catch (PSExtractHTMLException caE) {
      handleExtractionError(caE, response, fileName);
    } catch (Exception e) {
      // Log full detail server-side; never return raw e.getMessage() (info disclosure).
      logger.error(PSExceptionUtils.getMessageForLog(e));
      logger.debug(PSExceptionUtils.getDebugMessageForLog(e));
      safeWriteErrorResponse(
          response,
          fileName,
          "Upload failed due to an unexpected server error.",
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Best-effort error write: if the response is already committed or the write fails, only set
   * status 500 without attempting a second body write.
   */
  private void safeWriteErrorResponse(
      HttpServletResponse response, String fileName, String message, int statusCode) {
    try {
      writeErrorResponse(response, fileName, message, statusCode);
    } catch (IOException | IllegalStateException ex) {
      logger.error("Failed to write upload error response", ex);
      if (!response.isCommitted()) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
    }
  }

  /** Writes a JSON error response for upload failures. */
  private void writeErrorResponse(
      HttpServletResponse response, String fileName, String message, int statusCode)
      throws IOException {
    if (response.isCommitted()) {
      logger.warn("Response already committed; cannot write upload error: {}", message);
      return;
    }
    response.setStatus(statusCode);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    try (PrintWriter w = response.getWriter()) {
      var err = new JSONObject();
      try {
        err.put("error", message != null ? message : "Upload failed.");
        if (fileName != null && !fileName.isEmpty()) {
          err.put("name", fileName);
        }
      } catch (org.codehaus.jettison.json.JSONException je) {
        // Fallback plain text if JSON construction fails
        w.print("{\"error\":\"Upload failed.\"}");
        w.flush();
        return;
      }
      w.print(err.toString());
      w.flush();
    }
  }

  /**
   * Handles extraction errors. {@link PSExtractHTMLException} messages are curated product text
   * (e.g. empty/whitespace-only file) and are intentionally returned to the client.
   *
   * @param e the extraction error / exception, assumed not {@code null}.
   * @param response the HTTP response, assumed not {@code null}.
   * @param fileName the name of the file being uploaded (may be null).
   */
  private void handleExtractionError(
      PSExtractHTMLException e, HttpServletResponse response, String fileName) {
    logger.error(PSExceptionUtils.getMessageForLog(e));
    logger.debug(PSExceptionUtils.getDebugMessageForLog(e));
    String msg = e.getMessage();
    if (msg == null || msg.trim().isEmpty()) {
      msg = "Failed to extract content from the uploaded file.";
    }
    safeWriteErrorResponse(response, fileName, msg, HttpServletResponse.SC_BAD_REQUEST);
  }

  private static final Logger logger = LogManager.getLogger("PSAssetUploadServlet");

  /** Runtime collaborator — not part of servlet Java serialization. */
  private final transient PSAssetCreator assetCreator = new PSAssetCreator();
}
