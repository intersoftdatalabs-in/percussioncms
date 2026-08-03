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

import com.percussion.design.objectstore.IPSObjectStoreErrors;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import org.w3c.dom.Element;

/** Class to restore {@link PSApplicationIdContext} objects from their XML state. */
public class PSApplicationIDContextFactory {

  /** Default constructor for use via static methods. */
  public PSApplicationIDContextFactory() {}

  /**
   * Creates an application id context object from its xml representatation.
   *
   * @param sourceNode the XML element node to populate from, may not be <code>null</code>.
   * @return the new application id context, never <code>null</code>.
   * @throws IllegalArgumentException if <code>soureNode</code> is <code>null</code>.
   * @throws PSUnknownNodeTypeException if the XML element node does not represent a type supported
   *     by the class.
   */
  public static PSApplicationIdContext fromXml(Element sourceNode)
      throws PSUnknownNodeTypeException {
    if (sourceNode == null) {
      throw new IllegalArgumentException("sourceNode may not be null");
    }

    var nodeName = sourceNode.getNodeName();
    return switch (nodeName) {
      case PSAppCEItemIdContext.XML_NODE_NAME -> new PSAppCEItemIdContext(sourceNode);
      case PSAppConditionalIdContext.XML_NODE_NAME -> new PSAppConditionalIdContext(sourceNode);
      case PSAppDataMappingIdContext.XML_NODE_NAME -> new PSAppDataMappingIdContext(sourceNode);
      case PSAppDisplayMapperIdContext.XML_NODE_NAME -> new PSAppDisplayMapperIdContext(sourceNode);
      case PSAppEntryIdContext.XML_NODE_NAME -> new PSAppEntryIdContext(sourceNode);
      case PSAppExtensionCallIdContext.XML_NODE_NAME -> new PSAppExtensionCallIdContext(sourceNode);
      case PSAppExtensionParamIdContext.XML_NODE_NAME ->
          new PSAppExtensionParamIdContext(sourceNode);
      case PSAppIndexedItemIdContext.XML_NODE_NAME -> new PSAppIndexedItemIdContext(sourceNode);
      case PSAppNamedItemIdContext.XML_NODE_NAME -> new PSAppNamedItemIdContext(sourceNode);
      case PSAppUISetIdContext.XML_NODE_NAME -> new PSAppUISetIdContext(sourceNode);
      case PSAppUrlRequestIdContext.XML_NODE_NAME -> new PSAppUrlRequestIdContext(sourceNode);
      case PSBindingParamIdContext.XML_NODE_NAME -> new PSBindingParamIdContext(sourceNode);
      case PSBindingIdContext.XML_NODE_NAME -> new PSBindingIdContext(sourceNode);
      default ->
          throw new PSUnknownNodeTypeException(
              IPSObjectStoreErrors.XML_ELEMENT_WRONG_TYPE,
              new Object[] {"PSXApplicationIDContext", nodeName});
    };
  }
}
