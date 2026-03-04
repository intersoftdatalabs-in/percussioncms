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
package com.percussion.install;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.xml.PSXmlDocumentBuilder;

import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class PSUpgradePluginUpdateCategoryTreeTest
{
   @Test
   public void test() throws Exception
   {
      InputStream in = null;
      try
      {
         PSUpgradePluginUpdateCategoryTree plugin = new PSUpgradePluginUpdateCategoryTree();
         plugin.setLogger(System.out);
         in = getClass().getResourceAsStream("/com/percussion/rxupgrade/CategoryTree.xml");
         if (in == null) {
            throw new IllegalArgumentException("CategoryTree.xml not found in classpath");
         }
         Document doc = PSXmlDocumentBuilder.createXmlDocument(in, false);
         doc = plugin.updateDocument(doc);
         NodeList nodes = doc.getElementsByTagName("Node");
         for(int i=0; i<nodes.getLength();i++)
         {
            Element node = (Element) nodes.item(i);
            String label = StringUtils.defaultString(node.getAttribute("label"));
            String id = StringUtils.defaultString(node.getAttribute("id"));
            assertTrue(label.equals(id));
         }
      }
      finally
      {
         if(in != null)
         {
            in.close();
         }
      }
   }

}
