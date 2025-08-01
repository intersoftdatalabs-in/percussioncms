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

import static com.percussion.rx.publisher.jsf.nodes.PSLocationSchemeEditor.JEXL_GENERATOR;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSFolder;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.extension.IPSAssemblyLocation;
import com.percussion.extension.PSExtensionRef;
import com.percussion.rx.jsf.PSNodeBase;
import com.percussion.server.PSRequest;
import com.percussion.server.PSRequestContext;
import com.percussion.server.PSServer;
import com.percussion.server.webservices.PSServerFolderProcessor;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.util.PSBaseHttpUtils;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The backing bean for the test panel of the Location Scheme Editor.
 *
 * @author yubingchen
 */
public class PSSchemeJexlTestPanel
{
   /**
    * The class log.
    */
   private static final Logger ms_log = LogManager.getLogger(PSSchemeJexlTestPanel.class);

   /**
    * The backing bean for the site id. It may be <code>null</code> if has 
    * not been selected by user.
    */
   private IPSGuid m_siteId;
 
   /**
    * The item path, may be empty, never <code>null</code>.
    */
   private String m_itemPath = "";
   
   /**
    * The place holder for the extra parameters. 
    */
   private String m_extraParameters = "";
   
   /**
    * The place holder for the evaluated result.
    */
   private String m_evalResult = "";
   
   /**
    * The place holder for the status of the evaluated result. It can be
    * empty (initially), Success or Error.
    */
   private String m_evalStatus = "";
   
   /**
    * The parent backing bean or the Location Scheme Editor instance. It 
    * never <code>null</code> after constructor.
    */
   private final PSLocationSchemeEditor m_schemeEditor;
   
   /**
    * The node of all existing site. Set by {@link #getAllSites()}, never
    * <code>null</code> after that, but it may be empty.
    */
   private List<PSSiteNode> m_allSites;
   
   /**
    * The instance of the item browser.
    */
   private PSItemBrowser m_itemBrowser;
   
   /**
    * Constructor.
    * 
    * @param se the parent backing bean, never <code>null</code>.
    */
   public PSSchemeJexlTestPanel(PSLocationSchemeEditor se) {
      m_schemeEditor = java.util.Objects.requireNonNull(se, "schemeEditor must not be null");
   }

   /**
    * Invokes the Item Browser.
    * @return the outcome of the Item Browser, never <code>null</code> or empty.
    */
   public String browseItem() throws PSNotFoundException {
      m_itemBrowser = new PSItemBrowser(this);
      m_itemBrowser.setPath(getStartingFolderPath());
      return m_itemBrowser.gotoFolder();
   }

   /**
    * @return the extra parameters, never <code>null</code>, but may be empty.
    */
   public String getExtraParameters() {
      return m_extraParameters;
   }
   
   /**
    *  Set the extra parameters.
    * @param ps the new value of the extra parameters. If it is 
    *    <code>null</code>, then the extra parameters will be set to empty.
    */
   public void setExtraParameters(String ps) {
      m_extraParameters = ps == null ? "" : ps;
   }
   
   /**
    * @return the starting folder of the item browser.
    */
   private String getStartingFolderPath() throws PSNotFoundException {
      if (!StringUtils.isBlank(m_itemPath)) {
         try {
            var itemId = getFolderSrv().getIdByPath(m_itemPath);
            if (itemId != -1) {
               var locPath = getFolderSrv().getAncestorLocators(new PSLocator(itemId));
               var parent = locPath.get(locPath.size() - 1);
               var paths = getFolderSrv().getItemPaths(parent);
               return paths[0];
            }
         } catch (Exception e) {
            // ignore any error, including invalid path
         }
      }
      if (m_siteId != null) {
         for (var s : getAllSites()) {
            if (m_siteId.equals(s.getGUID())) {
               var siteRoot = s.getFolderRootPath();
               if (StringUtils.isNotBlank(siteRoot)) return siteRoot;
            }
         }
      }
      // default to the root of the virtual folder
      return PSFolder.PATH_SEP;
   }
   
   /**
    * @return the instance of the backing bean for browsing an item for testing
    * the JEXL expression, never <code>null</code>.
    */
   public PSItemBrowser getItemBrowser()
   {
      return m_itemBrowser;
   }
   
   /**
    * Determines whether the panel is empty. The panel will be empty initially.
    * However, it will not be empty if is any input or activities in the panel.
    * 
    * @return <code>true</code> the panel is empty.
    */
   public boolean isPanelEmpty() {
      return StringUtils.isBlank(m_evalStatus)
            && StringUtils.isBlank(m_evalResult)
            && StringUtils.isBlank(m_extraParameters)
            && StringUtils.isBlank(m_itemPath);
   }
   
   /**
    * @return the current site id, may be <code>null</code>.
    */
   public IPSGuid getSiteId() throws PSNotFoundException {
      if (m_siteId == null) {
         var allSites = getAllSites();
         if (!allSites.isEmpty()) m_siteId = allSites.get(0).getGUID();
      }
      return m_siteId;
   }

   /**
    * @return the item path, never <code>null</code>, but may be empty.
    */
   public String getItemPath() {
      return m_itemPath;
   }

   /**
    * Set item path.
    * @param path the new item path, may be <code>null</code> or empty.
    */
   public void setItemPath(String path) {
      m_itemPath = path == null ? "" : path;
   }
   
   /**
    * Set the site id.
    * @param id the new site id, never <code>null</code>.
    */
   public void setSiteId(IPSGuid id) {
      m_siteId = java.util.Objects.requireNonNull(id, "siteId must not be null");
   }

   /**
    * @return all available sites, never <code>null</code>, but may be empty.
    */
   public SelectItem[] getSites() throws PSNotFoundException {
      var siteList = new ArrayList<SelectItem>();
      for (var s : getAllSites()) {
         var si = new SelectItem(s.getTitle());
         si.setValue(s.getGUID());
         siteList.add(si);
      }
      return siteList.toArray(new SelectItem[0]);
   }

   /**
    * @return all site nodes, never <code>null</code>, but may be empty.
    */
   // ...existing code...
   private List<PSSiteNode> getAllSites() throws PSNotFoundException {
      if (m_allSites != null) return m_allSites;
      var root = m_schemeEditor.getParentNode().getParent().getParent();
      PSNodeBase sitesNode = null;
      for (var n : root.getChildren()) {
         if (n.getTitle().equalsIgnoreCase("Sites")) {
            sitesNode = n;
            break;
         }
      }
      if (sitesNode == null) {
         ms_log.error("Couldn't find Sites node.");
         throw new RuntimeException("Couldn't find Sites node.");
      }
      m_allSites = sitesNode.getChildren().stream()
         .filter(PSSiteNode.class::isInstance)
         .map(PSSiteNode.class::cast)
         .collect(java.util.stream.Collectors.toList());
      return m_allSites;
   }
   
   /**
    * @return the evaluated JEXL result, never <code>null</code> or empty.
    */
   public String getEvaluateResult() {
      return m_evalResult;
   }
   
   /**
    * @return the error message while evaluating the JEXL expression, 
    *    never <code>null</code>, but may be empty.
    */
   public String getEvaluateStatus() {
      return m_evalStatus;
   }
   
   /**
    * @return the folder processor, never <code>null</code>.
    */
   PSServerFolderProcessor getFolderSrv() {
      if (m_folderProcessor == null) {
         m_folderProcessor = PSServerFolderProcessor.getInstance();
      }
      return m_folderProcessor;
   }
   
   /**
    * The place folder for the folder processor. It is set by 
    * {@link #getFolderSrv()}
    */
   private PSServerFolderProcessor m_folderProcessor = null;
      
   /**
    * Evaluates the JEXL expression. It will validate the specified site and
    * item before perform the evaluation.
    * 
    * @return the outcome of the same page, never <code>null</code> or empty.
    */
   // ...existing code...
   public String evaluateExpression() {
      m_evalResult = "";
      var req = (PSRequest) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);
      var origParams = new HashMap<>(req.getParameters());
      var expression = m_schemeEditor.getExpression();
      try {
         var itemLoc = getItemId();
         setRequestParams(req, itemLoc, m_siteId.getUUID());
         var reqCxt = new PSRequestContext(req);
         var params = new Object[] { expression };
         m_evalResult = getGenerator().createLocation(params, reqCxt);
         m_evalStatus = "Success";
      } catch (Exception e) {
         var cause = e.getCause();
         m_evalResult = cause != null ? getExceptionMessage(cause) : getExceptionMessage(e);
         m_evalStatus = "Error";
      } finally {
         req.setParameters(origParams);
      }
      return perform();
   }

   /**
    * Get the error message from a given exception object.
    * @param e the exception, assumed not <code>null</code>.
    * @return the error message, never <code>null</code> or empty.
    */
   private String getExceptionMessage(Throwable e) {
      return StringUtils.isBlank(e.getLocalizedMessage())
         ? "Caught exception: " + e.getClass().getName()
         : e.getLocalizedMessage();
   }
   
   /**
    * @return the outcome of the Location Scheme page, never <code>null</code>
    *    or empty.
    */
   String perform() {
      return m_schemeEditor.perform();
   }
   
   /**
    * @return the instance of <code>sys_JexlAssemblyLocation</code> extension,
    *    never <code>null</code>.
    *    
    * @throws Exception if couldn't find the extension.
    */
   // ...existing code...
   private IPSAssemblyLocation getGenerator() throws Exception {
      var ref = new PSExtensionRef(JEXL_GENERATOR);
      var exitMgr = PSServer.getExtensionManager(null);
      var exit = exitMgr.prepareExtension(ref, null);
      return (IPSAssemblyLocation) exit;
   }
   
   /**
    * Set all necessary request parameters before run the evaluator of the
    * Location Generator.
    * 
    * @param req the request instance, assumed not <code>null</code>.
    * @param itemLoc the locator of the item that the evaluator will run 
    *    against with. It may be <code>null</code> if item path is not defined.
    * @param siteId the site id that the evaluator will run against with, 
    *    assumed it is an valid site id.
    *    
    * @throws PSCmsException if failed to get the parent folder of the item.
    */
   private void setRequestParams(PSRequest req, PSLocator itemLoc, int siteId) throws PSCmsException {
      var folderId = getParentFolderId(itemLoc);
      req.setParameter(IPSHtmlParameters.SYS_VARIANTID, m_schemeEditor.getEditedScheme().getTemplateId());
      req.setParameter(IPSHtmlParameters.SYS_SITEID, siteId);
      req.setParameter(IPSHtmlParameters.SYS_CONTEXT, m_schemeEditor.getParentNode().getContext().getGUID().getUUID());
      if (itemLoc != null) {
         req.setParameter(IPSHtmlParameters.SYS_CONTENTID, itemLoc.getId());
         req.setParameter(IPSHtmlParameters.SYS_REVISION, itemLoc.getRevision());
      }
      if (!StringUtils.isBlank(m_extraParameters)) {
         var params = PSBaseHttpUtils.parseQueryParamsString(m_extraParameters);
         req.putAllParameters(params);
      }
      req.setParameter(IPSHtmlParameters.SYS_FOLDERID, folderId);
   }

   /**
    * Get the parent folder id for the given item.
    * @param itemLoc the locator of the item, assumed not <code>null</code>.
    * @return the folder id.
    * @throws PSCmsException if error.
    */
   private int getParentFolderId(PSLocator itemLoc) throws PSCmsException {
      var locPath = getFolderSrv().getAncestorLocators(itemLoc);
      var loc = locPath.get(locPath.size() - 1);
      return loc.getId();
   }
   
   /**
    * Validates the item path. Make sure it does exist and under the specified
    * site. Throws exception if encounter any invalid value.
    *  
    * @return the locator of the specified item, never <code>null</code>. 
    * 
    * @throws Exception if any error occurs.
    */
   private PSLocator getItemId() throws Exception {
      if (StringUtils.isBlank(m_itemPath)) return null;
      var itemId = getFolderSrv().getIdByPath(m_itemPath);
      if (itemId == -1) return null;
      var cms = PSCmsObjectMgrLocator.getObjectManager();
      var ids = java.util.Collections.singletonList(itemId);
      var summarylist = cms.loadComponentSummaries(ids);
      return summarylist.get(0).getCurrentLocator();
   }
   
}
