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
package com.percussion.soln.linkback.servlet;

import static java.text.MessageFormat.format;

import com.percussion.soln.linkback.codec.LinkbackTokenCodec;
import com.percussion.soln.linkback.codec.impl.StringLinkBackTokenImpl;
import com.percussion.soln.linkback.utils.LinkbackUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Generic Linkback Controller. Expects a linkback token as request parameter.
 *
 * <p>Though mainly used as base class for specific controllers, it can be configured as a bean by
 * itself. The default behavior is to redirect to <code>redirectPath</code> with parameters decoded
 * from the linkback codec. The following property can be configured in the bean xml file:
 *
 * <ul>
 *   <li>redirectPath - mandatory if this class is used to configure a bean; optional if subclasses
 *       are used, in which case, it depends on the subclass implementation
 *   <li>helpViewName (optional) - help view name
 *   <li>linkbackCodec (optional) - implementation of {@link LinkbackTokenCodec}
 * </ul>
 *
 * <p>Creates a controller with default configuration.
 */
public class GenericLinkbackController extends AbstractController {

  /** Creates a generic linkback controller with default configuration. */
  public GenericLinkbackController() {}

  private static final Logger log = LogManager.getLogger(GenericLinkbackController.class);

  private String linkbackParameterName = LinkbackUtils.LINKBACK_PARAM_NAME;

  private String redirectPath = "";

  private String helpViewName = "";

  private String errorViewName = "";

  private LinkbackTokenCodec linkbackCodec = null;

  private List<String> requiredParameterNames = new ArrayList<>();

  private List<String> optionalParameterNames = new ArrayList<>();

  private Map<String, String> additionalParameters = new HashMap<>();

  /**
   * If linkback token is not blank, this method calls handleLinkBackRedirect() to create a
   * ModelAndView object; otherwise, return the helpview if exists.
   */
  @Override
  protected ModelAndView handleRequestInternal(
      HttpServletRequest request, HttpServletResponse response) {
    initCodec();

    String linkbackToken = request.getParameter(linkbackParameterName);
    log.debug("linkbackToken={}", linkbackToken);

    if (linkbackToken == null) {

      return createHelpView();
    } else if (StringUtils.isBlank(linkbackToken)) {
      return createErrorView(
          format("Linkback param ({0}) is an empty string.", getLinkbackParameterName()));
    }

    Map<String, String> params = linkbackCodec.decode(linkbackToken);
    log.debug("map: {}", params);
    return handleLinkBackRedirect(params);
  }

  /**
   * Return a ModelAndView object, using a RedirectView and the map as the model. Subclasses
   * override this method to determine how the linkback should be handled.
   *
   * @param tokenParams map of parameter values
   * @return ModelAndView
   */
  protected ModelAndView handleLinkBackRedirect(Map<String, String> tokenParams) {
    Map<String, String> params = new HashMap<>();
    if (!checkAndCopyRequiredParams(tokenParams, params)) {
      String message =
          "Required Parameter missing, required parameters are " + getRequiredParameterNames();
      return createErrorView(message);
    }
    copyOptionalParameters(tokenParams, params);
    params.putAll(getAdditionalParameters());
    modifyParameterMap(params);
    String path = getRedirectPath();
    log.debug("redirect path: {}", path);
    return new ModelAndView(new RedirectView(path, true), params);
  }

  /**
   * Check that required parameters exist and copy them to the output map. The parameters must not
   * be blank and must be numeric.
   *
   * @param inParams the input parameter map.
   * @param outParams the map to copy parameters to.
   * @return true if the required parameters exist.
   */
  protected boolean checkAndCopyRequiredParams(
      Map<String, String> inParams, Map<String, String> outParams) {
    for (String pname : getRequiredParameterNames()) {
      String pvalue = inParams.get(pname);
      if (StringUtils.isBlank(pvalue) || !StringUtils.isNumeric(pvalue)) {
        return false;
      }
      outParams.put(pname, pvalue);
    }
    return true;
  }

  /**
   * Copy the optional parameters.
   *
   * @param inParams the incoming parameters
   * @param outParams the parameter map to copy to.
   */
  protected void copyOptionalParameters(
      Map<String, String> inParams, Map<String, String> outParams) {
    for (String pname : getOptionalParameterNames()) {
      String pvalue = inParams.get(pname);
      if (StringUtils.isNotBlank(pvalue)) {
        outParams.put(pname, pvalue);
      }
    }
  }

  /**
   * Modify the parameter map. This is intended for subclasses that need special parameters.
   *
   * @param params the parameter map.
   */
  protected void modifyParameterMap(Map<String, String> params) {
    // do nothing for now.
  }

  /**
   * Create a help view if {@link #helpViewName} is defined in the bean config; otherwise, null;
   *
   * @return ModelAndView
   */
  protected ModelAndView createHelpView() {
    if (StringUtils.isBlank(getHelpViewName())) {
      return null;
    }
    return new ModelAndView(getHelpViewName(), "message", null);
  }

  /**
   * Create a error view if {@link #errorViewName} is defined in the bean config; otherwise, null;
   *
   * @param message custom message
   * @return ModelAndView
   */
  protected ModelAndView createErrorView(String message) {
    if (StringUtils.isBlank(getErrorViewName())) {
      return null;
    }
    return new ModelAndView(getErrorViewName(), "message", message);
  }

  private void initCodec() {
    if (linkbackCodec == null) {
      // create a default codec, if not specified in the bean config
      linkbackCodec = new StringLinkBackTokenImpl();
    }
  }

  /**
   * Gets the redirect path.
   *
   * @return the redirect path
   */
  public String getRedirectPath() {
    return redirectPath;
  }

  /**
   * Sets the redirect path.
   *
   * @param redirectPath the redirect path to set
   */
  public void setRedirectPath(String redirectPath) {
    this.redirectPath = redirectPath;
  }

  /**
   * Gets the help view name.
   *
   * @return the help view name
   */
  public String getHelpViewName() {
    return helpViewName;
  }

  /**
   * Sets the help view name.
   *
   * @param helpViewName the help view name to set
   */
  public void setHelpViewName(String helpViewName) {
    this.helpViewName = helpViewName;
  }

  /**
   * Gets the linkback codec.
   *
   * @return the linkback codec
   */
  public LinkbackTokenCodec getLinkbackCodec() {
    return linkbackCodec;
  }

  /**
   * Sets the linkback codec.
   *
   * @param linkbackCodec the linkback codec to set
   */
  public void setLinkbackCodec(LinkbackTokenCodec linkbackCodec) {
    this.linkbackCodec = linkbackCodec;
  }

  /**
   * Gets the linkback parameter name.
   *
   * @return the linkback parameter name
   */
  public String getLinkbackParameterName() {
    return linkbackParameterName;
  }

  /**
   * Sets the linkback parameter name.
   *
   * @param linkbackParameterName the linkback parameter name to set
   */
  public void setLinkbackParameterName(String linkbackParameterName) {
    this.linkbackParameterName = linkbackParameterName;
  }

  /**
   * Gets the required parameter names.
   *
   * @return the required parameter names
   */
  public List<String> getRequiredParameterNames() {
    return requiredParameterNames;
  }

  /**
   * Sets the required parameter names.
   *
   * @param requiredParameterNames the required parameter names to set
   */
  public void setRequiredParameterNames(List<String> requiredParameterNames) {
    this.requiredParameterNames = requiredParameterNames;
  }

  /**
   * Gets the optional parameter names.
   *
   * @return the optional parameter names
   */
  public List<String> getOptionalParameterNames() {
    return optionalParameterNames;
  }

  /**
   * Sets the optional parameter names.
   *
   * @param optionalParameterNames the optional parameter names to set
   */
  public void setOptionalParameterNames(List<String> optionalParameterNames) {
    this.optionalParameterNames = optionalParameterNames;
  }

  /**
   * Gets the additional parameters.
   *
   * @return the additional parameters
   */
  public Map<String, String> getAdditionalParameters() {
    return additionalParameters;
  }

  /**
   * Sets the additional parameters.
   *
   * @param additionalParameters the additional parameters to set
   */
  public void setAdditionalParameters(Map<String, String> additionalParameters) {
    this.additionalParameters = additionalParameters;
  }

  /**
   * Gets the error view name.
   *
   * @return the error view name
   */
  public String getErrorViewName() {
    return errorViewName;
  }

  /**
   * Sets the error view name.
   *
   * @param errorViewName the error view name to set
   */
  public void setErrorViewName(String errorViewName) {
    this.errorViewName = errorViewName;
  }
}
