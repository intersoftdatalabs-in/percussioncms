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

package com.percussion.soln.p13n.delivery.ds.web;

import com.percussion.security.validation.XSSValidation;

/**
 * See <a href="http://bob.pythonmac.org/archives/2005/12/05/remote-json-jsonp/">Remote JSON -
 * JSONP</a>
 *
 * <p>The callback name is sanitized via {@link XSSValidation#sanitizeJsonpCallback(String)} so a
 * crafted {@code jsoncallback} query parameter cannot break out of {@code callback(...)} script
 * padding (CodeQL {@code java/xss} / T044 alert #595).
 *
 * @author adamgent
 */
public class JSONP {
  private Object json;
  private String callback;

  public JSONP() {}

  public JSONP(String callback, Object json) {
    this();
    setCallback(callback);
    this.json = json;
  }

  @Override
  public String toString() {
    String jsonString = getJson() == null ? "undefined" : getJson().toString();
    if (getCallback() != null) {
      return getCallback() + "(" + jsonString + ")";
    }
    return jsonString;
  }

  public Object getJson() {
    return json;
  }

  public void setJson(Object json) {
    this.json = json;
  }

  public String getCallback() {
    return callback;
  }

  /**
   * Sets the JSONP callback name. Unsafe values are dropped (plain JSON is emitted instead).
   *
   * @param callback raw callback from the request; may be null
   */
  public void setCallback(String callback) {
    this.callback = XSSValidation.sanitizeJsonpCallback(callback);
  }
}
