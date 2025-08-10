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
import java.io.IOException;
import java.util.Iterator;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * @author erikserating
 */
public class TmxSettingsTag extends TagSupport {
  private String prefixes;
  private String lang = DEFAULT_LANG;
  private String debug = "false";
  private static final String DEFAULT_LANG = "en-us";

  public void setPrefixes(String prefixes) {
    this.prefixes = prefixes;
  }

  public void setLang(String lang) {
    if (lang == null || lang.isEmpty()) {
      return;
    }
    this.lang = lang;
  }

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
   * @throws IOException                  if IO error occurs
   * @throws ParserConfigurationException if parser config error occurs
   * @throws SAXException                 if XML error occurs
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
   * @param key      key to check
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
