/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.i18n.tmxdom;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * This class wraps the functionality of segment DOM element as an easy to use
 * TMX node. The TMX counterpart provides methods to manipulate the segment, the
 * most important one being to merge two nodes appying the merge configuration.
 */
public class PSTmxSegment
   extends PSTmxLeafNode
   implements IPSTmxSegment
{
   /**
    * Constructor. Takes the parent TMX document object and the DOM element
    * representing the segment. The value of the segment is constructed from the
    * supplied DOM element.
    * @param tmxdoc parent TMX document, nust not be <code>null</code>.
    * @param seg DOM element for the segment to be contstructed, must not be
    * <code>null</code>.
    * @throws IllegalArgumentException if tmxDoc or seg is <code>null</code>
    */
   PSTmxSegment(IPSTmxDocument tmxdoc, Element seg)
   {
      if(tmxdoc == null)
         throw new IllegalArgumentException("tmxdoc must not be null");
      if(seg == null)
         throw new IllegalArgumentException("seg must not be null");

      m_PSTmxDocument = tmxdoc;
      m_DOMElement = seg;
      Node node = m_DOMElement.getFirstChild();
      if(node instanceof Text)
         m_Value = ((Text)node).getData();
   }

   /*
    * Implementation of the method defined in the interface
    */
   public void merge(IPSTmxNode node)
      throws PSTmxDomException
   {
      if(node == null)
      {
         throw new IllegalArgumentException("node must not be null for merging");
      }
      else if(!(node instanceof IPSTmxSegment))
      {
         throw new PSTmxDomException("onlyOneTypeAllowedToMerge",
            "IPSTmxSegment");
      }
      IPSTmxSegment seg = (IPSTmxSegment)node;
      //No merge rules below the Segment node. Just replace the value.
      setValue(seg.getValue());
   }
}
