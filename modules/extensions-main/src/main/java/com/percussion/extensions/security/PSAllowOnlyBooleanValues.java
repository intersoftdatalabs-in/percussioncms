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

package com.percussion.extensions.security;

import com.percussion.data.PSConversionException;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSResultDocumentProcessor;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionParams;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.security.PSAuthorizationException;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.PSConsole;
import com.percussion.server.PSRequestValidationException;
import java.io.File;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/***
 * Validates that the supplied input is a boolean value. Accepted boolean strings are yes, no, true, false, 1, or 0.
 *
 * Will trim whitespace.
 *
 * Returns an empty string for all other values.
 *
 * @author natechadwick
 *
 * <p>Will trim whitespace.
 *
 * <p>Returns an empty string for all other values.
 *
 * @author natechadwick
 */
public class PSAllowOnlyBooleanValues
    implements IPSResultDocumentProcessor, IPSAllowOnlyItemInputValidator {

  /** Creates a new PSAllowOnlyBooleanValues. */
  public PSAllowOnlyBooleanValues() {}

  /** The TRUE boolean string. */
  public static String TRUE = "true";

  /** The YES boolean string. */
  public static String YES = "yes";

  /** The FALSE boolean string. */
  public static String FALSE = "false";

  /** The NO boolean string. */
  public static String NO = "no";

  /** The ONE boolean string. */
  public static String ONE = "1";

  /** The ZERO boolean string. */
  public static String ZERO = "0";

  private String ms_fullExtensionName;

  @Override
  public String validate(String value, String options) {
    String ret = "";

    if (value == null) return ret;

    value = value.trim();

    if (value.equalsIgnoreCase(TRUE)
        || value.equalsIgnoreCase(YES)
        || value.equalsIgnoreCase(ONE)) {
      ret = TRUE;
    } else if (value.equalsIgnoreCase(FALSE)
        || value.equalsIgnoreCase(NO)
        || value.equalsIgnoreCase(ZERO)) {
      ret = FALSE;
    }

    return ret;
  }

  /**
   * Validates and normalizes boolean parameters from the request.
   *
   * @param params the extension parameters.
   * @param request the request context.
   * @throws PSAuthorizationException if authorization fails.
   * @throws PSRequestValidationException if the request is invalid.
   * @throws PSParameterMismatchException if the parameters are mismatched.
   * @throws PSExtensionProcessingException if processing fails.
   */
  public void preProcessRequest(Object[] params, IPSRequestContext request)
      throws PSAuthorizationException,
          PSRequestValidationException,
          PSParameterMismatchException,
          PSExtensionProcessingException {
    PSExtensionParams ep = null;
    String paramCSV = null;
    ;

    try {
      ep = new PSExtensionParams(params);

      paramCSV = ep.getStringParam(0, null, true);
    } catch (PSConversionException e) {
      throw new PSParameterMismatchException(
          "Expected a string for parameter 0, got an unknown data type instead.");
    }

    if (paramCSV == null)
      throw new PSParameterMismatchException(
          "Missing value for parameter 0, expected a comma seperated list, got an empty list");

    String[] htmlParams = paramCSV.split(",");
    for (String s : htmlParams) {
      s = s.trim();
      if (!StringUtils.isEmpty(s)) {

        String v = request.getParameter(s);
        if (v != null) {
          request.setParameter(s, validate(v, null));
        }
      }
    }
  }

  @Override
  public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException {
    ms_fullExtensionName = def.getRef().toString();
  }

  @Override
  public boolean canModifyStyleSheet() {
    return false;
  }

  @Override
  public Document processResultDocument(
      Object[] params, IPSRequestContext request, Document resultDoc)
      throws PSParameterMismatchException, PSExtensionProcessingException {

    PSExtensionParams ep = null;
    String paramCSV = null;
    ;

    if (params == null || resultDoc == null) return resultDoc;

    Element elem = resultDoc.getDocumentElement();
    if (elem == null) return resultDoc;

    try {
      try {
        ep = new PSExtensionParams(params);

        paramCSV = ep.getStringParam(0, null, true);
      } catch (PSConversionException e) {
        throw new PSParameterMismatchException(
            "Expected a string for parameter 0, got an unknown data type instead.");
      }

      if (paramCSV == null)
        throw new PSParameterMismatchException(
            "Missing value for parameter 0, expected a comma seperated list, got an empty list");

      String[] htmlParams = paramCSV.split(",");
      for (String s : htmlParams) {
        s = s.trim();
        if (!StringUtils.isEmpty(s)) {

          String v = elem.getAttribute(s);
          if (v != null) {

            elem.setAttribute(s, validate(v, null));
          }
        }
      }
    } catch (Throwable t) // should never happen!
    {
      PSConsole.printMsg(ms_fullExtensionName, t);
    }

    return resultDoc;
  }
}
