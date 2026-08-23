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
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.util.PSXMLDomUtil;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class catalogs all global template names found in rhythmyx.
 *
 * <p>Declared {@code final} with a {@code final} collection field and a pure static XML parse
 * helper so constructors never call overridable instance methods (javac {@code this-escape}).
 */
public final class PSGlobalTemplateCataloger {
  /**
   * Default constructor for offline construction and tests. Must be followed by {@link
   * #fromXml(Element)}.
   */
  PSGlobalTemplateCataloger() {
    m_globalTemplates = new ArrayList<>();
  }

  /**
   * Constructs a new global template cataloger.
   *
   * @param urlBase the base URL to use for the catalog request, not <code>null</code>.
   * @throws PSCmsException for any error.
   */
  public PSGlobalTemplateCataloger(URL urlBase) throws PSCmsException {
    if (urlBase == null) throw new IllegalArgumentException("urlBase cannot be null");

    try {
      URL url = new URL(urlBase, "sys_psxCataloger/getGlobalTemplates.xml");
      Document doc = PSXmlDocumentBuilder.createXmlDocument(url.openStream(), false);

      m_globalTemplates = parseGlobalTemplates(doc.getDocumentElement());
    } catch (Exception e) {
      throw new PSCmsException(ContentExplorerErrorCodes.CATALOG_ERROR, e.getMessage());
    }
  }

  /**
   * Loads the global template names from the supplied XML element. The expected DTD is:
   * &lt;!ELEMENT GlobalTemplates (Template*)&gt; &lt;!ELEMENT Template EMPTY&gt; &lt;!ATTLIST
   * Template name CDATA #REQUIRED &gt;
   *
   * @param elemRoot the XML element from which to load the global template names, assumed not
   *     <code>null</code>.
   * @throws PSUnknownNodeTypeException for any unknown XML node.
   */
  void fromXml(Element elemRoot) throws PSUnknownNodeTypeException {
    Collection<String> parsed = parseGlobalTemplates(elemRoot);
    m_globalTemplates.clear();
    m_globalTemplates.addAll(parsed);
  }

  /**
   * Pure parse of global template catalog XML into a new mutable collection. Package-private for
   * unit tests.
   *
   * @param elemRoot the XML element from which to load the global template names, assumed not
   *     <code>null</code>
   * @return newly allocated collection of template names, never <code>null</code>
   * @throws PSUnknownNodeTypeException for any unknown XML node
   */
  static Collection<String> parseGlobalTemplates(Element elemRoot)
      throws PSUnknownNodeTypeException {
    Collection<String> templates = new ArrayList<>();

    PSXMLDomUtil.checkNode(elemRoot, ROOT_ELEM);

    NodeList templateNodes = elemRoot.getElementsByTagName(TEMPLATE_ELEM);
    for (int i = 0; i < templateNodes.getLength(); i++) {
      Element template = (Element) templateNodes.item(i);
      templates.add(template.getAttribute(NAME_ATTR));
    }

    return templates;
  }

  /**
   * Get the collection of all defined global template names.
   *
   * @return a collection of all global template names as <code>String</code> objects, never <code>
   *     null</code>, may be empty.
   */
  public Collection<String> getGlobalTemplates() {
    return Collections.unmodifiableCollection(m_globalTemplates);
  }

  /**
   * The collection of all global template names. Final reference; contents replaced via clear /
   * addAll with each call to {@link #fromXml(Element)}.
   */
  private final Collection<String> m_globalTemplates;

  // private XML constants
  private static final String ROOT_ELEM = "GlobalTemplates";
  private static final String TEMPLATE_ELEM = "Template";
  private static final String NAME_ATTR = "name";
}
