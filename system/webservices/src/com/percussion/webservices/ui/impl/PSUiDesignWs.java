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
package com.percussion.webservices.ui.impl;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.IPSDbComponent;
import com.percussion.cms.objectstore.PSAction;
import com.percussion.cms.objectstore.PSComponentProcessorProxy;
import com.percussion.services.menus.PSActionMenu;
import com.percussion.services.menus.RxmActionMenuConstants;
import com.percussion.cms.objectstore.PSDisplayColumn;
import com.percussion.cms.objectstore.PSDisplayFormat;
import com.percussion.cms.objectstore.PSDFColumns;
import com.percussion.cms.objectstore.PSDFMultiProperty;
import com.percussion.cms.objectstore.PSKey;
import com.percussion.cms.objectstore.PSMenuChild;
import com.percussion.cms.objectstore.PSSearch;
import com.percussion.cms.objectstore.PSVersionableDbComponent;
import com.percussion.data.PSIdGenerator;
import com.percussion.data.PSTableChangeEvent;
import com.percussion.data.utils.PSTableUpdateHandlerBase;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.assembly.impl.nav.PSNavConfig;
import com.percussion.fastforward.managednav.PSNavException;
import com.percussion.server.PSRequest;
import com.percussion.server.PSServer;
import com.percussion.server.cache.PSCacheManager;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.locking.IPSObjectLockService;
import com.percussion.services.locking.PSLockException;
import com.percussion.services.locking.PSObjectLockServiceLocator;
import com.percussion.services.locking.data.PSObjectLock;
import com.percussion.services.memory.IPSCacheAccess;
import com.percussion.services.memory.PSCacheAccessLocator;
import com.percussion.services.ui.IPSUiService;
import com.percussion.services.ui.PSUiException;
import com.percussion.services.ui.PSUiServiceLocator;
import com.percussion.services.ui.data.PSHierarchyNode;
import com.percussion.services.ui.data.PSHierarchyNodeProperty;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.jdbc.PSConnectionHelper;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.timing.PSTimer;
import com.intsof.percussioncms.auditlog.codes.WebserviceErrorCodes;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.PSWebserviceErrors;
import com.percussion.webservices.PSWebserviceUtils;
import com.percussion.webservices.ui.IPSUiDesignWs;
import com.percussion.webservices.ui.data.ActionType;
import org.apache.commons.lang3.StringUtils;
import com.percussion.webservices.ExceptionUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import static org.apache.commons.lang3.Validate.notEmpty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Element;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.NamingException;


/**
 * The private ui design webservice implementations.
 */
public class PSUiDesignWs extends PSUiBaseWs implements IPSUiDesignWs
{
   // Added to optimize hierarchy nodes
   private ConcurrentHashMap<PSHierarchyNode.NodeType, ConcurrentHashMap<IPSGuid, String>> nodeIdToPathMap = null;

   private ConcurrentHashMap<IPSGuid, IPSGuid> objectIdToNodeIdMap = null;

   private static volatile boolean initializing = false;

   private StringBuilder nodePath;

   // Done

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#createActions(List<String>, List<String>)
    */
   @Transactional
   public List<PSAction> createActions(List<String> names, List<ActionType> types, String session, String user)
         throws PSLockErrorException, PSErrorException
   {
      PSWebserviceUtils.validateParameters(names, "names", true, session, user);
      PSWebserviceUtils.validateParameters(types, "types", true, session, user);

      if (names.size() != types.size())
         throw new IllegalArgumentException("the size of names and types must be equal.");

      validateComponentNames(names, FIND_ACTIONS, PSAction.XML_NODE_NAME, PSTypeEnum.ACTION, PSAction.class);

      List<PSAction> actions = new ArrayList<>();
      for (int i = 0; i < names.size(); i++)
      {
         PSAction action = createAction(names.get(i), types.get(i));
         PSWebserviceUtils.createLock(action.getGUID(), session, user, null);
         actions.add(action);
      }

      return actions;
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#createDisplayFormats(List<String>)
    */
   @Transactional
   public List<PSDisplayFormat> createDisplayFormats(List<String> names, String session, String user)
         throws PSLockErrorException, PSErrorException
   {
      PSWebserviceUtils.validateParameters(names, "names", true, session, user);

      validateComponentNames(names, FIND_DISPLAY_FORMAT, "PSXDisplayFormat", PSTypeEnum.DISPLAY_FORMAT,
            PSDisplayFormat.class);

      List<PSDisplayFormat> results = new ArrayList<>();
      for (String name : names) {
         PSDisplayFormat displayFormat = createDisplayFormat(name);
         PSWebserviceUtils.createLock(displayFormat.getGUID(), session, user, null);
         results.add(displayFormat);
      }

      return results;
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#createHierarchyNodes(List<String>, List<IPSGuid>,
    * List<PSHierarchyNode.NodeType>, List<String>, List<String>)
    */
   @Transactional
   public List<PSHierarchyNode> createHierarchyNodes(List<String> names, List<IPSGuid> parents,
         List<PSHierarchyNode.NodeType> types, String session, String user)
   {
      ms_log.debug("entered createHierarchyNodes()");
      if (names == null || names.isEmpty())
         throw new IllegalArgumentException("names cannot be null or empty");

      if (parents == null || parents.isEmpty())
         throw new IllegalArgumentException("parents cannot be null or empty");

      if (types == null || types.isEmpty())
         throw new IllegalArgumentException("types cannot be null or empty");

      if (names.size() != parents.size() || names.size() != types.size())
         throw new IllegalArgumentException("names, parents and types must have the same size");

      List<PSHierarchyNode> nodes = new ArrayList<PSHierarchyNode>();
      int index = 0;
      for (String name : names)
      {
         if (StringUtils.isBlank(name))
            throw new IllegalArgumentException("name cannot be null or empty");

         IPSGuid parent = parents.get(index);

         PSHierarchyNode.NodeType type = types.get(index);
         if (type == null)
            throw new IllegalArgumentException("type cannot be null");

         IPSUiService service = PSUiServiceLocator.getUiService();

         if (!service.findHierarchyNodes(name, parent, null).isEmpty())
         {
            PSWebserviceUtils.throwObjectExistException(name, PSTypeEnum.HIERARCHY_NODE);
         }

         PSHierarchyNode node;
         try { node = service.createHierarchyNode(name, parent, type); } catch (PSUiException e) { throw new RuntimeException(e); }

         IPSObjectLockService lockService = PSObjectLockServiceLocator.getLockingService();
         try
         {
            lockService.createLock(node.getGUID(), session, user, node.getVersion(), false);
         }
         catch (PSLockException e)
         {
            // should never happen, ignore
         }

         nodes.add(node);
         index++;
      }

      return nodes;
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#createSearches(List<String>, List<String>)
    */
   @Transactional
   public List<PSSearch> createSearches(List<String> names, List<String> types, String session, String user)
         throws PSLockErrorException, PSErrorException
   {
      PSWebserviceUtils.validateParameters(names, "names", true, session, user);
      PSWebserviceUtils.validateParameters(types, "types", true, session, user);

      if (names.size() != types.size())
         throw new IllegalArgumentException("the size of names and types must be equal.");

      validateComponentNames(names, FIND_SEARCHES, PSSearch.XML_NODE_NAME, PSTypeEnum.SEARCH_DEF, PSSearch.class);

      List<PSSearch> searches = new ArrayList<>();
      for (int i = 0; i < names.size(); i++)
      {
         PSSearch s = createSearch(names.get(i), types.get(i));
         PSWebserviceUtils.createLock(s.getGUID(), session, user, null);
         searches.add(s);
      }

      return searches;
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#createViews(List<String>)
    */
   @Transactional
   public List<PSSearch> createViews(List<String> names, String session, String user) throws PSLockErrorException,
         PSErrorException
   {
      PSWebserviceUtils.validateParameters(names, "names", true, session, user);

      validateComponentNames(names, FIND_SEARCHES, PSSearch.XML_NODE_NAME, PSTypeEnum.SEARCH_DEF, PSSearch.class);

      List<PSSearch> searches = new ArrayList<>();
      for (String name : names) {
         PSSearch s = createSearch(name, PSSearch.TYPE_VIEW);
         PSWebserviceUtils.createLock(s.getGUID(), session, user, null);
         searches.add(s);
      }

      return searches;
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#deleteActions(List, boolean)
    */
   @Transactional
   public void deleteActions(List<IPSGuid> ids, boolean ignoreDependencies, String session, String user)
         throws PSErrorsException
   {
      PSWebserviceUtils.validateParameters(ids, "ids", true, session, user);

      try
      {
         deleteComponents(ids, PSAction.class, PSAction.getComponentType(PSAction.class), ignoreDependencies, session,
               user);
      }
      catch (PSErrorsException e)
      {
         // REST H2 often has RXMENUACTION without the XML design document
         // (PSTransactionSet: Xml Document Expected). Other errors — including
         // genuine dependency violations when ignoreDependencies is false —
         // must still fail closed.
         if (!isXmlDocumentExpected(e))
            throw e;
         log.debug("deleteComponents missed XML action; JDBC RXMENUACTION delete continues", e);
      }
      for (IPSGuid id : ids)
      {
         if (id != null)
            deleteActionRowPreferringHibernate(id.getUUID());
      }
      invalidateActionCatalog();
      evictActionMenuRegion();
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#deleteDisplayFormats(List, boolean)
    */
   @Transactional
   public void deleteDisplayFormats(List<IPSGuid> ids, boolean ignoreDependencies, String session, String user)
         throws PSErrorsException
   {
      PSWebserviceUtils.validateParameters(ids, "ids", true, session, user);

      deleteComponents(ids, PSDisplayFormat.class, PSDisplayFormat.getComponentType(PSDisplayFormat.class),
            ignoreDependencies, session, user);
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#deleteHierarchyNodes(List, boolean, String, String)
    */
   @Transactional
   public void deleteHierarchyNodes(List<IPSGuid> ids, boolean ignoreDependencies, String session, String user)
         throws PSErrorsException
   {
      if (PSGuidUtils.isBlank(ids))
         throw new IllegalArgumentException("ids cannot be null or empty");

      IPSUiService service = PSUiServiceLocator.getUiService();

      PSErrorsException results = new PSErrorsException();
      for (IPSGuid id : ids)
      {
         if (PSWebserviceUtils.hasValidLockForDelete(id, session, user))
         {
            boolean exists = false;
            try
            {
               service.loadHierarchyNode(id);
               exists = true;
            }
            catch (PSUiException e)
            {
               // ignore, just means that the node does not exist
            }

            if (exists)
            {
               // check for dependents if requested
               if (!ignoreDependencies)
               {
                  PSErrorException error = PSWebserviceUtils.checkDependencies(id);
                  if (error != null)
                  {
                     results.addError(id, error);
                     continue;
                  }
               }

               service.deleteHierarchyNode(id);
            }

            results.addResult(id);
         }
         else
         {
            PSWebserviceUtils.handleMissingLockError(id, PSHierarchyNode.class, results);
         }
      }

      // release locks for all successfully deleted objects
      PSWebserviceUtils.releaseLocks(results.getResults(), session, user);

      // Recreate the maps nodeIdToPathMap and objectIdToNodeIdMap
      recreateStaticMaps();

      if (results.hasErrors())
         throw results;
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#deleteSearches(List, boolean)
    */
   @Transactional
   public void deleteSearches(List<IPSGuid> ids, boolean ignoreDependencies, String session, String user)
         throws PSErrorsException
   {
      PSWebserviceUtils.validateParameters(ids, "ids", true, session, user);

      deleteComponents(ids, PSSearch.class, PSSearch.getComponentType(PSSearch.class), ignoreDependencies, session,
            user);
      for (IPSGuid id : ids)
      {
         if (id != null)
            ensureSearchRowDeleted(id.getUUID());
      }
      invalidateSearchCatalog();
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#deleteViews(List, boolean)
    */
   @Transactional
   public void deleteViews(List<IPSGuid> ids, boolean ignoreDependencies, String session, String user)
         throws PSErrorsException

   {
      deleteSearches(ids, ignoreDependencies, session, user);
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#findActions(String, String)
    */
   public List<IPSCatalogSummary> findActions(String name, String label, List<ActionType> types)
         throws PSErrorException
   {
      List<IPSDbComponent> actions = findComponentsByNameLabel(name, label, FIND_ACTIONS, PSAction.XML_NODE_NAME,
            PSAction.class);
      if (types == null || types.isEmpty())
         return getSummaries(actions);

      List<IPSDbComponent> result = new ArrayList<>();
      PSAction action;
      for (IPSDbComponent comp : actions)
      {
         action = (PSAction) comp;
         for (ActionType type : types)
         {
            if (ActionType.ITEM.equals(type) && action.isMenuItem())
               result.add(comp);
            else if (ActionType.CASCADING.equals(type) && action.isCascadedMenu())
               result.add(comp);
            else if (ActionType.DYNAMIC.equals(type) && action.isDynamicMenu())
               result.add(comp);
         }
      }

      return getSummaries(result);
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#findDisplayFormats(String, String)
    */
   public List<IPSCatalogSummary> findDisplayFormats(String name, String label) throws PSErrorException
   {
      List<IPSDbComponent> actions = findComponentsByNameLabel(name, label, FIND_DISPLAY_FORMAT, "PSXDisplayFormat",
            PSDisplayFormat.class);
      return getSummaries(actions);
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#findHierarchyNodes(String, PSHierarchyNode.NodeType)
    */
   public List<IPSCatalogSummary> findHierarchyNodes(String path, PSHierarchyNode.NodeType type)
   {

      List<IPSGuid> nodeIds = new ArrayList<>();
      String tempPath = null;
      Map<IPSGuid, String> nodePathMap = new ConcurrentHashMap<>();
      Iterator nodePathIt = null;

      PSTimer timer = new PSTimer(ms_log);
      IPSUiService service = PSUiServiceLocator.getUiService();

      initializeHierarchyNodeMaps();

      // Get all the node ids from the map for the given type that have path
      // containing the path provided.

      if (path != null && !path.isEmpty() && !path.equals("*"))
      {
         if (path.charAt(0) == '/')
            path = path.substring(1, path.length());

         tempPath = path.substring(0, path.length() - 2);
      }

      if (type == null)
      {

         nodePathMap = getNodesPathMapForAllTypes();

      }
      else
      {

         nodePathMap = nodeIdToPathMap.get(type);

      }

      nodePathIt = nodePathMap.entrySet().iterator();

      while (nodePathIt.hasNext())
      {

         Map.Entry<IPSGuid, String> pathEntry = (Map.Entry<IPSGuid, String>) nodePathIt.next();

         if (path == null || path.isEmpty() || path.equals("*"))
            nodeIds.add(pathEntry.getKey());
         else
         {

            if (path.contains("*"))
            {

               if (pathEntry.getValue().contains(tempPath))
                  nodeIds.add(pathEntry.getKey());

            }
            else
            {
               if (pathEntry.getValue().equalsIgnoreCase(path))
               {
                  nodeIds.add(pathEntry.getKey());

                  break;
               }
            }
         }
      }

      // Load Hierarchy Node for all the node ids that have the provided path as
      // their path of part of it.
      List<PSHierarchyNode> resultNodes = new ArrayList<>();

      for (IPSGuid id : nodeIds)
      {
         try
         {
            resultNodes.add(service.loadHierarchyNode(id));
         }
         catch (PSUiException e)
         {
            log.error(PSExceptionUtils.getMessageForLog(e));
         }
      }

      timer.logElapsed("Ready to the return the catalog summary list in the findHierarchyNodes(String path, PSHierarchyNode.NodeType type) ....");

      return PSWebserviceUtils.toObjectSummaries(resultNodes);
   }

   /**
    * Method to initialize the static maps: Map of node type and another map(map
    * of node guid and its path) AND Map of object guid and node guid.
    */

   public void initializeHierarchyNodeMaps()
   {
      if (!initializing && objectIdToNodeIdMap == null)
      {
         synchronized (this)
         {

            if (!initializing && objectIdToNodeIdMap == null)
            {
               initializing = true;
            nodeIdToPathMap = initializeHierarchyNodes();
            objectIdToNodeIdMap = getAllHierarchyNodesGuidProperties();
               initializing = false;
         }
      }
   }
   }

   /**
    * Initialize the map - Map of node type and another map(map of node guid and
    * its path)
    *
    * @return The initialized map
    */
   private ConcurrentHashMap<PSHierarchyNode.NodeType, ConcurrentHashMap<IPSGuid, String>> initializeHierarchyNodes()
   {

      IPSUiService service = PSUiServiceLocator.getUiService();
      ConcurrentHashMap<PSHierarchyNode.NodeType, ConcurrentHashMap<IPSGuid, String>> nodeMap = new ConcurrentHashMap<>();
      ConcurrentHashMap<IPSGuid, String> folderNodeMap = new ConcurrentHashMap<>();
      ConcurrentHashMap<IPSGuid, String> placeHolderNodeMap = new ConcurrentHashMap<>();

      nodeMap.put(PSHierarchyNode.NodeType.FOLDER, new ConcurrentHashMap<>());
      nodeMap.put(PSHierarchyNode.NodeType.PLACEHOLDER, new ConcurrentHashMap<>());

      List<PSHierarchyNode> nodes = service.getAllHierarchyNodes();

      for (PSHierarchyNode node : nodes)
      {

         nodePath = new StringBuilder();

         if (node.getType().equals(PSHierarchyNode.NodeType.FOLDER))
            folderNodeMap.put(node.getGUID(), getNodePath(node, nodes));
         else
         {
            if (node.getType().equals(PSHierarchyNode.NodeType.PLACEHOLDER))
               placeHolderNodeMap.put(node.getGUID(), getNodePath(node, nodes));
         }
      }

      nodeMap.get(PSHierarchyNode.NodeType.FOLDER).putAll(folderNodeMap);
      nodeMap.get(PSHierarchyNode.NodeType.PLACEHOLDER).putAll(placeHolderNodeMap);

      return nodeMap;
   }

   /**
    * Generate a path string for a given node
    *
    * @param node - for which the path string is to be created
    * @param nodes - list of all the nodes in the hierarchy node, so as to find
    *           the parent(s) of the given node
    * @return The path string generated here.
    */
   private String getNodePath(PSHierarchyNode node, List<PSHierarchyNode> nodes)
   {

      PSHierarchyNode parentNode = null;

      if (node.getParentId() == null)
      {

         nodePath.insert(0, "/" + node.getName());
      }
      else
      {

         nodePath.insert(0, "/" + node.getName());

         for (PSHierarchyNode pNode : nodes)
         {

            if (pNode.getGUID().equals(node.getParentId()))
            {

               parentNode = pNode;
               break;
            }
         }

         if (parentNode != null)
            getNodePath(parentNode, nodes);
         else {
            //if the parent node is not found - remove the parent id and treat as top level item
            ms_log.warn("Workbench parent folder: {} was not found, moving {} to top level.",
                    node.getParentId(),
                    node.getName());
            node.setParentId(null);
            IPSUiService service = PSUiServiceLocator.getUiService();
            try { service.saveHierarchyNode(node); } catch (PSUiException e) { throw new RuntimeException(e); }
            nodePath.insert(0, "/" + node.getName());
         }
      }

      return nodePath.toString();
   }

   /**
    * Get the object id of the node and create a map of the object guid and the
    * node guid of this object.
    *
    * @return Map of object guid and node guid.
    */
   private ConcurrentHashMap<IPSGuid, IPSGuid> getAllHierarchyNodesGuidProperties()
   {

      IPSUiService service = PSUiServiceLocator.getUiService();
      ConcurrentHashMap<IPSGuid, IPSGuid> objectNodeMap = new ConcurrentHashMap<>();
      ConcurrentHashMap<IPSGuid, String> allNodeTypeMap = getNodesPathMapForAllTypes();

      List<PSHierarchyNodeProperty> guidProps = service.getAllHierarchyNodesGuidProperties();

      for (PSHierarchyNodeProperty prop : guidProps)
      {

         if (allNodeTypeMap.containsKey(new PSGuid(prop.getNodeId())))

            // create the hashmap with the object guid as key and node id as the
            // value
            objectNodeMap.put(new PSGuid(prop.getValue()), new PSGuid(prop.getNodeId()));
      }

      return objectNodeMap;
   }

   /**
    * Combine the maps of node guid and path strings for the different node
    * types.
    *
    * @return The combined map.
    */
   private ConcurrentHashMap<IPSGuid, String> getNodesPathMapForAllTypes()
   {

      ConcurrentHashMap<IPSGuid, String> nodeMap = new ConcurrentHashMap<>();

      initializeHierarchyNodeMaps();

      if (nodeIdToPathMap != null && !nodeIdToPathMap.isEmpty())
      {
         nodeMap.putAll(nodeIdToPathMap.get(PSHierarchyNode.NodeType.PLACEHOLDER));
         nodeMap.putAll(nodeIdToPathMap.get(PSHierarchyNode.NodeType.FOLDER));
      }

      return nodeMap;
   }

   /**
    * To recreate the static maps, at various occasions like after the
    * operations - delete, save, move children and remove children to be sure
    * that the static maps are up to date always.
    */
   private void recreateStaticMaps()
   {

      ConcurrentHashMap<PSHierarchyNode.NodeType, ConcurrentHashMap<IPSGuid, String>> newNodeIdToPathMap = initializeHierarchyNodes();
      ConcurrentHashMap<IPSGuid, IPSGuid> newObjectIdToNodeIdMap = getAllHierarchyNodesGuidProperties();

      nodeIdToPathMap = newNodeIdToPathMap;
      objectIdToNodeIdMap = newObjectIdToNodeIdMap;

   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#findSearches(String, String)
    */
   public List<IPSCatalogSummary> findSearches(String name, String label) throws PSErrorException
   {
      List<IPSDbComponent> searchViews = findComponentsByNameLabel(name, label, FIND_SEARCHES, PSSearch.XML_NODE_NAME,
            PSSearch.class);

      List<PSSearch> searches = getSearchOrViews(searchViews, false);
      return getSummaries(new ArrayList<>(searches));
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#findViews(String, String)
    */
   public List<IPSCatalogSummary> findViews(String name, String label) throws PSErrorException
   {
      List<IPSDbComponent> searchViews = findComponentsByNameLabel(name, label, FIND_SEARCHES, PSSearch.XML_NODE_NAME,
            PSSearch.class);

      List<PSSearch> searches = getSearchOrViews(searchViews, true);
      return getSummaries(new ArrayList<>(searches));
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#getChildren(IPSGuid)
    */
   public List<IPSGuid> getChildren(IPSGuid id)
   {
      IPSUiService service = PSUiServiceLocator.getUiService();

      List<PSHierarchyNode> children = service.findHierarchyNodes("%", id, null);

      return PSGuidUtils.getIds(children);
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#idsToPaths(List)
    */

   public List<String> idsToPaths(List<IPSGuid> ids) throws PSErrorResultsException
   {
      if (ids == null || ids.isEmpty())
         throw new IllegalArgumentException("ids cannot be null or empty");

      PSErrorResultsException results = new PSErrorResultsException();
      for (IPSGuid id : ids)
      {
         try
         {
            List<PSHierarchyNode> tree = new ArrayList<>();
            getHierarchyNodeTree(id, tree);

            results.addResult(id, treeToPath(tree));
         }
         catch (PSUiException e)
         {
            var code = WebserviceErrorCodes.OBJECT_NOT_FOUND;
            PSDesignGuid guid = new PSDesignGuid(id);
            PSErrorException error = new PSErrorException(code, PSWebserviceErrors.createErrorMessage(code,
                  PSHierarchyNode.class.getName(), guid.getValue()), ExceptionUtils.getFullStackTrace(e));
            results.addError(id, error);
         }
      }

      if (results.hasErrors())
         throw results;

      return results.getResults(ids);
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#loadActions(List, boolean, boolean, String, String)
    */

   public List<PSAction> loadActions(List<IPSGuid> ids, boolean lock, boolean overrideLock, String session, String user)
         throws PSErrorResultsException
   {
      PSWebserviceUtils.validateParameters(ids, "ids", lock, session, user);

      return loadComponents(ids, PSAction.class, PSAction.getComponentType(PSAction.class), lock, overrideLock,
            session, user);
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#loadDisplayFormats(List, boolean, boolean, String,
    * String)
    */

   public List<PSDisplayFormat> loadDisplayFormats(List<IPSGuid> ids, boolean lock, boolean overrideLock,
         String session, String user) throws PSErrorResultsException
   {
      PSWebserviceUtils.validateParameters(ids, "ids", lock, session, user);

      @SuppressWarnings("unchecked")
      List<PSDisplayFormat> loaded =
            loadComponents(ids, PSDisplayFormat.class, PSDisplayFormat.getComponentType(PSDisplayFormat.class), lock,
            overrideLock, session, user);
      return reconcileDisplayFormatLoads(ids, loaded);
   }

   /*
    * (non-Javadoc)
    * @see com.percussion.webservices.ui.IPSUiDesignWs#findDisplayFormat(com.percussion.utils.guid.IPSGuid)
    */
   public PSDisplayFormat findDisplayFormat(IPSGuid id)
   {
      return  loadDisplayFormat(id);
   }

   /*
    * (non-Javadoc)
    * @see com.percussion.webservices.ui.IPSUiDesignWs#findDisplayFormat(java.lang.String)
    */
   public PSDisplayFormat findDisplayFormat(String name)
   {
      notEmpty(name);

      // load from repository
      return loadDisplayFormat(name);

   }

   /**
    * Loads a specified display format from repository.
    * @param name the name of the display format, assumed not <code>null</code> or empty.
    * @return the specified display format, it may be <code>null</code> if cannot find one.
    */
   private PSDisplayFormat loadDisplayFormat(String name)
   {
      List<IPSCatalogSummary> results = findDisplayFormats(name, null);
      for (IPSCatalogSummary summary : results)
      {
         if (summary.getName() != null && summary.getName().equalsIgnoreCase(name))
         {
            PSDisplayFormat loaded = loadDisplayFormat(summary.getGUID());
            if (loaded != null && name.equalsIgnoreCase(loaded.getName()))
            {
               return loaded;
            }
         }
      }
      return loadDisplayFormatFromDb(-1, name);
   }

   /**
    * Loads a display format for the given ID.
    *
    * @param id the ID of the display format in question, assumed not <code>null</code>.
    *
    * @see IPSUiDesignWs#loadHierachyNodes(List, boolean, boolean,
    *    String, String)
    * @see IPSUiDesignWs#loadHierachyNodes(List, boolean, boolean, String,
    * String)
    * @return the display format, it may be <code>null</code> if it does not exist or error occurs.
    */
   private PSDisplayFormat loadDisplayFormat(IPSGuid id)
   {
      try
      {
         String session = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
         String user = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
         List<PSDisplayFormat> dispList =
               loadDisplayFormats(Collections.singletonList(id), false, false, session, user);
         if (dispList.isEmpty() || dispList.get(0) == null)
            return id == null ? null : loadDisplayFormatFromDb(id.getUUID(), null);
         PSDisplayFormat loaded = dispList.get(0);
         if (id != null && loaded.getDisplayId() == id.getUUID())
            return loaded;
         ms_log.debug("Rejecting display format replay name={} displayId={} for requested {}",
               loaded.getName(), loaded.getDisplayId(), id);
      }
      catch (PSErrorResultsException e)
      {
         ms_log.error("Failed to load display format with id = {}. Error: {}" , id,
                 PSExceptionUtils.getMessageForLog(e));
      }
      return id == null ? null : loadDisplayFormatFromDb(id.getUUID(), null);
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#loadHierachyNodes(List, boolean, boolean,
    *    String, String)
    */

   public List<PSHierarchyNode> loadHierachyNodes(List<IPSGuid> ids, boolean lock, boolean overrideLock,
         String session, String user) throws PSErrorResultsException
   {
      if (PSGuidUtils.isBlank(ids))
         throw new IllegalArgumentException("ids cannot be null or empty");

      if (lock && StringUtils.isBlank(session))
         throw new IllegalArgumentException("session cannot be null or empty");

      if (lock && StringUtils.isBlank(user))
         throw new IllegalArgumentException("user cannot be null or empty");

      IPSUiService service = PSUiServiceLocator.getUiService();

      PSErrorResultsException results = new PSErrorResultsException();
      for (IPSGuid id : ids)
      {
         try
         {
            PSHierarchyNode node = service.loadHierarchyNode(id);
            results.addResult(id, node);
         }
         catch (PSUiException e)
         {
            var code = WebserviceErrorCodes.OBJECT_NOT_FOUND;
            PSDesignGuid guid = new PSDesignGuid(id);
            PSErrorException error = new PSErrorException(code, PSWebserviceErrors.createErrorMessage(code,
                  PSHierarchyNode.class.getName(), guid.getValue()), ExceptionUtils.getFullStackTrace(e));
            results.addError(id, error);
         }
      }

      if (lock)
      {
         IPSObjectLockService lockService = PSObjectLockServiceLocator.getLockingService();
         lockService.createLocks(results, session, user, overrideLock);
      }

      if (results.hasErrors())
         throw results;

      return results.getResults(ids);
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#loadSearches(List, boolean, boolean, String, String)
    */
   public List<PSSearch> loadSearches(List<IPSGuid> ids, boolean lock, boolean overrideLock, String session, String user)
         throws PSErrorResultsException
   {
      return loadSearchViews(ids, lock, overrideLock, session, user, false);
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#loadViews(List, boolean, boolean, String, String)
    */
   public List<PSSearch> loadViews(List<IPSGuid> ids, boolean lock, boolean overrideLock, String session, String user)
         throws PSErrorResultsException
   {
      return loadSearchViews(ids, lock, overrideLock, session, user, true);
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#moveChildren(IPSGuid, IPSGuid, List)
    */
   @Transactional
   public void moveChildren(IPSGuid source, IPSGuid target, List<IPSGuid> ids)
   {
      if (source == null)
         throw new IllegalArgumentException("source cannot be null");

      if (target == null)
         throw new IllegalArgumentException("target cannot be null");

      if (ids == null || ids.isEmpty())
         throw new IllegalArgumentException("ids cannot be null or empty");

      IPSUiService service = PSUiServiceLocator.getUiService();

      service.moveChildren(source, target, ids);

      // Recreate the maps nodeIdToPathMap and objectIdToNodeIdMap
      recreateStaticMaps();
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#pathsToIds(List)
    */
   public List<List<IPSGuid>> pathsToIds(List<String> paths) throws PSErrorException
   {
      if (paths == null || paths.isEmpty())
         throw new IllegalArgumentException("paths cannot be null or empty");

      List<List<IPSGuid>> results = new ArrayList<>();
      for (String path : paths)
      {
         List<PSHierarchyNode> tree = getHierarchyNodeTree(path);
         results.add(PSGuidUtils.toGuidList(tree));
      }

      return results;
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#removeChildren(IPSGuid, List)
    */
   @Transactional
   public void removeChildren(IPSGuid parent, List<IPSGuid> ids)
   {
      if (parent == null)
         throw new IllegalArgumentException("parent cannot be null");

      if (ids == null || ids.isEmpty())
         throw new IllegalArgumentException("ids cannot be null or empty");

      IPSUiService service = PSUiServiceLocator.getUiService();

      service.removeChildren(parent, ids);

      // Recreate the maps nodeIdToPathMap and objectIdToNodeIdMap
      recreateStaticMaps();
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#saveActions(List, boolean, String, String)
    */
   @Transactional
   public void saveActions(List<PSAction> actions, boolean release, String session, String user)
         throws PSErrorsException
   {
      PSWebserviceUtils.validateParameters(actions, "actions", true, session, user);

      List<IPSDbComponent> components = new ArrayList<>();
      List<IPSGuid> releasedIds = new ArrayList<>();
      for (PSAction action : actions)
      {
         PSAction prepared = prepareActionForSave(action);
         components.add(prepared);
         if (prepared.getGUID() != null)
            releasedIds.add(prepared.getGUID());
      }
      // SOAP / locator saveComponents writes the full RXMENUACTION graph
      // (params, visibility, relations). REST H2 can still post updateActions
      // with no XML document (PSTransactionSet: Xml Document Expected) and
      // leave 0 parent rows — JDBC then fills RXMENUACTION for GET catalog.
      try
      {
         saveComponents(components, PSAction.class, false, session, user);
      }
      catch (PSErrorsException e)
      {
         // Swallow only the REST H2 missing-XML-document case so Spring can
         // still roll back unexpected RuntimeException / dependency failures.
         if (!isXmlDocumentExpected(e))
            throw e;
         log.debug("saveComponents(updateActions) missed RXMENUACTION; JDBC fallback", e);
      }
      for (IPSDbComponent component : components)
      {
         if (component instanceof PSAction persisted)
            persistActionRowPreferringHibernate(persisted);
      }
      if (release && !releasedIds.isEmpty())
      {
         IPSObjectLockService lockService = PSObjectLockServiceLocator.getLockingService();
         List<PSObjectLock> locks = lockService.findLocksByObjectIds(releasedIds, session, user);
         lockService.releaseLocks(locks);
      }
      invalidateActionCatalog();
      evictActionMenuRegion();
      for (IPSGuid id : releasedIds)
      {
         if (id != null)
            evictActionMenuCache(id.getUUID());
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#saveDisplayFormats(List, boolean, String, String)
    */
   @Transactional
   public void saveDisplayFormats(List<PSDisplayFormat> displayFormats, boolean release, String session, String user)
         throws PSErrorsException
   {
      PSWebserviceUtils.validateParameters(displayFormats, "displayFormats", true, session, user);

      List<IPSDbComponent> components = new ArrayList<>();
      List<IPSGuid> releasedIds = new ArrayList<>();
      for (PSDisplayFormat df : displayFormats)
      {
         PSDisplayFormat prepared = prepareDisplayFormatForSave(df);
         components.add(prepared);
         if (prepared.getGUID() != null)
            releasedIds.add(prepared.getGUID());
      }
      // Locator saveComponents posts updateDisplayFormats with no XML document
      // (PSTransactionSet: Xml Document Expected). Persist PSX_DISPLAYFORMATS
      // via JDBC so GET-by-name / PUT columns round-trip (#4101).
      for (IPSDbComponent component : components)
      {
         if (component instanceof PSDisplayFormat persisted)
            ensureDisplayFormatRowPersisted(persisted);
      }
      if (release && !releasedIds.isEmpty())
      {
         IPSObjectLockService lockService = PSObjectLockServiceLocator.getLockingService();
         List<PSObjectLock> locks = lockService.findLocksByObjectIds(releasedIds, session, user);
         lockService.releaseLocks(locks);
      }
      invalidateDisplayFormatCatalog();
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#saveHierarchyNodes(List, boolean, String, String)
    */
   @Transactional
   public void saveHierarchyNodes(List<PSHierarchyNode> nodes, boolean release, String session, String user)
         throws PSErrorsException
   {
      if (nodes == null || nodes.isEmpty())
         throw new IllegalArgumentException("nodes cannot be null or empty");

      if (release && StringUtils.isBlank(session))
         throw new IllegalArgumentException("session cannot be null or empty");

      if (release && StringUtils.isBlank(user))
         throw new IllegalArgumentException("user cannot be null or empty");

      IPSUiService service = PSUiServiceLocator.getUiService();

      List<IPSGuid> ids = PSGuidUtils.toGuidList(nodes);

      IPSObjectLockService lockService = PSObjectLockServiceLocator.getLockingService();

      PSErrorsException results = new PSErrorsException();
      for (PSHierarchyNode node : nodes)
      {
         IPSGuid id = node.getGUID();
         try
         {
            if (lockService.isLockedFor(id, session, user))
            {
               // set the correct version
               Integer version = lockService.getLockedVersion(id);
               if (version != null)
                  node.setVersion(version);

               // save the object and extend the lock
               try {
                  service.saveHierarchyNode(node);
                  if (!release)
                     lockService.extendLock(id, session, user, node.getVersion());

                  results.addResult(id);
               }
               catch (PSUiException e)
               {
                  var code = WebserviceErrorCodes.SAVE_FAILED;
                  PSDesignGuid guid = new PSDesignGuid(id);
                  PSErrorException error = new PSErrorException(code, PSWebserviceErrors.createErrorMessage(code,
                        PSHierarchyNode.class.getName(), guid.getValue(), e.getLocalizedMessage()), ExceptionUtils.getFullStackTrace(e));
                  results.addError(id, error);
               }
            }
            else
            {
               PSObjectLock lock = lockService.findLockByObjectId(id, null, null);
               if (lock == null)
               {
                  var code = WebserviceErrorCodes.OBJECT_NOT_LOCKED;
                  PSDesignGuid guid = new PSDesignGuid(id);
                  PSErrorException error = new PSErrorException(code, PSWebserviceErrors.createErrorMessage(code,
                        PSHierarchyNode.class.getName(), guid.getValue()),
                        ExceptionUtils.getFullStackTrace(new Exception()));
                  results.addError(id, error);
               }
               else
               {
                  var code = WebserviceErrorCodes.OBJECT_NOT_LOCKED_FOR_REQUESTOR;
                  PSDesignGuid guid = new PSDesignGuid(id);
                  PSErrorException error = new PSErrorException(code, PSWebserviceErrors.createErrorMessage(code,
                        PSHierarchyNode.class.getName(), guid.getValue(), lock.getLocker(), lock.getRemainingTime()),
                        ExceptionUtils.getFullStackTrace(new Exception()));
                  results.addError(id, error);
               }
            }
         }
         catch (PSLockException e)
         {
            var code = WebserviceErrorCodes.SAVE_FAILED;
            PSDesignGuid guid = new PSDesignGuid(id);
            PSErrorException error = new PSErrorException(code, PSWebserviceErrors.createErrorMessage(code,
                  PSHierarchyNode.class.getName(), guid.getValue(), e.getLocalizedMessage()),
                  ExceptionUtils.getFullStackTrace(e));
            results.addError(id, error);
         }
      }

      if (release)
      {
         List<PSObjectLock> locks = lockService.findLocksByObjectIds(ids, session, user);
         lockService.releaseLocks(locks);
      }

      // Recreate the maps nodeIdToPathMap and objectIdToNodeIdMap
      recreateStaticMaps();

      if (results.hasErrors())
         throw results;
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#saveSearches(List, boolean, String, String)
    */
   @Transactional
   public void saveSearches(List<PSSearch> searches, boolean release, String session, String user)
         throws PSErrorsException
   {
      PSWebserviceUtils.validateParameters(searches, "searches", true, session, user);

      List<IPSDbComponent> components = new ArrayList<>();
      List<PSSearch> unpersisted = new ArrayList<>();
      for (PSSearch s : searches)
      {
         boolean wasNew = s != null && !s.isPersisted();
         PSSearch prepared = prepareSearchForSave(s);
         components.add(prepared);
         if (wasNew || (prepared != null && !prepared.isPersisted()))
            unpersisted.add(prepared);
      }
      // updateSearches Dataset431 (HTML SEARCHID IS NOT NULL) is DELETE-only
      // (allowInserts=no). Dataset11143 (SEARCHID IS NULL) uses Action/@dbAction
      // INSERT/UPDATE — the pipe createSearches must hit. inheritParams=true
      // copies REST HTML params; never inject SEARCHID on save.
      PSRequest req = (PSRequest) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);
      if (req != null && req.getParameter("SEARCHID") != null)
      {
         req.removeParameter("SEARCHID");
      }
      saveComponents(components, PSSearch.class, release, session, user);
      for (PSSearch prepared : unpersisted)
      {
         ensureSearchRowPersisted(prepared);
      }
      invalidateSearchCatalog();
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiDesignWs#saveViews(List, boolean, String, String)
    */@Transactional

   public void saveViews(List<PSSearch> views, boolean release, String session, String user) throws PSErrorsException
   {
      saveSearches(views, release, session, user);
   }

   /*
    * (non-Javadoc)
    *
    * @see com.percussion.webservices.ui.IPSUiDesignWs#findAllSearches()
    */
   public List<PSSearch> findAllSearches() throws PSErrorResultsException, PSErrorException
   {
      IPSCacheAccess cache = PSCacheAccessLocator.getCacheAccess();
      java.util.Optional<java.io.Serializable> cached = cache.get(ALL_SEARCHES_CACHE_KEY, IPSCacheAccess.IN_MEMORY_STORE);
      Vector<PSSearch> searches = cached.isPresent() ? (Vector<PSSearch>) cached.get() : null;
      if (searches == null)
      {
         List<IPSDbComponent> searchViews = findComponentsByNameLabel(null, null, FIND_SEARCHES,
               PSSearch.XML_NODE_NAME, PSSearch.class);
         List<PSSearch> s = getSearchOrViews(searchViews, false);
         searches = new Vector<PSSearch>();
         searches.addAll(s);
         cache.save(ALL_SEARCHES_CACHE_KEY, searches, IPSCacheAccess.IN_MEMORY_STORE);
      }
      return searches;
   }

   /*
    * (non-Javadoc)
    *
    * @see com.percussion.webservices.ui.IPSUiDesignWs#findAllViews()
    */
   public List<PSSearch> findAllViews() throws PSErrorResultsException, PSErrorException
   {
      IPSCacheAccess cache = PSCacheAccessLocator.getCacheAccess();
      java.util.Optional<java.io.Serializable> cached = cache.get(ALL_VIEWS_CACHE_KEY, IPSCacheAccess.IN_MEMORY_STORE);
      Vector<PSSearch> views = cached.isPresent() ? (Vector<PSSearch>) cached.get() : null;
      if (views == null)
      {
         List<IPSDbComponent> searchViews = findComponentsByNameLabel(null, null, FIND_SEARCHES,
               PSSearch.XML_NODE_NAME, PSSearch.class);
         List<PSSearch> s = getSearchOrViews(searchViews, true);
         views = new Vector<PSSearch>();
         views.addAll(s);
         cache.save(ALL_VIEWS_CACHE_KEY, views, IPSCacheAccess.IN_MEMORY_STORE);
      }
      return views;
   }

   /*
    * (non-Javadoc)
    *
    * @see com.percussion.webservices.ui.IPSUiDesignWs#objectIdToPath(IPSGuid
    * guid)
    */
   public String objectIdToPath(IPSGuid guid) throws PSErrorsException
   {
      PSTimer timer = new PSTimer(ms_log);

      PSErrorsException results = new PSErrorsException();
      IPSGuid id = null;
      Map<IPSGuid, String> paths;
      String nodePath="";

      if (nodeIdToPathMap == null || nodeIdToPathMap.isEmpty())
         initializeHierarchyNodeMaps();

      paths = nodeIdToPathMap.get(PSHierarchyNode.NodeType.PLACEHOLDER);

      IPSGuid node = objectIdToNodeIdMap.get(guid);
      nodePath = (node==null)? null : paths.get(node);

      // Only create dummy Navigation folder for content type guid.
      if (nodePath == null || nodePath.isEmpty() && (guid.getType() == PSTypeEnum.NODEDEF.ordinal()))
      {
         try
         {
            if (PSNavConfig.isManagedNavUsed())
            {
               PSNavConfig config = PSNavConfig.getInstance();
               if (config.getNavonTypes().contains(guid))
                  nodePath = CONTENTTYPES_NAV_PATH + guid.toString();
               if (config.getNavImageTypes().contains(guid))
                  nodePath = CONTENTTYPES_NAV_PATH + guid.toString();
               if (config.getNavTreeTypes().contains(guid))
                  nodePath = CONTENTTYPES_NAV_PATH + guid.toString();
            }
         }

         catch (PSNavException e)
         {
            var code = WebserviceErrorCodes.FAILED_TO_OBTAIN_PATH_FROM_OBJECT_ID;
            PSErrorException error = new PSErrorException(code, PSWebserviceErrors.createErrorMessage(code,
                  e.getLocalizedMessage()), ExceptionUtils.getFullStackTrace(e));
            results.addError(id, error);

         }
      }
      if (results.hasErrors())
         throw results;

      timer.logElapsed("Got the node path '" +nodePath+ "' for the guid " + guid);

      return nodePath != null ? nodePath : "";
   }

   /**
    * Spring property accessor
    *
    * @return get the cache service
    */
   public IPSCacheAccess getCache()
   {
      return m_cache;
   }

   /**
    * Set the cache service
    *
    * @param cache the service, never <code>null</code>
    */
   public void setCache(IPSCacheAccess cache)
   {
      if (cache == null)
      {
         throw new IllegalArgumentException("cache may not be null");
      }
      m_cache = cache;

      PSServer.addInitListener(new EvictionListener(m_cache));
   }

   /**
    * Creates an action object from a name and a type.
    *
    * @param name the name of the action, assumed not <code>null</code>.
    * @param type the type of the action, assumed not <code>null</code> or
    *           empty. It must be either {@link PSAction#TYPE_MENUITEM} or
    *           {@link PSAction#TYPE_MENU}.
    *
    * @return the created action object, never <code>null</code>.
    *
    * @throws RuntimeException if failed to get the next available id.
    */
   private PSAction createAction(String name, ActionType type)
   {
      PSAction source = new PSAction(name, name);
      if (ActionType.ITEM.equals(type))
      {
         source.setMenuType(PSAction.TYPE_MENUITEM);
      }
      else if (ActionType.CASCADING.equals(type))
      {
         source.setMenuType(PSAction.TYPE_MENU);
      }
      else if (ActionType.DYNAMIC.equals(type))
      {
         source.setMenuType(PSAction.TYPE_MENU);
         source.setMenuDynamic(true);
      }
      source.setClientAction(false);
      int id = getNextId(PSTypeEnum.ACTION.getKey());
      PSKey key = PSAction.createKey(String.valueOf(id));
      key.setPersisted(false);
      source.setLocator(key);

      return source;
   }

   /**
    * Creates a displayformat object from a name.
    *
    * @param name the name of the displayformat, assumed not <code>null</code>.
    *
    * @return the created displayformat object, never <code>null</code>.
    *
    * @throws RuntimeException if failed to get the next available id.
    */
   private PSDisplayFormat createDisplayFormat(String name)
   {
      PSDisplayFormat source;
      try
      {
         source = new PSDisplayFormat();
      }
      catch (PSCmsException e)
      {
        log.error(PSExceptionUtils.getMessageForLog(e));
         throw new RuntimeException("Failed to create displayformat object.", e);
      }

      source.setInternalName(name);
      source.setDisplayName(name);
      source.setDescription(name);
      int id = getNextId(PSTypeEnum.DISPLAY_FORMAT.getKey());
      PSKey key = PSDisplayFormat.createKey(new String[]
      {String.valueOf(id)});
      key.setPersisted(false);
      source.setLocator(key);
      // don't need to set allowed communities for workbench client
      // source.setAllowedCommunities(getAllCommunities());

      return source;
   }

   /**
    * Validates the specified component names, to make sure each name is not
    * blank, does not contain space character and is not used by an existing
    * component (case insensitive).
    *
    * @param names the names in question; assumed not <code>null</code> or
    *           empty.
    * @param resourcePath the resource path used to lookup the components,
    *           assumed not <code>null</code> or empty.
    * @param nodeName the XML node name of the component, assumed not
    *           <code>null</code> or empty.
    * @param type the type of the component; assumed not <code>null</code>. It
    *       must be one of the {@link PSTypeEnum#ACTION},
    *       {@link PSTypeEnum#SEARCH_DEF} or {@link PSTypeEnum#DISPLAY_FORMAT}.
    * @param objClass the class of the component, assumed not <code>null</code>.
    *           It must be {@link PSAction} for if type is
    *           {@link PSTypeEnum#ACTION}, {@link PSSearch} for if type is
    *           {@link PSTypeEnum#SEARCH_DEF}, or {@link PSDisplayFormat} for if
    *           type is {@link PSTypeEnum#DISPLAY_FORMAT}.
    *
    * @throws PSErrorException if failed to catalog the specified component.
    */
   private void validateComponentNames(List<String> names, String resourcePath, String nodeName, PSTypeEnum type,
         Class objClass) throws PSErrorException
   {
      // catalog the names of all component objects
      List<IPSDbComponent> comps = findComponentsByNameLabel(null, null, resourcePath, nodeName, objClass);
      List<String> existNames = new ArrayList<>();
      for (IPSDbComponent comp : comps)
      {
         String name = null;
         if (type == PSTypeEnum.ACTION)
         {
            name = ((PSAction) comp).getName();
         }
         else if (type == PSTypeEnum.SEARCH_DEF)
         {
            name = ((PSSearch) comp).getName();
         }
         else if (type == PSTypeEnum.DISPLAY_FORMAT)
         {
            name = ((PSDisplayFormat) comp).getName();
         }
         else
         {
            throw new IllegalArgumentException("type must be ACTION, SEARCH_DEF or DISPLAY_FORMAT");
         }

         existNames.add(name.toLowerCase());
      }

      // validating the specified names
      for (String name : names)
      {
         if (StringUtils.isBlank(name))
            throw new IllegalArgumentException("name must not be null or empty.");
         if (StringUtils.contains(name, ' '))
            throw new IllegalArgumentException("name cannot contain spaces");

         if (existNames.contains(name.toLowerCase()))
         {
            PSWebserviceUtils.throwObjectExistException(name, type);
         }
      }
   }

   /**
    * Creates a search definition from a name and a type.
    *
    * @param name the name of the new search def, assumed not <code>null</code>
    *           or empty.
    * @param type the search type, it must be one of the PSSearch.TYPE_XXXSEARCH
    *           values.
    *
    * @return the created object, never <code>null</code>.
    */
   private PSSearch createSearch(String name, String type)
   {
      if ((!type.equals(PSSearch.TYPE_STANDARDSEARCH)) && (!type.equals(PSSearch.TYPE_CUSTOMSEARCH))
            && (!type.equals(PSSearch.TYPE_USERSEARCH)) && (!type.equals(PSSearch.TYPE_VIEW)))
         throw new IllegalArgumentException("type must be one of the following values, " + PSSearch.TYPE_STANDARDSEARCH
               + ", " + PSSearch.TYPE_CUSTOMSEARCH + ", " + PSSearch.TYPE_USERSEARCH + ", " + PSSearch.TYPE_VIEW + ".");

      PSSearch source;

      try
      {
         source = new PSSearch(name);
      }
      catch (PSCmsException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         throw new RuntimeException("Failed creating PSSearch with \"" + name + "\" and \"" + type
               + "\" type, due to exception: " + e);
      }
      source.setType(type);

      int id = getNextId(PSTypeEnum.SEARCH_DEF.getKey());
      PSKey key = PSSearch.createKey(new String[]
      {String.valueOf(id)});
      key.setPersisted(false);
      source.setLocator(key);
      if (source.getState() != IPSDbComponent.DBSTATE_NEW)
      {
         source.setState(IPSDbComponent.DBSTATE_NEW);
      }

      return source;
   }

   /**
    * Deletes a list of components specified by the given parameters.
    *
    * @param ids the ids of the components, assumed not <code>null</code>.
    * @param objClass the class of the component, assumed not <code>null</code>.
    * @param objType the component type, assumed not <code>null</code> or empty.
    * @param ignoreDependencies the ignore dependencies flag, see other public
    *           deleteXXX API for detail.
    * @param session current session, assumed not <code>null</code> or empty.
    * @param user current user name, assumed not <code>null</code> or empty.
    *
    * @throws PSErrorsException if an error occurs.
    */
   private void deleteComponents(List<IPSGuid> ids, Class objClass, String objType, boolean ignoreDependencies,
         String session, String user) throws PSErrorsException
   {
      PSErrorsException results = new PSErrorsException();
      for (IPSGuid id : ids)
      {
         PSKey key = getComponentKey(id, objType);

         if (!ignoreDependencies)
         {
            PSErrorException error = PSWebserviceUtils.checkDependencies(id);
            if (error != null)
            {
               results.addError(id, error);
               continue;
            }
         }

         if (PSWebserviceUtils.hasValidLockForDelete(id, session, user))
         {
            deleteComponentReqired(key, objType, id, objClass, results);
            results.addResult(id);
         }
         else
         {
            PSWebserviceUtils.handleMissingLockError(id, objClass, results);
         }
      }

      // release locks for all successfully deleted objects
      PSWebserviceUtils.releaseLocks(results.getResults(), session, user);

      if (results.hasErrors())
         throw results;
   }

   /**
    * Gets the next available id for the given key.
    *
    * @param key the key used to get the next number, assumed not
    *           <code>null</code> or empty.
    *
    * @return the next number.
    */
   private int getNextId(String key)
   {
      try
      {
         return PSIdGenerator.getNextId(key);
      }
      catch (SQLException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         throw new RuntimeException("Failed to get next id for \"" + key + "\", due to exception.", e);
      }
   }

   /**
    * Convenience method, just like
    * {@link #deleteComponent(PSKey, String, IPSGuid, Class, PSErrorsException)}
    * except this method does not throw exception.
    *
    * @param results it used to store the GUID if successful or an
    *           {@link PSErrorException} object if failed the delete operation.
    *           It may not be <code>null</code>.
    */
   private void deleteComponentReqired(PSKey key, String componentType, IPSGuid id, Class cz, PSErrorsException results)
   {
      if (results == null)
         throw new IllegalArgumentException("result may not be null.");

      try
      {
         deleteComponent(key, componentType, id, cz, results);
      }
      catch (PSCmsException e)
      {
         // This is NOT possible, ignore
      }

   }

   /**
    * Converts a list of components to a list of catalog summaries.
    *
    * @param components the to be converted components, assumed not
    *           <code>null</code> and it has implemented
    *           {@link IPSCatalogSummary}.
    * @return the converted catalog summaries, never <code>null</code>, but may
    *         be empty.
    */
   private List<IPSCatalogSummary> getSummaries(List<IPSDbComponent> components)
   {
      return PSWebserviceUtils.toObjectSummaries(components);
   }

   /**
    * Just like
    * {@link IPSUiDesignWs#loadSearches(List, boolean, boolean, String, String)}
    * or {@link IPSUiDesignWs#loadViews(List, boolean, boolean, String, String)}
    * , except this is loading either searches or views according to isView
    * parameter.
    *
    * @param isView <code>true</code> if loading views; otherwise loading
    *           searches.
    */
   private List<PSSearch> loadSearchViews(List<IPSGuid> ids, boolean lock, boolean overrideLock, String session,
         String user, boolean isView) throws PSErrorResultsException
   {
      PSWebserviceUtils.validateParameters(ids, "ids", lock, session, user);

      // Processor loadSearches remaps H2 rows to View_All (UI-07 hole). Catalog
      // from getSearches.xml / findAllSearches sees JDBC-ensured creates; use it
      // for lock+delete so REST UI-06 delete is not 409 on a visible row.
      try
      {
         List<PSSearch> catalog = isView ? findAllViews() : findAllSearches();
         List<PSSearch> matched = matchSearchesByGuids(catalog, ids);
         if (matched.size() == ids.size())
         {
            if (lock)
            {
               IPSObjectLockService lockService = PSObjectLockServiceLocator.getLockingService();
               for (int i = 0; i < ids.size(); i++)
               {
                  PSSearch s = matched.get(i);
                  Integer version = s.getVersion() != null ? s.getVersion() : Integer.valueOf(0);
                  lockService.createLock(ids.get(i), session, user, version, overrideLock);
               }
            }
            return matched;
         }
      }
      catch (PSErrorException e)
      {
         log.debug("Search catalog load fallback to processor: {}", e.toString());
      }
      catch (PSLockException e)
      {
         PSErrorResultsException results = new PSErrorResultsException();
         results.addError(ids.get(0), e);
         throw results;
      }

      List sv = loadComponents(ids, PSSearch.class, PSSearch.getComponentType(PSSearch.class), lock, overrideLock,
            session, user);

      return getSearchOrViews(sv, isView);
   }

   static PSSearch matchSearchByGuid(List<PSSearch> catalog, IPSGuid id)
   {
      if (catalog == null || id == null)
         return null;
      long want = id.longValue();
      int uuid = id.getUUID();
      for (PSSearch s : catalog)
      {
         if (s == null)
            continue;
         IPSGuid g = s.getGUID();
         if (g != null && g.longValue() == want)
            return s;
         if (s.getId() == uuid)
            return s;
      }
      return null;
   }

   static List<PSSearch> matchSearchesByGuids(List<PSSearch> catalog, List<IPSGuid> ids)
   {
      List<PSSearch> matched = new ArrayList<>();
      if (ids == null)
         return matched;
      for (IPSGuid id : ids)
      {
         PSSearch hit = matchSearchByGuid(catalog, id);
         if (hit == null)
            return new ArrayList<>();
         matched.add(hit);
      }
      return matched;
   }

   /**
    * Loads the specified components.
    *
    * @param ids the ids of the components to be loaded, assumed not
    *           <code>null</code>.
    * @param objClass the component class, assumed not <code>null</code>.
    * @param objType the component type, assumed not <code>null</code> or empty.
    * @param lock the lock flag, see public saveXXXX() API for detail.
    * @param overrideLock override lock flag, see public saveXXX() API for
    *           detail.
    * @param session current session, assumed not <code>null</code> or empty.
    * @param user current user, assumed not <code>null</code> or empty.
    * @return the loaded components, never <code>null</code>.
    * @throws PSErrorResultsException if an error occurs.
    */
   private List loadComponents(List<IPSGuid> ids, Class objClass, String objType, boolean lock, boolean overrideLock,
         String session, String user) throws PSErrorResultsException
   {
      IPSObjectLockService lockService = PSObjectLockServiceLocator.getLockingService();

      PSErrorResultsException results = new PSErrorResultsException();
      for (IPSGuid id : ids)
      {
         PSKey key = getComponentKey(id, objType);

         try
         {
            // load the component
            IPSDbComponent component = loadComponent(key, objType, id, objClass);

            // create or extend lock with version from component if versionable,
            // otherwise set version = 1
            if (lock)
            {
               Integer version;
               if (component instanceof PSVersionableDbComponent)
               {
                  version = ((PSVersionableDbComponent) component).getVersion();
               }
               else
               {
                  version = 1;
               }

               lockService.createLock(id, session, user, version, overrideLock);
            }

            results.addResult(id, component);
         }
         catch (PSErrorException | PSLockException e)
         {
            results.addError(id, e);
         }
      }

      if (results.hasErrors())
         throw results;

      return results.getResults(ids);
   }

   /**
    * Create a component key for the supplied id and object type.
    *
    * @param id the id for which to create the component key, assumed not
    *           <code>null</code>.
    * @param objType the type of the object for which to create the component
    *           key, assumed not <code>null</code> or empty.
    * @return the component key for the specified id and object type, never
    *         <code>null</code>.
    */
   private PSKey getComponentKey(IPSGuid id, String objType)
   {
      PSKey key = null;

      if (objType.equals(PSAction.getComponentType(PSAction.class)))
         key = PSAction.createKey(String.valueOf(id.longValue()));
      else if (objType.equals(PSDisplayFormat.getComponentType(PSDisplayFormat.class)))
         key = PSDisplayFormat.createKey(new String[]
         {String.valueOf(id.getUUID())});
      else if (objType.equals(PSSearch.getComponentType(PSSearch.class)))
         key = PSSearch.createKey(new String[]
         {String.valueOf(id.longValue())});
      else
         // should never happen
         throw new RuntimeException("Cannot create component key for object type: " + objType);

      return key;
   }

   /**
    * Saves a list of components. It will delete a persisted component, then
    * save the updated one to avoid merging child components between the
    * original and the updated component. For a persisted component, it is
    * assumed the version property of its lock is not <code>null</code>;
    * otherwise it is not a persisted component.
    *
    * @param components the to be saved component list, assumed not
    *           <code>null</code> or empty. The component must implemented
    *           {@link IPSCatalogSummary}.
    * @param cz the class of the saved component, assumed not <code>null</code>.
    * @param release <code>true</code> to release all object locks after the
    *           save, <code>false</code> to keep the locks. Defaults to
    *           <code>true</code> if not supplied.
    * @param session the rhythmyx session for which to release the saved
    *           objects, not <code>null</code> or empty.
    * @param user the user for which to release the saved objects, not
    *           <code>null</code> or empty.
    *
    * @throws PSErrorsException if failed to save at least one of the component.
    */
   private void saveComponents(List<IPSDbComponent> components, Class cz, boolean release, String session, String user)
         throws PSErrorsException
   {
      IPSObjectLockService lockService = PSObjectLockServiceLocator.getLockingService();

      PSErrorsException results = new PSErrorsException();
      List<IPSGuid> releasedIds = new ArrayList<>();
      for (IPSDbComponent component : components)
      {
         IPSCatalogSummary summary = (IPSCatalogSummary) component;
         IPSGuid id = summary.getGUID();
         if (lockService.isLockedFor(id, session, user))
         {
            try
            {
               Integer version = lockService.getLockedVersion(id);

               // Unpersisted creates must INSERT, not delete-then-insert (the
               // delete resource selects updateSearches Dataset431 via HTML
               // SEARCHID and can leave inheritParams polluted for the save).
               if (version != null && !component.isPersisted())
                  version = null;

               if (!saveComponent(component, id, cz, results, version))
                  continue;

               if (!release)
               {
                  if (component instanceof PSVersionableDbComponent)
                  {
                     version = ((PSVersionableDbComponent) component).getVersion();
                  }
                  else
                  {
                     version = 1;
                  }

                  PSWebserviceUtils.extendLock(id, cz, session, user, version, results);
               }
               else
               {
                  releasedIds.add(id);
               }
               results.addResult(id);
            }
            catch (Exception e)
            {
               results.addError(id, e);
               continue;
            }
         }
         else
         {
            PSWebserviceUtils.handleMissingLockError(id, cz, results);
         }
      }

      if (release && !releasedIds.isEmpty())
      {
         List<PSObjectLock> locks = lockService.findLocksByObjectIds(releasedIds, session, user);
         lockService.releaseLocks(locks);
      }

      if (results.hasErrors())
         throw results;
   }

   /**
    * Save a specified component.
    *
    * @param comp the to be saved component, assumed not <code>null</code>.
    * @param id the GUID of the component, assumed not <code>null</code>.
    * @param cz the class of the to be saved component, assumed not
    *           <code>null</code>.
    * @param results the object to add the failure info, assumed not
    *           <code>null</code>.
    * @param version version to restore on the template before saving it.
    *
    * @return <code>true</code> if successfully saved the component;
    *         <code>false</code> if failed to save the component and added the
    *         error into the <code>results</code>.
    */
   private boolean saveComponent(IPSDbComponent comp, IPSGuid id, Class cz, PSErrorsException results, Integer version)
   {
      try
      {
         int saveVersion;
         if (version != null)
         {
            // delete the original component first, then save the updated object
            // this is a quick & dirty updating, so that we don't have to
            // "merge" all child lists.
            deleteComponent(comp.getLocator(), comp.getComponentType(), id, cz, null);

            saveVersion = version;
         }
         else
         {
            saveVersion = 0;
         }

         if (comp instanceof PSVersionableDbComponent)
         {
            ((PSVersionableDbComponent) comp).setVersion(saveVersion);
         }

         getComponentProxy().save(new IPSDbComponent[]
         {comp});
      }
      catch (PSCmsException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));

         PSDesignGuid guid = new PSDesignGuid(id);
         PSErrorException error = new PSErrorException(WebserviceErrorCodes.SAVE_FAILED,
                 PSWebserviceErrors.createErrorMessage(WebserviceErrorCodes.SAVE_FAILED,
                         cz.getName(),
               guid.longValue(),
                         PSExceptionUtils.getMessageForLog(e)),
                 PSExceptionUtils.getDebugMessageForLog(e));
         results.addError(guid, error);
         return false;
      }

      return true;
   }

   /**
    * Loads a component with the supplied id.
    *
    * @param key the to be loaded component key.
    * @param componentType the to be loaded component type.
    * @param id the to be loaded component id.
    * @param objClass the to be loaded component class.
    *
    * @return the loaded component, never <code>null</code>.
    *
    * @throws PSErrorException if cannot find the component or failed to load
    *            due to an error.
    */
   private IPSDbComponent loadComponent(PSKey key, String componentType, IPSGuid id, Class objClass)
         throws PSErrorException
   {
      Element[] elem = null;
      PSDesignGuid guid = new PSDesignGuid(id);
      PSRequest req = (PSRequest) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);
      Object previousDisplayId = null;
      boolean setDisplayId = false;
      // getDisplayFormats Dataset (DISPLAYID IS NOT NULL) builds IN (:DISPLAYID).
      // Without the HTML param, the IS NULL dataset returns the whole catalog
      // and the first row (By_Author) is replayed for every GUID (#3269 / #4101).
      if (req != null && PSDisplayFormat.class.equals(objClass) && id != null)
      {
         previousDisplayId = req.getParameter("DISPLAYID");
         req.setParameter("DISPLAYID", String.valueOf(id.getUUID()));
         setDisplayId = true;
      }
      try
      {
         elem = getComponentProxy().load(componentType, new PSKey[]
         {key});

         if (elem.length == 0) // if failed to load the object (not exist)
         {
            var code = WebserviceErrorCodes.OBJECT_NOT_FOUND;
            throw new PSErrorException(code, PSWebserviceErrors.createErrorMessage(code,
                  objClass.getName(), guid.longValue()), ExceptionUtils.getFullStackTrace(new Exception()));

         }

         return createComponent(elem[0], objClass);
      }
      catch (PSCmsException e)
      {
         var code = WebserviceErrorCodes.LOAD_FAILED;
         throw new PSErrorException(code, PSWebserviceErrors.createErrorMessage(code,
               objClass.getName(), guid.longValue()), ExceptionUtils.getFullStackTrace(e));

      }
      finally
      {
         if (setDisplayId && req != null)
         {
            if (previousDisplayId != null)
               req.setParameter("DISPLAYID", previousDisplayId);
            else
               req.removeParameter("DISPLAYID");
         }
      }
   }

   /**
    * Get the children recursive for all supplied nodes.
    *
    * @param nodes the nodes for which to get the children recursive, assumed
    *           not null, may be empty.
    * @param type the node type for which to filter the results, may be
    *           <code>null</code> to ignore this filter.
    * @param results the list into which to collect all results, assumed not
    *           <code>null</code>, may be empty.
    */
   private void getChildrenRecursive(List<PSHierarchyNode> nodes, PSHierarchyNode.NodeType type,
         List<PSHierarchyNode> results)
   {
      IPSUiService service = PSUiServiceLocator.getUiService();

      for (PSHierarchyNode node : nodes)
      {
         List<PSHierarchyNode> children = service.findHierarchyNodes("%", node.getGUID(), type);
         results.addAll(children);

         getChildrenRecursive(children, type, results);
      }
   }

   /**
    * Get the hierarchy node tree starting with the supplied id up the tree to
    * the root.
    *
    * @param id the id of the node to start with, not <code>null</code>.
    * @param tree the tree into which to fill the found nodes, not
    *           <code>null</code> may be empty. The list is filled with the root
    *           node first following by all sub nodes.
    * @throws PSUiException if any of the specified or referenced node is not
    *            found.
    */
   private void getHierarchyNodeTree(IPSGuid id, List<PSHierarchyNode> tree) throws PSUiException
   {
      if (id == null)
         throw new IllegalArgumentException("id cannot be null");

      if (tree == null)
         throw new IllegalArgumentException("tree cannot be null");

      IPSUiService service = PSUiServiceLocator.getUiService();

      PSHierarchyNode node = service.loadHierarchyNode(id);
      IPSGuid parentId = node.getParentId();
      if (parentId != null)
         getHierarchyNodeTree(parentId, tree);

      tree.add(node);
   }

   /**
    * Convert the supplied list of nodes into a path string of the form
    * <code>/tree[0].getName()/tree[1].getName()/...</code>.
    *
    * @param tree the list of nodes to convert, not <code>null</code>, may be
    *           empty.
    * @return the path string, never <code>null</code> or empty.
    */
   private String treeToPath(List<PSHierarchyNode> tree)
   {
      if (tree == null)
         throw new IllegalArgumentException("tree cannot be null");

      StringBuilder path = new StringBuilder();
      for (PSHierarchyNode node : tree) {
         path.append("/").append(node.getName());
      }

      return path.toString();
   }

   /**
    * Get the hierarchy node tree for the supplied path.
    *
    * @param path the path for which to get the node tree, not <code>null</code>
    *           or empty.
    * @return the hierarchy node tree as list stating with the root node, never
    *         <code>null</code> or empty.
    * @throws PSErrorException if any of the nodes defined in the path is not
    *            found.
    */
   private List<PSHierarchyNode> getHierarchyNodeTree(String path) throws PSErrorException
   {
      if (StringUtils.isBlank(path))
         throw new IllegalArgumentException("path cannot be null or empty");

      IPSUiService service = PSUiServiceLocator.getUiService();

      List<PSHierarchyNode> tree = new ArrayList<>();
      IPSGuid parentId = null;
      String[] names = StringUtils.split(path, '/');
      for (String name : names)
      {
         List<PSHierarchyNode> nodes = service.findHierarchyNodes(name, parentId, null);
         if (nodes.isEmpty())
         {
            String parent = "root";
            if (parentId != null)
               parent = parentId.toString();

            var code = WebserviceErrorCodes.MISSING_HIERARCHY_NODE_FOR_PARENT;
            throw new PSErrorException(code, PSWebserviceErrors.createErrorMessage(code, name,
                  parent), ExceptionUtils.getFullStackTrace(new Exception()));

         }
         if (nodes.size() > 1)
         {
            String parent = "root";
            if (parentId != null)
               parent = parentId.toString();

            var code = WebserviceErrorCodes.DUPLICATE_HIERARCHY_NODE_FOR_PARENT;
            throw new PSErrorException(code, PSWebserviceErrors.createErrorMessage(code, name,
                  parent), ExceptionUtils.getFullStackTrace(new Exception()));
         }

         PSHierarchyNode node = nodes.get(0);
         parentId = node.getGUID();
         tree.add(node);
      }

      return tree;
   }

   /**
    * Deletes a specified component.
    *
    * @param key the key of the component, assumed not <code>null</code>.
    * @param id the GUID of the component, assumed not <code>null</code>.
    * @param cz the class of the to be deleted object, assumed not
    *           <code>null</code>.
    * @param results it used to store the GUID if successful or an
    *           {@link PSErrorException} object if failed the delete operation.
    *           It may be <code>null</code> if the caller wants to catch the
    *           exception due the failure of the delete operation.
    *
    * @throws PSCmsException if failed to delete the component and the 'results'
    *            parameter is <code>null</code>.
    */
   private void deleteComponent(PSKey key, String componentType, IPSGuid id, Class cz, PSErrorsException results)
         throws PSCmsException
   {
      try
      {
         getComponentProxy().delete(componentType, new PSKey[]
         {key});
         if (results != null)
            results.addResult(id);
      }
      catch (PSCmsException e)
      {
         PSExceptionUtils.getMessageForLog(e);
         if (results == null)
         {
            throw e;
         }
         else
         {
            PSErrorException error = new PSErrorException(WebserviceErrorCodes.DELETE_FAILED,
                    PSWebserviceErrors.createErrorMessage(WebserviceErrorCodes.DELETE_FAILED,
                  cz.getName(), id.longValue(), PSExceptionUtils.getMessageForLog(e)),
                    PSExceptionUtils.getDebugMessageForLog(e));
            results.addError(id, error);
         }
      }
   }

   /**
    * @return a local component proxy, never <code>null</code>.
    */
   private PSComponentProcessorProxy getComponentProxy()
   {
      PSRequest req = (PSRequest) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);

      try
      {
         return new PSComponentProcessorProxy(PSComponentProcessorProxy.PROCTYPE_SERVERLOCAL, req);
      }
      catch (PSCmsException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));

         throw new RuntimeException("Failed to create PSComponentProcessorProxy.");
      }
   }

   /**
    * Prepare a search for {@link #saveSearches}. New (unpersisted) objects keep
    * {@code sys_community} so {@code sys_SearchCommunityHandler} can write the
    * AnyCommunity ACL, and are forced to {@code DBSTATE_NEW} so the processor
    * emits INSERT. Persisted updates still strip community properties (Workbench
    * already processed ACLs on the SOAP path).
    *
    * @param source never {@code null}
    * @return clone ready for the component processor, never {@code null}
    */
   static PSSearch prepareSearchForSave(PSSearch source)
   {
      if (source == null)
         throw new IllegalArgumentException("search cannot be null");

      PSSearch s = (PSSearch) source.cloneFull();
      if (s.isPersisted())
      {
         String[] values = s.getPropertyValues(PSSearch.PROP_COMMUNITY);
         if (values != null)
         {
            for (String comm : values)
            {
               s.removeProperty(PSSearch.PROP_COMMUNITY, comm);
            }
         }
      }
      else
      {
         PSKey key = s.getLocator();
         key.setPersisted(false);
         s.setLocator(key);
         if (s.getState() != IPSDbComponent.DBSTATE_NEW)
         {
            s.setState(IPSDbComponent.DBSTATE_NEW);
         }
      }
      return s;
   }

   /**
    * Column values for a durable {@code PSX_SEARCHES} INSERT when the XML
    * {@code updateSearches} resource reports success with 0 rows (H2 REST UI-06).
    */
   static final class SearchRowSpec
   {
      final int searchId;
      final String internalName;
      final String displayName;
      final int parentCategory;
      final String customUrl;
      final String type;
      final Integer displayFormat;
      final int maximumItems;
      final String description;
      final int caseSensitive;
      final int version;

      SearchRowSpec(int searchId, String internalName, String displayName, int parentCategory,
            String customUrl, String type, Integer displayFormat, int maximumItems, String description,
            int caseSensitive, int version)
      {
         this.searchId = searchId;
         this.internalName = internalName;
         this.displayName = displayName;
         this.parentCategory = parentCategory;
         this.customUrl = customUrl;
         this.type = type;
         this.displayFormat = displayFormat;
         this.maximumItems = maximumItems;
         this.description = description;
         this.caseSensitive = caseSensitive;
         this.version = version;
      }
   }

   static SearchRowSpec searchRowSpec(PSSearch search)
   {
      if (search == null)
         throw new IllegalArgumentException("search cannot be null");
      int id = search.getId();
      if (id <= 0)
         throw new IllegalArgumentException("search id must be assigned before persist");
      String internal = search.getInternalName();
      if (StringUtils.isBlank(internal))
         throw new IllegalArgumentException("search internal name is required");
      String display = StringUtils.defaultIfBlank(search.getDisplayName(), internal);
      String url = StringUtils.trimToNull(search.getUrl());
      String type = StringUtils.defaultIfBlank(search.getType(), PSSearch.TYPE_STANDARDSEARCH);
      Integer displayFormat = parseDisplayFormatId(search.getDisplayFormatId());
      String description = StringUtils.trimToNull(search.getDescription());
      int version = search.getVersion() != null ? search.getVersion().intValue() : 0;
      return new SearchRowSpec(id, internal, display, search.getParentCategory(), url, type, displayFormat,
            search.getMaximumResultSize(), description, search.isCaseSensitive() ? 1 : 0, version);
   }

   static Integer parseDisplayFormatId(String raw)
   {
      if (StringUtils.isBlank(raw))
         return Integer.valueOf(1);
      try
      {
         return Integer.valueOf(raw.trim());
      }
      catch (NumberFormatException e)
      {
         return Integer.valueOf(1);
      }
   }

   /**
    * If {@code updateSearches} did not INSERT, write {@code PSX_SEARCHES} so
    * {@link #findSearches} / {@link #findAllViews} can catalog the name.
    *
    * <p>Skip only when {@code INTERNALNAME} is already present. A colliding
    * {@code SEARCHID} (H2 next-number vs seed rows) must not skip the insert
    * — that left POST 200 then GET list 0 / no duplicate 409 for UI-07 views.
    */
   static void ensureSearchRowPersisted(PSSearch search)
   {
      SearchRowSpec spec = searchRowSpec(search);
      Connection conn = null;
      try
      {
         conn = PSConnectionHelper.getDbConnection();
         if (searchNameExists(conn, spec.internalName))
            return;
         if (searchIdExists(conn, spec.searchId))
         {
            int freeId = nextFreeSearchId(conn);
            applySearchId(search, freeId);
            spec = searchRowSpec(search);
         }
         try
         {
            insertSearchRow(conn, spec);
         }
         catch (SQLException insertEx)
         {
            if (searchNameExists(conn, spec.internalName))
               return;
            throw insertEx;
         }
      }
      catch (NamingException | SQLException e)
      {
         throw new IllegalStateException(
               "Search was saved but is not visible to findSearches: " + spec.internalName, e);
      }
      finally
      {
         PSConnectionHelper.releaseDbConnection(conn);
      }
   }

   /**
    * True when a row matches {@code SEARCHID} or {@code INTERNALNAME}. Tests
    * still use this OR; persist must use {@link #searchNameExists}.
    */
   static boolean searchRowExists(Connection conn, int searchId, String internalName) throws SQLException
   {
      String sql = "SELECT SEARCHID FROM PSX_SEARCHES WHERE SEARCHID = ? OR INTERNALNAME = ?";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         ps.setInt(1, searchId);
         ps.setString(2, internalName);
         try (ResultSet rs = ps.executeQuery())
         {
            return rs.next();
         }
      }
   }

   static boolean searchNameExists(Connection conn, String internalName) throws SQLException
   {
      if (StringUtils.isBlank(internalName))
         return false;
      String sql = "SELECT SEARCHID FROM PSX_SEARCHES WHERE LOWER(INTERNALNAME) = LOWER(?)";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         ps.setString(1, internalName);
         try (ResultSet rs = ps.executeQuery())
         {
            return rs.next();
         }
      }
   }

   static boolean searchIdExists(Connection conn, int searchId) throws SQLException
   {
      String sql = "SELECT SEARCHID FROM PSX_SEARCHES WHERE SEARCHID = ?";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         ps.setInt(1, searchId);
         try (ResultSet rs = ps.executeQuery())
         {
            return rs.next();
         }
      }
   }

   static int nextFreeSearchId(Connection conn) throws SQLException
   {
      String sql = "SELECT COALESCE(MAX(SEARCHID), 0) + 1 FROM PSX_SEARCHES";
      try (PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery())
      {
         if (rs.next())
            return rs.getInt(1);
      }
      return 1;
   }

   static void applySearchId(PSSearch search, int searchId)
   {
      if (search == null)
         throw new IllegalArgumentException("search cannot be null");
      if (searchId <= 0)
         throw new IllegalArgumentException("search id must be assigned before persist");
      PSKey key = PSSearch.createKey(new String[] {String.valueOf(searchId)});
      key.setPersisted(false);
      search.setLocator(key);
      if (search.getState() != IPSDbComponent.DBSTATE_NEW)
         search.setState(IPSDbComponent.DBSTATE_NEW);
   }

   static void ensureSearchRowDeleted(int searchId)
   {
      Connection conn = null;
      try
      {
         conn = PSConnectionHelper.getDbConnection();
         deleteSearchRow(conn, searchId);
      }
      catch (NamingException | SQLException e)
      {
         log.debug("Could not JDBC-delete PSX_SEARCHES SEARCHID={}: {}", searchId, e.toString());
      }
      finally
      {
         PSConnectionHelper.releaseDbConnection(conn);
      }
   }

   static void deleteSearchRow(Connection conn, int searchId) throws SQLException
   {
      try (PreparedStatement fields = conn.prepareStatement("DELETE FROM PSX_SEARCHFIELDS WHERE SEARCHID = ?"))
      {
         fields.setInt(1, searchId);
         fields.executeUpdate();
      }
      try (PreparedStatement props = conn.prepareStatement("DELETE FROM PSX_SEARCHPROPERTIES WHERE PROPERTYID = ?"))
      {
         props.setInt(1, searchId);
         props.executeUpdate();
      }
      try (PreparedStatement searches = conn.prepareStatement("DELETE FROM PSX_SEARCHES WHERE SEARCHID = ?"))
      {
         searches.setInt(1, searchId);
         searches.executeUpdate();
      }
   }

   static void insertSearchRow(Connection conn, SearchRowSpec spec) throws SQLException
   {
      String sql = "INSERT INTO PSX_SEARCHES (SEARCHID, INTERNALNAME, DISPLAYNAME, PARENTCATEGORY, "
            + "CUSTOMURL, TYPE, DISPLAYFORMAT, MAXIMUMITEMS, DESCRIPTION, CASESENSITIVE, VERSION) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         ps.setInt(1, spec.searchId);
         ps.setString(2, spec.internalName);
         ps.setString(3, spec.displayName);
         ps.setInt(4, spec.parentCategory);
         ps.setString(5, spec.customUrl);
         ps.setString(6, spec.type);
         if (spec.displayFormat == null)
            ps.setNull(7, java.sql.Types.INTEGER);
         else
            ps.setInt(7, spec.displayFormat.intValue());
         ps.setInt(8, spec.maximumItems);
         ps.setString(9, spec.description);
         ps.setInt(10, spec.caseSensitive);
         ps.setInt(11, spec.version);
         ps.executeUpdate();
      }
   }

   /**
    * Drop in-memory {@link #ALL_SEARCHES_CACHE_KEY} / {@link #ALL_VIEWS_CACHE_KEY}
    * and the XML resource cache for {@code sys_DisplayFormats/getSearches} so
    * {@link #findSearches} / {@link #findViews} see the row just saved or deleted.
    */
   static void invalidateSearchCatalog()
   {
      try
      {
         IPSCacheAccess cache = PSCacheAccessLocator.getCacheAccess();
         if (cache != null)
         {
            cache.evict(ALL_SEARCHES_CACHE_KEY, IPSCacheAccess.IN_MEMORY_STORE);
            cache.evict(ALL_VIEWS_CACHE_KEY, IPSCacheAccess.IN_MEMORY_STORE);
         }
      }
      catch (RuntimeException e)
      {
         log.debug("Could not evict in-memory search/view catalog cache: {}", e.toString());
      }
      flushDisplayFormatApplicationCache();
   }

   /**
    * Drop the XML resource cache for {@code sys_DisplayFormats} so
    * {@link #findDisplayFormats} / GET-by-name see a row just saved.
    */
   static void invalidateDisplayFormatCatalog()
   {
      flushDisplayFormatApplicationCache();
   }

   static void flushDisplayFormatApplicationCache()
   {
      try
      {
         if (PSCacheManager.isAvailable())
         {
            PSCacheManager.getInstance().flushApplication("sys_DisplayFormats");
         }
      }
      catch (RuntimeException e)
      {
         log.debug("Could not flush sys_DisplayFormats resource cache: {}", e.toString());
      }
   }

   /**
    * Drop the XML resource cache for {@code sys_psxCms} so {@link #findActions}
    * sees a row just saved or deleted in {@code RXMENUACTION}.
    */
   static void invalidateActionCatalog()
   {
      try
      {
         if (PSCacheManager.isAvailable())
         {
            PSCacheManager.getInstance().flushApplication("sys_psxCms");
         }
      }
      catch (RuntimeException e)
      {
         log.debug("Could not flush sys_psxCms action catalog cache: {}", e.toString());
      }
   }

   void evictActionMenuCache(int actionId)
   {
      evictActionMenuRegion();
      try
      {
         SessionFactory factory = getSessionFactory();
         if (factory != null && factory.getCache() != null && actionId > 0)
         {
            factory.getCache().evictEntityData(PSActionMenu.class, Integer.valueOf(actionId));
         }
      }
      catch (RuntimeException e)
      {
         log.debug("Could not evict Hibernate RXMENUACTION cache for {}: {}", actionId, e.toString());
      }
   }

   /**
    * Drop the whole {@code PSActionMenu} L2 region so GET catalog does not
    * reuse a READ_WRITE snapshot from before the JDBC INSERT/DELETE.
    */
   void evictActionMenuRegion()
   {
      try
      {
         SessionFactory factory = getSessionFactory();
         if (factory != null && factory.getCache() != null)
         {
            factory.getCache().evictEntityData(PSActionMenu.class);
         }
      }
      catch (RuntimeException e)
      {
         log.debug("Could not evict Hibernate RXMENUACTION region: {}", e.toString());
      }
   }

   /**
    * Prepare an action for {@link #saveActions}. Unpersisted objects are forced
    * to {@code DBSTATE_NEW} so INSERT (not delete-then-insert) is the intent.
    *
    * @param source never {@code null}
    * @return clone ready for JDBC persist, never {@code null}
    */
   static PSAction prepareActionForSave(PSAction source)
   {
      if (source == null)
         throw new IllegalArgumentException("action cannot be null");

      PSAction action = (PSAction) source.cloneFull();
      if (action.getId() <= 0 && source.getId() > 0)
      {
         PSKey sourceKey = source.getLocator();
         PSKey copy = PSAction.createKey(String.valueOf(source.getId()));
         if (sourceKey != null)
            copy.setPersisted(sourceKey.isPersisted());
         action.setLocator(copy);
      }
      if (!action.isPersisted())
      {
         PSKey key = action.getLocator();
         if (key != null)
         {
            key.setPersisted(false);
            action.setLocator(key);
         }
         if (action.getState() != IPSDbComponent.DBSTATE_NEW)
         {
            action.setState(IPSDbComponent.DBSTATE_NEW);
         }
      }
      return action;
   }

   /**
    * Column values for a durable {@code RXMENUACTION} INSERT/UPDATE when
    * {@code updateActions} reports success with 0 rows (H2 REST #4119).
    */
   /** Property written on REST/JDBC-created user menus so DELETE can tell them from packaged rows. */
   static final String REST_USER_MENU_PROP = RxmActionMenuConstants.REST_USER_MENU_PROP;

   static final class ActionRowSpec
   {
      final int actionId;
      final String name;
      final String displayName;
      final String description;
      final String url;
      final int sortOrder;
      final String type;
      final String handler;
      final int version;
      final boolean restUserMenu;

      ActionRowSpec(int actionId, String name, String displayName, String description, String url, int sortOrder,
            String type, String handler, int version, boolean restUserMenu)
      {
         this.actionId = actionId;
         this.name = name;
         this.displayName = displayName;
         this.description = description;
         this.url = url;
         this.sortOrder = sortOrder;
         this.type = type;
         this.handler = handler;
         this.version = version;
         this.restUserMenu = restUserMenu;
      }
   }

   static ActionRowSpec actionRowSpec(PSAction action)
   {
      if (action == null)
         throw new IllegalArgumentException("action cannot be null");
      int id = action.getId();
      if (id <= 0)
         throw new IllegalArgumentException("action id must be assigned before persist");
      String name = action.getName();
      if (StringUtils.isBlank(name))
         throw new IllegalArgumentException("action name is required");
      String display = StringUtils.defaultIfBlank(action.getLabel(), name);
      String description = StringUtils.trimToNull(action.getDescription());
      String url = StringUtils.trimToNull(action.getURL());
      String type = StringUtils.defaultIfBlank(action.getMenuType(), PSAction.TYPE_MENU);
      String handler = action.isClientAction() ? PSAction.HANDLER_CLIENT : PSAction.HANDLER_SERVER;
      int version = action.getVersion() != null ? action.getVersion().intValue() : 0;
      boolean restUser =
            StringUtils.equalsIgnoreCase(action.getProperty(REST_USER_MENU_PROP), PSAction.YES);
      return new ActionRowSpec(id, name, display, description, url, action.getSortRank(), type, handler, version,
            restUser);
   }

   /**
    * If {@code updateActions} did not INSERT, write {@code RXMENUACTION} so
    * Hibernate {@code findActionMenusTree} / GET catalog can list the name.
    * Prefer the Hibernate {@code Session} JDBC connection so the write enrolls
    * in the Spring {@code @Transactional} on {@link #saveActions}.
    */
   void persistActionRowPreferringHibernate(PSAction action)
   {
      SessionFactory factory = getSessionFactory();
      if (factory != null)
      {
         if (runActionJdbcOnCurrentSession(factory, conn -> persistActionRowOn(conn, action),
               action.getName()))
         {
            return;
         }
         runActionJdbcOnOwnSession(factory, conn -> persistActionRowOn(conn, action), action.getName());
         return;
      }
      ensureActionRowPersisted(action);
   }

   @FunctionalInterface
   interface ActionJdbcWork
   {
      void accept(Connection conn) throws SQLException;
   }

   /**
    * @return {@code true} when the current Hibernate session ran the work
    */
   boolean runActionJdbcOnCurrentSession(SessionFactory factory, ActionJdbcWork work, String actionName)
   {
      try
      {
         Session session = factory.getCurrentSession();
         session.doWork(conn -> work.accept(conn));
         session.flush();
         return true;
      }
      catch (IllegalStateException e)
      {
         throw e;
      }
      catch (org.hibernate.HibernateException e)
      {
         if (e.getCause() instanceof SQLException)
         {
            throw new IllegalStateException(
                  "Action menu was saved but is not visible to findActionMenusTree: " + actionName, e);
         }
         log.debug("No Hibernate current session for RXMENUACTION JDBC; own session", e);
         return false;
      }
      catch (RuntimeException e)
      {
         log.debug("No Hibernate current session for RXMENUACTION JDBC; own session", e);
         return false;
      }
   }

   /**
    * Independent Hibernate session that commits so GET catalog sees the row
    * when Spring did not bind {@code SessionFactory.getCurrentSession()}.
    */
   void runActionJdbcOnOwnSession(SessionFactory factory, ActionJdbcWork work, String actionName)
   {
      Session session = factory.openSession();
      Transaction tx = session.beginTransaction();
      try
      {
         session.doWork(conn -> work.accept(conn));
         tx.commit();
      }
      catch (RuntimeException e)
      {
         if (tx.isActive())
            tx.rollback();
         throw new IllegalStateException(
               "Action menu was saved but is not visible to findActionMenusTree: " + actionName, e);
      }
      finally
      {
         session.close();
      }
   }

   static void persistActionRowOn(Connection conn, PSAction action) throws SQLException
   {
      ActionRowSpec spec = actionRowSpec(action);
      if (actionRowExists(conn, spec.actionId, spec.name))
      {
         updateActionRow(conn, spec);
      }
      else
      {
         try
         {
            insertActionRow(conn, spec);
         }
         catch (SQLException insertEx)
         {
            if (!isPrimaryKeyViolation(insertEx) || !updateActionRowMatchingName(conn, spec))
               throw insertEx;
         }
      }
      if (spec.restUserMenu)
         ensureRestUserMenuProperty(conn, spec.actionId);
      else
         clearRestUserMenuProperty(conn, spec.actionId);
      persistActionRelationsOn(conn, action);
   }

   /**
    * Rewrite {@code RXMENUACTIONRELATION} for a parent whose child collection is dirty so GET
    * {@code findActionMenusTree} round-trips REST children PUT on the H2 JDBC fallback path
    * (locator {@code updateActions} can skip the XML graph). Unmodified empty children are
    * skipped so label PUT does not wipe existing associations.
    */
   static void persistActionRelationsOn(Connection conn, PSAction action) throws SQLException
   {
      if (conn == null || action == null || action.getChildren() == null)
         return;
      if (action.getChildren().getState() == IPSDbComponent.DBSTATE_UNMODIFIED)
         return;
      int parentId = action.getId();
      if (parentId <= 0)
         return;
      try (PreparedStatement del = conn.prepareStatement("DELETE FROM RXMENUACTIONRELATION WHERE ACTIONID = ?"))
      {
         del.setInt(1, parentId);
         del.executeUpdate();
      }
      String insertSql = "INSERT INTO RXMENUACTIONRELATION (ACTIONID, CHILDACTIONID) VALUES (?, ?)";
      String sortSql = "UPDATE RXMENUACTION SET SORTORDER = ? WHERE ACTIONID = ?";
      int sort = 1;
      Iterator<?> it = action.getChildren().iterator();
      while (it.hasNext())
      {
         int childId = relationChildId(it.next());
         if (childId <= 0)
            continue;
         try (PreparedStatement ins = conn.prepareStatement(insertSql))
         {
            ins.setInt(1, parentId);
            ins.setInt(2, childId);
            ins.executeUpdate();
         }
         try (PreparedStatement sortPs = conn.prepareStatement(sortSql))
         {
            sortPs.setInt(1, sort);
            sortPs.setInt(2, childId);
            sortPs.executeUpdate();
         }
         sort++;
      }
   }

   static int relationChildId(Object node)
   {
      if (node instanceof PSMenuChild child)
      {
         try
         {
            return Integer.parseInt(StringUtils.trimToEmpty(child.getChildActionId()));
         }
         catch (NumberFormatException e)
         {
            return -1;
         }
      }
      if (node instanceof PSAction child)
         return child.getId();
      return -1;
   }

   static void ensureActionRowPersisted(PSAction action)
   {
      ActionRowSpec spec = actionRowSpec(action);
      Connection conn = null;
      try
      {
         conn = PSConnectionHelper.getDbConnection();
         persistActionRowOn(conn, action);
      }
      catch (NamingException | SQLException e)
      {
         throw new IllegalStateException(
               "Action menu was saved but is not visible to findActionMenusTree: " + spec.name, e);
      }
      finally
      {
         PSConnectionHelper.releaseDbConnection(conn);
      }
   }

   static boolean actionRowExists(Connection conn, int actionId, String name) throws SQLException
   {
      String sql = "SELECT ACTIONID FROM RXMENUACTION WHERE ACTIONID = ?";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         ps.setInt(1, actionId);
         try (ResultSet rs = ps.executeQuery())
         {
            return rs.next();
         }
      }
   }

   static void insertActionRow(Connection conn, ActionRowSpec spec) throws SQLException
   {
      String sql = "INSERT INTO RXMENUACTION (ACTIONID, NAME, DISPLAYNAME, DESCRIPTION, URL, SORTORDER, TYPE, "
            + "HANDLER, VERSION) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         bindActionRow(ps, spec, false);
         ps.executeUpdate();
      }
   }

   static void updateActionRow(Connection conn, ActionRowSpec spec) throws SQLException
   {
      String sql = "UPDATE RXMENUACTION SET NAME = ?, DISPLAYNAME = ?, DESCRIPTION = ?, URL = ?, SORTORDER = ?, "
            + "TYPE = ?, HANDLER = ?, VERSION = ? WHERE ACTIONID = ?";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         bindActionRow(ps, spec, true);
         ps.executeUpdate();
      }
   }

   /**
    * Concurrent insert of the same {@code ACTIONID} must not rename a different
    * menu. Returns {@code true} only when this {@code NAME} already owns the id.
    */
   static boolean updateActionRowMatchingName(Connection conn, ActionRowSpec spec) throws SQLException
   {
      String sql = "UPDATE RXMENUACTION SET DISPLAYNAME = ?, DESCRIPTION = ?, URL = ?, SORTORDER = ?, "
            + "TYPE = ?, HANDLER = ?, VERSION = ? WHERE ACTIONID = ? AND NAME = ?";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         ps.setString(1, spec.displayName);
         ps.setString(2, spec.description);
         ps.setString(3, spec.url);
         ps.setInt(4, spec.sortOrder);
         ps.setString(5, spec.type);
         ps.setString(6, spec.handler);
         ps.setInt(7, spec.version);
         ps.setInt(8, spec.actionId);
         ps.setString(9, spec.name);
         return ps.executeUpdate() > 0;
      }
   }

   static final String XML_DOCUMENT_EXPECTED = "Xml Document Expected";

   /**
    * Locator {@code updateActions} posts with no XML document and surfaces this
    * {@link com.percussion.data.PSTransactionSet} message. Other
    * {@link PSErrorsException} cases (dependencies, lock, unexpected) must not
    * be swallowed inside {@code @Transactional} methods.
    */
   static boolean isXmlDocumentExpected(Throwable error)
   {
      for (Throwable cur = error; cur != null; cur = cur.getCause())
      {
         if (messageHasXmlDocumentExpected(cur.getMessage()))
            return true;
         if (cur instanceof PSErrorsException pe)
         {
            for (Object nested : pe.getErrors().values())
            {
               if (nested instanceof Throwable t && isXmlDocumentExpected(t))
                  return true;
               if (nested != null && messageHasXmlDocumentExpected(String.valueOf(nested)))
                  return true;
            }
         }
      }
      return false;
   }

   static boolean messageHasXmlDocumentExpected(String message)
   {
      return message != null && message.contains(XML_DOCUMENT_EXPECTED);
   }

   /**
    * SQL-92 unique/PK violation ({@code 23505}) plus common vendor codes so the
    * insert-retry path does not treat every {@link SQLException} as a race.
    */
   static boolean isPrimaryKeyViolation(SQLException error)
   {
      for (SQLException cur = error; cur != null; cur = cur.getNextException())
      {
         String state = cur.getSQLState();
         if ("23505".equals(state) || "23000".equals(state))
            return true;
         int vendor = cur.getErrorCode();
         if (vendor == 2627 || vendor == 2601 || vendor == 1062 || vendor == 1)
            return true;
      }
      return false;
   }

   static void bindActionRow(PreparedStatement ps, ActionRowSpec spec, boolean update) throws SQLException
   {
      if (update)
      {
         ps.setString(1, spec.name);
         ps.setString(2, spec.displayName);
         ps.setString(3, spec.description);
         ps.setString(4, spec.url);
         ps.setInt(5, spec.sortOrder);
         ps.setString(6, spec.type);
         ps.setString(7, spec.handler);
         ps.setInt(8, spec.version);
         ps.setInt(9, spec.actionId);
      }
      else
      {
         ps.setInt(1, spec.actionId);
         ps.setString(2, spec.name);
         ps.setString(3, spec.displayName);
         ps.setString(4, spec.description);
         ps.setString(5, spec.url);
         ps.setInt(6, spec.sortOrder);
         ps.setString(7, spec.type);
         ps.setString(8, spec.handler);
         ps.setInt(9, spec.version);
      }
   }

   /**
    * Prefer the Hibernate {@code Session} JDBC connection so child-row DELETEs
    * enroll in the Spring {@code @Transactional} on {@link #deleteActions}.
    */
   void deleteActionRowPreferringHibernate(int actionId)
   {
      SessionFactory factory = getSessionFactory();
      String label = String.valueOf(actionId);
      if (factory != null)
      {
         if (runActionJdbcOnCurrentSession(factory, conn -> deleteActionRow(conn, actionId), label))
         {
            return;
         }
         runActionJdbcOnOwnSession(factory, conn -> deleteActionRow(conn, actionId), label);
         return;
      }
      ensureActionRowDeleted(actionId);
   }

   static void ensureActionRowDeleted(int actionId)
   {
      Connection conn = null;
      try
      {
         conn = PSConnectionHelper.getDbConnection();
         deleteActionRow(conn, actionId);
      }
      catch (NamingException | SQLException e)
      {
         throw new IllegalStateException(
               "Action menu child rows could not be deleted for ACTIONID=" + actionId, e);
      }
      finally
      {
         PSConnectionHelper.releaseDbConnection(conn);
      }
   }

   /** Bind count for {@link #deleteActionChildRows} (not inferred from SQL text). */
   enum ActionSqlBinds
   {
      ACTION_ID,
      ACTION_ID_AND_CHILD
   }

   static void deleteActionRow(Connection conn, int actionId) throws SQLException
   {
      deleteActionChildRows(conn, "DELETE FROM RXMENUACTIONPARAM WHERE ACTIONID = ?", actionId,
            ActionSqlBinds.ACTION_ID);
      deleteActionChildRows(conn, "DELETE FROM RXMENUACTIONPROPERTIES WHERE ACTIONID = ?", actionId,
            ActionSqlBinds.ACTION_ID);
      deleteActionChildRows(conn, "DELETE FROM RXMENUVISIBILITY WHERE ACTIONID = ?", actionId,
            ActionSqlBinds.ACTION_ID);
      deleteActionChildRows(conn, "DELETE FROM RXMODEUICONTEXTACTION WHERE ACTIONID = ?", actionId,
            ActionSqlBinds.ACTION_ID);
      deleteActionChildRows(conn,
            "DELETE FROM RXMENUACTIONRELATION WHERE ACTIONID = ? OR CHILDACTIONID = ?", actionId,
            ActionSqlBinds.ACTION_ID_AND_CHILD);
      deleteActionChildRows(conn, "DELETE FROM RXMENUACTION WHERE ACTIONID = ?", actionId,
            ActionSqlBinds.ACTION_ID);
   }

   static void deleteActionChildRows(Connection conn, String sql, int actionId, ActionSqlBinds binds)
         throws SQLException
   {
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         ps.setInt(1, actionId);
         if (binds == ActionSqlBinds.ACTION_ID_AND_CHILD)
            ps.setInt(2, actionId);
         ps.executeUpdate();
      }
   }

   static void clearRestUserMenuProperty(Connection conn, int actionId) throws SQLException
   {
      String sql = "DELETE FROM RXMENUACTIONPROPERTIES WHERE ACTIONID = ? AND PROPNAME = ?";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         ps.setInt(1, actionId);
         ps.setString(2, REST_USER_MENU_PROP);
         ps.executeUpdate();
      }
   }

   static void ensureRestUserMenuProperty(Connection conn, int actionId) throws SQLException
   {
      String existsSql = "SELECT PROPNAME FROM RXMENUACTIONPROPERTIES WHERE ACTIONID = ? AND PROPNAME = ?";
      try (PreparedStatement ps = conn.prepareStatement(existsSql))
      {
         ps.setInt(1, actionId);
         ps.setString(2, REST_USER_MENU_PROP);
         try (ResultSet rs = ps.executeQuery())
         {
            if (rs.next())
               return;
         }
      }
      String sql = "INSERT INTO RXMENUACTIONPROPERTIES (ACTIONID, PROPNAME, PROPVALUE, DESCRIPTION) VALUES (?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         ps.setInt(1, actionId);
         ps.setString(2, REST_USER_MENU_PROP);
         ps.setString(3, PSAction.YES);
         ps.setString(4, "Created via REST action menu persist");
         ps.executeUpdate();
      }
   }

   /**
    * Prepare a display format for {@link #saveDisplayFormats}. Unpersisted
    * objects are forced to {@code DBSTATE_NEW} so the processor emits INSERT
    * (Dataset105 requires HTML {@code DISPLAYID IS NULL}).
    *
    * @param source never {@code null}
    * @return clone ready for the component processor, never {@code null}
    */
   static PSDisplayFormat prepareDisplayFormatForSave(PSDisplayFormat source)
   {
      if (source == null)
         throw new IllegalArgumentException("display format cannot be null");

      PSDisplayFormat df = (PSDisplayFormat) source.cloneFull();
      if (df.getDisplayId() <= 0 && source.getDisplayId() > 0)
      {
         df = source;
      }
      if (!df.isPersisted())
      {
         PSKey key = df.getLocator();
         if (key != null)
         {
            key.setPersisted(false);
            df.setLocator(key);
         }
         if (df.getState() != IPSDbComponent.DBSTATE_NEW)
         {
            df.setState(IPSDbComponent.DBSTATE_NEW);
         }
      }
      return df;
   }

   /**
    * Column values for a durable {@code PSX_DISPLAYFORMATS} INSERT when
    * {@code updateDisplayFormats} reports success with 0 rows (H2 REST #4101).
    */
   static final class DisplayFormatRowSpec
   {
      final int displayId;
      final String internalName;
      final String displayName;
      final String description;
      final int version;
      final List<DisplayFormatColumnSpec> columns;
      final List<DisplayFormatPropertySpec> properties;

      DisplayFormatRowSpec(int displayId, String internalName, String displayName, String description,
            int version, List<DisplayFormatColumnSpec> columns, List<DisplayFormatPropertySpec> properties)
      {
         this.displayId = displayId;
         this.internalName = internalName;
         this.displayName = displayName;
         this.description = description;
         this.version = version;
         this.columns = columns;
         this.properties = properties;
      }
   }

   static final class DisplayFormatColumnSpec
   {
      final String source;
      final String displayName;
      final int type;
      final String renderType;
      final String sortOrder;
      final int sequence;
      final String description;
      final Integer width;

      DisplayFormatColumnSpec(String source, String displayName, int type, String renderType, String sortOrder,
            int sequence, String description, Integer width)
      {
         this.source = source;
         this.displayName = displayName;
         this.type = type;
         this.renderType = renderType;
         this.sortOrder = sortOrder;
         this.sequence = sequence;
         this.description = description;
         this.width = width;
      }
   }

   static final class DisplayFormatPropertySpec
   {
      final String name;
      final String value;
      final String description;

      DisplayFormatPropertySpec(String name, String value, String description)
      {
         this.name = name;
         this.value = value;
         this.description = description;
      }
   }

   static DisplayFormatRowSpec displayFormatRowSpec(PSDisplayFormat df)
   {
      if (df == null)
         throw new IllegalArgumentException("display format cannot be null");
      int id = df.getDisplayId();
      if (id <= 0)
         throw new IllegalArgumentException("display format id must be assigned before persist");
      String internal = df.getInternalName();
      if (StringUtils.isBlank(internal))
         throw new IllegalArgumentException("display format internal name is required");
      String display = StringUtils.defaultIfBlank(df.getDisplayName(), internal);
      String description = StringUtils.trimToNull(df.getDescription());
      int version = df.getVersion() != null ? df.getVersion().intValue() : 0;
      List<DisplayFormatColumnSpec> columns = new ArrayList<>();
      if (df.getColumnContainer() != null)
      {
         for (int i = 0; i < df.getColumnContainer().size(); i++)
         {
            Object raw = df.getColumnContainer().get(i);
            if (!(raw instanceof PSDisplayColumn col))
               continue;
            String source = col.getSource();
            if (StringUtils.isBlank(source))
               continue;
            String colLabel = StringUtils.defaultIfBlank(col.getDisplayName(), source);
            String render = StringUtils.defaultIfBlank(col.getRenderType(), PSDisplayColumn.DATATYPE_TEXT);
            String sort = col.isAscendingSort() ? "A" : "D";
            String colDesc = StringUtils.trimToNull(col.getDescription());
            Integer width = col.getWidth() > 0 ? Integer.valueOf(col.getWidth()) : null;
            columns.add(new DisplayFormatColumnSpec(source, colLabel, col.isCategorized() ? 1 : 0, render, sort,
                  col.getPosition(), colDesc, width));
         }
      }
      if (columns.isEmpty())
      {
         columns.add(new DisplayFormatColumnSpec("sys_title", "Content Title", 0,
               PSDisplayColumn.DATATYPE_TEXT, "A", 0, null, null));
      }
      List<DisplayFormatPropertySpec> properties = new ArrayList<>();
      if (df.getProperties() != null)
      {
         java.util.Iterator<PSDFMultiProperty> props = df.getProperties();
         while (props.hasNext())
         {
            PSDFMultiProperty mp = props.next();
            if (mp == null || StringUtils.isBlank(mp.getName()))
               continue;
            java.util.Iterator<String> values = mp.iterator();
            while (values.hasNext())
            {
               String value = values.next();
               if (StringUtils.isBlank(value))
                  continue;
               properties.add(new DisplayFormatPropertySpec(mp.getName(), value, null));
            }
         }
      }
      return new DisplayFormatRowSpec(id, internal, display, description, version, columns, properties);
   }

   /**
    * If {@code updateDisplayFormats} did not INSERT, write {@code PSX_DISPLAYFORMATS}
    * (+ columns + properties) so {@link #findDisplayFormats} / GET-by-name can catalog
    * the name and community visibility. Catalog query inner-joins columns, so
    * sys_title is required. Properties are replaced (not insert-only) so
    * {@code sys_community=-1} does not linger after a restricted PUT.
    */
   static void ensureDisplayFormatRowPersisted(PSDisplayFormat df)
   {
      DisplayFormatRowSpec spec = displayFormatRowSpec(df);
      Connection conn = null;
      try
      {
         conn = PSConnectionHelper.getDbConnection();
         if (!displayFormatRowExists(conn, spec.displayId, spec.internalName))
         {
            try
            {
               insertDisplayFormatRow(conn, spec);
            }
            catch (SQLException insertEx)
            {
               if (!displayFormatRowExists(conn, spec.displayId, spec.internalName))
                  throw insertEx;
            }
         }
         ensureDisplayFormatColumns(conn, spec);
         ensureDisplayFormatProperties(conn, spec);
      }
      catch (NamingException | SQLException e)
      {
         throw new IllegalStateException(
               "Display format was saved but is not visible to findDisplayFormats: " + spec.internalName, e);
      }
      finally
      {
         PSConnectionHelper.releaseDbConnection(conn);
      }
   }

   static boolean displayFormatRowExists(Connection conn, int displayId, String internalName) throws SQLException
   {
      String sql = "SELECT DISPLAYID FROM PSX_DISPLAYFORMATS WHERE DISPLAYID = ? OR INTERNALNAME = ?";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         ps.setInt(1, displayId);
         ps.setString(2, internalName);
         try (ResultSet rs = ps.executeQuery())
         {
            return rs.next();
         }
      }
   }

   static void insertDisplayFormatRow(Connection conn, DisplayFormatRowSpec spec) throws SQLException
   {
      String sql = "INSERT INTO PSX_DISPLAYFORMATS (DISPLAYID, INTERNALNAME, DISPLAYNAME, DESCRIPTION, VERSION) "
            + "VALUES (?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         ps.setInt(1, spec.displayId);
         ps.setString(2, spec.internalName);
         ps.setString(3, spec.displayName);
         ps.setString(4, spec.description);
         ps.setInt(5, spec.version);
         ps.executeUpdate();
      }
   }

   static void ensureDisplayFormatColumns(Connection conn, DisplayFormatRowSpec spec) throws SQLException
   {
      deleteDisplayFormatColumnsNotInSpec(conn, spec);
      for (DisplayFormatColumnSpec col : spec.columns)
      {
         if (displayFormatColumnExists(conn, spec.displayId, col.source))
         {
            updateDisplayFormatColumn(conn, spec.displayId, col);
         }
         else
         {
            insertDisplayFormatColumn(conn, spec.displayId, col);
         }
      }
   }

   static void deleteDisplayFormatColumnsNotInSpec(Connection conn, DisplayFormatRowSpec spec) throws SQLException
   {
      if (spec.columns.isEmpty())
         return;
      StringBuilder in = new StringBuilder();
      for (int i = 0; i < spec.columns.size(); i++)
      {
         if (i > 0)
            in.append(',');
         in.append('?');
      }
      String sql = "DELETE FROM PSX_DISPLAYFORMATCOLUMNS WHERE DISPLAYID = ? AND LOWER(SOURCE) <> 'sys_title' "
            + "AND SOURCE NOT IN (" + in + ")";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         ps.setInt(1, spec.displayId);
         for (int i = 0; i < spec.columns.size(); i++)
            ps.setString(i + 2, spec.columns.get(i).source);
         ps.executeUpdate();
      }
   }

   static boolean displayFormatColumnExists(Connection conn, int displayId, String source) throws SQLException
   {
      String sql = "SELECT SOURCE FROM PSX_DISPLAYFORMATCOLUMNS WHERE DISPLAYID = ? AND SOURCE = ?";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         ps.setInt(1, displayId);
         ps.setString(2, source);
         try (ResultSet rs = ps.executeQuery())
         {
            return rs.next();
         }
      }
   }

   static void insertDisplayFormatColumn(Connection conn, int displayId, DisplayFormatColumnSpec col)
         throws SQLException
   {
      String sql = "INSERT INTO PSX_DISPLAYFORMATCOLUMNS (DISPLAYID, SOURCE, DISPLAYNAME, TYPE, RENDERTYPE, "
            + "SORTORDER, SEQUENCE, DESCRIPTION, WIDTH) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         ps.setInt(1, displayId);
         ps.setString(2, col.source);
         ps.setString(3, col.displayName);
         ps.setInt(4, col.type);
         ps.setString(5, col.renderType);
         ps.setString(6, col.sortOrder);
         ps.setInt(7, col.sequence);
         ps.setString(8, col.description);
         if (col.width == null)
            ps.setNull(9, java.sql.Types.INTEGER);
         else
            ps.setInt(9, col.width.intValue());
         ps.executeUpdate();
      }
   }

   static void updateDisplayFormatColumn(Connection conn, int displayId, DisplayFormatColumnSpec col)
         throws SQLException
   {
      String sql = "UPDATE PSX_DISPLAYFORMATCOLUMNS SET DISPLAYNAME = ?, TYPE = ?, RENDERTYPE = ?, "
            + "SORTORDER = ?, SEQUENCE = ?, DESCRIPTION = ?, WIDTH = ? WHERE DISPLAYID = ? AND SOURCE = ?";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         ps.setString(1, col.displayName);
         ps.setInt(2, col.type);
         ps.setString(3, col.renderType);
         ps.setString(4, col.sortOrder);
         ps.setInt(5, col.sequence);
         ps.setString(6, col.description);
         if (col.width == null)
            ps.setNull(7, java.sql.Types.INTEGER);
         else
            ps.setInt(7, col.width.intValue());
         ps.setInt(8, displayId);
         ps.setString(9, col.source);
         ps.executeUpdate();
      }
   }

   /**
    * Replace {@code PSX_DISPLAYFORMATPROPERTIES} for this display id with the
    * spec. Insert-only left {@code sys_community=-1} beside a restricted
    * community so GET still looked like all communities (#4098).
    */
   static void ensureDisplayFormatProperties(Connection conn, DisplayFormatRowSpec spec) throws SQLException
   {
      deleteDisplayFormatProperties(conn, spec.displayId);
      for (DisplayFormatPropertySpec prop : spec.properties)
      {
         insertDisplayFormatProperty(conn, spec.displayId, prop);
      }
   }

   static void deleteDisplayFormatProperties(Connection conn, int displayId) throws SQLException
   {
      String sql = "DELETE FROM PSX_DISPLAYFORMATPROPERTIES WHERE PROPERTYID = ?";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         ps.setInt(1, displayId);
         ps.executeUpdate();
      }
   }

   static boolean displayFormatPropertyExists(Connection conn, int displayId, String name, String value)
         throws SQLException
   {
      String sql = "SELECT PROPERTYNAME FROM PSX_DISPLAYFORMATPROPERTIES WHERE PROPERTYID = ? AND PROPERTYNAME = ? "
            + "AND PROPERTYVALUE = ?";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         ps.setInt(1, displayId);
         ps.setString(2, name);
         ps.setString(3, value);
         try (ResultSet rs = ps.executeQuery())
         {
            return rs.next();
         }
      }
   }

   static void insertDisplayFormatProperty(Connection conn, int displayId, DisplayFormatPropertySpec prop)
         throws SQLException
   {
      String sql = "INSERT INTO PSX_DISPLAYFORMATPROPERTIES (PROPERTYID, PROPERTYNAME, PROPERTYVALUE, DESCRIPTION) "
            + "VALUES (?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         ps.setInt(1, displayId);
         ps.setString(2, prop.name);
         ps.setString(3, prop.value);
         ps.setString(4, prop.description);
         ps.executeUpdate();
      }
   }

   /**
    * Replace XML-load replay (By_Author) with the JDBC row for the requested GUID
    * so GET/PUT after REST create see the persisted user format (#4101).
    */
   static List<PSDisplayFormat> reconcileDisplayFormatLoads(List<IPSGuid> ids, List<PSDisplayFormat> loaded)
   {
      if (ids == null || ids.isEmpty())
         return loaded == null ? new ArrayList<>() : loaded;
      List<PSDisplayFormat> out = new ArrayList<>(ids.size());
      for (int i = 0; i < ids.size(); i++)
      {
         IPSGuid id = ids.get(i);
         PSDisplayFormat df = loaded != null && i < loaded.size() ? loaded.get(i) : null;
         if (id != null && df != null && df.getDisplayId() == id.getUUID())
         {
            out.add(df);
            continue;
         }
         PSDisplayFormat fromDb = id == null ? null : loadDisplayFormatFromDb(id.getUUID(), null);
         out.add(fromDb != null ? fromDb : df);
      }
      return out;
   }

   /**
    * Load a display format from {@code PSX_DISPLAYFORMATS} when the XML
    * getDisplayFormats resource replays another catalog row.
    *
    * @param displayId DISPLAYID, or {@code -1} to match {@code internalName} only
    * @param internalName optional INTERNALNAME match
    * @return hydrated format, or {@code null} if no row
    */
   static PSDisplayFormat loadDisplayFormatFromDb(int displayId, String internalName)
   {
      Connection conn = null;
      try
      {
         conn = PSConnectionHelper.getDbConnection();
         return loadDisplayFormatFromDb(conn, displayId, internalName);
      }
      catch (NamingException | SQLException | PSCmsException e)
      {
         log.debug("JDBC display format load failed displayId={} name={}: {}", displayId, internalName,
               e.toString());
         return null;
      }
      finally
      {
         PSConnectionHelper.releaseDbConnection(conn);
      }
   }

   static PSDisplayFormat loadDisplayFormatFromDb(Connection conn, int displayId, String internalName)
         throws SQLException, PSCmsException
   {
      if (conn == null)
         throw new IllegalArgumentException("connection is required");
      String sql = displayId > 0
            ? "SELECT DISPLAYID, INTERNALNAME, DISPLAYNAME, DESCRIPTION, VERSION FROM PSX_DISPLAYFORMATS "
                  + "WHERE DISPLAYID = ? OR INTERNALNAME = ?"
            : "SELECT DISPLAYID, INTERNALNAME, DISPLAYNAME, DESCRIPTION, VERSION FROM PSX_DISPLAYFORMATS "
                  + "WHERE INTERNALNAME = ?";
      int id = -1;
      String internal = null;
      String display = null;
      String description = null;
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         if (displayId > 0)
         {
            ps.setInt(1, displayId);
            ps.setString(2, StringUtils.defaultString(internalName));
         }
         else
         {
            if (StringUtils.isBlank(internalName))
               return null;
            ps.setString(1, internalName);
         }
         try (ResultSet rs = ps.executeQuery())
         {
            if (!rs.next())
               return null;
            id = rs.getInt(1);
            internal = rs.getString(2);
            display = rs.getString(3);
            description = rs.getString(4);
         }
      }
      if (id <= 0 || StringUtils.isBlank(internal))
         return null;
      if (StringUtils.isNotBlank(internalName) && !internal.equalsIgnoreCase(internalName))
         return null;
      PSDisplayFormat df = new PSDisplayFormat();
      PSKey key = PSDisplayFormat.createKey(new String[] {String.valueOf(id)});
      df.setLocator(key);
      df.setInternalName(internal);
      if (StringUtils.isNotBlank(display))
         df.setDisplayName(display);
      if (description != null)
         df.setDescription(description);
      PSDFColumns cols;
      try
      {
         cols = new PSDFColumns();
      }
      catch (ClassNotFoundException e)
      {
         throw new PSCmsException(0, e.toString());
      }
      String colSql = "SELECT SOURCE, DISPLAYNAME, TYPE, RENDERTYPE, SORTORDER, SEQUENCE, DESCRIPTION, WIDTH "
            + "FROM PSX_DISPLAYFORMATCOLUMNS WHERE DISPLAYID = ? ORDER BY SEQUENCE";
      try (PreparedStatement ps = conn.prepareStatement(colSql))
      {
         ps.setInt(1, id);
         try (ResultSet rs = ps.executeQuery())
         {
            while (rs.next())
            {
               String source = rs.getString(1);
               if (StringUtils.isBlank(source))
                  continue;
               String colLabel = StringUtils.defaultIfBlank(rs.getString(2), source);
               int grouping = rs.getInt(3);
               String render = StringUtils.defaultIfBlank(rs.getString(4), PSDisplayColumn.DATATYPE_TEXT);
               String sort = rs.getString(5);
               boolean asc = sort == null || sort.trim().isEmpty() || sort.trim().startsWith("A")
                     || sort.trim().startsWith("a");
               PSDisplayColumn col = new PSDisplayColumn(source, colLabel,
                     grouping == 1 ? PSDisplayColumn.GROUPING_CATEGORY : PSDisplayColumn.GROUPING_FLAT, render,
                     StringUtils.defaultString(rs.getString(7)), asc);
               col.setPosition(rs.getInt(6));
               int width = rs.getInt(8);
               if (!rs.wasNull() && width > 0)
                  col.setWidth(width);
               cols.add(col);
            }
         }
      }
      df.setColumnList(cols);
      applyDisplayFormatPropertiesFromDb(conn, df, id);
      return df;
   }

   /**
    * Hydrate properties (including {@code sys_community}) from JDBC. {@link
    * PSDisplayFormat} construction defaults to all communities; without this,
    * GET after a restricted PUT still looked like {@code sys_community=-1}.
    */
   static void applyDisplayFormatPropertiesFromDb(Connection conn, PSDisplayFormat df, int displayId)
         throws SQLException
   {
      if (conn == null || df == null || displayId <= 0)
         return;
      String sql = "SELECT PROPERTYNAME, PROPERTYVALUE FROM PSX_DISPLAYFORMATPROPERTIES WHERE PROPERTYID = ?";
      List<DisplayFormatPropertySpec> rows = new ArrayList<>();
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         ps.setInt(1, displayId);
         try (ResultSet rs = ps.executeQuery())
         {
            while (rs.next())
            {
               String name = rs.getString(1);
               String value = rs.getString(2);
               if (StringUtils.isBlank(name) || StringUtils.isBlank(value))
                  continue;
               rows.add(new DisplayFormatPropertySpec(name, value, null));
            }
         }
      }
      if (rows.isEmpty())
         return;
      boolean hasSpecificCommunity = false;
      boolean hasCommunityAll = false;
      for (DisplayFormatPropertySpec prop : rows)
      {
         if (!PSDisplayFormat.PROP_COMMUNITY.equals(prop.name))
            continue;
         if (PSDisplayFormat.PROP_COMMUNITY_ALL.equals(prop.value))
            hasCommunityAll = true;
         else
            hasSpecificCommunity = true;
      }
      if (hasSpecificCommunity || hasCommunityAll)
      {
         df.removeProperty(PSDisplayFormat.PROP_COMMUNITY, null, false);
         if (hasSpecificCommunity)
         {
            for (DisplayFormatPropertySpec prop : rows)
            {
               if (PSDisplayFormat.PROP_COMMUNITY.equals(prop.name)
                     && !PSDisplayFormat.PROP_COMMUNITY_ALL.equals(prop.value))
               {
                  df.addCommunity(prop.value);
               }
            }
         }
         else
         {
            df.addCommunity(null);
         }
      }
      for (DisplayFormatPropertySpec prop : rows)
      {
         if (PSDisplayFormat.PROP_COMMUNITY.equals(prop.name))
            continue;
         df.setProperty(prop.name, prop.value, true);
      }
   }

   /**
    * This listener responds to table change notices by removing the cached
    * cllection of all searches.
    */
   public static class EvictionListener extends PSTableUpdateHandlerBase
   {
      /**
       * Search tables
       */
      static String[] msi_tables =
      {"PSX_SEARCHES", "PSX_SEARCHFIELDS", "PSX_SEARCHPROPERTIES"};

      /**
       * Access to the cache service, wired when the service is wired.
       */
      IPSCacheAccess mi_cache = null;

      /**
       * Ctor
       *
       * @param cache the cache accessor, never <code>null</code>
       */
      public EvictionListener(IPSCacheAccess cache)
      {
         super(msi_tables);
         if (cache == null)
         {
            throw new IllegalArgumentException("cache may not be null");
         }
         mi_cache = cache;
      }

      /**
       * This listener doesn't care about the columns
       */
      public Iterator getColumns(@SuppressWarnings("unused")
      String tableName, @SuppressWarnings("unused")
      int actionType)
      {
         return Collections.emptyList().iterator();
      }

      /**
       * Just destroy everything in the ALL_SEARCHES_CACHE_KEY section of the
       * cache.
       */
      public void tableChanged(@SuppressWarnings("unused")
      PSTableChangeEvent e)
      {
         mi_cache.evict(ALL_SEARCHES_CACHE_KEY, IPSCacheAccess.IN_MEMORY_STORE);
         mi_cache.evict(ALL_VIEWS_CACHE_KEY, IPSCacheAccess.IN_MEMORY_STORE);
         ms_log.debug("Clearing cache key: " + ALL_SEARCHES_CACHE_KEY);
      }

   }


   /**
    * The cache key for storing the collection of searches in the
    * IPSCacheAccess.IN_MEMORY_STORE region.
    */
   private static final String ALL_SEARCHES_CACHE_KEY = "All_Searches_In_System";

   /**
    * The cache key for storing the collection of CX views in the
    * IPSCacheAccess.IN_MEMORY_STORE region. Views share {@code PSX_SEARCHES}
    * with searches; both keys are evicted together.
    */
   private static final String ALL_VIEWS_CACHE_KEY = "All_Views_In_System";

   /**
    * The cache key for storing the collection of searches in the
    * IPSCacheAccess.IN_MEMORY_STORE region.
    */
   private static final String All_OBJECT_PATHS_CACHE_KEY = "All_Object_Paths_In_System";

   /**
    * Hierarchy node path prefix for navigation content types
    *
    */
   private static final String CONTENTTYPES_NAV_PATH = "/contentTypes/Navigation/";

   /**
    * The cache key for storing the map that maps the ID to its related
    * display format object in the IPSCacheAccess.IN_MEMORY_STORE region.
    */
   private static final String DISPLAY_FORMAT_ID_OBJ_MAP = "displayformat_id_object_map";

   /**
    * Cache service
    */
   IPSCacheAccess m_cache = null;

   /**
    * Commons logger
    */
    private static final Logger ms_log = LogManager.getLogger(PSUiDesignWs.class);

}
