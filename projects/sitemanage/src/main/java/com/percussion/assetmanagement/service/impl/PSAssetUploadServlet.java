/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.percussion.assetmanagement.service.impl;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.error.PSExceptionUtils;
import com.percussion.security.SecureStringUtils;
import com.percussion.share.service.exception.PSExtractHTMLException;
import com.percussion.sitemanage.importer.theme.PSAssetCreator;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Servlet responsible for uploading a file and creating an asset from it. It will check-in the
 * asset after creation.
 *
 * @author erikserating
 */
@WebServlet("/assetUploadServlet")
@MultipartConfig
public class PSAssetUploadServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  /** Utility method to get file name from HTTP header content-disposition */
  private String getFileName(Part part) {
    String contentDisp = part.getHeader("content-disposition");
    logger.debug("content-disposition header={}", contentDisp);
    String[] tokens = contentDisp.split(";");
    for (String token : tokens) {
      if (token.trim().startsWith("filename")) {
        return token.substring(token.indexOf("=") + 2, token.length() - 1);
      }
    }
    return "";
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String folderpath = request.getParameter("folder");
    if (folderpath == null) folderpath = "/Assets/uploads/";
    String assetType = request.getParameter("assetType");
    if (assetType == null) assetType = "file";
    String selector = request.getParameter("cssSelector");
    String includeElement = request.getParameter("includeElement");
    boolean includeOuterHtml = includeElement != null && includeElement.equals("outerhtml");
    String approve = request.getParameter("approveOnUpload");
    boolean approveOnUpload = approve != null && approve.equalsIgnoreCase("true");

    String fileName = null;
    try {
      PSAsset newAsset = null;
      for (Part part : request.getParts()) {

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

      // Note: this could be updated to return formatted JSON
      // to display successful uploads, etc.
      // See documentation at https://github.com/blueimp/jQuery-File-Upload
      // {"files":[{"thumbnailUrl":"https://jquery-file-upload.appspot.com/image%2Fjpeg/5760114746625974273/oyster.jpeg.80x80.png","name":"oyster.jpeg","url":"https://jquery-file-upload.appspot.com/image%2Fjpeg/5760114746625974273/oyster.jpeg","deleteType":"DELETE","type":"image/jpeg","deleteUrl":"https://jquery-file-upload.appspot.com/image%2Fjpeg/5760114746625974273/oyster.jpeg","size":10420}]}
      if (newAsset != null) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("result", newAsset.getName());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter w = response.getWriter()) {
          w.print(jsonObject.toString());
          w.flush();
        }
      } else {
        // No asset created (e.g. empty part or no file) - return explicit error
        writeErrorResponse(response, fileName, "No valid file was provided for upload.", 400);
      }
    } catch (PSExtractHTMLException caE) {
      handleExtractionError(caE, response, fileName);
    } catch (Exception e) {
      logger.error(PSExceptionUtils.getMessageForLog(e));
      String msg = e.getMessage();
      if (msg == null || msg.trim().isEmpty()) {
        msg = "Upload failed due to an unexpected server error.";
      }
      writeErrorResponse(response, fileName, msg, 500);
    }
  }

  /** Writes a JSON error response for upload failures. */
  private void writeErrorResponse(
      HttpServletResponse response, String fileName, String message, int statusCode)
      throws IOException {
    response.setStatus(statusCode);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    try (PrintWriter w = response.getWriter()) {
      try {
        JSONObject err = new JSONObject();
        err.put("error", message != null ? message : "Upload failed.");
        if (fileName != null && !fileName.isEmpty()) {
          err.put("name", fileName);
        }
        w.print(err.toString());
      } catch (JSONException je) {
        // Extremely unlikely (only if message contains illegal JSON chars that Jettison rejects);
        // fall back to a safe minimal error payload so the client still gets structured feedback.
        logger.warn("Failed to build JSON error response for upload failure", je);
        w.print("{\"error\":\"Upload failed. See server logs for details.\"}");
      }
      w.flush();
    }
  }

  /**
   * Handles extraction errors.
   *
   * @param e the extraction error / exception, assumed not <code>null</code>.
   * @param response the HTTP response, assumed not <code>null</code>.
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
    try {
      writeErrorResponse(response, fileName, msg, HttpServletResponse.SC_BAD_REQUEST);
    } catch (IOException ex) {
      response.setStatus(500);
    }
  }

  /** The logger */
  private static final Logger logger = LogManager.getLogger("PSAssetUploadServlet");

  /** The asset creator */
  private PSAssetCreator assetCreator = new PSAssetCreator();
}
