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

// REFACTORED: CP-JAVA11
package com.percussion.rx.publisher.jsf.nodes;

import com.percussion.rx.jsf.PSCategoryNodeBase;
import com.percussion.services.sitemgr.IPSSite;

/**
 * The runtime edition list node for each site.
 */
public class PSRuntimeEditionListNode extends PSCategoryNodeBase
{
   /**
    * Constructs a runtime edition list node for a site.
    * @param site the site, never null
    */
   public PSRuntimeEditionListNode(IPSSite site) {
      super("Editions", "pub-runtime-editionlist");
      if (site == null) throw new IllegalArgumentException("site may not be null.");
      setKey("Editions-" + site.getGUID().longValue());
   }
   
   /**
    * Gets the help topic for this node.
    */
   @Override
   public String getHelpTopic() {
      return "RuntimeEditionList";
   }
}
