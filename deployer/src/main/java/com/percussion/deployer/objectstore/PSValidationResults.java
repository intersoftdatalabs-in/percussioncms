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

package com.percussion.deployer.objectstore;

import com.percussion.design.objectstore.IPSObjectStoreErrors;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.xml.PSXmlTreeWalker;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Encapsulates a list of <code>PSValidationResult</code> objects, and a list of absent ancestors
 * (as <code>PSDependency</code>) objects.
 */
public class PSValidationResults implements IPSDeployComponent {
  /** Constructing the default object. */
  public PSValidationResults() {}

  /**
   * Create this object from its XML representation
   *
   * @param source The source element. See {@link #toXml(Document)} for the expected format. May not
   *     be <code>null</code>.
   * @throws IllegalArgumentException If <code>source</code> is <code>null</code>.
   * @throws PSUnknownNodeTypeException <code>source</code> is malformed.
   */
  public PSValidationResults(Element source) throws PSUnknownNodeTypeException {
    if (source == null) throw new IllegalArgumentException("source may not be null");

    fromXml(source);
  }

  /**
   * Get a list of <code>PSValidationResult</code> objects.
   *
   * @return A list of <code>PSValidationResult</code> objects. It will never be <code>null</code>,
   *     but may be empty.
   */
  public Iterator<PSValidationResult> getResults() {
    return m_validateResults.iterator();
  }

  /**
   * Get a result for the specified dependency.
   *
   * @param dep The dependency for which a result is to be returned, may not be <code>null</code>.
   * @return The result, or <code>null</code> if no result has been added for the specified
   *     dependency.
   * @throws IllegalArgumentException if <code>dep</code> is <code>null</code>.
   */
  public PSValidationResult getResult(PSDependency dep) {
    if (dep == null) throw new IllegalArgumentException("dep may not be null");

    PSValidationResult result = null;

    String depKey = dep.getKey();
    for (PSValidationResult test : m_validateResults) {
      if (test.getDependency().getKey().equals(depKey)) result = test;
    }

    return result;
  }

  /**
   * Adds a <code>PSValidationResult</code> to this object.
   *
   * @param result The object to be added, it may not be <code>null</code>.
   * @throws IllegalArgumentException if <code>result</code> is <code>null</code>.
   */
  public void addResult(PSValidationResult result) {
    if (result == null) throw new IllegalArgumentException("result may not be null");

    m_validateResults.add(result);
  }

  /**
   * Determines if any result object is an error.
   *
   * @return <code>true</code> if any result object is an error; <code>false</code> otherwise.
   */
  public boolean hasErrors() {
    return m_validateResults.stream().anyMatch(PSValidationResult::isError);
  }

  /**
   * Get a list of dependency objects.
   *
   * @return List of zero or more <code>PSDependency</code> objects. It will never be <code>null
   *     </code>, but may be empty.
   */
  public Iterator getAbsentAncestors() {
    return m_absentAncestors.iterator();
  }

  /**
   * Adds an dependency into the absent ancestor list if the dependency has not already been added.
   *
   * @param dep The dependency object to be added. It may not be <code>null</code>
   * @throws IllegalArgumentException if <code>dep</code> is <code>null</code>.
   */
  public void addAbsentAncestor(PSDependency dep) {
    if (dep == null) throw new IllegalArgumentException("dep may not be null");

    if (!m_absentAncestors.contains(dep)) m_absentAncestors.add(dep);
  }

  /**
   * Serializes this object's state to its XML representation. The format is:
   *
   * <pre><code>
   * &lt;!ELEMENT PSXValidationResults
   *    (PSXValidationResult*)
   * >
   * </code></pre>
   *
   * See {@link IPSDeployComponent#toXml(Document)} for more info.
   */
  public Element toXml(Document doc) {
    if (doc == null) {
      throw new IllegalArgumentException("doc may not be null");
    }

    var root = doc.createElement(XML_NODE_NAME);

    m_validateResults.forEach(vr -> root.appendChild(vr.toXml(doc)));
    m_absentAncestors.forEach(dep -> root.appendChild(dep.toXml(doc)));

    return root;
  }

  /**
   * Restores this object's state from its XML representation. See {@link #toXml(Document)} for
   * format of XML. See {@link IPSDeployComponent#fromXml(Element)} for more info on method
   * signature.
   *
   * @throws PSUnknownNodeTypeException if <code>sourceNode</code> is malformed XML.
   */
  public void fromXml(Element sourceNode) throws PSUnknownNodeTypeException {
    if (sourceNode == null) {
      throw new IllegalArgumentException("sourceNode may not be null");
    }

    if (!XML_NODE_NAME.equals(sourceNode.getNodeName())) {
      throw new PSUnknownNodeTypeException(
          IPSObjectStoreErrors.XML_ELEMENT_WRONG_TYPE,
          new Object[] {XML_NODE_NAME, sourceNode.getNodeName()});
    }

    var tree = new PSXmlTreeWalker(sourceNode);
    var childEl = tree.getNextElement(FIRST_FLAGS);

    m_validateResults.clear();
    while (childEl != null && PSValidationResult.XML_NODE_NAME.equals(childEl.getNodeName())) {
      m_validateResults.add(new PSValidationResult(childEl));
      childEl = tree.getNextElement(NEXT_FLAGS);
    }

    m_absentAncestors.clear();
    while (childEl != null
        && (PSDeployableElement.XML_NODE_NAME.equals(childEl.getNodeName())
            || PSDeployableObject.XML_NODE_NAME.equals(childEl.getNodeName())
            || PSUserDependency.XML_NODE_NAME.equals(childEl.getNodeName()))) {
      switch (childEl.getNodeName()) {
        case PSDeployableElement.XML_NODE_NAME ->
            m_absentAncestors.add(new PSDeployableElement(childEl));
        case PSDeployableObject.XML_NODE_NAME ->
            m_absentAncestors.add(new PSDeployableObject(childEl));
        case PSUserDependency.XML_NODE_NAME -> m_absentAncestors.add(new PSUserDependency(childEl));
      }
      childEl = tree.getNextElement(NEXT_FLAGS);
    }
  }

  // see IPSDeployComponent interface
  public void copyFrom(IPSDeployComponent obj) {
    if (obj == null) throw new IllegalArgumentException("obj may not be null");

    if ((obj instanceof PSValidationResults))
      throw new IllegalArgumentException("obj is not be PSValidationResults");

    PSValidationResults obj2 = (PSValidationResults) obj;

    m_absentAncestors.clear();
    m_absentAncestors.addAll(obj2.m_absentAncestors);
    m_validateResults.clear();
    m_validateResults.addAll(obj2.m_validateResults);
  }

  // see IPSDeployComponent interface
  @Override
  public int hashCode() {
    return m_absentAncestors.hashCode() + m_validateResults.hashCode();
  }

  // see IPSDeployComponent interface
  @Override
  public boolean equals(Object obj) {
    boolean isEqual = false;

    if ((obj instanceof PSValidationResults)) {
      PSValidationResults obj2 = (PSValidationResults) obj;
      isEqual =
          m_absentAncestors.equals(obj2.m_absentAncestors)
              && m_validateResults.equals(obj2.m_validateResults);
    }
    return isEqual;
  }

  /** Root node name of this object's XML representation. */
  public static final String XML_NODE_NAME = "PSXValidationResults";

  /**
   * A list of Absent Ancestors (as <code>PSDependency</code> object). It will never be <code>null
   * </code>, but may be empty.
   */
  private List<PSDependency> m_absentAncestors = new ArrayList<>();

  /**
   * A list of <code>PSValidationResult</code> objects. It will never be <code>null</code>, but may
   * be empty.
   */
  private List<PSValidationResult> m_validateResults = new ArrayList<>();

  /** flags to walk to a child node of a XML tree */
  private static final int FIRST_FLAGS =
      PSXmlTreeWalker.GET_NEXT_ALLOW_CHILDREN | PSXmlTreeWalker.GET_NEXT_RESET_CURRENT;

  /** flags to walk to a sibling node of a XML tree */
  private static final int NEXT_FLAGS =
      PSXmlTreeWalker.GET_NEXT_ALLOW_SIBLINGS | PSXmlTreeWalker.GET_NEXT_RESET_CURRENT;
}
