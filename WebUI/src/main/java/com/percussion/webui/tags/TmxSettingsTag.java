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

import com.percussion.i18n.PSTmxResourceBundle;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.TagSupport;
import java.io.IOException;
import java.util.Iterator;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * JSP tag handler that initializes TMX (Translation Memory eXchange) settings for the page.
 *
 * <p>This tag sets up the language and optional key prefixes for the translation cache, making
 * localized messages available to other TMX tags on the page.
 *
 * <p>Usage example:
 *
 * <pre>
 * &lt;tmx:settings lang="en-us" prefixes="myapp,common"/&gt;
 * </pre>
 *
 * <p>Attributes:
 *
 * <ul>
 *   <li>{@code lang} - language code (default: "en-us")
 *   <li>{@code prefixes} - comma-separated key prefixes to load (default: all keys)
 *   <li>{@code debug} - set to "true" to display raw keys when translations are missing
 * </ul>
 *
 * <p>After processing, this tag sets page context attributes:
 *
 * <ul>
 *   <li>{@code sys_lang} - the configured language code
 *   <li>{@code debug} - the debug flag
 * </ul>
 *
 * @author erikserating
 */
public class TmxSettingsTag extends TagSupport {
  /** Safe to serialize. */
  private static final long serialVersionUID = 1L;

  /** No-op constructor. */
  public TmxSettingsTag() {
    // no-op
  }

  /** Comma-separated list of key prefixes to load from the translation bundle. */
  private String prefixes;

  /** Language code for translations (e.g., "en-us", "fr"). */
  private String lang = DEFAULT_LANG;

  /** Debug flag - when true, displays raw keys if translation is missing. */
  private String debug = "false";

  /** Default language code. */
  private static final String DEFAULT_LANG = "en-us";

  /**
   * Sets the comma-separated key prefixes to load.
   *
   * @param prefixes the prefixes, or null/empty for all keys
   */
  public void setPrefixes(String prefixes) {
    this.prefixes = prefixes;
  }

  /**
   * Sets the language code for translations.
   *
   * @param lang the language code (e.g., "en-us", "fr")
   */
  public void setLang(String lang) {
    if (lang == null || lang.isEmpty()) {
      return;
    }
    this.lang = lang;
  }

  /**
   * Sets the debug flag for displaying raw keys.
   *
   * @param debug "true" to enable debug mode
   */
  public void setDebug(String debug) {
    if (debug == null) {
      debug = "false";
    }
    this.debug = debug;
  }

  @Override
  public int doStartTag() throws JspException {
    try {
      pageContext.setAttribute("debug", debug);
      pageContext.setAttribute("sys_lang", lang);
      loadTmx();
    } catch (Exception e) {
      throw new JspException(e);
    }
    return SKIP_BODY;
  }

  /**
   * Loads the tmx keys for the specified lang and prefixes if not yet cached.
   *
   * @throws IOException if IO error occurs
   * @throws ParserConfigurationException if parser config error occurs
   * @throws SAXException if XML error occurs
   */
  private void loadTmx() throws IOException, ParserConfigurationException, SAXException {
    TmxCache cache = TmxCache.getInstance();
    String prefixStr = (prefixes == null || prefixes.isEmpty()) ? "*" : prefixes;
    if (cache.isIndexed(lang, prefixStr)) {
      return;
    }
    String[] prefixArr = prefixStr.split(",");
    PSTmxResourceBundle tmxBundle = PSTmxResourceBundle.getInstance();
    Iterator<?> keys = tmxBundle.getKeys(lang);
    while (keys.hasNext()) {
      String key = (String) keys.next();
      if (!prefixStr.equals("*") && !accept(prefixArr, key)) {
        continue;
      }
      String val = tmxBundle.getString(key, lang).replace("\"", "\\\"");
      cache.addEntry(lang, key, val);
    }
    cache.setIndexed(lang, prefixStr);
  }

  /**
   * Checks if the key starts with any of the given prefixes.
   *
   * @param prefixes array of prefixes
   * @param key key to check
   * @return true if key matches any prefix
   */
  public boolean accept(String[] prefixes, String key) {
    for (String prefix : prefixes) {
      if (key.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }
}
