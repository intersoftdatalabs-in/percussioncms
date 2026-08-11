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
package com.percussion.sitemanage.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A mutable HTTP servlet request wrapper. Refactored for Java 11 and Google Java Style.
 *
 * <p>Final so the constructor may copy {@link #getParameterMap()} without {@code this-escape} from
 * further subclasses.
 */
public final class PSServletRequestWrapper extends HttpServletRequestWrapper {
  private Map<String, String[]> wrappedParams = new LinkedHashMap<>();

  public PSServletRequestWrapper(HttpServletRequest request) {
    super(request);
    wrappedParams.putAll(super.getParameterMap());
  }

  @Override
  public String getParameter(String name) {
    var p = getParameterValues(name);
    if (p == null || p.length == 0) {
      return null;
    }
    return p[0];
  }

  @Override
  public Map<String, String[]> getParameterMap() {
    return Collections.unmodifiableMap(getMergedParameters());
  }

  @Override
  public Enumeration<String> getParameterNames() {
    return Collections.enumeration(getMergedParameters().keySet());
  }

  @Override
  public String[] getParameterValues(String name) {
    return getMergedParameters().get(name);
  }

  /**
   * Sets the parameter map for this request.
   *
   * @param params the params to set
   */
  public void setParameterMap(Map<String, String[]> params) {
    this.wrappedParams = params;
  }

  /**
   * Merge the superclass and local parameters together; the local parameters overwrite the super
   * parameters.
   *
   * @return merged parameter map, never {@code null}, may be empty.
   */
  private Map<String, String[]> getMergedParameters() {
    var mergedParams = new LinkedHashMap<String, String[]>(super.getParameterMap());
    mergedParams.putAll(wrappedParams);
    return mergedParams;
  }
}
