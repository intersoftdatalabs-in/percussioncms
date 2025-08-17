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

package com.percussion.server;

import com.percussion.cms.objectstore.PSUserInfo;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSResultDocumentProcessor;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import java.io.File;
import java.util.Objects;
import java.util.Optional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This extension adds the server configuration parameters to the result document. All parameters
 * are added as last child elements of the specified element by name via first parameter. Element
 * name is optional and if not specified the root element of the result document is considered.
 *
 * <p>Note: Only user session timeout is being added now. This class needs to be modified if more
 * parameters are required.
 */
public final class PSAddServerConfigParams implements IPSResultDocumentProcessor {
  /**
   * Name of the element for the user session timeout in seconds. This will be the last child
   * element of the element whose name is specified via first parameter (optional) to the extension.
   * If not specified, the element is added as last child of the root element of the document.
   */
  public static final String ELEM_SESSIONTIMEOUT = PSUserInfo.XML_ELEM_SESSIONTIMEOUT;

  /**
   * Required by the interface. This exit never modifies the stylesheet.
   *
   * @see IPSResultDocumentProcessor#canModifyStyleSheet()
   */
  @Override
  public boolean canModifyStyleSheet() {
    return false;
  }

  /**
   * Initializes this extension.
   *
   * @param extensionDef the extension definition, may be {@code null}
   * @param file the configuration file, may be {@code null}
   * @throws PSExtensionException if initialization fails
   */
  @Override
  public void init(IPSExtensionDef extensionDef, File file) throws PSExtensionException {
    // No initialization required for this extension
  }

  /**
   * Implementation of the method defined by the interface.
   *
   * @param params the parameters for this extension, may be {@code null} or empty
   * @param request the request context, never {@code null}
   * @param resultDoc the result document to modify, never {@code null}
   * @return the modified result document, never {@code null}
   * @throws PSExtensionProcessingException if processing fails
   */
  @Override
  public Document processResultDocument(
      Object[] params, IPSRequestContext request, Document resultDoc)
      throws PSExtensionProcessingException {
    Objects.requireNonNull(request, "request cannot be null");
    Objects.requireNonNull(resultDoc, "resultDoc cannot be null");

    var targetElement = findTargetElement(params, resultDoc).orElse(resultDoc.getDocumentElement());

    if (targetElement == null) {
      return resultDoc;
    }

    addSessionTimeoutElement(targetElement, resultDoc);
    return resultDoc;
  }

  /**
   * Finds the target element based on the first parameter.
   *
   * @param params the extension parameters
   * @param resultDoc the result document
   * @return Optional containing the target element if found
   */
  private Optional<Element> findTargetElement(Object[] params, Document resultDoc) {
    if (params == null || params.length == 0 || params[0] == null) {
      return Optional.empty();
    }

    var elemName = params[0].toString().trim();
    if (elemName.length() <= 1) {
      return Optional.empty();
    }

    var nodeList = resultDoc.getElementsByTagName(elemName);
    if (nodeList != null && nodeList.getLength() > 0) {
      return Optional.of((Element) nodeList.item(0));
    }

    return Optional.empty();
  }

  /**
   * Adds the session timeout element to the target element.
   *
   * @param targetElement the element to add the session timeout to, never {@code null}
   * @param resultDoc the document to create new elements in, never {@code null}
   */
  private void addSessionTimeoutElement(Element targetElement, Document resultDoc) {
    var config = PSServer.getServerConfiguration();
    var sessionTimeOut = config.getUserSessionTimeout();

    var child = resultDoc.createElement(ELEM_SESSIONTIMEOUT);
    child.appendChild(resultDoc.createTextNode(String.valueOf(sessionTimeOut)));

    targetElement.appendChild(child);
  }
}
