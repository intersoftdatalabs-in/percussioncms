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
package com.percussion.cms.objectstore;

import com.intsof.percussioncms.auditlog.codes.ObjectStoreErrorCodes;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import java.util.Iterator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class represents a <code>PSObjectAcl</code> for a folder. It is used when restoring a
 * folder's acl from the database without the context of the enclosing folder object, so that it may
 * provide any folder level information required for calculating security permissions at runtime.
 */
public class PSFolderAcl extends PSObjectAcl {
  /**
   * The default constructor to create an empty folder ACL.
   *
   * @param contentId The id of the folder for which this ACL defines security settings.
   * @param communityId The community id of the folder for which this ACL defines security settings.
   */
  public PSFolderAcl(int contentId, int communityId) {
    super();
    m_contentId = contentId;
    m_communityId = communityId;
  }

  /**
   * Constructs a folder ACL by copying ACL entries from a folder's {@link PSObjectAcl} and
   * attaching the folder's content and community ids. Prefer this over round-tripping through
   * {@code toXml()}/{@code fromXml()} when converting a live folder ACL for cache use.
   *
   * <p>Does not invent a separate XML root name: folder ACLs share the {@code PSXObjectAcl} wire
   * format of {@link PSObjectAcl}.
   *
   * @param source the object ACL to copy, may not be <code>null</code>
   * @param contentId The id of the folder for which this ACL defines security settings.
   * @param communityId The community id of the folder for which this ACL defines security settings.
   * @throws IllegalArgumentException if <code>source</code> is <code>null</code>
   */
  public PSFolderAcl(PSObjectAcl source, int contentId, int communityId) {
    super();
    if (source == null) {
      throw new IllegalArgumentException("source may not be null");
    }
    m_contentId = contentId;
    m_communityId = communityId;
    copyEntriesFrom(source);
  }

  /**
   * Just like {@link #PSFolderAcl(Element)}, except content and community ids are supplied by the
   * caller rather than read from element attributes. Accepts a {@code PSXObjectAcl} root element
   * (the wire format produced by {@link PSObjectAcl#toXml(Document)}).
   *
   * @param element the element to load from, may not be <code>null</code>
   * @param contentId The id of the folder for which this ACL defines security settings.
   * @param communityId The community id of the folder for which this ACL defines security settings.
   * @throws IllegalArgumentException if element is <code>null</code>
   * @throws PSUnknownNodeTypeException if element is not of expected format.
   */
  public PSFolderAcl(Element element, int contentId, int communityId)
      throws PSUnknownNodeTypeException {
    // Empty super first: Element construction via PSDbComponentSet uses defaultNodeNameFor
    // (PSFolderAcl → PSXFolderAcl), which does not match the shared PSXObjectAcl wire format.
    // After full construction, fromXml honors PSObjectAcl.getNodeName() → PSXObjectAcl.
    super();
    if (element == null) {
      throw new IllegalArgumentException("element may not be null");
    }
    super.fromXml(element);
    m_contentId = contentId;
    m_communityId = communityId;
  }

  /**
   * Constructs this object from the supplied element. See <code>fromXml()</code> for the expected
   * form of xml. Root element name is {@code PSXObjectAcl} (same as {@link PSObjectAcl}), with
   * additional {@code contentId} and {@code communityId} attributes.
   *
   * @param element the element to load from, may not be <code>null</code>
   * @throws IllegalArgumentException if element is <code>null</code>
   * @throws PSUnknownNodeTypeException if element is not of expected format.
   */
  public PSFolderAcl(Element element) throws PSUnknownNodeTypeException {
    super();
    if (element == null) {
      throw new IllegalArgumentException("element may not be null");
    }
    fromXml(element);
  }

  /**
   * Constructs this object from the supplied element. The xml format is that expected by <code>
   * PSXObjectAcl.fromXml()</code> with additional "contentid" and "communityId" attributes set on
   * the root element.
   *
   * @param src the element to load from, may not be <code>null</code>
   * @throws IllegalArgumentException if <code>src</code> is <code>null</code>
   * @throws PSUnknownNodeTypeException if element is not of expected format.
   */
  public void fromXml(Element src) throws PSUnknownNodeTypeException {
    super.fromXml(src);
    loadState(src);
  }

  /*
   *  (non-Javadoc)
   * @see com.percussion.cms.objectstore.IPSCmsComponent#toXml(org.w3c.dom.Document)
   */
  public Element toXml(Document doc) {
    Element root = super.toXml(doc);
    root.setAttribute(XML_ATTR_COMMUNITYID, String.valueOf(m_communityId));
    root.setAttribute(XML_ATTR_CONTENTID, String.valueOf(m_contentId));

    return root;
  }

  /**
   * Get the community id of the folder for which this ACL defines security settings.
   *
   * @return The community id, or <code>-1</code> if the folder is accessable by all communities.
   */
  public int getCommunityId() {
    return m_communityId;
  }

  /**
   * Get the id of the folder for which this ACL defines security settings.
   *
   * @return The id.
   */
  public int getContentId() {
    return m_contentId;
  }

  /**
   * Extract folder acl state from the provided element.
   *
   * @param src The element containing the folder acl state, assumed not <code>null</code>. See
   *     <code>fromXml()</code> for the expected form of the xml.
   * @throws PSUnknownNodeTypeException if the expected values cannot be found.
   */
  private void loadState(Element src) throws PSUnknownNodeTypeException {
    m_communityId = getIntAttrVal(src, XML_ATTR_COMMUNITYID);
    m_contentId = getIntAttrVal(src, XML_ATTR_CONTENTID);
  }

  /**
   * Copies ACL entries from {@code source} into this set. Entries are cloned so the source
   * collection is not shared with the folder ACL cache entry.
   *
   * @param source never {@code null}
   */
  private void copyEntriesFrom(PSObjectAcl source) {
    Iterator<PSObjectAclEntry> entries = source.iterator();
    while (entries.hasNext()) {
      add((PSObjectAclEntry) entries.next().clone());
    }
  }

  /**
   * Get the specified attribute value as an integer.
   *
   * @param src The element containing the attribute, assumed not <code>null</code>.
   * @param attrName The name of the attribute, assumed not <code>null</code> or empty.
   * @return The attribute value.
   * @throws PSUnknownNodeTypeException if the expected attribute value cannot be found or does not
   *     represent an integer value.
   */
  private int getIntAttrVal(Element src, String attrName) throws PSUnknownNodeTypeException {
    String temp = PSComponentUtils.getRequiredAttribute(src, attrName);
    try {
      return Integer.parseInt(temp);
    } catch (Exception ex) {
      Object[] args = {src.getNodeName(), attrName, temp};
      throw new PSUnknownNodeTypeException(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_ATTR, args);
    }
  }

  /**
   * The community id of this acl's folder, or <code>-1</code> if the folder is accessable by all
   * communities. Set during the ctor, never modified after that.
   */
  private int m_communityId;

  /** The content id of this acl's folder, set during the ctor, never modified after that. */
  private int m_contentId;

  private static final String XML_ATTR_COMMUNITYID = "communityId";
  private static final String XML_ATTR_CONTENTID = "contentId";
}
