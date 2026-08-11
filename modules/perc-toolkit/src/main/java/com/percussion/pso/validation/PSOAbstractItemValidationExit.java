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
package com.percussion.pso.validation;

import com.percussion.error.PSException;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSItemValidator;
import com.percussion.extension.IPSResultDocumentProcessor;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.pso.workflow.IPSOWorkflowInfoFinder;
import com.percussion.pso.workflow.PSOWorkflowInfoFinder;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.workflow.data.PSState;
import com.percussion.system.utils.PSItemErrorDoc;
import com.percussion.xml.PSXmlDocumentBuilder;
import com.percussion.xml.PSXmlTreeWalker;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Base class for Item Validation exits. Provides generic routines for handling of error documents,
 * fields and lookup of destination workflow states.
 *
 * @author DavidBenua
 */
public abstract class PSOAbstractItemValidationExit extends PSOItemXMLSupport
    implements IPSItemValidator, IPSResultDocumentProcessor {
  // REFACTORED: CP-JAVA11
  private static final Logger log = LogManager.getLogger(PSOAbstractItemValidationExit.class);

  private IPSOWorkflowInfoFinder finder = null;

  /**
   * Default constructor.
   * Creates a new PSOAbstractItemValidationExit.
   *
   */
  protected PSOAbstractItemValidationExit() {}

  /** Initialize the service pointers. */
  private void initServices() {
    if (finder == null) {
      finder = new PSOWorkflowInfoFinder();
    }
  }

  /**
   * See referenced member.
   * @see com.percussion.extension.IPSResultDocumentProcessor#canModifyStyleSheet()
   * @return the result
   */
  public boolean canModifyStyleSheet() {
    return false;
  }

  /**
   * processResultDocument operation.
   *
   * @see
   *     com.percussion.extension.IPSResultDocumentProcessor#processResultDocument(java.lang.Object[],
   *     com.percussion.server.IPSRequestContext, org.w3c.dom.Document)
   * @param params the params
   * @param request the request
   * @param resultDoc the result doc
   * @return the result
   * @throws PSParameterMismatchException if an error occurs
   * @throws PSExtensionProcessingException if an error occurs
   */
  public Document processResultDocument(
      Object[] params, IPSRequestContext request, Document resultDoc)
      throws PSParameterMismatchException, PSExtensionProcessingException {
    Document errorDoc = PSXmlDocumentBuilder.createXmlDocument();
    try {
      validateDocs(resultDoc, errorDoc, request, params);
    } catch (Exception ex) {
      log.error("Unexpected Exception {}", ex.getMessage());
      log.debug(ex.getMessage(), ex);
      throw new PSExtensionProcessingException(getClass().getName(), ex);
    }
    if (hasErrors(errorDoc)) {
      log.debug("validation errors found");
      return errorDoc;
    }
    log.debug("validation successful");
    return null;
  }

  /**
   * validateDocs operation.
   *
   * @param inputDoc the input doc
   * @param errorDoc the error doc
   * @param req the req
   * @param params the params
   * @throws Exception if an error occurs
   */
  protected abstract void validateDocs(
      Document inputDoc, Document errorDoc, IPSRequestContext req, Object[] params)
      throws Exception;

  /**
   * Determines if an error document contains errors.
   *
   * @param errorDoc the error document
   * @return <code>true</code> if there are any errors.
   */
  protected boolean hasErrors(Document errorDoc) {
    Element root = errorDoc.getDocumentElement();
    if (root == null) {
      return false;
    }
    PSXmlTreeWalker w = new PSXmlTreeWalker(root);
    Element e =
        w.getNextElement(
            PSItemErrorDoc.ERROR_FIELD_SET_ELEM, PSXmlTreeWalker.GET_NEXT_ALLOW_CHILDREN);
    if (e == null) {
      return false;
    }
    e = w.getNextElement(PSItemErrorDoc.ERROR_FIELD_ELEM, PSXmlTreeWalker.GET_NEXT_ALLOW_CHILDREN);
    return (e != null);
  }

  /**
   * Matches the current item workflow state and transition ids with a comma delimited list of
   * workflow state names. This method will return true if the destination state matches one of the
   * listed states. If the state list is blank or contains a single "*" it is assumed to match all
   * states.
   *
   * @param contentid the content id of the item
   * @param transitionid the transition id
   * @param allowedStates the list of destination state names that match.
   * @return <code>true</code> when a match occurs,<code>false</code> otherwise.
   * @throws PSException if an error occurs
   */
  protected boolean matchDestinationState(
      String contentid, String transitionid, String allowedStates) throws PSException {
    if (StringUtils.isBlank(allowedStates)) { // match everything
      return true;
    }
    if (allowedStates.trim().equals("*")) {
      return true;
    }
    initServices();
    List<String> allowed = splitAndTrim(allowedStates);
    PSState state = finder.findDestinationState(contentid, transitionid);
    if (state == null) {
      log.warn("Workflow state not found for item {}", contentid);
      return false; // assume no match.
    }
    return allowed.contains(state.getName());
  }

  /**
   * splitAndTrim operation.
   *
   * @param input the input
   * @return the result
   */
  protected List<String> splitAndTrim(String input) {
    return splitAndTrim(input, ",");
  }

  /**
   * splitAndTrim operation.
   *
   * @param input the input
   * @param delimiter the delimiter
   * @return the result
   */
  protected List<String> splitAndTrim(String input, String delimiter) {
    List<String> result = new ArrayList<>();
    if (StringUtils.isBlank(input)) return result;
    String[] parts = input.split(delimiter);
    for (String part : parts) {
      if (!StringUtils.isBlank(part)) {
        result.add(part.trim());
      }
    }
    return result;
  }

  /**
   * init operation.
   *
   * @see com.percussion.extension.IPSExtension#init(com.percussion.extension.IPSExtensionDef,
   *     java.io.File)
   * @param def the def
   * @param codeRoot the code root
   * @throws PSExtensionException if an error occurs
   */
  public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException {}

  /**
   * Sets the finder.
   * @param finder the finder to set. Used only for unit test.
   */
  protected void setFinder(IPSOWorkflowInfoFinder finder) {
    this.finder = finder;
  }
}
