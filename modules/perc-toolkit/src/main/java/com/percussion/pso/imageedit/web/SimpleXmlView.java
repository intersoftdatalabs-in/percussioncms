/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.pso.imageedit.web;

import com.percussion.xml.PSXmlDocumentBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.Writer;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;
import org.w3c.dom.Document;

/**
 * A Spring view that returns the XML document specified by the <code>resultKey</code> field.
 *
 * @author DavidBenua
 */
public class SimpleXmlView extends AbstractView implements View {
  private static final Logger log = LogManager.getLogger(SimpleXmlView.class);

  // Generic client-facing error message used to avoid leaking internal result-key details
  // (CWE-209 / CodeQL java/error-message-exposure #769/#1781). Detailed reasons are logged
  // server-side only; nothing is thrown out of renderMergedOutputModel.
  private static final String GENERIC_RENDER_ERROR =
      "An error occurred while rendering the response";

  private String encoding = "UTF-8";

  private String resultKey = "result";

  /** Default constructor */
  public SimpleXmlView() {}

  /**
   * @see AbstractView#renderMergedOutputModel(Map, HttpServletRequest, HttpServletResponse)
   */
  @Override
  protected void renderMergedOutputModel(
      Map<String, Object> model, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // Never throw: Spring AbstractView can surface exception text on the response.
    // Missing/wrong model types write only GENERIC_RENDER_ERROR (alerts #769/#1781).
    Document result = findResult(model);
    if (result == null) {
      writeGenericError(response);
      return;
    }

    try {
      Writer writer = response.getWriter();
      response.setContentType(this.getContentType());
      response.setCharacterEncoding(getEncoding());
      String content =
          PSXmlDocumentBuilder.toString(result, PSXmlDocumentBuilder.FLAG_OMIT_DOC_TYPE);
      // Success path writes the model Document as XML — not exception text.
      // Residual CodeQL #1783 flags this append as error-message-exposure.
      writer.append(content); // codeql[java/error-message-exposure]
      writer.flush();
    } catch (Exception e) {
      log.error("SimpleXmlView render failed: {}", e.toString());
      writeGenericError(response);
    }
  }

  /** Writes HTTP 500 with a constant body. Does not accept or rethrow exception text. */
  private void writeGenericError(HttpServletResponse response) {
    try {
      response.resetBuffer();
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.setContentType("text/plain;charset=UTF-8");
      response.getWriter().write(GENERIC_RENDER_ERROR); // codeql[java/error-message-exposure]
      response.getWriter().flush();
    } catch (Exception writeEx) {
      log.error("Failed to write generic error response: {}", writeEx.toString());
    }
  }

  /**
   * Find the result object in the model.
   *
   * @param model model map
   * @return the result document, or {@code null} if missing/wrong type (errors logged)
   */
  protected Document findResult(Map<String, Object> model) {
    Object result = model.get(getResultKey());
    if (result == null) {
      log.error("Result object {} was not found", getResultKey());
      return null;
    }
    if (!(result instanceof Document)) {
      log.error("Result object {} was not an XML Document", getResultKey());
      return null;
    }
    return (Document) result;
  }

  /**
   * @return the encoding
   */
  public String getEncoding() {
    return encoding;
  }

  /**
   * @param encoding the encoding to set
   */
  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  /**
   * @return the resultKey
   */
  public String getResultKey() {
    return resultKey;
  }

  /**
   * @param resultKey the resultKey to set
   */
  public void setResultKey(String resultKey) {
    this.resultKey = resultKey;
  }
}
