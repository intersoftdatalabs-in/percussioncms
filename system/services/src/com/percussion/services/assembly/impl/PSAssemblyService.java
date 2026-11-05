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
package com.percussion.services.assembly.impl;

import com.percussion.cms.IPSConstants;
import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.server.PSRelationshipProcessor;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.extension.IPSExtension;
import com.percussion.extension.IPSExtensionManager;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionRef;
import com.percussion.fastforward.globaltemplate.PSRxGlobals;
import com.percussion.security.xml.PSSecureXMLUtils;
import com.percussion.security.xml.PSXmlSecurityOptions;
import com.percussion.server.PSServer;
import com.percussion.services.assembly.IPSAssembler;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSAssemblyResult;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSAssemblyTemplate.AAType;
import com.percussion.services.assembly.IPSAssemblyTemplate.OutputFormat;
import com.percussion.services.assembly.IPSSlotContentFinder;
import com.percussion.services.assembly.IPSContentFinder;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSTemplateNotImplementedException;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.assembly.data.PSAssemblyWorkItem;
import com.percussion.services.assembly.data.PSTemplateBinding;
import com.percussion.services.assembly.data.PSTemplateSlot;
import com.percussion.services.assembly.impl.nav.PSNavConfig;
import com.percussion.services.assembly.impl.nav.PSNavHelper;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSCatalogException;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.IPSNode;
import com.percussion.services.contentmgr.IPSNodeDefinition;
import com.percussion.services.contentmgr.PSContentMgrConfig;
import com.percussion.services.contentmgr.PSContentMgrLocator;
import com.percussion.services.contentmgr.PSContentMgrOption;
import com.percussion.services.contentmgr.data.PSContentNode;
import com.percussion.services.contentmgr.data.PSNodeDefinition;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.filter.PSFilterException;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.services.memory.IPSCacheAccess;
import com.percussion.services.memory.PSCacheAccessLocator;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.PSSiteHelper;
import com.percussion.services.sitemgr.PSSiteManagerLocator;
import com.percussion.services.utils.general.PSServiceConfigurationBean;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.system.utils.PSBaseBean;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.util.PSStopwatch;
import com.percussion.utils.codec.PSXmlEncoder;
import com.percussion.utils.collections.PSFacadeMap;
import com.percussion.utils.exceptions.PSExceptionHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.jexl.IPSScript;
import com.percussion.utils.jexl.PSJexlEvaluator;
import com.percussion.utils.timing.PSStopwatchStack;
import com.percussion.utils.xml.PSInvalidXmlException;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.intsof.percussioncms.auditlog.codes.AssemblyErrorCodes;
import com.intsof.percussioncms.auditlog.codes.CatalogErrorCodes;

/**
 * The assembly service assembles items and provides methods for managing
 * templates and slots.
 *
 * @author dougrand
 */
@PSBaseBean("sys_assemblyService")
@Transactional(noRollbackFor = PSAssemblyException.class)
public class PSAssemblyService implements IPSAssemblyService
{
   @PersistenceContext
   private EntityManager entityManager;

   /**
    * Cache key for content cache.
    */
   static class ContentCacheKey implements Serializable
   {
      /**
       * Serialization id.
       */
      private static final long serialVersionUID = -3176331948324016797L;

      /**
       * The filter id, initialized in ctor, assumed never <code>null</code>.
       */
      IPSGuid mi_filterid;

      /**
       * The item id, initialized in ctor, assumed never <code>null</code>.
       */
      IPSGuid mi_itemid;

      /**
       * The item context, initialized in the ctor.
       */
      int mi_context;

      /**
       * This is set to <code>true</code> if the item was loaded in AA mode. AA
       * mode items have different embedded links and need to be segregated.
       */
      boolean mi_isAA;

      /**
       * Keep an association between a given content id and the cache keys used.
       * The per content item set is added to when loading and shrunk on
       * eviction.
       */
      static Map<IPSGuid, Set<ContentCacheKey>> ms_keys = new HashMap<>();

      /**
       * Create a key.
       *
       * @param itemid item id, assumed never <code>null</code>
       * @param filterid filter id, assumed never <code>null</code>
       * @param aa <code>true</code> for aa
       * @param context the context for the item
       */
      public ContentCacheKey(IPSGuid itemid, IPSGuid filterid, boolean aa, int context)
      {
         mi_filterid = filterid;
         mi_itemid = itemid;
         mi_isAA = aa;
         mi_context = context;
         synchronized (ContentCacheKey.class)
         {
            Set<ContentCacheKey> keys = ms_keys.computeIfAbsent(itemid, k -> new HashSet<>());
            keys.add(this);
         }
      }

      /**
       * Get the keys associated with the given item, and removes the set from
       * the key map. You must take a synchronization lock on the class
       * {@link ContentCacheKey} before calling this method and you must not
       * release it until you are done with the set.
       *
       * @param itemid the item, assumed never <code>null</code>.
       * @return the set, may be <code>null</code> if no keys have been
       *         allocated for the given item.
       */
      public static Set<ContentCacheKey> getAndClearKeys(IPSGuid itemid)
      {
         return ms_keys.remove(itemid);
      }

      @Override
      public boolean equals(Object obj)
      {
         if (obj instanceof ContentCacheKey)
         {
            ContentCacheKey key = (ContentCacheKey) obj;
            return mi_filterid.equals(key.mi_filterid) && mi_itemid.equals(key.mi_itemid) && mi_isAA == key.mi_isAA
                  && mi_context == key.mi_context;
         }
         return false;
      }

      @Override
      public int hashCode()
      {
         return mi_filterid.hashCode() + mi_itemid.hashCode();
      }
   }

   /**
    * Deal with the content cache for the assembly service. Cached items are
    * held in the memory service in a separate region. Items are flushed from
    * the cache either on a per item basis (for content changes) or wholesale
    * for changes to design objects. The design objects that trigger a flush
    * are:
    * <ul>
    * <li>Template
    * <li>Slot
    * <li>Item Filter
    * <li>Location Scheme or param
    * </ul>
    * This wholesale flush deals with the body having been prefiltered for
    * cached items.
    */
   static class AssemblyContentChangedListener implements IPSNotificationListener
   {
      /**
       * List of types that require invalidation.
       */
      static List<Short> ms_invalidatefor;

      static
      {
         ms_invalidatefor = new ArrayList<>();
         ms_invalidatefor.add(PSTypeEnum.TEMPLATE.getOrdinal());
         ms_invalidatefor.add(PSTypeEnum.SLOT.getOrdinal());
         ms_invalidatefor.add(PSTypeEnum.ITEM_FILTER.getOrdinal());
         ms_invalidatefor.add(PSTypeEnum.LOCATION_SCHEME.getOrdinal());
         ms_invalidatefor.add(PSTypeEnum.LOCATION_PROPERTY.getOrdinal());
      }

      public void notifyEvent(PSNotificationEvent notification)
      {
         var cache = PSCacheAccessLocator.getCacheAccess();
         var sw = new PSStopwatch();
         sw.start();
         if (notification.getType().equals(EventType.CONTENT_CHANGED))
         {
            // Callers pass either IPSGuid (content repository) or String id
            // (e.g. PSPageChangeHandler). Accept both so page create does not
            // fail the content-changed listener with ClassCastException.
            var ccid = toGuid(notification.getTarget());
            if (ccid == null)
            {
               return;
            }
            synchronized (ContentCacheKey.class)
            {
               var keys = ContentCacheKey.getAndClearKeys(ccid);
               if (keys != null)
               {
                  for (var key : keys)
                  {
                     cache.evict(key, CONTENT_REGION);
                  }
               }
            }
         }
         else if (notification.getType().equals(EventType.OBJECT_INVALIDATION))
         {
            var guid = toGuid(notification.getTarget());
            if (guid == null)
            {
               return;
            }
            short type = guid.getType();
            if (ms_invalidatefor.contains(type))
            {
               cache.clear(CONTENT_REGION);
            }
         }
      }

      /**
       * Resolve notification target to a GUID. Supports {@link IPSGuid} and
       * string forms (legacy content guids such as {@code 16777215-101-12345}).
       */
      private static IPSGuid toGuid(Object target)
      {
         if (target == null)
         {
            return null;
         }
         if (target instanceof IPSGuid)
         {
            return (IPSGuid) target;
         }
         if (target instanceof String)
         {
            String s = ((String) target).trim();
            if (s.isEmpty())
            {
               return null;
            }
            try
            {
               // Prefer legacy content guid parse for page/item ids
               return new PSLegacyGuid(s);
            }
            catch (Exception e)
            {
               try
               {
                  return new PSGuid(s);
               }
               catch (Exception e2)
               {
                  return null;
               }
            }
         }
         return null;
      }
   }

   /**
    * Listener for the assembly service that takes care of locally cached
    * read-only data. The listener is instantiated and registered on the first
    * access to the map.
    */
   class PSAssemblyNotificationListener implements IPSNotificationListener
   {
      public void notifyEvent(PSNotificationEvent notification)
      {
         var guid = (IPSGuid) notification.getTarget();
         short type = guid.getType();
         if (type == PSTypeEnum.NODEDEF.getOrdinal() || type == PSTypeEnum.TEMPLATE.getOrdinal())
         {
            m_cache.evict(TEMPLATE_NAME_KEY, IPSCacheAccess.IN_MEMORY_STORE);
            if (type == PSTypeEnum.TEMPLATE.getOrdinal())
            {
               m_cache.evict(TEMPLATE_NAME_ID_MAP_KEY, IPSCacheAccess.IN_MEMORY_STORE);
            }
         }
      }
   }

   /**
    * Content region.
    */
   private static final String CONTENT_REGION = "content";

   /**
    * Used to identify error information in the bindings.
    */
   public static final String ERROR_VAR = "$___error___";

   /**
    * String identifying the debug assembler in Extensions.xml.
    */
   private static final String DEBUG_ASSEMBLER = "Java/global/percussion/assembly/debugAssembler";

   /**
    * Cache key for template map.
    */
   private static final String TEMPLATE_NAME_KEY = "template_name_map";

   /**
    * Cache key for the template name to id map key.
    */
   private static final String TEMPLATE_NAME_ID_MAP_KEY = "template_name_id_map";

   /**
    * Logger for the assembler.
    */
   private static final Logger log = LogManager.getLogger(IPSConstants.ASSEMBLY_LOG);

   /**
    * Counter used to generate job ids for items that have none specified.
    */
   private static AtomicLong ms_internalJobId = new AtomicLong(-1);

   /**
    * Pagelink preparsed expression.
    */
   private static IPSScript ms_pagelink = PSJexlEvaluator.createStaticExpression("$pagelink");

   /**
    * Store the current assembly item in thread local storage for access where
    * appropriate.
    */
   private static ThreadLocal<IPSAssemblyItem> ms_item = new ThreadLocal<>();

   /**
    * Notification service, wired by spring.
    */
   private IPSNotificationService m_nsvc;

   /**
    * Cache service, used to invalidate content information.
    */
   private IPSCacheAccess m_cache;

   /**
    * The service configuration bean, used in the assembly service to obtain the
    * maximum cached size.
    */
   private PSServiceConfigurationBean m_configurationBean = null;

   /**
    * Cache region identifier, look at ehcache.xml for more information.
    */
   private static final String CACHE_REGION = "assemblyqueries";

   /**
    * Static expression for page count.
    */
   private static final IPSScript PAGE_COUNT = PSJexlEvaluator.createStaticExpression("$sys.pagecount");

   /**
    * Static expression for page.
    */
   private static final IPSScript PAGE = PSJexlEvaluator.createStaticExpression("$sys.page");

   /**
    * Load specific assembler. This static method is available to the assembly
    * service implementation for the purposes of resolving the reference to a
    * particular assembler.
    * <p>
    * This loader uses the extensions manager to find a class that implements
    * {@link IPSAssembler} with the given name.
    *
    * @param name name of assembler to load, never <code>null</code>. The word
    *           "Assembler" is added to the name to lookup the extension. So if
    *           the assembler name is passed as "freemarker" this method looks
    *           for an extension named "freemarkerAssembler".
    *
    * @return an instance of {@link IPSAssembler}, never <code>null</code>
    * @throws PSAssemblyException if the bean does not exist
    */
   static IPSAssembler getAssembler(String name) throws PSAssemblyException
   {
      if (StringUtils.isBlank(name))
      {
         throw new IllegalArgumentException("name may not be null or empty");
      }
      var emgr = PSServer.getExtensionManager(null);
      try
      {
         var assemblerref = new PSExtensionRef(name);
         return (IPSAssembler) emgr.prepareExtension(assemblerref, null);
      }
      catch (PSExtensionException e)
      {
         log.error("Serious problem, cannot instantiate {} Error: {}", name, PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new PSAssemblyException(AssemblyErrorCodes.ASSEMBLER_INST, name, e);
      }
      catch (com.percussion.error.PSNotFoundException e)
      {
         log.error("Serious problem, cannot find {} Error: {}" ,name,PSExceptionUtils.getMessageForLog(e));
         throw new PSAssemblyException(AssemblyErrorCodes.ASSEMBLER_INST, name, e);
      }
   }

   @Transactional
   public IPSAssemblyResult processServletRequest(HttpServletRequest request, String templatename, String variantidstr)
         throws PSAssemblyException
   {
      try
      {
         if ((StringUtils.isBlank(variantidstr) && StringUtils.isBlank(templatename))
               || (!StringUtils.isBlank(variantidstr) && !StringUtils.isBlank(templatename)))
         {
            throw new PSAssemblyException(AssemblyErrorCodes.PARAMS_VARIANT_OR_TEMPLATE);
         }

         var params = new PSFacadeMap<>(request.getParameterMap());
         long jobId;
         if (params.get(IPSHtmlParameters.SYS_PUBSTATUSID) != null)
         {
            jobId = Long.parseLong(params.get(IPSHtmlParameters.SYS_PUBSTATUSID)[0]);
         }
         else
         {
            jobId = ms_internalJobId.decrementAndGet();
         }

         var path = request.getParameter(IPSHtmlParameters.SYS_PATH);

         var work = createAssemblyItem();
         work.setPath(path);
         work.setParameters(params);
         if (work.getJobId() == 0)
         {
            work.setJobId(jobId);
         }
         work.setDebug(request.getRequestURL().toString().endsWith("/debug"));
         if (isUseEditRevisions(request))
            work.setUserName(request.getRemoteUser());
         var publish = work.getParameterValue(IPSHtmlParameters.SYS_PUBLISH, "publish");
         work.setPublish(!publish.equalsIgnoreCase("unpublish"));
         work.normalize();
         var page = request.getParameter("sys_page");
         if (StringUtils.isNotBlank(page) && StringUtils.isNumeric(page))
         {
            work.setPage(Integer.valueOf(page));
         }
         var assemblyitems = Collections.singletonList(work);
         var results = assemble(assemblyitems);
         if (results.isEmpty())
         {
            return null;
         }
         else
         {
            return results.get(0);
         }
      }
      catch (PSAssemblyException e)
      {
         // Rethrow assembly exceptions
         throw e;
      }
      catch (Exception e)
      {
         var cause = PSExceptionHelper.findRootCause(e, true);
         log.error("Failure while processing assembly item", cause);
         throw new PSAssemblyException(AssemblyErrorCodes.UNKNOWN_ERROR, cause, e.getLocalizedMessage());
      }
   }

   /**
    * Evaluate the flag whether to use edit revisions or current revisions for
    * the parent as well as AA related items.
    * <p>
    * This flag will be evaluated to <code>true</code>:
    * <ul>
    * <li>If the requested URL is for active assembly, i.e. sys_command=editrc,
    * Or</li>
    * <li>If the request has the parameter useEditRevisions=yes</li>
    * </ul>
    *
    * @param request request object, assumed not <code>null</code>.
    * @return <code>true</code> if to use edit revisions, <code>false</code>
    *         otherwise.
    */
   private boolean isUseEditRevisions(HttpServletRequest request)
   {
      var editrc = request.getParameter(IPSHtmlParameters.SYS_COMMAND);
      if (editrc == null)
         editrc = "";
      var isEditRevisions = request.getParameter("useEditRevisions");
      if (isEditRevisions == null)
         isEditRevisions = "";
     return editrc.equalsIgnoreCase(IPSHtmlParameters.SYS_ACTIVE_ASSEMBLY)
            || isEditRevisions.equalsIgnoreCase("yes");

   }

   public IPSAssemblyItem createAssemblyItem(String path, long jobid, int refid, IPSAssemblyTemplate template,
         Map<String, String> variables, Map<String, String[]> params, Node optNode, boolean isDebug)
   {
      var rval = new PSAssemblyWorkItem();
      rval.setPath(path);
      rval.setJobId(jobid);
      rval.setReferenceId(refid);
      rval.setTemplate(template);
      rval.setVariables(variables);
      rval.setParameters(params);
      rval.setNode(optNode);
      rval.setDebug(isDebug);
      rval.setUserName(rval.getParameterValue(IPSHtmlParameters.SYS_USER, null));
      try {
         rval.normalize();
      } catch (PSAssemblyException e) {
         log.error("Failed to normalize assembly item", e);
         throw new RuntimeException(e);
      }

      return rval;
   }

   public IPSAssemblyItem createAssemblyItem()
   {
      var rval = new PSAssemblyWorkItem();

      rval.setReferenceId(0);
      rval.setJobId(0);

      return rval;
   }

   /**
    * Process and evaluate the binding variables that are used to assemble the
    * given item.
    *
    * @param item the to be processed item, assumed not <code>null</code>.
    * @param paginatedItems this is used to collect the paginated items, assumed
    *           not <code>null</code>, but may be empty.
    * @param debugItems this is used to collect items that will be assembled by
    *           {@link #DEBUG_ASSEMBLER}, assumed not <code>null</code>, but may
    *           be empty.
    * @param eval the evaluator, assumed not <code>null</code>.
    * @param isLegacy <code>true</code> if the assembler is legacy assembler.
    *
    */
   private void processItemBinding(IPSAssemblyItem item, Set<IPSAssemblyResult> paginatedItems,
         List<IPSAssemblyItem> debugItems, PSAssemblyJexlEvaluator eval, boolean isLegacy)
   {

      try
      {
         ms_item.set(item);
         processBindings(item, eval);
         Number count = null;
         try
         {
            processBindings(item, eval);
            count = (Number) eval.evaluate(PAGE_COUNT);
         }
         catch (Exception e)
         {
            log.error("Problem while processing {} binding for item: {}. Error: {}",
                    PAGE_COUNT,
                    item,
                    PSExceptionUtils.getMessageForLog(e));
         }
         int context = item.getContext();
         if (item.getParentPageReferenceId() == null && count != null && count.intValue() > 1)
         {
            item.setPaginated(true);
            if (context == 0)
            {
               if (item.getPage() == null)
               {
                  item.setPage(1);
                  eval.bind("$sys.page", item.getPage());
               }
            }
            else
            {
               paginatedItems.add((IPSAssemblyResult) item);
            }
         }
      }
      finally
      {
         ms_item.remove();
      }

      // Legacy handles debugging itself
      if (item.isDebug() && !isLegacy)
      {
         debugItems.add(item);
      }
   }

   @Transactional(readOnly = true, rollbackFor =
   {}, noRollbackFor =
   {RuntimeException.class})
   public List<IPSAssemblyResult> assemble(List<IPSAssemblyItem> items) throws PSAssemblyException, PSFilterException,
         PSTemplateNotImplementedException
   {
      if (items == null)
      {
         throw new IllegalArgumentException("items may not be null");
      }
      // map (original) item to its result.
      var assemblyResultMap = new HashMap<IPSAssemblyItem, IPSAssemblyResult>();

      handleItemTemplates(items);
      var byAssembler = groupItemsByAssembler(items);

      // Process each and gather results. Note: keep getLandingPageUrl up
      // to date as this code changes. getLandingPageUrl is not a complete
      // assembly process
      var sws = PSStopwatchStack.getStack();
      for (var assemblerName : byAssembler.keySet())
      {
         try
         {
            var perAssemblerItems = byAssembler.get(assemblerName);
            var debugItems = new ArrayList<IPSAssemblyItem>();
            var paginatedItems = new HashSet<IPSAssemblyResult>();

            for (var item : perAssemblerItems)
            {
               var isLegacy = item.getTemplate().getAssembler().equals(IPSExtension.LEGACY_ASSEMBLER);
               var eval = setupItemForAssembly(item, isLegacy);

               var assembler = getAssembler(assemblerName);
               assembler.preProcessItemBinding(item, eval);

               processItemBinding(item, paginatedItems, debugItems, eval, isLegacy);
            }

            perAssemblerItems = getAssemblyItems(perAssemblerItems, paginatedItems, debugItems, assemblyResultMap);

            assembleItems(assemblerName, sws, perAssemblerItems, debugItems, assemblyResultMap);
         }
         catch (PSAssemblyException e)
         {
            log.error("Error while assembling items for assembler {} no items processed for that assembler. Error: {}", assemblerName, PSExceptionUtils.getMessageForLog(e));
         }
         catch (PSCmsException e)
         {
            log.error("Error during assembly. Error: {}", PSExceptionUtils.getMessageForLog(e));
         }
         catch (PathNotFoundException e)
         {
            log.error("Missing content during assembly. Error: {}", PSExceptionUtils.getMessageForLog(e));
         }
         catch (RepositoryException e)
         {
            log.error("Illegal repository operation during assembly. Error: {}", PSExceptionUtils.getMessageForLog(e));
         }
      }


      checkItemsForEviction(items);

      return getAssemblyResult(items, assemblyResultMap);
   }

   public void preProcessItemBinding(IPSAssemblyItem item, PSAssemblyJexlEvaluator eval) {
      // do nothing by default
   }

   /**
    * Group the given items by the name of the assembler.
    *
    * @param items to be sorted items, assumed not <code>null</code>.
    * @return the sorted map, never <code>null</code>, but may be empty if the
    *         items is empty.
    */
   private Map<String, List<IPSAssemblyItem>> groupItemsByAssembler(List<IPSAssemblyItem> items)
   {
      return items.stream()
              .filter(item -> item.getTemplate() != null)
              .collect(Collectors.groupingBy(item -> item.getTemplate().getAssembler()));
   }

   /**
    * Assemble given items with the specified assembler.
    *
    * @param assemblerName the name of the assembler, assumed not
    *           <code>null</code> or empty.
    * @param sws stop watch, assumed not <code>null</code> or empty.
    * @param perAssemblerItems to be assembled items by the specified assembler.
    *           Assumed not <code>null</code>, may be empty.
    * @param debugItems to be assembled items by {@link #DEBUG_ASSEMBLER}
    *           assembler.
    * @param assemblyResultMap the map that maps the item to its assembled
    *           result. This is used to collect the assembled result.
    *
    * @throws PSAssemblyException if there's a problem rendering the content
    * @throws ItemNotFoundException if an item is missing from the repository
    * @throws PSFilterException if there's a problem finding or interpreting the
    *            item filter
    * @throws RepositoryException if an error occurs loading data from the
    *            repository
    * @throws PSTemplateNotImplementedException if a passed template is not
    *            supported
    */
   private void assembleItems(String assemblerName, PSStopwatchStack sws, List<IPSAssemblyItem> perAssemblerItems,
         List<IPSAssemblyItem> debugItems, Map<IPSAssemblyItem, IPSAssemblyResult> assemblyResultMap)
         throws PSAssemblyException, PSFilterException, RepositoryException,
         PSTemplateNotImplementedException
   {
      if (!perAssemblerItems.isEmpty())
      {
         try
         {
            sws.start(assemblerName + "#assemble");

            var assembler = getAssembler(assemblerName);
            var res = assembler.assemble(perAssemblerItems);
            for (int i = 0; i < res.size(); i++)
            {
               assemblyResultMap.put(perAssemblerItems.get(i), res.get(i));
            }
         }
         finally
         {
            sws.stop();
         }
      }

      if (!debugItems.isEmpty())
      {
         var debugAssembler = getAssembler(DEBUG_ASSEMBLER);
         var res = debugAssembler.assemble(debugItems);
         for (int i = 0; i < res.size(); i++)
         {
            assemblyResultMap.put(debugItems.get(i), res.get(i));
         }
      }
   }

   /**
    * Gets to be assembled items by a specific assembler, without the given
    * paginated items and items to be assembled by {@link #DEBUG_ASSEMBLER}.
    *
    * @param perAssemblerItems the original to be assembled items, which may
    *           include the paginated and debugged items. Assumed not
    *           <code>null</code>, but may be empty.
    * @param paginatedItems the paginated items, assumed not <code>null</code>,
    *           but may be empty.
    * @param debugItems the items to be assembled by {@link #DEBUG_ASSEMBLER},
    *           assumed not <code>null</code>, but may be empty.
    * @param assemblyResultMap the map that maps the item to its assembled
    *           result. This is used to collect the assembled result.
    *
    * @return the items without paginated and debugged items, never
    *         <code>null</code>, but may be empty.
    */
   private List<IPSAssemblyItem> getAssemblyItems(List<IPSAssemblyItem> perAssemblerItems,
         Set<IPSAssemblyResult> paginatedItems, List<IPSAssemblyItem> debugItems,
         Map<IPSAssemblyItem, IPSAssemblyResult> assemblyResultMap)
   {
      if (!paginatedItems.isEmpty())
      {
         // Paginated items should not be assembled, just pass through
         for (var paginatedItem : paginatedItems)
         {
            var message = "Preview of paginated items in context other than 0 " + "is not supported.";
            try {
               paginatedItem.setResultData(message.getBytes());
            }catch(IOException e){
               log.error(PSExceptionUtils.getMessageForLog(e));
            }
            assemblyResultMap.put(paginatedItem, paginatedItem);
         }
         perAssemblerItems.removeAll(paginatedItems);
      }

      if (!debugItems.isEmpty())
      {
         perAssemblerItems.removeAll(debugItems);
      }

      return perAssemblerItems;
   }

   /**
    * Gets the assembly result and make sure to keep the results in the same
    * order as the input/original assembly items.
    *
    * @param items the original assembly items, never <code>null</code>, but may
    *           be empty.
    * @param assemblyResultMap the map that maps the item to its assembled
    *           result. This is used to collect the assembled result.
    *
    * @return the assembly result, which is in the same order as the given
    *         items, never <code>null</code>, but may be empty.
    */
   private List<IPSAssemblyResult> getAssemblyResult(List<IPSAssemblyItem> items,
         Map<IPSAssemblyItem, IPSAssemblyResult> assemblyResultMap)
   {
      // Get the assembly result and make sure to keep the results in the same
      // order as the input assembly items.
      return items.stream()
              .map(assemblyResultMap::get)
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
   }

   /**
    * If we're caching content items, check each content item to see if the item
    * should be kept in the in-memory cache.
    *
    * @param items the items, may be empty but never <code>null</code>
    */
   private void checkItemsForEviction(List<IPSAssemblyItem> items)
   {
      long maxSize = m_configurationBean.getMaxCachedContentNodeSize();
      if (maxSize == 0)
         return;

      var sws = PSStopwatchStack.getStack();
      sws.start(getClass().getName() + "#checkItemsForEviction");
      try
      {
         for (var i : items)
         {
            try {
               var n = (IPSNode) i.getNode();
               long size = n.getSizeInBytes();
               if (size > maxSize) {
                  log.warn("Not caching assembly item: {} which is approx: {} bytes, maximum size allowed is: {} bytes. Forcing eviction from cache if present.",
                          n.getGuid(),
                          size,
                          maxSize);
                  m_cache.evict(n.getGuid(), CONTENT_REGION);
               }
            } catch (RepositoryException e) {
               log.warn(PSExceptionUtils.getMessageForLog(e));
               log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            }
         }
      }
      finally
      {
         sws.stop();
      }
   }

   /**
    * Lookup the template for one or more assembly items. The method first
    * creates a map of the content types of the items. Then it runs through the
    * items. The map could be optimized out for cases where only the ids are
    * passed in, but moving toward names this may prove to be the common case.
    *
    * @param items the items to look up the templates for, assumed not
    *           <code>null</code>
    */
   @Transactional
   public void handleItemTemplates(List<IPSAssemblyItem> items)
   {
      try {
         // Optimize in case the templates are already present, i.e. they
         // were passed into the assembly engine
         boolean allpresent = items.stream().allMatch(item -> item.getTemplate() != null);
         if (allpresent)
            return;

         // First see if any need additional work
         var idsToLoad = items.stream()
                 .filter(i -> i.getTemplate() == null)
                 .map(i -> ((PSLegacyGuid) i.getId()).getContentId())
                 .collect(Collectors.toList());

         if (idsToLoad.isEmpty())
            return;

         var cms = PSCmsObjectMgrLocator.getObjectManager();
         var summaries = cms.loadComponentSummaries(idsToLoad);
         var contentIdToType = summaries.stream()
                 .collect(Collectors.toMap(PSComponentSummary::getContentId, PSComponentSummary::getContentTypeId));

         for (var item : items)
         {
            if (item.getTemplate() == null)
            {
               var templatename = item.getParameterValue(IPSHtmlParameters.SYS_TEMPLATE, null);
               var variantid = item.getParameterValue(IPSHtmlParameters.SYS_VARIANTID, null);
               IPSAssemblyTemplate template = null;
               if (templatename == null && variantid == null)
               {
                  //TODO: Replace with AssemblyException or Illegal Argument
                  throw new RuntimeException("No template name or id present");
               }
               else if (StringUtils.isNumeric(templatename) || StringUtils.isNumeric(variantid))
               {
                  var idstr = StringUtils.isNumeric(templatename) ? templatename : variantid;
                  template = loadUnmodifiableTemplate(idstr);
                  if (template == null)
                  {
                     log.error("Template could not be loaded for id {}", idstr);
                  }
               }
               else if (StringUtils.isNotBlank(templatename) && !StringUtils.isNumeric(templatename))
               {
                  template = findTemplateByNameAndContentType(item, templatename, contentIdToType);
               }
               item.setTemplate(template);
            }
         }
      } catch (PSAssemblyException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Find template by name and content type with enhanced validation.
    *
    * @param item the assembly item
    * @param templatename the template name
    * @param contentIdToType content ID to type mapping
    * @return the found template, may be {@code null}
    * @throws PSAssemblyException if template lookup fails
    */
   private IPSAssemblyTemplate findTemplateByNameAndContentType(
         IPSAssemblyItem item, String templatename, Map<Integer, Long> contentIdToType)
         throws PSAssemblyException {
      if (!(item.getId() instanceof PSLegacyGuid)) {
         log.warn("Item ID is not a PSLegacyGuid, cannot determine content type for template lookup");
         return null;
      }
      var guid = (PSLegacyGuid) item.getId();
      var contentId = guid.getContentId();
      var contentTypeId = contentIdToType.get(contentId);

      if (contentTypeId == null) {
         log.warn("Content type ID not found for content ID: {}", contentId);
         return null;
      }

      var ctypeguid = new PSGuid(PSTypeEnum.NODEDEF, contentTypeId);
      var template = findTemplateByNameAndType(templatename, ctypeguid);

      if (template == null) {
         log.error("Template could not be loaded for name '{}' and type '{}'", templatename, ctypeguid);
      }

      return template;
   }

   /**
    * Process jexl bindings for the template and assign results to the assembly
    * item. When this completes, the evaluated bindings from the eval param and
    * the binding process will replace any bindings currently on the work item.
    *
    * @param item the assembly item, assumed non-<code>null</code>
    * @param eval the evaluator, assumed non-<code>null</code>
    */
   private void processBindings(IPSAssemblyItem item, PSAssemblyJexlEvaluator eval)
   {
      var t = item.getTemplate();
      var bindings = t.getBindings();
      for (var binding : bindings)
      {
         if (binding == null)
            continue;
         var var1 = binding.getVariable();
         IPSScript exp = null;
         try
         {
            exp = (binding).getJexlScript();
            exp.setOwnerType(t.getTemplateType().name());
            exp.setOwnerName(t.getName());
            eval.evaluate(var1, exp);
         }
         catch (Exception e)
         {
            if (item.isDebug())
            {
               synchronized (eval.getVars())
               {
                  Map<String, Throwable> emap = (Map<String, Throwable>) eval.getVars().get(ERROR_VAR);
                  if (emap == null)
                  {
                     emap = new HashMap<>();
                     eval.getVars().put(ERROR_VAR, emap);
                  }
                  emap.put(var1, e);
               }
            }
            else
            {
               var debugMessage = MessageFormat.format("Problem when evaluating expression \"{1}\" "
                     + "for variable \"{0}\": {2}", var1 != null ? var1 : "<no variable>", exp != null
                     ? exp.getSourceText()
                     : "<null>",PSExceptionUtils.getMessageForLog(e));

               log.debug("{} Error: {}", debugMessage, PSExceptionUtils.getDebugMessageForLog(e));
               log.error("Problem when evaluating JEXL binding expression. One or more bindings not evaluated for Template: {} and Content Id: {}.  Expression: {}. Error: {}",
                       item.getTemplate().getName(),
                       item.getId(),
                       exp,
                       PSExceptionUtils.getMessageForLog(e));
            }
         }
      }
      item.setBindings(eval.getVars());
   }

   /**
    * Setup initial variables for assembly. If the item has not yet been loaded
    * from the content manager, that will be done first. Then all initial
    * variables will be bound into the context. Then all available JEXL
    * extensions are loaded and bound (user and system). Last the bindings are
    * evaluated.
    * <p>
    * For legacy items, a more limited set of data is set up to enable global
    * template handling to work correctly.
    *
    * @param work the assembly item, assumed not <code>null</code>
    * @param isLegacy <code>true</code> when assembling legacy templates
    * @return a jexl evaluator, never <code>null</code>
    */
   private PSAssemblyJexlEvaluator setupItemForAssembly(IPSAssemblyItem work, boolean isLegacy)
         throws PSAssemblyException, PSFilterException, PSCmsException, RepositoryException
   {
      var sws = PSStopwatchStack.getStack();
      try
      {
         sws.start(getClass().getName() + "#setupItemForAssembly");

         var siteidstr = work.getParameterValue(IPSHtmlParameters.SYS_SITEID, null);
         var contextstr = work.getParameterValue(
               IPSHtmlParameters.SYS_CONTEXT, null);

         var sys_command = work.getParameterValue(IPSHtmlParameters.SYS_COMMAND, null);
         var defaultAAMode = IPSHtmlParameters.SYS_AAMODE_ICONS;
         var isHummingbirdEnabled = "false";
         boolean isHBE = false;
         var props = PSServer.getServerProps();
         if (props != null) {
            defaultAAMode = StringUtils.defaultIfEmpty((String) props.get("defaultActiveAssemblyMode"),
                  IPSHtmlParameters.SYS_AAMODE_ICONS);
            isHummingbirdEnabled = StringUtils.defaultIfEmpty(
                  (String)props.get("isHummingbirdEnabled"),
                     "false");
         }
         var sys_aamode = work.getParameterValue(IPSHtmlParameters.SYS_ACTIVE_ASSEMBLY_MODE, defaultAAMode);

         boolean isForAaSlot = Boolean.parseBoolean(work.getParameterValue(IPSHtmlParameters.SYS_FORAASLOT,
               Boolean.toString(true)));
         var rval = new PSAssemblyJexlEvaluator(work);
         boolean nonHTML = work.getTemplate().getActiveAssemblyType().equals(AAType.NonHtml);
         boolean isAA;
         // As we do not active assemble child items set $sys.activeAssembly to
         // false if it is a child table item.
         boolean isChildTableItem = work.getId() instanceof PSLegacyGuid && ((PSLegacyGuid) work.getId()).isChildGuid();
         if (isForAaSlot && !nonHTML && StringUtils.isNotEmpty(sys_command)
               && sys_command.equals(IPSHtmlParameters.SYS_ACTIVE_ASSEMBLY) && !isChildTableItem)
         {
            rval.bind("$sys.activeAssembly", true);
            isAA = true;
         }
         else
         {
            rval.bind("$sys.activeAssembly", false);
            isAA = false;
         }

         if (isAA && StringUtils.isNotBlank(sys_aamode))
         {
            rval.bind("$sys_aamode", Integer.valueOf(sys_aamode));
         }

         if (work.getPage() != null)
         {
            rval.bind("$sys.page", work.getPage());
         }

         if (!work.hasNode())
         {
            loadContentItem(work, rval, isAA);
         }

         if(isHummingbirdEnabled.equals("true"))
         {
            isHBE = true;
            rval.bind("$sys.isHummingbirdEnabled", isHBE);
         }
         else
         {
            rval.bind("$sys.isHummingbirdEnabled", isHBE);
         }

         /*
          * Setup initial bindings
          */
         try
         {
            if (siteidstr != null && Integer.parseInt(siteidstr) > 0)
            {
               PSSiteHelper.setupSiteInfo(rval, siteidstr, contextstr);
            }
         }
         catch (NumberFormatException | PSNotFoundException e)
         {
            log.warn(PSExceptionUtils.getMessageForLog(e));
            // This should not happen at this level...
            log.debug("Skipping site information setting as the supplied siteid is not an integer.", e);
         }

         var templ = work.getTemplate();
         if (!isLegacy || !templ.getBindings().isEmpty())
         {
            rval.bind("$sys.item", work.getNode());
            //TODO: Should this be a new instance or just passing static reference to the class?
            rval.bind("$sys.aautils", new PSAAUtils());
         }
         rval.bind("$sys.template", templ.getTemplate());
         rval.bind("$sys.mimetype", templ.getMimeType());
         rval.bind("$sys.charset", templ.getCharset());
         rval.bind("$sys.asm", this);
         rval.bind("$sys.assemblyItem", work);

         return rval;
      }
      finally
      {
         sws.stop();
      }
   }

   /**
    * Load the content item from the data in the parameters. Validates the data
    * as well. Either the contentid + revision or the path must be specified in
    * the parameters. This method is not called if the item is already loaded in
    * the work item.
    *
    * @param work the work item, assumed not null.
    * @param eval the jexl evaluator to set variables upon, assumed never
    *           <code>null</code>.
    * @param isAA if <code>true</code> this assembly is for active assembly, if
    *           <code>false</code> it is not. This is used to keep items loaded
    *           for AA separate as they have embedded links that are different
    *           from non-AA items.
    *
    * @throws PSAssemblyException
    * @throws RepositoryException
    * @throws PSCmsException
    * @throws PathNotFoundException
    * @throws UnsupportedRepositoryOperationException
    * @throws ValueFormatException
    * @throws PSFilterException
    */
   private void loadContentItem(IPSAssemblyItem work, PSAssemblyJexlEvaluator eval, boolean isAA)
         throws PSAssemblyException, RepositoryException, PSCmsException, PSFilterException
   {
      Node item = null;
      IPSCacheAccess cache = null;
      ContentCacheKey key = null;
      // long maxSize = m_configurationBean.getMaxCachedContentNodeSize();
      long maxSize = 0;

      if (maxSize > 0)
      {
         cache = PSCacheAccessLocator.getCacheAccess();
         key = new ContentCacheKey(work.getId(), work.getFilter().getGUID(), isAA, work.getContext());
         var cached = cache.get(key, CONTENT_REGION);
         if (cached.isPresent()) {
             item = (Node) cached.get();
         }
      }
      if (item == null)
      {
         var contentmgr = PSContentMgrLocator.getContentMgr();
         // Handle content lookup
         Collection<Node> items = null;

         var guids = new ArrayList<IPSGuid>();
         guids.add(work.getId());

            var siteid = work.getParameterValue(IPSHtmlParameters.SYS_SITEID, "");
            var config = new PSContentMgrConfig();
            config.addOption(PSContentMgrOption.LAZY_LOAD_CHILDREN);
            config.addOption(PSContentMgrOption.LOAD_MINIMAL);
            config.setBodyAccess(new PSInlineLinkProcessor(work.getFilter(), work));
            config.setNamespaceCleanup( PSNamespaceCleanup2.getInstance());
            items = contentmgr.findItemsByGUID(guids, config);


         if (items.isEmpty())
            throw new ItemNotFoundException("Can't find item for guid: " + work.getId());

         item = items.iterator().next();

         var node = (PSContentNode) item;
         //TODO: It seems like this caching code does nothing and is not used.  Either need to remove it or make it work.
         if (maxSize > 0 && node.getSizeInBytes() < maxSize)
         {
            cache.save(key, node, CONTENT_REGION);
         }
      }

      // Check to see if this item is a managed nav object. If it is we'll
      // wrap it up into a proxy
      var navcfg = PSNavConfig.getInstance();
      var itemtype = new PSGuid(PSTypeEnum.NODEDEF, item.getProperty("sys_contenttypeid").getString());
      if (navcfg.isManagedNavType(itemtype))
      {
         var h = work.getNavHelper();
         var lg = (PSLegacyGuid) work.getId();
         item = h.getNavNode(lg.getLocator());
         h.setupNavValues(work, eval, item);
      }
      work.setNode(item);
   }

   public PSTypeEnum[] getTypes()
   {

      return new PSTypeEnum[]
      {PSTypeEnum.TEMPLATE, PSTypeEnum.SLOT};
   }

   @Transactional
   public List<IPSCatalogSummary> getSummaries(PSTypeEnum type)
   {
      List<IPSCatalogSummary> rval = new ArrayList<>();

      if (type.equals(PSTypeEnum.TEMPLATE))
         rval  = findAllTemplates().stream()
                 .map(t -> new PSObjectSummary(t.getGUID(), t.getName(), t.getLabel(), t.getDescription())).collect(Collectors.toList());
      else if (type.equals(PSTypeEnum.SLOT))
         rval  = findAllSlots().stream()
                 .map(t -> new PSObjectSummary(t.getGUID(), t.getName(), t.getLabel(), t.getDescription())).collect(Collectors.toList());


      return rval;
   }

   @Transactional
   public void loadByType(PSTypeEnum type, String item) throws PSCatalogException
   {
      var session = entityManager.unwrap(Session.class);
      try
      {
         if (type.equals(PSTypeEnum.TEMPLATE))
         {
            var guid = PSXmlSerializationHelper.getIdFromXml(PSTypeEnum.TEMPLATE, item);
            Integer tversion = null;
            Map<Long, Integer> bversions = new HashMap<>();

            // Snapshot optimistic-lock versions only — never fromXML onto the
            // managed loadTemplate instance (Hibernate 7 treats that as a dirty
            // managed graph and merge fails with StaleObjectStateException /
            // package install abort / maintenance mode, #2540).
            try
            {
               var existing = loadTemplate(guid, false);
               tversion = existing.getVersion();
               for (var b : existing.getBindings())
               {
                  // getId() boxes primitive long — never null; guard binding only.
                  if (b != null)
                  {
                     bversions.put(b.getId(), b.getVersion());
                  }
               }
               session.evict(existing);
            }
            catch (PSAssemblyException e)
            {
               // New template for this GUID — versions stay null until ensure.
            }

            // Always deserialize into a fresh detached instance.
            var temp = new PSAssemblyTemplate();
            temp.fromXML(item);

            // Restore optimistic-lock versions from the previously loaded row so
            // merge updates rather than fighting a null @Version on assigned ids.
            if (tversion != null)
            {
               temp.setVersion(null);
               temp.setVersion(tversion);
               for (var b : temp.getBindings())
               {
                  if (b == null)
                  {
                     continue;
                  }
                  var bversion = bversions.get(b.getId());
                  if (bversion != null)
                  {
                     b.setVersion(null);
                     b.setVersion(bversion);
                  }
               }
            }

            saveTemplate(temp);
         }
         else if (type.equals(PSTypeEnum.SLOT))
         {
            var guid = PSXmlSerializationHelper.getIdFromXml(PSTypeEnum.SLOT, item);
            IPSTemplateSlot temp;
            if (findSlot(guid) != null)
            {
               temp = loadSlotModifiable(guid);
               ((PSTemplateSlot) temp).setVersion(null);
            }
            else
            {
               temp = createSlot();
            }
            temp.fromXML(item);
            saveSlot(temp);
         }
         else
         {
            throw new PSCatalogException(CatalogErrorCodes.UNKNOWN_TYPE, type.toString());
         }
      }
      catch (PSAssemblyException e)
      {
         throw new PSCatalogException(CatalogErrorCodes.REPOSITORY, e, type);
      }
      catch (IOException e)
      {
         throw new PSCatalogException(CatalogErrorCodes.IO, e, type);
      }
      catch (SAXException | PSInvalidXmlException e)
      {
         throw new PSCatalogException(CatalogErrorCodes.XML, e, item);
      }


   }

   @Transactional()
   public String saveByType(IPSGuid id) throws PSCatalogException
   {
      try
      {
         if (id.getType() == PSTypeEnum.TEMPLATE.getOrdinal())
         {
            var temp = loadTemplate(id, true);
            return temp.toXML();
         }
         else if (id.getType() == PSTypeEnum.SLOT.getOrdinal())
         {
            var slot = loadSlot(id);
            return slot.toXML();
         }
         else
         {
            var type = PSTypeEnum.valueOf(id.getType());
            throw new PSCatalogException(CatalogErrorCodes.UNKNOWN_TYPE, type);
         }
      }
      catch (PSAssemblyException e)
      {
         throw new PSCatalogException(CatalogErrorCodes.REPOSITORY, e, id);
      }
      catch (IOException e)
      {
         throw new PSCatalogException(CatalogErrorCodes.IO, e, id);
      }
      catch (SAXException e)
      {
         throw new PSCatalogException(CatalogErrorCodes.TOXML, e);
      }
   }

   @Transactional()
   public PSAssemblyTemplate createTemplate()
   {
      var gmgr = PSGuidManagerLocator.getGuidMgr();
      var newvar = new PSAssemblyTemplate();
      newvar.setGUID(gmgr.createGuid(PSTypeEnum.TEMPLATE));
      return newvar;
   }

   @Transactional
   public PSAssemblyTemplate loadTemplate(String guidstr, boolean loadSlots) throws PSAssemblyException
   {
      var guid = new PSGuid(PSTypeEnum.TEMPLATE, guidstr);
      var template = loadTemplate(guid, loadSlots);
      Hibernate.initialize(template);
      return template;
   }

   @Transactional
   public PSAssemblyTemplate loadTemplate(IPSGuid id, boolean loadSlots) throws PSAssemblyException
   {
      var var = findTemplate(id, loadSlots);
      Hibernate.initialize(var);
      if (var == null)
      {
         throw new PSAssemblyException(AssemblyErrorCodes.TEMPLATE_MISSING, id);
      }
      return var;
   }

   @Transactional(readOnly = true)
   public IPSAssemblyTemplate loadUnmodifiableTemplate(IPSGuid tid) throws PSAssemblyException
   {
      if (tid == null)
      {
         throw new IllegalArgumentException("tid may not be null");
      }
      var var = findTemplate(tid);
      if (var == null)
      {
         throw new PSAssemblyException(AssemblyErrorCodes.TEMPLATE_MISSING, tid);
      }
      return var;
   }

   @Transactional
   public IPSAssemblyTemplate findTemplate(IPSGuid tid)
   {
      if (tid == null)
      {
         throw new IllegalArgumentException("tid may not be null");
      }

      return findTemplate(tid, true);
   }

   /**
    * Gets a Template from the repository.
    *
    * @param id the ID of the template, assumed not <code>null</code>.
    * @param loadSlots if <code>true</code> loading all associated slots;
    *           otherwise don't load the slots.
    *
    * @return the Template. It may be <code>null</code> if the Template does not
    *         exist.
    */
   @Transactional
   public PSAssemblyTemplate findTemplate(IPSGuid id, boolean loadSlots)
   {
      var session = entityManager.unwrap(Session.class);

         var var = session.get(PSAssemblyTemplate.class, id.longValue());

         if (var == null)
         {
            // Try masked id to address issues with legacy ids
            var nid = new PSGuid(id.getHostId(), PSTypeEnum.INTERNAL, id.getUUID());
            var = session.get(PSAssemblyTemplate.class, nid.longValue());
         }

         if (var == null)
         {
            return null;
         }

         if (loadSlots)
         {
            forceSlotLoad(var);
         }

         return var;

   }

   public IPSAssemblyTemplate loadUnmodifiableTemplate(String tidstr) throws PSAssemblyException
   {
      var guid = new PSGuid(PSTypeEnum.TEMPLATE, tidstr);
      return loadUnmodifiableTemplate(guid);
   }

   /**
    * Force the related template slots to be loaded for all supplied templates.
    *
    * @param templates the templates, assumed not <code>null</code>.
    */
   private void forceSlotLoad(List<IPSAssemblyTemplate> templates)
   {
      for (var template : templates)
         forceSlotLoad(template);
   }

   /**
    * Force the related template slots to be loaded for the passed template.
    *
    * @param template the template, assumed never <code>null</code>
    */
   private void forceSlotLoad(IPSAssemblyTemplate template)
   {
      try
      {
         // Force slot loading
         for (var slot : template.getSlots())
         {
            slot.getGUID();
         }
      }
      catch (RuntimeException re)
      {
         log.error("Couldn't force load one or more slots for template: {}, Error: {}",
                  template.getName(), re.getMessage());
         log.debug(re.getMessage(),re);
      }
   }

   @Transactional
   public void saveTemplate(IPSAssemblyTemplate var) throws PSAssemblyException
   {
      var session = entityManager.unwrap(Session.class);
      try
      {
         // ideally, we need to make sure the saved object is not in the
         // "memory" region of EHcache. However, this strategy does not work
         // due to the following scenario:
         // EHcache may contain the objects that are retrieved from the 1st
         // level cache of hibernate, and loadSlotModifiable() may also retrieve
         // the objects from the 1st level cache.
         //
         // so we arse not check the saved object against the object stored in
         // "memory" region of EHcache, which is the same way in 6.5.2.
         //
         // Package install (loadByType → fromXML → saveTemplate) assigns archive
         // GUIDs while @Version stays null (suppressed from design XML). Hibernate
         // 6/7 then rejects merge with "Detached entity with generated id has an
         // uninitialized version value 'null'" on PSTemplateBinding.VERSION /
         // PSAssemblyTemplate.version — perc.Baseline / FileAsset / image packages
         // fail and PSStartupPkgInstaller enters maintenance mode (#2540 / H2 qa-up).
         ensureOptimisticLockVersions(var);
         session.merge(var);
         session.flush();

         // the object will be evicted by the framework,
         // see PSEhCacheAccessor.notifyEvent()
      }

      catch (Exception e)
      {
         log.error("Failed to save template id={}, name={}. Error: {}",
                 var.getGUID(),
                 var.getName(),
                 PSExceptionUtils.getMessageForLog(e));
         throw new PSAssemblyException(AssemblyErrorCodes.UNKNOWN_CRUD_ERROR, e);
      }

   }

   /**
    * Ensure every binding has an application-assigned id before merge/persist.
    *
    * <p>{@link PSTemplateBinding} uses assigned ids (no {@code @GeneratedValue}) so
    * package import can merge with null {@code @Version}. Create paths that build
    * bindings without ids get a GUID here. Package-private for unit tests.
    *
    * @param template template being saved; may be {@code null} (no-op)
    */
   static void ensureOptimisticLockVersions(IPSAssemblyTemplate template) {
      if (!(template instanceof PSAssemblyTemplate assemblyTemplate)) {
         return;
      }
      // Leave template/binding @Version null when unset so Hibernate merge treats
      // the graph as insert (non-null version + missing row → StaleObjectState).
      var bindings = assemblyTemplate.getBindings();
      if (bindings == null) {
         return;
      }
      IPSGuidManager gmgr = null;
      for (var binding : bindings) {
         if (binding == null) {
            continue;
         }
         if (binding.getBindingId() == 0L) {
            if (gmgr == null) {
               gmgr = PSGuidManagerLocator.getGuidMgr();
            }
            // INTERNAL next-number block (historical PSGuidHibernateGenerator default).
            binding.setBindingId(gmgr.createGuid(PSTypeEnum.INTERNAL).longValue());
         }
      }
   }




   @Transactional
   public PSAssemblyTemplate findTemplateByName(String name) throws PSAssemblyException
   {
      if (name == null || StringUtils.isBlank(name))
      {
         throw new IllegalArgumentException("name may not be null or empty");
      }
      var session = entityManager.unwrap(Session.class);
      var template = session.bySimpleNaturalId(PSAssemblyTemplate.class).load(name);
      if (template == null)
      {
         throw new PSAssemblyException(AssemblyErrorCodes.TEMPLATE_MISSING, name);
      }
      return template;
   }

   @Transactional
   public IPSAssemblyTemplate findTemplateByNameAndType(String name, IPSGuid contenttype) throws PSAssemblyException
   {

      var template = findTemplateByName(name);

      var templateTypes = findTemplatesByContentType(contenttype);

      for(var t : templateTypes){
         if(t.getName().equalsIgnoreCase(template.getName())){
            return template;
         }
      }

      throw new PSAssemblyException(AssemblyErrorCodes.TEMPLATE_BY_ID_MISSING, name, contenttype.longValue());
   }

   @Transactional
   public List<IPSAssemblyTemplate> findTemplatesByAssemblyUrl(String url, boolean loadSlot)
   {
      var session = entityManager.unwrap(Session.class);
      CriteriaBuilder builder = session.getCriteriaBuilder();
      CriteriaQuery<PSAssemblyTemplate> criteria = builder.createQuery(PSAssemblyTemplate.class);
      Root<PSAssemblyTemplate> root = criteria.from(PSAssemblyTemplate.class);
      criteria.where(builder.like(root.get("assemblyUrl"), url));

      TypedQuery<PSAssemblyTemplate> query = session.createQuery(criteria);
      query.setHint("org.hibernate.cacheable", true);
      query.setHint("org.hibernate.cacheRegion", CACHE_REGION);

      List<PSAssemblyTemplate> templatesList = query.getResultList();
      List<IPSAssemblyTemplate> templates = new ArrayList<>(templatesList);

      if (loadSlot)
      {
         forceSlotLoad(templates);
      }

      return templates;
   }

   @Transactional
   public List<IPSAssemblyTemplate> findTemplatesBySlot(IPSTemplateSlot slot) throws PSAssemblyException
   {
      var session = entityManager.unwrap(Session.class);
      CriteriaBuilder builder = session.getCriteriaBuilder();
      CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
      Root<PSAssemblyTemplate> root = criteria.from(PSAssemblyTemplate.class);
      root.join("slots");
      criteria.select(root.get("id")).where(builder.equal(root.get("slots").get("id"), slot.getGUID().longValue()));

      TypedQuery<Long> query = session.createQuery(criteria);
      query.setHint("org.hibernate.cacheable", true);
      query.setHint("org.hibernate.cacheRegion", CACHE_REGION);

      List<Long> templateIds = query.getResultList();
      List<IPSAssemblyTemplate> rval = new ArrayList<>();
      for (var l : templateIds)
      {
         var templateid = new PSGuid(PSTypeEnum.TEMPLATE, l);
         var templ = loadTemplate(templateid, true);
         rval.add(templ);
      }

      return rval;
   }

   @Transactional
   public List<IPSAssemblyTemplate> findTemplates(String name, String contentType,
         Set<IPSAssemblyTemplate.OutputFormat> outputFormats, IPSAssemblyTemplate.TemplateType type,
         Boolean globalFilter, Boolean legacyFilter, String assembler) throws PSAssemblyException
   {
      var session = entityManager.unwrap(Session.class);
      try
      {
         CriteriaBuilder builder = session.getCriteriaBuilder();
         CriteriaQuery<PSAssemblyTemplate> criteria = builder.createQuery(PSAssemblyTemplate.class);
         Root<PSAssemblyTemplate> root = criteria.from(PSAssemblyTemplate.class);
         List<Predicate> predicates = new ArrayList<>();

         // get all templates if no name was specified
         String searchName = (StringUtils.isBlank(name)) ? "%" : name;
         predicates.add(builder.like(builder.lower(root.get("name")), searchName.toLowerCase()));

         if (outputFormats != null && !outputFormats.isEmpty())
         {
            List<Integer> ordinals = outputFormats.stream().map(Enum::ordinal).collect(Collectors.toList());
            predicates.add(root.get("outputFormat").in(ordinals));
         }
         if (type != null)
            predicates.add(builder.equal(root.get("templateType"), type.ordinal()));
         if (!StringUtils.isBlank(assembler))
            predicates.add(builder.like(root.get("assembler"), assembler));
         if (globalFilter != null)
         {
            if (globalFilter)
               predicates.add(builder.equal(root.get("outputFormat"), OutputFormat.Global.ordinal()));
            else
               predicates.add(builder.notEqual(root.get("outputFormat"), OutputFormat.Global.ordinal()));
         }
         if (legacyFilter != null)
         {
            if (legacyFilter)
            {
               predicates.add(builder.or(builder.equal(root.get("assembler"), IPSExtension.LEGACY_ASSEMBLER),
                     builder.isNull(root.get("assembler"))));
            }
            else
            {
               predicates.add(builder.notEqual(root.get("assembler"), IPSExtension.LEGACY_ASSEMBLER));
            }
         }

         criteria.where(predicates.toArray(new Predicate[0])).orderBy(builder.asc(root.get("name")));

         TypedQuery<PSAssemblyTemplate> query = session.createQuery(criteria);
         query.setHint("org.hibernate.cacheable", true);
         query.setHint("org.hibernate.cacheRegion", CACHE_REGION);

         Set<IPSAssemblyTemplate> templates = query.getResultList().stream().collect(Collectors.toSet());
         Set<IPSAssemblyTemplate> cttemplates = new HashSet<>();

         if (!StringUtils.isBlank(contentType) && !contentType.equals("%"))
         {
            var cmgr = PSContentMgrLocator.getContentMgr();
            var defs = cmgr.findNodeDefinitionsByName(contentType);
            for (var def : defs)
               cttemplates.addAll(findTemplatesByContentType(def.getGUID()));

            templates.retainAll(cttemplates);
         }

         var resultList = new ArrayList<>(templates);
         forceSlotLoad(resultList);

         return resultList;
      }
      catch (RepositoryException e)
      {
         throw new PSAssemblyException(AssemblyErrorCodes.UNKNOWN_ERROR, e, e.getMessage());
      }

   }

   @Transactional
   public Set<IPSAssemblyTemplate> findAllTemplates()
   {
      List<PSAssemblyTemplate> list=null;
      var session = entityManager.unwrap(Session.class) ;
         var builder = session.getCriteriaBuilder();
         var criteria = builder.createQuery(PSAssemblyTemplate.class);
         var root = criteria.from(PSAssemblyTemplate.class);
         criteria.select(root);

         Query query = session.createQuery(criteria);
         list = query.getResultList();

      return list == null ? Collections.emptySet() : new HashSet<>(new ArrayList<IPSAssemblyTemplate>(list));
   }

   /**
    * Find all the slots in the database.
    *
    * @return a set of slots, could be empty but never <code>null</code>
    */
   private List<IPSTemplateSlot> findAllSlots()
   {
      var session = entityManager.unwrap(Session.class);
      CriteriaBuilder builder = session.getCriteriaBuilder();
      CriteriaQuery<PSTemplateSlot> criteria = builder.createQuery(PSTemplateSlot.class);
      Root<PSTemplateSlot> root = criteria.from(PSTemplateSlot.class);
      criteria.select(root);
      List<PSTemplateSlot> slots = session.createQuery(criteria).getResultList();
      return new ArrayList<>(slots);

   }

   @Transactional
   public Set<IPSAssemblyTemplate> findAllGlobalTemplates()
   {
      var session = entityManager.unwrap(Session.class);
      CriteriaBuilder builder = session.getCriteriaBuilder();
      CriteriaQuery<PSAssemblyTemplate> criteria = builder.createQuery(PSAssemblyTemplate.class);
      Root<PSAssemblyTemplate> root = criteria.from(PSAssemblyTemplate.class);
      criteria.where(builder.equal(root.get("outputFormat"), OutputFormat.Global.ordinal()));

      Set<IPSAssemblyTemplate> rval = session.createQuery(criteria).getResultList().stream().collect(Collectors.toSet());
      for (var template : rval)
      {
         forceSlotLoad(template);
      }

      return rval;
   }

   // see base
   public Set<String> findAll57GlobalTemplates() throws PSAssemblyException
   {
      final var templateNames = new HashSet<String>();
      final var stylesheets = getGlobalTemplatesDir().listFiles(getXslFileFilter());
      if (stylesheets == null)
      {
         return templateNames;
      }
      try
      {
         for (final var stylesheet : stylesheets)
         {
            if (stylesheetContainsRootTemplate(stylesheet))
            {
               templateNames.add(extractXslFileName(stylesheet));
            }
         }
      }
      catch (IOException | SAXException e)
      {
         throw new PSAssemblyException(EXCEPTION_MSG, e);
      }
      return templateNames;
   }

   /**
    * Extracts the base XSL file name from the provided stylesheet.
    *
    * @param stylesheet the stylesheet file to extract file name from. Assumed
    *           not <code>null</code> and that it ends with
    *           {@link #XSL_EXTENSION}.
    * @return the XSL file name without extension.
    */
   private String extractXslFileName(final File stylesheet)
   {
      var name = stylesheet.getName();
      name = name.substring(0, name.indexOf(XSL_EXTENSION));
      return name;
   }

   /**
    * Reads the provided stylesheet and checks that the stylesheet template name
    * corresponds to the provided name.
    *
    * @param stylesheet the stylesheet file. Assumed not <code>null</code> and
    *           that it ends with {@link #XSL_EXTENSION}.

    * @throws IOException
    * @throws SAXException
    */
   private boolean stylesheetContainsRootTemplate(File stylesheet) throws IOException, SAXException
   {
      final var name = extractXslFileName(stylesheet);
      boolean foundRootTemplate = false;
      try (var is = new FileInputStream(stylesheet)) {

         var doc = PSXmlDocumentBuilder.createXmlDocument(is, false);

         var templates = doc.getElementsByTagName("xsl:template");
         for (int j = 0; j < templates.getLength() && !foundRootTemplate; j++) {
            var template = (Element) templates.item(j);
            var templateName = template.getAttribute(NAME_ATTR);
            int pos = templateName.indexOf(ROOT_EXTENSION);
            if (pos != -1) {
               foundRootTemplate = name.equals(templateName.substring(0, pos));
            }
         }

      }
      return foundRootTemplate;
   }

   @Transactional
   public void deleteTemplate(IPSGuid id) throws PSAssemblyException
   {
      var session = entityManager.unwrap(Session.class);

      try
      {
         var template = loadTemplate(id, false);
         session.remove(template);
         // The saved object will be (indirectly) evicted by the framework
      }
      catch (DataAccessException e)
      {
         throw new PSAssemblyException(AssemblyErrorCodes.UNKNOWN_CRUD_ERROR, e);
      }
   }

   @Transactional()
   public IPSTemplateSlot createSlot()
   {
      var gmgr = PSGuidManagerLocator.getGuidMgr();
      var newslot = new PSTemplateSlot();
      newslot.setGUID(gmgr.createGuid(PSTypeEnum.SLOT));
      return newslot;
   }

   @Transactional
   public IPSTemplateSlot loadSlot(String idstr) throws PSAssemblyException {
      return loadSlot(new PSGuid(PSTypeEnum.SLOT, idstr));
   }

   @Transactional
   public IPSTemplateSlot loadSlot(IPSGuid id) throws PSAssemblyException {
      var slot = findSlot(id);
      if (slot == null)
      {
         throw new PSAssemblyException(AssemblyErrorCodes.MISSING_SLOT);
      }

      return slot;
   }

   @Transactional
   public IPSTemplateSlot loadSlotModifiable(IPSGuid id) throws PSAssemblyException {
      var slot = getSlotById(id);
      if (slot == null)
      {
         throw new PSAssemblyException(AssemblyErrorCodes.MISSING_SLOT, id);
      }

      return slot;
   }

   @Transactional
   public IPSTemplateSlot findSlot(IPSGuid id)
   {
      if (id == null)
      {
         throw new IllegalArgumentException("id may not be null");
      }

      return getSlotById(id);
   }

   /**
    * Gets the slot from the repository.
    *
    * @param id the ID of the requested slot, assumed not <code>null</code>.
    * @return the slot object, which may be <code>null</code> if the slot does
    *         not exist.
    */
   @Transactional
   public IPSTemplateSlot getSlotById(IPSGuid id)
   {
      var session = entityManager.unwrap(Session.class);
      return session.get(PSTemplateSlot.class, id.longValue());
   }

   @Transactional
   public List<IPSTemplateSlot> loadSlots(List<IPSGuid> ids) throws PSAssemblyException
   {
      if (PSGuidUtils.isBlank(ids))
         throw new IllegalArgumentException("ids cannot be null or empty");

      return ids.stream().map(g -> {
         try {
            return loadSlot(g);
         } catch (PSAssemblyException e) {
            log.error("Failed to load slot for guid: {}. Error: {}", g, PSExceptionUtils.getMessageForLog(e));
            return null;
         }
      }).filter(Objects::nonNull).collect(Collectors.toList());
   }

   @Transactional
   public void saveSlot(IPSTemplateSlot slot) throws PSAssemblyException
   {
      var session = entityManager.unwrap(Session.class);
      try
      {
         session.merge( slot );
      }
      catch (Exception e)
      {
         log.error("Failed to save slot id={}, name={}. Error: {}",
                 slot.getGUID(),
                 slot.getName(),
                 PSExceptionUtils.getMessageForLog(e));
         throw new PSAssemblyException(AssemblyErrorCodes.UNKNOWN_CRUD_ERROR, e);
      }
      finally
      {
         session.flush();

      }
   }

   @Transactional
   public IPSTemplateSlot findSlotByName(String name) throws PSAssemblyException
   {
      var session = entityManager.unwrap(Session.class);
      var slot = session.bySimpleNaturalId(PSTemplateSlot.class).load(name);

      if (slot == null)
      {
         throw new PSAssemblyException(AssemblyErrorCodes.MISSING_SLOT, name);
      }
      return slot;

   }

   @Transactional
   public List<IPSTemplateSlot> findSlotsByName(String name)
   {
      var session = entityManager.unwrap(Session.class);
      // get all slots if no name was specified
      if (StringUtils.isBlank(name) || name.equals("%"))
         return findAllSlots();

      if (!name.contains("%"))
      {
         try {
            var slot = findSlotByName(name);
            return slot == null ? Collections.emptyList() : Collections.singletonList(slot);
         } catch (PSAssemblyException e) {
            return Collections.emptyList();
         }
      }

      CriteriaBuilder builder = session.getCriteriaBuilder();
      CriteriaQuery<PSTemplateSlot> criteria = builder.createQuery(PSTemplateSlot.class);
      Root<PSTemplateSlot> root = criteria.from(PSTemplateSlot.class);
      criteria.where(builder.like(builder.lower(root.get("name")), name.toLowerCase()));
      criteria.orderBy(builder.asc(root.get("name")));

      TypedQuery<PSTemplateSlot> query = session.createQuery(criteria);
      query.setHint("org.hibernate.cacheable", true);
      query.setHint("org.hibernate.cacheRegion", CACHE_REGION);

      List<PSTemplateSlot> slots = query.getResultList();
      return slots.stream().map(s -> (IPSTemplateSlot) s).collect(Collectors.toList());
   }

   @Transactional
   public List<IPSTemplateSlot> findSlotsByNames(List<String> names)
   {
      var session = entityManager.unwrap(Session.class);

      if (names == null)
      {
         throw new IllegalArgumentException("names may not be null");
      }

      return names.stream().map(name -> {
                 try {
                    return findSlotByName(name);
                 } catch (PSAssemblyException e) {
                    return null;
                 }
              })
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
   }

   @Transactional
   public void deleteSlot(IPSGuid id) throws PSAssemblyException
   {
      try
      {
         var session = entityManager.unwrap(Session.class);
         var slot = loadSlot(id);
         session.remove(slot);
      }
      catch (DataAccessException e)
      {
         throw new PSAssemblyException(AssemblyErrorCodes.UNKNOWN_CRUD_ERROR, e);
      }
   }

   public IPSSlotContentFinder loadFinder(String finder) throws PSAssemblyException
   {
      if (StringUtils.isBlank(finder))
      {
         throw new IllegalArgumentException("finder may not be null or empty");
      }

      var emgr = PSServer.getExtensionManager(null);
      try
      {
         var ref = new PSExtensionRef(finder);
         return (IPSSlotContentFinder) emgr.prepareExtension(ref, null);
      }
      catch (PSExtensionException | com.percussion.error.PSNotFoundException e)
      {
         throw new PSAssemblyException(AssemblyErrorCodes.MISSING_FINDER, e);
      }
   }

   @Override
   public IPSContentFinder loadContentFinder(String finder) throws PSAssemblyException
   {
      // Delegate to slot content finder loader; IPSSlotContentFinder implements IPSContentFinder
      return (IPSContentFinder) loadFinder(finder);
   }

   @Override
   public List<IPSAssemblyTemplate> findTemplatesByContentType(IPSGuid contenttype) throws PSAssemblyException
   {
      if (contenttype == null)
         throw new IllegalArgumentException("contenttype may not be null");
      try
      {

         var ids = (List<Long>) entityManager.createNamedQuery("template.findByType").setParameter("ctype", contenttype.longValue()).getResultList();
         List<IPSAssemblyTemplate> results = new ArrayList<>();
         for (Long id : ids)
         {
            PSAssemblyTemplate t = entityManager.find(PSAssemblyTemplate.class, id);
            if (t != null)
               results.add(t);
         }
         return results;
      }
      catch (Exception e)
      {
         throw new PSAssemblyException(AssemblyErrorCodes.UNKNOWN_CRUD_ERROR, e);
      }
   }

   @Transactional
   public String getLandingPageLink(IPSAssemblyItem parentItem, Node landingPage, IPSGuid templateId)
         throws PSAssemblyException
   {
      // Load the template and decide what to do
      var template = loadUnmodifiableTemplate(templateId);
      String url = null;
      var sws = PSStopwatchStack.getStack();
      sws.start("getLandingPage");
      try
      {
         var processor = PSRelationshipProcessor.getInstance();
         var cn = (IPSNode) landingPage;
         var lg = (PSLegacyGuid) cn.getGuid();
         Property fidprop = null;
         Property sidprop = null;
         // get folderid and siteid properties
         try
         {
            fidprop = landingPage.getProperty(PSNavHelper.PROP_NAV_LANDINPAGE_FOLDERID);
         }
         catch (Exception e)
         {
            // Not having folder id property is not an error.
            // Ignore it.
         }
         try
         {
            sidprop = landingPage.getProperty(PSNavHelper.PROP_NAV_LANDINPAGE_SITEID);
         }
         catch (Exception e)
         {
            // Not having siteid property is not an error.
            // Ignore it.
         }
         var fid = fidprop == null ? "" : StringUtils.defaultString(fidprop.getString());
         var sid = sidprop == null ? "" : StringUtils.defaultString(sidprop.getString());
         if (StringUtils.isBlank(sid) && parentItem.getSiteId() != null)
         {
            sid = String.valueOf(parentItem.getSiteId().longValue());
         }
         if (StringUtils.isBlank(fid))
         {
            fid = String.valueOf(parentItem.getFolderId());
         }
         if (StringUtils.isBlank(fid) && StringUtils.isNotBlank(sid))
         {
            var smgr = PSSiteManagerLocator.getSiteManager();
            var siteguid = PSGuidManagerLocator.getGuidMgr().makeGuid(sid, PSTypeEnum.SITE);
            var fguid = smgr.getSiteFolderId(siteguid, lg);
            if (fguid != null)
            {
               var flg = (PSLegacyGuid) fguid;
               fid = Integer.toString(flg.getContentId());
            }
         }

         var folders = processor.getParents(PSRelationshipConfig.TYPE_FOLDER_CONTENT, lg.getLocator());

         if (StringUtils.isBlank(fid) && (folders == null || folders.isEmpty()))
         {
            throw new IllegalStateException("Navon must be contained in a folder: " + lg);
         }

         var clone = (IPSAssemblyResult) parentItem.clone();

         // Use the landing page template and node
         clone.setNode(landingPage);
         clone.setTemplate(null);
         clone.setParameterValue(IPSHtmlParameters.SYS_TEMPLATE, Long.toString(templateId.longValue()));
         clone.setParameterValue(IPSHtmlParameters.SYS_CONTENTID, Integer.toString(lg.getContentId()));
         clone.setParameterValue(IPSHtmlParameters.SYS_REVISION, Integer.toString(lg.getRevision()));
         if (StringUtils.isBlank(fid))
         {
            fid = Integer.toString(folders.get(0).getId());
         }
         clone.setParameterValue(IPSHtmlParameters.SYS_FOLDERID, fid);
         if (StringUtils.isNotBlank(sid))
         {
            clone.setParameterValue(IPSHtmlParameters.SYS_SITEID, sid);
            clone.setSiteId(PSGuidManagerLocator.getGuidMgr().makeGuid(sid, PSTypeEnum.SITE));
         }

         clone.setTemplate(template);

         if (StringUtils.isBlank(template.getAssembler())
               || template.getAssembler().equals(IPSExtension.LEGACY_ASSEMBLER))
         {
            // Assemble snippet
            var items = new ArrayList<IPSAssemblyItem>();
            items.add(clone);
            var results = assemble(items);
            if (results == null || results.isEmpty())
            {
               throw new PSAssemblyException(AssemblyErrorCodes.LANDING_PAGE_URL_1, lg);
            }
            else
            {
               var result = results.get(0);
               var doc = result.toResultString();
               try(Reader r = new StringReader(doc)) {
                  var fact = PSSecureXMLUtils.getSecuredXMLInputFactory(
                          new PSXmlSecurityOptions(
                                  true,
                                  true,
                                  true,
                                  false,
                                  true,
                                  false
                          )
                  );

                  var reader = fact.createXMLEventReader(r);
                  while (reader.hasNext()) {
                     var event = reader.nextEvent();
                     if (event.isStartElement()) {
                        var e = event.asStartElement();
                        if (e.getName().getLocalPart().equalsIgnoreCase("a")) {
                           // Get href, and we're done
                           var enc = new PSXmlEncoder();
                           var a = e.getAttributeByName(new QName("href"));
                           return (String) enc.encode(a.getValue());
                        }
                     }
                  }
               }
            }
         }
         else
         {
            var eval = setupItemForAssembly(clone, false);
            processBindings(clone, eval);
            // Extract the information we need
            url = (String) eval.evaluate(ms_pagelink);
            if (url == null)
            {
               throw new PSAssemblyException(AssemblyErrorCodes.MISSING_PAGELINK, templateId);
            }
         }
      }
      catch (PSAssemblyException e)
      {
         throw e;
      }
      catch (Exception e)
      {
         log.error("Problem extracting URL from landing page snippet", e);
      }
      finally
      {
         sws.stop();
      }

      return url;
   }


   public void setCurrentAssemblyItem(IPSAssemblyItem item)
   {
      if (item == null)
         ms_item.remove();
      else
         ms_item.set(item);
   }

   public IPSAssemblyItem getCurrentAssemblyItem()
   {
      return ms_item.get();
   }

   /**
    * @return the configurationBean
    */
   public PSServiceConfigurationBean getConfigurationBean()
   {
      return m_configurationBean;
   }

   /**
    * @param configurationBean the configurationBean to set
    */
   @Autowired
   public void setConfigurationBean(PSServiceConfigurationBean configurationBean)
   {
      m_configurationBean = configurationBean;
   }

   /**
    * @return the nsvc
    */
   public IPSNotificationService getNotificationService()
   {
      return m_nsvc;
   }

   /**
    * @param nsvc the nsvc to set
    */
   @Autowired
   public void setNotificationService(IPSNotificationService nsvc)
   {
      m_nsvc = nsvc;

      if (m_nsvc != null)
      {
         IPSNotificationListener listener = new AssemblyContentChangedListener();
         m_nsvc.addListener(EventType.CONTENT_CHANGED, listener);
         m_nsvc.addListener(EventType.OBJECT_INVALIDATION, listener);
         // Listener for the maps
         nsvc.addListener(EventType.OBJECT_INVALIDATION, new PSAssemblyNotificationListener());
      }
   }

   /**
    * Spring property accessor.
    *
    * @return get the cache service
    */
   public IPSCacheAccess getCache()
   {
      return m_cache;
   }

   /**
    * Set the cache service.
    *
    * @param cache the service, never <code>null</code>
    */
   @Autowired
   public void setCache(IPSCacheAccess cache)
   {
      if (cache == null)
      {
         throw new IllegalArgumentException("cache may not be null");
      }
      m_cache = cache;
   }

   /*
    * (non-Javadoc)
    *
    * @see
    * com.percussion.services.assembly.IPSTemplateService#createBindings(java
    * .util.LinkedHashMap)
    */
   public List<PSTemplateBinding> createBindings(LinkedHashMap<String, String> bindings, int startingOrder)
   {
      if (bindings == null)
         throw new IllegalArgumentException("bindings must not be null");

      return bindings.entrySet().stream()
              .peek(entry -> {
                 if (StringUtils.isBlank(entry.getKey()))
                    throw new IllegalArgumentException("the key of the bindings map must not be blank.");
              })
              .map(entry -> new PSTemplateBinding(entry.getKey(), entry.getValue()))
              .collect(Collectors.toList());
   }

   /**
    * Creates a filter filtering XSL files.
    *
    * @return the XSL files filter. Never <code>null</code>.
    */
   private FileFilter getXslFileFilter()
   {
      return pathname -> pathname.getPath().toLowerCase().endsWith(XSL_EXTENSION);
   }

   /**
    * Directory where legacy 5.7 global templates reside.
    *
    * @return the global templates directory. Never <code>null</code>.
    */
   private File getGlobalTemplatesDir()
   {
      return new File(PSRxGlobals.ABS_GLOBAL_TEMPLATES_PATH);
   }

   /**
    * The error number indicating to <code>PSException</code> to use the message
    * from the provided exception.
    */
   private static final int EXCEPTION_MSG = 1002;

   /**
    * The file extension used for XSL files.
    */
   private static final String XSL_EXTENSION = ".xsl";

   /**
    * The extension appended to the file name for the root template name.
    */
   private static final String ROOT_EXTENSION = "_root";

   // private XML constants
   private static final String NAME_ATTR = "name";

}
