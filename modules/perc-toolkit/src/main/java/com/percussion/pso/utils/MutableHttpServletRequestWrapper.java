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
package com.percussion.pso.utils;

import com.percussion.utils.collections.PSFacadeMap;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;

/**
 * An HttpServletRequestWrapper that allows modification of the request parameters and headers. This
 * is generally useful when forwarding to servlets or building filter chains.
 *
 * <p>Parameters are modified by setting the parameter either as a single String value or as an
 * array of String values. Parameter names are case sensitive.
 *
 * <p>Headers are similarly modified, except that the names are case insensitive.
 *
 * <p>Any header or parameter which is modified in this wrapper loses all values from the wrapped
 * request. If you need to add values, you should copy them from the original request into this
 * class.
 *
 * @author DavidBenua
 */
public class MutableHttpServletRequestWrapper extends HttpServletRequestWrapper
    implements HttpServletRequest {
  PSFacadeMap<String, String[]> localParams;

  Map<String, String[]> localHeaders;

  /**
   * Constructs a new wrapper based on an existing request.
   *
   * @param request the request to wrap.
   */
  public MutableHttpServletRequestWrapper(HttpServletRequest request) {
    super(request);
    localParams = new PSFacadeMap<String, String[]>(request.getParameterMap());

    localHeaders = new HashMap<String, String[]>();
  }

  /**
   * Add a parameter with multiple values
   *
   * @param key the parameter name
   * @param values the values to add;
   */
  public void setParameter(String key, String[] values) {
    localParams.put(key, values);
  }

  /**
   * Add a parameter with a single value
   *
   * @param key the parameter name.
   * @param value the new value.
   */
  public void setParameter(String key, String value) {
    String[] values = new String[] {value};
    setParameter(key, values);
  }

  /**
   * Sets a header with a single value. Convenience method for {@link #setHeader(String, String[])}.
   *
   * @param name header name. Never <code>null</code> or <code>empty</code>.
   * @param value the value to set.
   */
  public void setHeader(String name, String value) {
    String[] values = new String[] {value};
    setHeader(name, values);
  }

  /**
   * Sets a header with multiple values. Header names are case insensitive. Any header which is
   * overridden here will have only the local values, the values from the underlying request will
   * not be considered.
   *
   * @param name header name. Never <code>null</code> or <code>empty</code>.
   * @param values the values to set. May be <code>null</code>
   */
  public void setHeader(String name, String[] values) {
    Validate.notEmpty(name);
    String key = name.toUpperCase(); // header names are case insensitive.
    localHeaders.put(key, values);
  }

  /**
   * See referenced member.
   * @see jakarta.servlet.ServletRequestWrapper#getParameter(java.lang.String)
   * @param name the name
   * @return the result
   */
  @Override
  public String getParameter(String name) {
    String[] vals = getParameterValues(name);
    if (vals == null || vals.length == 0) {
      return null;
    }
    return vals[0];
  }

  /**
   * See referenced member.
   * @see jakarta.servlet.ServletRequestWrapper#getParameterMap()
   * @return the result
   */
  @Override
  public Map<String, String[]> getParameterMap() {
    return Collections.unmodifiableMap(localParams);
  }

  /**
   * See referenced member.
   * @see jakarta.servlet.ServletRequestWrapper#getParameterNames()
   * @return the result
   */
  @Override
  public Enumeration<String> getParameterNames() {
    return Collections.enumeration(localParams.keySet());
  }

  /**
   * See referenced member.
   * @see jakarta.servlet.ServletRequestWrapper#getParameterValues(java.lang.String)
   * @param name the name
   * @return the result
   */
  @Override
  public String[] getParameterValues(String name) {
    return localParams.get(name);
  }

  /**
   * See referenced member.
   * @see jakarta.servlet.http.HttpServletRequestWrapper#getHeader(java.lang.String)
   * @param name the name
   * @return the result
   */
  @Override
  public String getHeader(String name) {
    Validate.notEmpty(name);
    String key = name.toUpperCase();
    if (localHeaders.containsKey(key)) {
      String[] values = localHeaders.get(key);
      if (values == null || values.length == 0) {
        return null;
      }
      return values[0];
    }
    return super.getHeader(name);
  }

  /**
   * See referenced member.
   * @see jakarta.servlet.http.HttpServletRequestWrapper#getHeaderNames()
   * @return the result
   */
  @Override
  public Enumeration<String> getHeaderNames() {
    Set<String> names = new HashSet<>();
    names.addAll(localHeaders.keySet());
    Enumeration<String> e = super.getHeaderNames();
    while (e.hasMoreElements()) {
      String nm = e.nextElement().toLowerCase();
      if (!names.contains(nm)) // faster this way
      {
        names.add(nm);
      }
    }
    return Collections.enumeration(names);
  }

  /**
   * See referenced member.
   * @see jakarta.servlet.http.HttpServletRequestWrapper#getHeaders(java.lang.String)
   * @param name the name
   * @return the result
   */
  @Override
  public Enumeration<String> getHeaders(String name) {
    Validate.notEmpty(name);
    String key = name.toUpperCase();
    if (localHeaders.containsKey(key)) {
      List<String> values = Arrays.asList(localHeaders.get(key));
      return Collections.enumeration(values);
    }
    return super.getHeaders(name);
  }

  /**
   * See referenced member.
   * @see jakarta.servlet.http.HttpServletRequestWrapper#getIntHeader(java.lang.String)
   * @param name the name
   * @return the result
   */
  @Override
  public int getIntHeader(String name) {
    Validate.notEmpty(name);
    String key = name.toUpperCase();
    if (localHeaders.containsKey(key)) {
      String value = getHeader(name);
      return Integer.parseInt(value);
    }
    return super.getIntHeader(name);
  }

  /**
   * See referenced member.
   * @see jakarta.servlet.http.HttpServletRequestWrapper#getDateHeader(java.lang.String)
   * @param name the name
   * @return the result
   */
  @Override
  public long getDateHeader(String name) {
    Validate.notEmpty(name);
    String key = name.toUpperCase();
    if (localHeaders.containsKey(key)) {
      String value = getHeader(name);
      return Long.parseLong(value);
    }
    return super.getDateHeader(name);
  }
}
