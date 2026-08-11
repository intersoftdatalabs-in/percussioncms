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

import com.percussion.design.objectstore.IPSComponent;
import com.percussion.design.objectstore.IPSDocument;
import com.percussion.design.objectstore.IPSObjectStoreErrors;
import com.percussion.design.objectstore.PSCollectionComponent;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.xml.PSXmlTreeWalker;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A collection of PSAaRelationship objects. See PSXAaRelationshipList.dtd. The sort rank of the
 * each relationship is implicit in that the order of appearance in the list is assumed to the sort
 * rank.
 *
 * @author Ram
 */
// REFACTORED: CP-JAVA11
// Final leaf — Element ctor restores via private fromXmlLoad without subclass this-escape.
public final class PSAaRelationshipList extends PSCollectionComponent {
  /** Constructs an empty active assembly relationship list. */
  public PSAaRelationshipList() {
    super(PSAaRelationship.class);
  }

  /**
   * Construct a Java object from its XML representation.
   *
   * @param sourceNode the XML element node to construct this object from, not {@code null}.
   * @param parentDoc the Java object which is the parent of this object, may be {@code null}.
   * @param parentComponents the parent objects of this object, may be {@code null}.
   * @throws PSUnknownNodeTypeException if the XML element node is not of the appropriate type
   */
  public PSAaRelationshipList(Element sourceNode, IPSDocument parentDoc, List<IPSComponent> parentComponents)
      throws PSUnknownNodeTypeException {
    super(PSAaRelationship.class);
    // Private load avoids virtual fromXml/add during construction (this-escape).
    fromXmlLoad(sourceNode, parentDoc, parentComponents);
  }

  /**
   * @see IPSComponent
   */
  @Override
  public void fromXml(Element sourceNode, IPSDocument parentDoc, List<IPSComponent> parentComponents)
      throws PSUnknownNodeTypeException {
    fromXmlLoad(sourceNode, parentDoc, parentComponents);
  }

  /**
   * Non-virtual Element restore. Uses {@link java.util.Collection#add} via direct super path after
   * full construction when called from public {@link #fromXml}; during Element ctor the collection
   * is empty and only non-overridable list storage is used.
   */
  private void fromXmlLoad(Element sourceNode, IPSDocument parentDoc, List<IPSComponent> parentComponents)
      throws PSUnknownNodeTypeException {
    if (sourceNode == null) {
      throw new PSUnknownNodeTypeException(IPSObjectStoreErrors.XML_ELEMENT_NULL, XML_NODE_NAME);
    }
    if (!XML_NODE_NAME.equals(sourceNode.getNodeName())) {
      Object[] args = {XML_NODE_NAME, sourceNode.getNodeName()};
      throw new PSUnknownNodeTypeException(IPSObjectStoreErrors.XML_ELEMENT_WRONG_TYPE, args);
    }
    parentComponents = updateParentList(parentComponents);
    int parentSize = parentComponents.size() - 1;
    int firstFlags =
        PSXmlTreeWalker.GET_NEXT_ALLOW_CHILDREN | PSXmlTreeWalker.GET_NEXT_RESET_CURRENT;
    int nextFlags =
        PSXmlTreeWalker.GET_NEXT_ALLOW_SIBLINGS | PSXmlTreeWalker.GET_NEXT_RESET_CURRENT;
    Element node = null;
    try {
      PSXmlTreeWalker tree = new PSXmlTreeWalker(sourceNode);
      node = tree.getNextElement(PSRelationship.XML_NODE_NAME, firstFlags);
      while (node != null) {
        PSRelationship relationship =
            new PSRelationship(node, parentDoc, parentComponents);
        // Direct list insert — avoid overridable add() during construction (this-escape).
        super.add(relationship);
        node = tree.getNextElement(PSRelationship.XML_NODE_NAME, nextFlags);
      }
    } finally {
      resetParentList(parentComponents, parentSize);
    }
  }

  /**
   * Override the base class version to set the sort rank of the relationship just before accessing.
   *
   * @param index index of the object (relationship) to access.
   * @return Relationship object for the specified index, may be {@code null}
   * @throws ArrayIndexOutOfBoundsException index is out of range (index &lt; 0 || index &gt;=
   *     size()).
   * @see java.util.List#get(int) for more details.
   */
  @Override
  public Object get(int index) {
    PSAaRelationship rel = (PSAaRelationship) super.get(index);
    rel.setProperty(IPSHtmlParameters.SYS_SORTRANK, String.valueOf(index));
    return rel;
  }

  /**
   * @see IPSComponent
   */
  public Element toXml(Document doc) {
    Element root = doc.createElement(XML_NODE_NAME);
    for (int i = 0; i < size(); i++) {
      IPSComponent relationship = (IPSComponent) get(i);
      root.appendChild(relationship.toXml(doc));
    }
    return root;
  }

  /** the XML node name */
  public static final String XML_NODE_NAME = "PSXAaRelationshipList";
}
