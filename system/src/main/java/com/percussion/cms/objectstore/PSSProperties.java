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

// REFACTORED: CP-JAVA11
package com.percussion.cms.objectstore;

import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import org.w3c.dom.Element;

/** See base class {@link com.percussion.cms.objectstore.PSDbComponentList} for details. */
public class PSSProperties extends PSDbComponentCollection {
  /** See base class {@link com.percussion.cms.objectstore.PSDbComponentList} for description. */
  public PSSProperties() {
    super(PSSearchMultiProperty.class);
  }

  /**
   * See base class {@link com.percussion.cms.objectstore.PSDbComponentCollection} for description.
   *
   * <p>Passes {@link #XML_NODE_NAME} explicitly: the base Element path uses a class-derived default
   * ({@code PSXSProperties}) to avoid virtual {@link #getNodeName()} during construction
   * (this-escape). Wire XML and {@link #toXml} use {@code PSX_PROPERTIES}, so the expected root
   * must be supplied here.
   */
  public PSSProperties(Element source) throws PSUnknownNodeTypeException {
    super(source, XML_NODE_NAME);
  }

  /**
   * Get the XML element name used for this class.
   *
   * @return the XML element name, never <code>null</code> or empty.
   */
  public String getNodeName() {
    return XML_NODE_NAME;
  }

  /** The XML node name for this class. */
  public static final String XML_NODE_NAME = "PSX_PROPERTIES";
}
