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
package com.percussion.webui.tags;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.TagSupport;
import java.io.IOException;

/**
 * JSP tag handler that resolves and displays a localized message key from the TMX cache.
 *
 * <p>The tag retrieves the language from the page context attribute {@code sys_lang} and looks up
 * the specified key in the translation cache. If no translation is found, it either displays the
 * key (in debug mode) or attempts to extract a display value.
 *
 * <p>Usage example:
 *
 * <pre>
 * &lt;tmx:message key="myapp.welcome"/&gt;
 * </pre>
 *
 * <p>Required attributes:
 *
 * <ul>
 *   <li>{@code key} - the message key to resolve
 * </ul>
 *
 * <p>The page context must contain a {@code sys_lang} attribute set to the desired language code
 * (e.g., "en", "fr").
 *
 * @author erikserating
 */
public class TmxMessageTag extends TagSupport {
  /** Safe to serialize. */
  private static final long serialVersionUID = 1L;

  /** No-op constructor. */
  public TmxMessageTag() {
    // no-op
  }

  /** The message key to look up in the translation cache. */
  private String key;

  /**
   * Sets the message key to be resolved.
   *
   * @param key the message key, must not be null or empty
   */
  public void setKey(String key) {
    this.key = key;
  }

  @Override
  public int doStartTag() throws JspException {
    String lang = (String) pageContext.getAttribute("sys_lang");
    String debug = (String) pageContext.getAttribute("debug");
    if (key == null || key.isEmpty()) {
      throw new IllegalArgumentException("The key must be specified.");
    }
    TmxCache cache = TmxCache.getInstance();
    String val = cache.getValue(lang, key);
    if (val == null || val.isEmpty()) {
      val = (debug != null && debug.equalsIgnoreCase("true")) ? key : getKeyDisplayValue(key);
    }
    try {
      pageContext.getOut().print(val);
    } catch (IOException e) {
      throw new JspException(e);
    }
    return SKIP_BODY;
  }

  /**
   * Returns the display value for a key. If the key contains '@', returns the part after '@'.
   * Otherwise, returns the key itself.
   *
   * @param key the key string
   * @return display value
   */
  private String getKeyDisplayValue(String key) {
    String[] temp = key.split("@");
    if (temp.length == 1 || temp.length > 2) {
      return key;
    }
    return temp[1];
  }
}
