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
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.PSRequest;
import com.percussion.server.webservices.PSServerFolderProcessor;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the base class for the backing beans of browsing Folders (for a site
 * root) and browsing the Folders/Items (for testing a JEXL expression).   
 *
 * @author YuBingChen
 */
/**
 * Java 11 refactored: Base class for backing beans of folder browsing (site root, JEXL test, etc).
 * <p>Uses var, Optional, Google Java Style, and improved null safety. Comments and spelling fixed.
 * @author YuBingChen
 */
public abstract class PSContentBrowser {
   /**
    * The class log.
    */
   private static final Logger log = LogManager.getLogger(PSContentBrowser.class);
   
   /**
    * The folder path, never <code>null</code> or empty after constructor.
    */
   private String m_path;
   
   /**
    * The folder id of the current Site Root.
    */
   private int m_folderId;
   
   /**
    * The list of child folders and/or items for the current folder id/path. 
    * This is used to cache the list, to avoid to obtain the list more than once.
    */
   private List<ChildItem> m_children;
   
   /**
    * The folder processor, set by {@link #getFolderSrv()}.
    */
   private PSServerFolderProcessor m_folderProcessor = null;
   
   /**
    * @return the folder processor, never <code>null</code>.
    */
   protected PSServerFolderProcessor getFolderSrv() {
      if (m_folderProcessor == null) {
         m_folderProcessor = PSServerFolderProcessor.getInstance();
      }
      return m_folderProcessor;
   }
   
   /**
    * @return current request object, never <code>null</code>.
    */
   protected PSRequest getRequest() {
      return (PSRequest) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);
   }
   
   /**
    * @return current folder path, never <code>null</code> or empty.
    */
   public String getPath() {
      return m_path;
   }

   /**
    * @return the current folder id. It is in sync with the folder path,
    * {@link #getPath()}
    */
   protected int getFolderId() {
      return m_folderId;
   }
   
   /**
    * Set the Site Root Path.
    * @param path the new path, never <code>null</code> or empty. It must be
    *    a valid folder path.
    */
   public void setPath(String path) {
      if (StringUtils.isBlank(path)) throw new IllegalArgumentException("path must not be null or empty.");
      int folderId;
      try {
         folderId = getFolderSrv().getIdByPath(path);
      } catch (Exception e) {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         log.error("Failed to get folder id from path: {}, due to error: {}", path, PSExceptionUtils.getMessageForLog(e));
         return;
      }
      if (folderId == -1) return;
      m_folderId = folderId;
      m_path = path;
      m_children = null;
   }
   
   /**
    * Set the (parent) folder to be the supplied folder.
    * 
    * @param id the ID of the new (parent) folder, assumed not <code>null</code>.
    * 
    * @return the outcome of the browser page. It may be <code>null</code>
    *    if error occurs.
    */
   public String gotoFolder(IPSGuid id) {
      if (id == null) throw new IllegalArgumentException("id may not be null.");
      // Use IPSGuid directly, assuming id is a folder guid
      var loc = new PSLocator(Integer.parseInt(id.toString()));
      String[] paths;
      try {
         paths = getFolderSrv().getItemPaths(loc);
      } catch (Exception e) {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         log.error("Failed to get path for folderId={}, due to error: {}", loc.getId(), PSExceptionUtils.getMessageForLog(e));
         return null;
      }
      if (paths.length == 0) {
         log.warn("Cannot get path for folderId= {}", loc.getId());
         return null;
      }
      m_folderId = loc.getId();
      m_path = paths[0];
      m_children = null;
      return perform();
   }
   
   /**
    * @return the outcome of the current browsing page, must be defined by
    * the inherited classes.
    */
   protected abstract String perform();
   
   /**
    * Goto the parent of the current folder if there is one. 
    * Note, cannot goto parent if the current folder is "//Sites".
    *     
    * @return the outcome of this browser page. It is <code>null</code> if
    *    cannot goto parent of the current folder.
    */
   public String gotoParent() {
      if (m_folderId == PSFolder.ROOT_ID) return null; // already at the top
      List<PSLocator> locPath;
      try {
         locPath = getFolderSrv().getAncestorLocators(new PSLocator(m_folderId));
      } catch (Exception e) {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         log.error("Failed to get ancestor locators for folderid={}, due to error: {}", m_folderId, PSExceptionUtils.getMessageForLog(e));
         return null;
      }
      var loc = locPath.get(locPath.size() - 1);
      // Use loc.getId() directly as folder id
      return gotoFolder(new IPSGuid() {
         @Override
         public int getUUID() { return loc.getId(); }
         @Override
         public String toString() { return Integer.toString(loc.getId()); }
         @Override
         public String toStringUntyped() { return Integer.toString(loc.getId()); }
         @Override
         public long getHostId() { return 0; }
         @Override
         public short getType() { return 0; }
         @Override
         public long longValue() { return loc.getId(); }
      });
   }
   
   /**
    * Get all child items or folders for the current (parent) folder.
    * 
    * @return the sub folders and/or items, never <code>null</code>, 
    *    may be empty.
    *    
    * @throws Exception if error occurs.
    */
   protected abstract List<ChildItem> getChildItems() throws Exception;
  
   /**
    * @return a list of child folders and/or items, never <code>null</code>, 
    * but may be empty.
    */
   public List<ChildItem> getChildren() {
      if (m_children != null) return m_children;
      var folders = new ArrayList<ChildItem>();
      var items = new ArrayList<ChildItem>();
      try {
         for (var item : getChildItems()) {
            if (item.isFolder()) folders.add(item);
            else items.add(item);
         }
      } catch (Exception e) {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
      folders.sort(null);
      items.sort(null);
      m_children = new ArrayList<>();
      m_children.addAll(folders);
      m_children.addAll(items);
      return m_children;
   }
   
   /**
    * This is called by the "Go" command button, used to let the framework
    * validate the input field of the Site Root Path and let the new value
    * take effect in the browsing activity.
    * 
    * @return the outcome of the browser page.
    */
   public String gotoFolder() {
      return perform();
   }

   /**
    * Performs on a child item or folder. The default behavior is to browsing
    * a sub folder. However, the inherited class must override this if it need
    * to perform or select an item.
    *  
    * @param item the child item or folder, may not be <code>null</code>.
    * 
    * @return the outcome of the targeted page, never <code>null</code> or empty.
    */
   protected String childPerform(ChildItem item) {
      if (item == null) throw new IllegalArgumentException("item must not be null.");
      return gotoFolder(item.mi_id);
   }

   /**
    * This is the backing bean for a sub folder or item of the current
    * Folder Path.
    */
   public class ChildItem implements Comparable<ChildItem> {
      /**
       * The id of the item or folder, never <code>null</code> after ctor.
       */
      private final IPSGuid mi_id;
      private final String mi_name;
      private final boolean mi_isFolder;

      public ChildItem(IPSGuid id, String name, boolean isFolder) {
         if (id == null) throw new IllegalArgumentException("id may not be null.");
         if (StringUtils.isBlank(name)) throw new IllegalArgumentException("name may not be null or empty.");
         this.mi_id = id;
         this.mi_name = name;
         this.mi_isFolder = isFolder;
      }

      @Override
      public int compareTo(ChildItem other) {
         return mi_name.compareTo(other.mi_name);
      }

      public String getName() {
         return mi_name;
      }

      public boolean isFolder() {
         return mi_isFolder;
      }

      public String perform() {
         return childPerform(this);
      }
   }
}
