/*
 * Copyright (c) 2023 Intersoft Data Labs, Inc.
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
package com.percussion.cx.catalogers;

import com.intsof.percussioncms.auditlog.codes.ContentExplorerErrorCodes;
import com.percussion.cms.PSCmsException;
import com.percussion.design.objectstore.PSEntry;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Catalogs all locales in the CMS by querying the ../sys_i18nSupport/languagelookup.xml app.
 *
 * <p>Declared {@code final} with a {@code final} collection field and a pure static XML parse
 * helper so constructors never call overridable instance methods (javac {@code this-escape}).
 *
 * @author RammohanVangapalli
 */
public final class PSLocaleCataloger {
  /**
   * Default constructor. Does nothing. Must be followed by call to fromXml() method. This is useful
   * only to build an object in the fly means the state information might not come from the Rhythmyx
   * server.
   */
  public PSLocaleCataloger() {
    m_locales = new ArrayList<>();
  }

  /**
   * Constructor meant to be used in the context of an applet. This may not work in other contexts
   * since there is no way of supplying credentials for logging in.
   *
   * @param urlBase the document or code base for the applet.
   * @throws PSCmsException if request to server to get the data fails for any reason.
   */
  public PSLocaleCataloger(URL urlBase) throws PSCmsException {
    try {
      URL url = new URL(urlBase, "sys_i18nSupport/languagelookup.xml");
      Document doc = PSXmlDocumentBuilder.createXmlDocument(url.openStream(), false);
      m_locales = parseLocales(doc.getDocumentElement());
    } catch (Exception e) {
      throw new PSCmsException(ContentExplorerErrorCodes.CATALOG_ERROR, e.getMessage());
    }
  }

  /**
   * Loads the cataloged locales from the supplied XML element.
   *
   * @param elemSrc the element representing the catalog response, may not be <code>null</code>.
   * @throws PSUnknownNodeTypeException if the element does not have the expected structure.
   */
  public void fromXml(Element elemSrc) throws PSUnknownNodeTypeException {
    Collection<PSEntry> parsed = parseLocales(elemSrc);
    m_locales.clear();
    m_locales.addAll(parsed);
  }

  /**
   * Pure parse of locale catalog XML into a new mutable collection. Package-private for unit tests.
   *
   * @param elemSrc the element representing the catalog response, may not be <code>null</code>
   * @return newly allocated collection of locale entries, never <code>null</code>
   * @throws PSUnknownNodeTypeException if the element does not have the expected structure
   */
  static Collection<PSEntry> parseLocales(Element elemSrc) throws PSUnknownNodeTypeException {
    Collection<PSEntry> locales = new ArrayList<>();

    NodeList nl = elemSrc.getElementsByTagName(PSEntry.XML_NODE_NAME);
    Element elem = null;
    PSEntry entry = null;
    for (int i = 0; i < nl.getLength(); i++) {
      elem = (Element) nl.item(i);
      entry = new PSEntry(elem, null, null);
      locales.add(entry);
    }

    return locales;
  }

  /**
   * Get the list of locale entries.
   *
   * @return iterator of {@link PSEntry} object representing each locale in the system. Never <code>
   *     null</code>.
   */
  public Iterator<PSEntry> getLocales() {
    return m_locales.iterator();
  }

  /**
   * Collection of {@link PSEntry} objects. Each objects represents a locale in the CMS. Final
   * reference; contents replaced via clear / addAll.
   */
  private final Collection<PSEntry> m_locales;
}
