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

package com.percussion.workflow;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * Represents a workflow state in the Rhythmyx system.
 * 
 * <p>This class encapsulates a workflow state element and provides methods for
 * accessing its properties and generating XML representations. Each state has
 * an index, dimensions, and various attributes like ID, name, and link information.</p>
 * 
 * <p>States are typically loaded from workflow configuration and used to build
 * the workflow editor UI or to process workflow transitions.</p>
 */
public class State {
  
  /** The index of this state in the workflow */
  int m_nIndex;
  
  /** The underlying DOM element containing state attributes */
  Element m_ElemState;
  
  /** The width of this state for layout purposes */
  int m_nWidth;
  
  /** The height of this state for layout purposes */
  int m_nHeight;

  /**
   * Creates a new State instance with the specified parameters.
   * 
   * @param nIndex the index of this state in the workflow sequence
   * @param elem the DOM element containing state attributes
   * @param width the width of this state for rendering/layout purposes
   * @param height the height of this state for rendering/layout purposes
   */
  public State(int nIndex, Element elem, int width, int height) {
    m_nIndex = nIndex;
    m_ElemState = elem;
    m_nWidth = width;
    m_nHeight = height;
  }

  /**
   * Creates an XML element representing this state for inclusion in a workflow document.
   * 
   * <p>All attributes from the source state element are copied to the new element,
   * and additional positioning attributes (xloc, yloc, width) are set.</p>
   * 
   * @param elemParent the parent element for the new state element; used to obtain the owner document
   * @param yLoc the Y-axis location for the new state element
   * @return a new Element representing this state, ready for insertion into a document
   */
  public Element makeElement(Element elemParent, int yLoc) {
    Document doc = elemParent.getOwnerDocument();
    Element elemState = doc.createElement("state");
    NamedNodeMap attrs = m_ElemState.getAttributes();
    for (int i = 0; i < attrs.getLength(); i++) {
      Attr importNode = (Attr) doc.importNode(attrs.item(i), true);
      elemState.setAttributeNode(importNode);
    }
    elemState.setAttribute("xloc", Integer.toString(m_nIndex * m_nWidth));
    elemState.setAttribute("yloc", Integer.toString(yLoc));
    elemState.setAttribute("width", Integer.toString(m_nWidth));

    return elemState;
  }

  /**
   * Gets the unique identifier for this workflow state.
   * 
   * @return the state ID string, or empty string if not set
   */
  public String getID() {
    return m_ElemState.getAttribute("id");
  }

  /**
   * Gets the URL link associated with this state.
   * 
   * @return the link URL, or empty string if not set
   */
  public String getLink() {
    return m_ElemState.getAttribute("link");
  }

  /**
   * Gets the URL for creating a new transition from this state.
   * 
   * @return the new transition link URL, or empty string if not set
   */
  public String getLinkNewTransition() {
    return m_ElemState.getAttribute("linknewtransition");
  }

  /**
   * Gets the URL for creating a new aging transition from this state.
   * 
   * @return the new aging transition link URL, or empty string if not set
   */
  public String getLinkNewAgingTransition() {
    return m_ElemState.getAttribute("linknewagingtransition");
  }

  /**
   * Gets the index of this state in the workflow.
   * 
   * @return the zero-based index of this state
   */
  public int getIndex() {
    return m_nIndex;
  }

  /**
   * Gets the display name of this workflow state.
   * 
   * @return the state name, or empty string if not set
   */
  public String getName() {
    return m_ElemState.getAttribute("name");
  }

  /**
   * Gets the X coordinate of the center point of this state for layout purposes.
   * 
   * @return the middle X coordinate (index * width + width/2)
   */
  public int getMidX() {
    return m_nIndex * m_nWidth + m_nWidth / 2;
  }

  /**
   * Gets the width of this state.
   * 
   * @return the width in layout units
   */
  public int getWidth() {
    return m_nWidth;
  }
}
