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
package com.percussion.rx.publisher.jsf.data;

import com.percussion.design.objectstore.PSLocator;
import com.percussion.rx.publisher.jsf.beans.PSPubLogBean;
import com.percussion.rx.publisher.jsf.beans.PSRuntimeNavigation;
import com.percussion.rx.publisher.jsf.nodes.PSPublishingStatusHelper;
import com.percussion.server.PSRequest;
import com.percussion.server.webservices.PSServerFolderProcessor;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.publisher.IPSPubItemStatus;
import com.percussion.services.publisher.IPSSiteItem.Operation;
import com.percussion.services.publisher.IPSSiteItem.Status;
import com.percussion.services.publisher.data.PSPubItem;
import com.percussion.utils.guid.IPSGuid;


import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * Java 11 refactored: A simple bean that will set the right detail item in the runtime navigation.
 * <p>Uses Optional, var, and Google Java Style. All comments and spelling fixed. Properties are immutable where possible.
 * N.B. A subset of properties is made explicitly available through getters. The names of these properties must correspond exactly to the names in {@link PSPubItem} to allow sorting to work correctly.
 * @author dougrand
 */
public class PSPubItemEntry {
   /**
    * Format for elapsed time in seconds
    */
   private static final DecimalFormat ms_elapsedFormat = new DecimalFormat(
         "###,###.###s");

   /**
    * The index of this log entry.
    */
   private final int m_index;
   
   /**
    * Runtime navigation, never <code>null</code> after constructor
    */
   private final PSRuntimeNavigation m_nav;
   
   /**
    * The properties, setup in the {@link #initProperties()} method.
    */
   private final Map<String, Object> m_properties = new HashMap<>();

   /**
    * The publishing item status, initialized by constructor, never
    * <code>null</code> after that.
    */
   private final IPSPubItemStatus m_itemStatus;

   /**
    * The parent backing bean of this item log entry. Initialized by 
    * constructor, never <code>null</code> after that.
    */
   private final PSPubLogBean m_parent;
   
   
   public PSPubItemEntry(PSRuntimeNavigation nav, PSPubLogBean parent, IPSPubItemStatus status, int index) {
      if (nav == null) throw new IllegalArgumentException("nav may not be null");
      if (parent == null) throw new IllegalArgumentException("parent may not be null");
      if (status == null) throw new IllegalArgumentException("status may not be null");
      this.m_parent = parent;
      this.m_index = index;
      this.m_nav = nav;
      this.m_itemStatus = status;
      initProperties();
   }

   /**
    * Get the original item log entry.
    * @return the log entry, never <code>null</code>.
    */
   public IPSPubItemStatus getItemStatus() {
      return m_itemStatus;
   }

   /**
    * @return the properties, never <code>null</code>.
    */
   public Map<String, Object> getProperties() {
      return m_properties;
   }

   /**
    * Initialize this object from the passed data
    */
   protected void initProperties() {
      // operation
      var op = m_itemStatus.getOperation();
      m_properties.put("operation", op == Operation.PUBLISH ? "publish" : "unpublish");

      // elapsed
      var elapsed = Optional.ofNullable(m_itemStatus.getElapsed()).orElse(0);
      var value = ms_elapsedFormat.format(elapsed / 1000.0);
      m_properties.put("elapsed", value);

      // status
      var status = m_itemStatus.getStatus();
      if (status == Status.SUCCESS) value = "success";
      else if (status == Status.CANCELLED) value = "cancelled";
      else if (status == Status.FAILURE) value = "failure";
      else value = "";
      m_properties.put("status", value);

      // date
      value = DateFormat.getDateTimeInstance().format(m_itemStatus.getDate());
      m_properties.put("date", value);

      // siteFolder
      value = getSiteFolder();
      m_properties.put("siteFolder", value);

      // template
      value = getTemplate();
      m_properties.put("template", value);
   }

   /**
    * Gets the site folder path from the folder ID of {@link #m_itemStatus}.
    * @return the site folder if the folder id is set, or an empty string
    * if unknown.
    */
   private String getSiteFolder() {
      var folder = m_itemStatus.getFolderId();
      if (folder != null && folder != 0) {
         var proc = PSServerFolderProcessor.getInstance();
         try {
            var paths = proc.getItemPaths(new PSLocator(folder));
            if (paths.length == 1) return paths[0];
            return "Error: cannot find folder path for fid = " + folder;
         } catch (Exception e) {
            return "Error: cannot find folder path for fid = " + folder;
         }
      }
      return "";
   }

   /**
    * @return get the messages, may be empty but not <code>null</code>.
    */
   public List<String> getMessages() {
      return PSPublishingStatusHelper.splitMessages(m_itemStatus.getMessage());
   }
   
   /**
    * @return <code>true</code> if there are messages to display
    */
   public boolean getHasMessages() {
      return StringUtils.isNotBlank(m_itemStatus.getMessage());
   }

   /**
    * @return the template label for the given template id, it may be 
    *     empty if failed to get the label of the template.
    */
   private String getTemplate() {
      var templateId = m_itemStatus.getTemplateId();
      if (templateId == null) return "";
      var asvc = PSAssemblyServiceLocator.getAssemblyService();
      var gmgr = PSGuidManagerLocator.getGuidMgr();
      var tguid = gmgr.makeGuid(templateId.toString(), PSTypeEnum.TEMPLATE);
      try {
         var template = asvc.loadUnmodifiableTemplate(tguid);
         return template.getLabel();
      } catch (Exception e) {
         return "";
      }
   }

   /**
    * Setup reference to this bean for further viewing in detail
    * 
    * @return the outcome, never <code>null</code>
    */
   public String perform() {
      m_nav.setDetailItem(this);
      return "pub-runtime-log-item";
   }
   
   /**
    * Action to go to the previous item
    * @return the outcome, never <code>null</code>.
    */
   public String previous() {
      if (m_index > 0) {
         m_parent.setRowIndex(m_index - 1);
         var entry = (PSPubItemEntry) m_parent.getRowData();
         m_nav.setDetailItem(entry);
      }
      return "previous";
   }

   /**
    * Action to go to the next item
    * @return the outcome, never <code>null</code>.
    */   
   public String next() {
      var count = m_parent.getRowCount();
      if ((count - m_index) > 1) {
         m_parent.setRowIndex(m_index + 1);
         var entry = (PSPubItemEntry) m_parent.getRowData();
         m_nav.setDetailItem(entry);
      }
      return "next";
   }
}
