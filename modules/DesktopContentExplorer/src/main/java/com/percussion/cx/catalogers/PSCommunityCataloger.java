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

import com.percussion.cms.PSCmsException;
import com.percussion.cx.error.IPSContentExplorerErrors;
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
 * Catalogs all server communities by querying the ../sys_cmpCommunities/communities.xml app.
 *
 * <p>Declared {@code final} with a {@code final} collection field and a pure static XML parse
 * helper so constructors never call overridable instance methods (javac {@code this-escape}).
 */
public final class PSCommunityCataloger {
  /**
   * Default constructor. Does nothing. Must be followed by call to fromXml() method. This is useful
   * only to build an object in the fly means the state information might not come from the Rhythmyx
   * server.
   */
  public PSCommunityCataloger() {
    m_collCommunities = new ArrayList<>();
  }

  /**
   * Constructor meant to be used in the context of an applet. This may not work in other contexts
   * since there is no way of supplying credentials for logging in.
   *
   * @param urlBase the document or code base for the applet.
   * @throws PSCmsException if request to server to get the data fails for any reason.
   */
  public PSCommunityCataloger(URL urlBase) throws PSCmsException {
    try {
      URL url = new URL(urlBase, "sys_cmpCommunities/communities.xml");
      Document doc = PSXmlDocumentBuilder.createXmlDocument(url.openStream(), false);
      m_collCommunities = parseCommunities(doc.getDocumentElement());
    } catch (Exception e) {
      throw new PSCmsException(IPSContentExplorerErrors.CATALOG_ERROR, e.getMessage());
    }
  }

  /*
   * Implementation of the interface method.
   */
  public Object clone() {
    PSCommunityCataloger clone = new PSCommunityCataloger();

    for (Community community : m_collCommunities) {
      Object communityClone = community.clone();
      if (communityClone instanceof Community) {
        clone.m_collCommunities.add((Community) communityClone);
      }
    }

    return clone;
  }

  public boolean equals(Object object) {
    if (this == object) return true;

    if (!(object instanceof PSCommunityCataloger)) return false;

    PSCommunityCataloger that = (PSCommunityCataloger) object;

    return new org.apache.commons.lang3.builder.EqualsBuilder()
        .append(m_collCommunities, that.m_collCommunities)
        .isEquals();
  }

  public int hashCode() {
    return new org.apache.commons.lang3.builder.HashCodeBuilder(17, 37)
        .append(m_collCommunities)
        .toHashCode();
  }

  /** Represents a single community. */
  public static final class Community {
    /**
     * Default constructor. Does nothing. Must be followed by call to fromXml() method. This is
     * useful only to build an object in the fly means the state information might not come from the
     * Rhythmyx server.
     */
    public Community() {}

    /**
     * Allows to create a community instance from passed params, main use is for the Cx UI to add an
     * entry for the -1 community id.
     *
     * @param id community Id, -1 is reserved for "All"
     * @param name, never <code>null</code>
     * @param description, may be <code>null</code>
     */
    public Community(int id, String name, String description) {
      m_communityId = id;

      if (name == null) throw new IllegalArgumentException("community name may not be null");

      m_communityName = name;
      m_communityDesc = description;
    }

    /**
     * Constructor that loads from XML via the private apply helper (no overridable method on {@code
     * this} during construction).
     *
     * @param elemRoot the element that contains data for a single community, never <code>null
     *     </code>
     * @throws PSUnknownNodeTypeException if the supplied element does not match the expected
     *     schema.
     */
    public Community(Element elemRoot) throws PSUnknownNodeTypeException {
      applyFromXml(elemRoot);
    }

    /**
     * Populates this community instance from the supplied XML element.
     *
     * @param elemRoot the element that contains data for a single community, never <code>null
     *     </code>.
     * @throws PSUnknownNodeTypeException if the supplied element does not match the expected
     *     schema.
     */
    public void fromXml(Element elemRoot) throws PSUnknownNodeTypeException {
      applyFromXml(elemRoot);
    }

    /**
     * Applies community XML fields onto this instance. Private so constructors do not invoke an
     * overridable method.
     */
    private void applyFromXml(Element elemRoot) throws PSUnknownNodeTypeException {
      PSXMLDomUtil.checkNode(elemRoot, XML_ELEM_LIST);
      Element el = PSXMLDomUtil.getFirstElementChild(elemRoot, XML_ELEM_COMMUNITYNAME);
      m_communityName = PSXMLDomUtil.getElementData(el);

      el = PSXMLDomUtil.getNextElementSibling(el, XML_ELEM_COMMUNITYID);
      String strComm = PSXMLDomUtil.getElementData(el);
      try {
        m_communityId = -1;
        m_communityId = Integer.parseInt(strComm);
      } catch (NumberFormatException e) {
      }

      el = PSXMLDomUtil.getNextElementSibling(el, XML_ELEM_COMMUNITYDESC);
      m_communityDesc = PSXMLDomUtil.getElementData(el);
    }

    /**
     * Get the unique identifier of this community.
     *
     * @return community id
     */
    public int getId() {
      return m_communityId;
    }

    /**
     * Get the name of this community.
     *
     * @return community name, never <code>null</code>
     */
    public String getName() {
      return m_communityName;
    }

    /**
     * Get the description of this community.
     *
     * @return community description, never <code>null</code>
     */
    public String getDesc() {
      return m_communityDesc;
    }

    /*
     * Implementation of the interface method
     */
    public boolean equals(Object obj) {
      if (!(obj instanceof Community)) return false;
      else {
        return ((Community) obj).getName().equals(getName());
      }
    }

    /*
     * Implementation of the interface method
     */
    public Object clone() {
      return new Community(m_communityId, m_communityName, m_communityDesc);
    }

    /*
     * Implementation of the interface method
     */
    public String toString() {
      return m_communityName;
    }

    /*
     * Implementation of the interface method
     */
    public int hashCode() {
      return m_communityName.hashCode();
    }

    /** */
    private String m_communityName;

    /** */
    private int m_communityId = -1;

    /** */
    private String m_communityDesc;
  }

  /**
   * Populates this cataloger from the supplied XML element by clearing any existing communities and
   * cataloging each community entry found.
   *
   * @param elemRoot the root element containing the communities data, never <code>null</code>.
   * @throws PSUnknownNodeTypeException if the supplied element does not match the expected schema.
   */
  public void fromXml(Element elemRoot) throws PSUnknownNodeTypeException {
    Collection<Community> parsed = parseCommunities(elemRoot);
    m_collCommunities.clear();
    m_collCommunities.addAll(parsed);
  }

  /**
   * Pure parse of community catalog XML into a new mutable collection. Package-private for unit
   * tests; used by the URL constructor and {@link #fromXml(Element)} so constructors never call
   * overridable instance methods.
   *
   * @param elemRoot the root element containing the communities data, never <code>null</code>
   * @return newly allocated collection of communities, never <code>null</code>
   * @throws PSUnknownNodeTypeException if the element does not match the expected schema
   */
  static Collection<Community> parseCommunities(Element elemRoot)
      throws PSUnknownNodeTypeException {
    Collection<Community> communities = new ArrayList<>();

    PSXMLDomUtil.checkNode(elemRoot, XML_ELEM_ROOT);

    NodeList nl = elemRoot.getElementsByTagName(XML_ELEM_LIST);
    if (nl == null || nl.getLength() <= 0)
      throw new IllegalArgumentException("must have at least one community");

    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);
      if (n.getNodeType() != Node.ELEMENT_NODE) continue;

      communities.add(new Community((Element) n));
    }

    return communities;
  }

  /**
   * Returns an unmodifiable view of all cataloged community instances.
   *
   * @return unmodifiable collection of cataloged Community instances, never {@code null}
   */
  public Collection<Community> getCommunities() {
    return Collections.unmodifiableCollection(m_collCommunities);
  }

  /**
   * Allows to create a community instance from passed params, main use is for the Cx UI to create
   * an new Community instance for the special community id = -1. This instance is not cached or
   * owned by the cataloger.
   *
   * @param id community Id
   * @param name, never <code>null</code>
   * @param description, may be <code>null</code>
   * @return newly created community instance, the itself cataloger doesn't own this instance.
   */
  public static Community createCommunity(int id, String name, String description) {
    return new Community(id, name, description);
  }

  /**
   * Collection of cataloged Community instances. Final reference; contents are replaced via clear /
   * addAll so constructors never reassign after init (javac {@code this-escape}).
   */
  private final Collection<Community> m_collCommunities;

  /** The XML root element name containing all cataloged communities. */
  public static final String XML_ELEM_ROOT = "communities";

  /** The XML element name for a single community entry within the list. */
  public static final String XML_ELEM_LIST = "list";

  /** The XML element name for the community name. */
  public static final String XML_ELEM_COMMUNITYNAME = "communityname";

  /** XML element name for the community identifier. */
  public static final String XML_ELEM_COMMUNITYID = "communityid";

  /** XML element name for the community description. */
  public static final String XML_ELEM_COMMUNITYDESC = "communitydesc";
}
