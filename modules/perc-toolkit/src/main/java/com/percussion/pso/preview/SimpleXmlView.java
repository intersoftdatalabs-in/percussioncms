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
package com.percussion.pso.preview;

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
  // (CWE-209 / CodeQL java/error-message-exposure). The detailed reason is always logged
  // server-side before this generic message is thrown.
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
  @SuppressWarnings({"rawtypes"})
  protected void renderMergedOutputModel(
      Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {
    // Catch locally so Spring AbstractView cannot expose exception text on the
    // response (CodeQL java/error-message-exposure #770). Details stay in logs.
    try {
      Document result = findResult(model);

      Writer writer = response.getWriter();
      response.setContentType(this.getContentType());
      response.setCharacterEncoding(getEncoding());
      String content =
          PSXmlDocumentBuilder.toString(result, PSXmlDocumentBuilder.FLAG_OMIT_DOC_TYPE);
      writer.append(content);
      writer.flush();
    } catch (RuntimeException e) {
      log.error("SimpleXmlView render failed: {}", e.toString());
      response.resetBuffer();
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.setContentType("text/plain;charset=UTF-8");
      response.getWriter().write(GENERIC_RENDER_ERROR);
      response.getWriter().flush();
    }
  }

  /**
   * Find the result object in the model.
   *
   * @param model
   * @return the result document.
   */
  @SuppressWarnings({"rawtypes"})
  protected Document findResult(Map model) {
    Object result = model.get(getResultKey());
    if (result == null) {
      log.error("Result object {} was not found", getResultKey());
      throw new RuntimeException(GENERIC_RENDER_ERROR);
    }
    if (!(result instanceof Document)) {
      log.error("Result object {} was not an XML Document", getResultKey());
      throw new RuntimeException(GENERIC_RENDER_ERROR);
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
