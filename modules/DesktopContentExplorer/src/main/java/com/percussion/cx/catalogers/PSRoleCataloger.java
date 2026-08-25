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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Catalogs all server roles by querying the ../sys_components/getRole.xml app.
 *
 * <p>Declared {@code final} with a {@code final} collection field and a pure static XML parse
 * helper so constructors never call overridable instance methods (javac {@code this-escape}).
 */
public final class PSRoleCataloger {
  /**
   * Default constructor. Does nothing. Must be followed by call to fromXml() method. This is useful
   * only to build an object in the fly means the state information might not come from the Rhythmyx
   * server.
   */
  public PSRoleCataloger() {
    m_collRoles = new ArrayList<>();
  }

  /**
   * Constructor meant to be used in the context of an applet. This may not work in other contexts
   * since there is no way of supplying credentials for logging in.
   *
   * @param urlBase the document or code base for the applet.
   * @throws PSCmsException if request to server to get the data fails for any reason.
   */
  public PSRoleCataloger(URL urlBase) throws PSCmsException {
    try {
      URL url = new URL(urlBase, "sys_components/getRole.xml");
      Document doc = PSXmlDocumentBuilder.createXmlDocument(url.openStream(), false);
      m_collRoles = parseRoles(doc.getDocumentElement());
    } catch (Exception e) {
      throw new PSCmsException(ContentExplorerErrorCodes.CATALOG_ERROR, e.getMessage());
    }
  }

  /** Implementation of the clone. */
  public Object clone() {
    PSRoleCataloger clone = new PSRoleCataloger();

    for (Role role : m_collRoles) {
      Object roleClone = role.clone();
      if (roleClone instanceof Role) {
        clone.m_collRoles.add((Role) roleClone);
      }
    }

    return clone;
  }

  public boolean equals(Object object) {
    if (this == object) return true;

    if (!(object instanceof PSRoleCataloger)) return false;

    PSRoleCataloger that = (PSRoleCataloger) object;

    return new org.apache.commons.lang3.builder.EqualsBuilder()
        .append(m_collRoles, that.m_collRoles)
        .isEquals();
  }

  public int hashCode() {
    return new org.apache.commons.lang3.builder.HashCodeBuilder(17, 37)
        .append(m_collRoles)
        .toHashCode();
  }

  /**
   * Represents a single role.
   *
   * <p>Static nested type so constructing a {@code Role} never captures the enclosing cataloger
   * (javac {@code this-escape}).
   */
  public static final class Role {
    /**
     * Default constructor. Does nothing. Must be followed by call to fromXml() method. This is
     * useful only to build an object in the fly means the state information might not come from the
     * Rhythmyx server.
     */
    public Role() {}

    /**
     * Constructor that loads from XML via the private apply helper.
     *
     * @param elemRoot the element that contains data for a single Role, never <code>null</code>.
     * @throws PSUnknownNodeTypeException if the element is not in the expected format.
     */
    public Role(Element elemRoot) throws PSUnknownNodeTypeException {
      applyFromXml(elemRoot);
    }

    /**
     * Implementation of the interface method.
     *
     * @param elemRoot the element that contains data for a single role, may not be <code>null
     *     </code>.
     * @throws PSUnknownNodeTypeException if the element is not in the expected format.
     */
    public void fromXml(Element elemRoot) throws PSUnknownNodeTypeException {
      applyFromXml(elemRoot);
    }

    private void applyFromXml(Element elemRoot) throws PSUnknownNodeTypeException {
      PSXMLDomUtil.checkNode(elemRoot, XML_ELEM_PSXROLE);
      Element el = PSXMLDomUtil.getFirstElementChild(elemRoot, XML_ELEM_NAME);
      m_name = PSXMLDomUtil.getElementData(el);
    }

    /**
     * Gets the name of the role.
     *
     * @return role name, never <code>null</code>
     */
    public String getName() {
      return m_name;
    }

    /*
     * Implementation of the interface method
     */
    public boolean equals(Object obj) {
      if (!(obj instanceof Role)) return false;
      else {
        return ((Role) obj).getName().equals(getName());
      }
    }

    /*
     * Implementation of the interface method
     */
    public Object clone() {
      Role clone = new Role();
      clone.m_name = m_name;
      return clone;
    }

    /*
     * Implementation of the interface method
     */
    public String toString() {
      return m_name;
    }

    /*
     * Implementation of the interface method
     */
    public int hashCode() {
      return m_name.hashCode();
    }

    /** The role name. */
    private String m_name;
  }

  /**
   * Implementation of the interface method.
   *
   * @param elemRoot the root element of the catalog response, may not be <code>null</code>.
   * @throws PSUnknownNodeTypeException if the element is not in the expected format.
   */
  public void fromXml(Element elemRoot) throws PSUnknownNodeTypeException {
    Collection<Role> parsed = parseRoles(elemRoot);
    m_collRoles.clear();
    m_collRoles.addAll(parsed);
  }

  /**
   * Pure parse of role catalog XML into a new mutable collection. Package-private for unit tests.
   *
   * @param elemRoot the root element of the catalog response, may not be <code>null</code>
   * @return newly allocated collection of roles, never <code>null</code>
   * @throws PSUnknownNodeTypeException if the element is not in the expected format
   */
  static Collection<Role> parseRoles(Element elemRoot) throws PSUnknownNodeTypeException {
    Collection<Role> roles = new ArrayList<>();

    PSXMLDomUtil.checkNode(elemRoot, XML_ELEM_ROOT);

    NodeList nl = elemRoot.getElementsByTagName(XML_ELEM_PSXROLE);

    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);
      if (n.getNodeType() != Node.ELEMENT_NODE) continue;

      roles.add(new Role((Element) n));
    }

    return roles;
  }

  /**
   * Implementation of the interface method.
   *
   * @param doc the document used to create the new element, may not be <code>null</code>.
   * @return the new root element, never <code>null</code>.
   */
  public Element toXml(Document doc) {
    Element elem = PSXmlDocumentBuilder.createRoot(doc, XML_ELEM_ROOT);

    return elem;
  }

  /**
   * Gets the cataloged roles.
   *
   * @return unmodifiable collection of cataloged Role instances , never <code>null</code>.
   */
  public Collection<Role> getRoles() {
    return Collections.unmodifiableCollection(m_collRoles);
  }

  /**
   * Collection of cataloged Role instances. Final reference; contents replaced via clear / addAll.
   */
  private final Collection<Role> m_collRoles;

  /** Root element name in the catalog response. */
  public static final String XML_ELEM_ROOT = "getRole";

  /** Role element name in the catalog response. */
  public static final String XML_ELEM_PSXROLE = "PSXRole";

  /** Name element in the role XML. */
  public static final String XML_ELEM_NAME = "name";
}
