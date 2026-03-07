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
package com.percussion.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/** A multimap class for storing HTTP headers (which can be 1:many). */
public class PSHttpHeaders {
  private final Map<String, Object> headers = new HashMap<>();

  /**
   * Adds a header value.
   *
   * @param headerName header name
   * @param headerValue header value
   */
  public void addHeader(String headerName, String headerValue) {
    addMultiMapping(headers, headerName.toUpperCase(), headerValue);
  }

  /**
   * Replaces a header value.
   *
   * @param headerName header name
   * @param headerValue header value
   */
  public void replaceHeader(String headerName, String headerValue) {
    headers.put(headerName.toUpperCase(), headerValue);
  }

  /**
   * Gets all values for a header name.
   *
   * @param headerName header name
   * @return iterator of header values
   */
  public Iterator<String> getHeaders(String headerName) {
    return getMultiValues(headers, headerName.toUpperCase());
  }

  /**
   * Gets the first value for a header name.
   *
   * @param headerName header name
   * @return first header value or null
   */
  public String getHeader(String headerName) {
    String val = null;
    Iterator<String> i = getHeaders(headerName);
    if (i.hasNext()) {
      val = i.next();
    }
    return val;
  }

  /**
   * Gets all header names.
   *
   * @return set of header names
   */
  public Set<String> getHeaderNames() {
    return headers.keySet();
  }

  /**
   * Adds all headers from another PSHttpHeaders instance.
   *
   * @param otherHeaders headers to add
   */
  public void addAll(PSHttpHeaders otherHeaders) {
    if (otherHeaders == this) return;
    Collection<String> keySet = otherHeaders.getHeaderNames();
    for (Iterator<String> i = keySet.iterator(); i.hasNext(); ) {
      String headerName = i.next();
      for (Iterator<String> j = otherHeaders.getHeaders(headerName); j.hasNext(); ) {
        addHeader(headerName, j.next());
      }
    }
  }

  /**
   * Adds a value to a Map in a 1:1 or 1:many way, if applicable. 1:many mappings will be Lists
   * containing values.
   *
   * @author chad loder
   * @version 1.0 1999/8/20
   * @param m
   * @param key
   * @param value
   */
  /**
   * Adds a value to a Map in a 1:1 or 1:many way, if applicable. 1:many mappings will be Lists
   * containing values.
   *
   * @param m map to add to
   * @param key header name
   * @param value header value
   */
  protected void addMultiMapping(Map<String, Object> m, Object key, Object value) {
    // we support 1:many header mappings by storing Lists for 1:many
    // headers. For 1:1 headers, we store Strings

    Object existingVal = m.get(key);
    if (existingVal instanceof List) {

      List<String> valList = (List<String>) existingVal;
      valList.add((String) value);
    } else if (existingVal != null) {
      List<String> valList = new ArrayList<>();
      valList.add((String) existingVal);
      valList.add((String) value);
      existingVal = valList;
      m.put((String) key, existingVal);
    } else {
      m.put((String) key, value);
    }
  }

  /**
   * Gets all the values out of the multimap for a particular key
   *
   * @author chad loder
   * @version 1.0 1999/8/20
   * @param m
   * @param key
   * @return Iterator
   */
  /**
   * Gets all the values out of the multimap for a particular key.
   *
   * @param m map to get from
   * @param key header name
   * @return iterator of header values
   */
  protected Iterator<String> getMultiValues(Map<String, Object> m, String key) {
    class SingleIterator implements Iterator<String> {
      public SingleIterator(Object value) {
        m_value = (String) value;
      }

      public boolean hasNext() {
        return m_value != null;
      }

      public String next() {
        if (m_value == null) throw new NoSuchElementException();
        String val = m_value;
        m_value = null;
        return val;
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }

      private String m_value;
    }
    Object val = m.get(key);
    if (val instanceof List) {

      List<String> valList = (List<String>) val;
      return valList.iterator();
    }
    return new SingleIterator(val);
  }
}
