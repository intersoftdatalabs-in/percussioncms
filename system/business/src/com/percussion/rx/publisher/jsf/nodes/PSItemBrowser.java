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
package com.percussion.rx.publisher.jsf.nodes;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.rx.ui.jsf.beans.PSHelpTopicMapping;
import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.content.PSContentWsLocator;

import java.util.ArrayList;
import java.util.List;

public class PSItemBrowser extends PSContentBrowser
{
   private PSSchemeJexlTestPanel m_testPanel = null;
   private static final String ITEM_BROWSER = "pub-design-item-browser";
   
   /**
    * Constructs an item browser for the JEXL test panel.
    * @param testPanel the JEXL test panel, may not be null
    */
   PSItemBrowser(PSSchemeJexlTestPanel testPanel) {
      if (testPanel == null) throw new IllegalArgumentException("testPanel may not be null.");
      m_testPanel = testPanel;
   }

   /**
    * Performs on a child item or folder. Browses subfolder or sets item path for test panel.
    */
   @Override
   protected String childPerform(ChildItem item) {
      if (item.isFolder()) return super.childPerform(item);
      m_testPanel.setItemPath(getPath() + "/" + item.getName());
      return m_testPanel.perform();
   }

   /**
    * Returns the outcome for the item browser page.
    */
   @Override
   protected String perform() {
      return ITEM_BROWSER;
   }
   
   /**
    * Gets all child items or folders for the current folder.
    * @return list of child items
    */
   @Override
   protected List<ChildItem> getChildItems() throws Exception {
      var cws = PSContentWsLocator.getContentWebservice();
      var mgr = PSGuidManagerLocator.getGuidMgr();
      var id = mgr.makeGuid(new PSLocator(getFolderId()));
      var summaries = cws.findFolderChildren(id, false);
      var items = new ArrayList<ChildItem>();
      for (var item : summaries) {
         var isFolder = item.getContentTypeId() == PSFolder.FOLDER_CONTENT_TYPE_ID;
         items.add(new ChildItem(item.getGUID(), item.getName(), isFolder));
      }
      return items;
   }
   
   /**
    * Get the actual help file name for the Location Scheme Editor page.
    * 
    * @return  the help file name, never <code>null</code> or empty.
    */
   /**
    * Gets the help file name for the Item Browser page.
    * @return help file name, never null or empty
    */
   public String getHelpFile() {
      return PSHelpTopicMapping.getFileName("ItemBrowser");
   }
   
}
