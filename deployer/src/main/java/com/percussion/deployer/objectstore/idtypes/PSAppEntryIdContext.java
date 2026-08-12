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

package com.percussion.deployer.objectstore.idtypes;

import com.percussion.deployer.objectstore.IPSDeployComponent;
import com.intsof.percussioncms.auditlog.codes.ObjectStoreErrorCodes;
import com.percussion.design.objectstore.PSEntry;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.xml.PSXmlTreeWalker;
import java.text.MessageFormat;
import java.util.Optional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** ID Context to represent a <code>PSEntry</code> object */
public final class PSAppEntryIdContext extends PSApplicationIdContext {
  /**
   * Construct this context from the entry object
   *
   * @param entry The entry, may not be <code>null</code>.
   * @throws IllegalArgumentException if <code>call</code> is <code>null</code>.
   */
  public PSAppEntryIdContext(PSEntry entry) {
    if (entry == null) throw new IllegalArgumentException("entry may not be null");

    m_entry = entry;
    m_origEntry = entry;
  }

  /**
   * Create this object from its XML representation
   *
   * @param source The source element. See {@link #toXml(Document)} for the expected format. May not
   *     be <code>null</code>.
   * @throws IllegalArgumentException If <code>source</code> is <code>null</code>.
   * @throws PSUnknownNodeTypeException <code>source</code> is malformed.
   */
  public PSAppEntryIdContext(Element source) throws PSUnknownNodeTypeException {
    if (source == null) throw new IllegalArgumentException("source may not be null");

    fromXml(source);
  }

  /**
   * Get the current entry represented by this context.
   *
   * @return The entry, never <code>null</code>.
   */
  public PSEntry getEntry() {
    return m_entry;
  }

  /**
   * Get the entry this object was constructed with.
   *
   * @return The entry, never <code>null</code>.
   */
  public PSEntry getOriginalEntry() {
    return m_origEntry;
  }

  // see PSApplicationIdContext
  public String getDisplayText() {

    String text =
        MessageFormat.format(
            getBundle().getString("appIdEntry"),
            new Object[] {
              m_entry.getLabel().getText(),
              m_entry.getValue(),
              String.valueOf(m_entry.getSequence())
            });
    text = addParentDisplayText(text);

    return text;
  }

  // see PSApplicationIdContext
  public void updateCtxValue(Object value) {
    if (value == null) throw new IllegalArgumentException("value may not be null");

    if (!(value instanceof PSEntry))
      throw new IllegalArgumentException("value must be instanceof PSEntry");

    m_entry = (PSEntry) value;
  }

  /**
   * Serializes this object's state to its XML representation. The format is:
   * <!--
   *    PSXApplicationIdContext is a place holder for the root node of the XML
   *    representation of any class derived from PSApplicationIdContext that
   *    is this context's parent context.
   * -->
   *
   * <pre><code>
   * &lt;!ELEMENT PSXAppEntryIdContext (PSXApplicationIDContext?)>
   * &lt;!ATTLIST PSXAppEntryIdContext
   *    name CDATA #IMPLIED
   * >
   * </code></pre>
   *
   * See {@link IPSDeployComponent#toXml(Document)} for more info.
   */
  public Element toXml(Document doc) {
    if (doc == null) {
      throw new IllegalArgumentException("doc should not be null");
    }

    var root = doc.createElement(XML_NODE_NAME);
    root.appendChild(m_entry.toXml(doc));
    if (getParentCtx() != null) {
      root.appendChild(getParentCtx().toXml(doc));
    }
    return root;
  }

  /**
   * Restores this object's state from its XML representation. See {@link #toXml(Document)} for
   * format of XML. See {@link IPSDeployComponent#fromXml(Element)} for more info on method
   * signature.
   */
  public void fromXml(Element sourceNode) throws PSUnknownNodeTypeException {
    if (sourceNode == null) {
      throw new IllegalArgumentException("sourceNode should not be null");
    }

    if (!XML_NODE_NAME.equals(sourceNode.getNodeName())) {
      throw new PSUnknownNodeTypeException(
          ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE,
          new Object[] {XML_NODE_NAME, sourceNode.getNodeName()});
    }

    var tree = new PSXmlTreeWalker(sourceNode);
    var entryEl =
        Optional.ofNullable(tree.getNextElement(PSXmlTreeWalker.GET_NEXT_ALLOW_CHILDREN))
            .orElseThrow(
                () ->
                    new PSUnknownNodeTypeException(
                        ObjectStoreErrorCodes.XML_ELEMENT_INVALID_CHILD,
                        new Object[] {XML_NODE_NAME, "null", "null"}));
    m_entry = new PSEntry(entryEl, null, null);
    m_origEntry = m_entry;

    var ctxEl = tree.getNextElement(PSXmlTreeWalker.GET_NEXT_ALLOW_SIBLINGS);
    if (ctxEl != null) {
      setParentCtx(PSApplicationIDContextFactory.fromXml(ctxEl));
    }
  }

  // see IPSDeployComponent interface
  public void copyFrom(IPSDeployComponent obj) {
    if (obj == null) throw new IllegalArgumentException("obj may not be null");

    if (!(obj instanceof PSAppEntryIdContext)) throw new IllegalArgumentException("obj wrong type");

    PSAppEntryIdContext other = (PSAppEntryIdContext) obj;
    m_entry = other.m_entry;
    m_origEntry = m_entry;
    super.copyFrom(other);
  }

  // see IPSDeployComponent interface
  public boolean equals(Object obj) {
    boolean isEqual = true;

    if (!(obj instanceof PSAppEntryIdContext)) isEqual = false;
    else {
      PSAppEntryIdContext other = (PSAppEntryIdContext) obj;
      if (!m_entry.equals(other.m_entry)) isEqual = false;
      else if (!super.equals(other)) isEqual = false;
    }

    return isEqual;
  }

  // see IPSDeployComponent
  public int hashCode() {
    return m_entry.hashCode() + super.hashCode();
  }

  /**
   * Entry this context represents. Never <code>null</code> after ctor, modified by a calls to
   * <code>copyFrom()</code> and <code>updateCtxValue()</code>.
   */
  private PSEntry m_entry;

  /**
   * The entry this context represented at construction time, initially the same as {@link
   * #m_entry}, but immutable after contruction. This value is not used as part of {@link
   * #equals(Object)}, {@link #hashCode()}, nor is it serialized to and from this object's XML
   * representation.
   */
  private PSEntry m_origEntry;

  /** Root node name of this object's XML representation. */
  public static final String XML_NODE_NAME = "PSXAppEntryIdContext";
}
