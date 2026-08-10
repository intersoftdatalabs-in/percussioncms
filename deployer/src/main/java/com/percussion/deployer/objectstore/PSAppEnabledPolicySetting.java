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
package com.percussion.deployer.objectstore;

import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Encapsulates app enable policy. */
public final class PSAppEnabledPolicySetting extends PSAppPolicySetting {
  /**
   * Default constructor. Defaults to disabling the log policy, {@link #isAppEnabled()} returns
   * <code>false</code>.
   */
  public PSAppEnabledPolicySetting() {
    // Default constructor
  }

  /**
   * Create this object from its XML representation.
   *
   * @param source The source element. See {@link #toXml(Document)} for the expected format. May not
   *     be <code>null</code>.
   * @throws IllegalArgumentException If <code>source</code> is <code>null</code>.
   * @throws PSUnknownNodeTypeException If <code>source</code> is malformed.
   */
  public PSAppEnabledPolicySetting(Element source) throws PSUnknownNodeTypeException {
    if (source == null) {
      throw new IllegalArgumentException("source may not be null");
    }
    fromXml(source);
  }

  /**
   * Determines if the app policy is enabled.
   *
   * @return <code>true</code> if the app policy is enabled; <code>false</code> otherwise.
   */
  public boolean isAppEnabled() {
    return true; // No specific setting for this class yet, hardcoded for now
  }

  /**
   * Sets to enable or disable the app policy.
   *
   * @param isEnabled <code>true</code> if set to enable the app policy; <code>false</code>
   *     otherwise.
   */
  public void setAppEnabled(boolean isEnabled) {
    // No specific setting for this class yet, no-op for now
  }

  /** Serializes this object's state to its XML representation. */
  @Override
  public Element toXml(Document doc) {
    return toXml(doc, XML_NODE_NAME);
  }

  @Override
  public void fromXml(Element sourceNode) throws PSUnknownNodeTypeException {
    fromXml(sourceNode, XML_NODE_NAME);
  }

  /** Root node name of this object's XML representation. */
  public static final String XML_NODE_NAME = "PSXAppEnabledPolicySetting";
}
