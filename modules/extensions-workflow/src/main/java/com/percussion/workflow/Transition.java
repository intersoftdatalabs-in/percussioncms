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

import java.util.HashMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Represents a workflow transition between two states in the Rhythmyx system.
 *
 * <p>This class encapsulates a workflow transition element and provides methods for accessing its
 * properties and generating XML representations. Transitions define how content moves between
 * workflow states and can have various types (e.g., approval, rejection, automatic).
 *
 * <p>Transitions are used in conjunction with {@link State} objects to build workflow
 * visualizations and process workflow actions.
 *
 * @see State
 * @see PreviewWorkflow
 */
public class Transition {

  /** The DOM element containing transition attributes */
  Element m_ElemTransition = null;

  /** Constant for line image type */
  static final String LINE = "line";

  /** Constant for middle line image type */
  static final String MLINE = "mline";

  /** Constant for blank image type */
  static final String BLANK = "blank";

  /** Constant for vertical line image type */
  static final String VLINE = "vline";

  /** Constant for self-loop image type */
  static final String SELF = "self";

  /** Constant for right arrow image type */
  static final String RARROW = "rarrow";

  /** Constant for left arrow image type */
  static final String LARROW = "larrow";

  /**
   * Creates a new Transition instance from a DOM element.
   *
   * @param elemTransition the DOM element containing transition attributes
   */
  public Transition(Element elemTransition) {
    m_ElemTransition = elemTransition;
  }

  /**
   * Gets the unique identifier for this transition.
   *
   * @return the transition ID
   */
  public String getID() {
    return m_ElemTransition.getAttribute("id");
  }

  /**
   * Gets the URL link associated with this transition.
   *
   * @return the link URL, or empty string if not set
   */
  public String getLink() {
    return m_ElemTransition.getAttribute("link");
  }

  /**
   * Gets the display label for this transition.
   *
   * @return the transition label, or empty string if not set
   */
  public String getLabel() {
    return m_ElemTransition.getAttribute("label");
  }

  /**
   * Gets the trigger type for this transition.
   *
   * <p>The trigger defines what causes this transition to execute, such as automatic, manual, or
   * event-based triggers.
   *
   * @return the trigger type, or empty string if not set
   */
  public String getTrigger() {
    return m_ElemTransition.getAttribute("trigger");
  }

  /**
   * Gets the source state ID for this transition.
   *
   * @return the "from" state ID
   */
  public String getFrom() {
    return m_ElemTransition.getAttribute("from");
  }

  /**
   * Gets the target state ID for this transition.
   *
   * @return the "to" state ID
   */
  public String getTo() {
    return m_ElemTransition.getAttribute("to");
  }

  /**
   * Gets the type of this transition.
   *
   * <p>Common transition types include approval, rejection, and workflow steps.
   *
   * @return the transition type, or empty string if not set
   */
  public String getType() {
    return m_ElemTransition.getAttribute("type");
  }

  /**
   * Constructs the image filename for a given GIF name and transition type.
   *
   * @param gifName the base GIF name
   * @return the complete GIF filename (e.g., "line_approval.gif")
   */
  private String makeGif(String gifName) {
    return gifName + "_" + getType() + ".gif";
  }

  /**
   * Creates an XML element representing this transition for inclusion in a workflow document.
   *
   * <p>This method generates the visual representation of the transition, including the appropriate
   * arrows and lines based on whether the transition is forward, backward, or self-referencing.
   *
   * @param elemTransitions the parent element to which the new transition element will be appended
   * @param statesMap a map of state IDs to State objects for reference
   * @param height the height of the transition elements
   * @return a new Element representing this transition, ready for insertion into a document
   */
  public Element makeElement(
      Element elemTransitions, HashMap<String, State> statesMap, int height) {
    int indexFrom = statesMap.get(getFrom()).getIndex();
    int indexTo = statesMap.get(getTo()).getIndex();
    boolean bForward = (indexTo > indexFrom) ? true : false;
    int indexBegin = indexFrom;
    int indexEnd = indexTo;
    if (!bForward) {
      indexBegin = indexTo;
      indexEnd = indexFrom;
    }

    Document doc = elemTransitions.getOwnerDocument();
    Element elemTransition = doc.createElement("transition");
    elemTransition.setAttribute("label", getLabel());
    elemTransition.setAttribute("link", getLink());
    elemTransition.setAttribute("type", getType());
    Object[] keys = statesMap.keySet().toArray();
    State state = null;
    String key = "";
    Element elem = null;
    int last = keys.length - 1;
    int midWidth = Math.round(20 * PreviewWorkflow.scale);
    int ii;
    for (int i = 0; i <= last; i++) {
      key = keys[i].toString();
      state = statesMap.get(key);
      ii = i; // state.getIndex();
      if (0 == ii) {
        elem = doc.createElement("draw");
        elem.setAttribute("image", makeGif(BLANK));
        elem.setAttribute("width", Integer.toString((state.getWidth() - midWidth) / 2));
        elem.setAttribute("height", Integer.toString(height));
        elemTransition.appendChild(elem);
      }

      if (ii == indexBegin && ii == indexEnd) // self transition
      {
        elem = doc.createElement("draw");
        elem.setAttribute("image", makeGif(SELF));
        elem.setAttribute("width", Integer.toString(midWidth));
        elem.setAttribute("height", Integer.toString(height));
        elemTransition.setAttribute(
            "xloc", Integer.toString(state.getWidth() * ii + state.getWidth() * 1 / 2 + midWidth));
        elemTransition.appendChild(elem);

        elem = doc.createElement("draw");
        elem.setAttribute("image", makeGif(BLANK));
        if (ii == last)
          elem.setAttribute("width", Integer.toString((state.getWidth() - midWidth) / 2));
        else elem.setAttribute("width", Integer.toString(state.getWidth() - midWidth));
        elem.setAttribute("height", Integer.toString(height));
        elemTransition.appendChild(elem);
      } else if (ii == indexBegin) // beginning of transition
      {
        elem = doc.createElement("draw");
        if (bForward) elem.setAttribute("image", makeGif(VLINE));
        else {
          elem.setAttribute("image", makeGif(LARROW));
          elemTransition.setAttribute(
              "xloc", Integer.toString(state.getWidth() * ii + state.getWidth() * 3 / 4));
        }

        elem.setAttribute("width", Integer.toString(midWidth));
        elem.setAttribute("height", Integer.toString(height));
        elemTransition.appendChild(elem);

        elem = doc.createElement("draw");
        elem.setAttribute("image", makeGif(LINE));

        if (ii == last)
          elem.setAttribute("width", Integer.toString((state.getWidth() - midWidth) / 2));
        else elem.setAttribute("width", Integer.toString(state.getWidth() - midWidth));

        elem.setAttribute("height", Integer.toString(height));
        elemTransition.appendChild(elem);
      } else if (ii == indexEnd) // end of the transition
      {
        elem = doc.createElement("draw");
        if (bForward) {
          elem.setAttribute("image", makeGif(RARROW));
          elemTransition.setAttribute("xloc", Integer.toString(state.getWidth() * ii));
        } else elem.setAttribute("image", makeGif(VLINE));

        elem.setAttribute("width", Integer.toString(midWidth));
        elem.setAttribute("height", Integer.toString(height));
        elemTransition.appendChild(elem);

        elem = doc.createElement("draw");
        elem.setAttribute("image", makeGif(BLANK));
        if (ii == last)
          elem.setAttribute("width", Integer.toString((state.getWidth() - midWidth) / 2));
        else elem.setAttribute("width", Integer.toString(state.getWidth() - midWidth));

        elem.setAttribute("height", Integer.toString(height));
        elemTransition.appendChild(elem);
      } else if (ii < indexBegin || ii > indexEnd) // no transition
      {
        elem = doc.createElement("draw");
        elem.setAttribute("image", makeGif(VLINE));
        elem.setAttribute("width", Integer.toString(midWidth));
        elem.setAttribute("height", Integer.toString(height));
        elemTransition.appendChild(elem);

        elem = doc.createElement("draw");
        elem.setAttribute("image", makeGif(BLANK));
        if (ii == last)
          elem.setAttribute("width", Integer.toString((state.getWidth() - midWidth) / 2));
        else elem.setAttribute("width", Integer.toString(state.getWidth() - midWidth));
        elem.setAttribute("height", Integer.toString(height));
        elemTransition.appendChild(elem);
      } else if (ii > indexBegin && ii < indexEnd) // middle of the transition
      {
        elem = doc.createElement("draw");
        elem.setAttribute("image", makeGif(MLINE));
        elem.setAttribute("width", Integer.toString(midWidth));
        elem.setAttribute("height", Integer.toString(height));
        elemTransition.appendChild(elem);

        elem = doc.createElement("draw");
        elem.setAttribute("image", makeGif(LINE));
        elem.setAttribute("width", Integer.toString(state.getWidth() - midWidth));
        elem.setAttribute("height", Integer.toString(height));
        elemTransition.appendChild(elem);
      }
    }
    return elemTransition;
  }
}
