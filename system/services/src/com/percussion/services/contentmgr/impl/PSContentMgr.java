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
package com.percussion.services.contentmgr.impl;

import antlr.ANTLRException;
import antlr.CharScanner;
import com.percussion.cms.IPSConstants;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.IPSBackEndMapping;
import com.percussion.design.objectstore.PSBackEndColumn;
import com.percussion.design.objectstore.PSField;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.IPSNodeDefinition;
import com.percussion.services.contentmgr.PSContentMgrConfig;
import com.percussion.services.contentmgr.data.PSContentTemplateDesc;
import com.percussion.services.contentmgr.data.PSContentTypeWorkflow;
import com.percussion.services.contentmgr.data.PSNodeDefinition;
import com.percussion.services.contentmgr.data.PSQuery;
import com.percussion.services.contentmgr.impl.query.SqlLexer;
import com.percussion.services.contentmgr.impl.query.SqlParser;
import com.percussion.services.contentmgr.impl.query.XpathLexer;
import com.percussion.services.contentmgr.impl.query.XpathParser;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.util.PSSqlHelper;
import com.percussion.utils.guid.IPSGuid;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.Reader;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of the content manager
 *
 * @author dougrand
 */
@Transactional(propagation = Propagation.REQUIRED)
public class PSContentMgr  implements IPSContentMgr
{

   @PersistenceContext
   private EntityManager entityManager;

   private org.hibernate.Session getSession(){

      return entityManager.unwrap(org.hibernate.Session.class);

   }


   /**
    * The logger for the content manager
    */
    private static final Logger ms_log = LogManager.getLogger(IPSConstants.CONTENTREPOSITORY_LOG);

   /**
    * The region that caches information for the content manager. Used for
    * hibernate's query cache.
    */
   static final String CACHE_REGION = "contentmanagerqueries";

   /**
    * The underlying content repository that implements the persistence layer
    */
   private IPSContentRepository m_repository = null;

   public NodeType getNodeType(@SuppressWarnings("unused") String arg0)
   {
      throw new UnsupportedOperationException("Not implemented");
   }

   public NodeTypeIterator getAllNodeTypes()
   {
      throw new UnsupportedOperationException("Not implemented");
   }

   public NodeTypeIterator getPrimaryNodeTypes()
   {
      throw new UnsupportedOperationException("Not implemented");
   }

   public NodeTypeIterator getMixinNodeTypes()
   {
      throw new UnsupportedOperationException("Not implemented");
   }

   @Override
   public boolean hasNodeType(String nodeTypeName) throws RepositoryException
   {
      throw new UnsupportedRepositoryOperationException(
            "Node type manager hasNodeType is not implemented");
   }

   @Override
   public NodeTypeTemplate createNodeTypeTemplate()
         throws UnsupportedRepositoryOperationException, RepositoryException
   {
      throw new UnsupportedRepositoryOperationException(
            "Node type templates are not supported");
   }

   @Override
   public NodeTypeTemplate createNodeTypeTemplate(NodeTypeDefinition ntd)
         throws UnsupportedRepositoryOperationException, RepositoryException
   {
      throw new UnsupportedRepositoryOperationException(
            "Node type templates are not supported");
   }

   @Override
   public NodeDefinitionTemplate createNodeDefinitionTemplate()
         throws UnsupportedRepositoryOperationException, RepositoryException
   {
      throw new UnsupportedRepositoryOperationException(
            "Node definition templates are not supported");
   }

   @Override
   public PropertyDefinitionTemplate createPropertyDefinitionTemplate()
         throws UnsupportedRepositoryOperationException, RepositoryException
   {
      throw new UnsupportedRepositoryOperationException(
            "Property definition templates are not supported");
   }

   @Override
   public NodeType registerNodeType(NodeTypeDefinition ntd, boolean allowUpdate)
         throws InvalidNodeTypeDefinitionException, NodeTypeExistsException,
         UnsupportedRepositoryOperationException, RepositoryException
   {
      throw new UnsupportedRepositoryOperationException(
            "Dynamic node type registration is not supported");
   }

   @Override
   public NodeTypeIterator registerNodeTypes(NodeTypeDefinition[] ntds, boolean allowUpdate)
         throws InvalidNodeTypeDefinitionException, NodeTypeExistsException,
         UnsupportedRepositoryOperationException, RepositoryException
   {
      throw new UnsupportedRepositoryOperationException(
            "Dynamic node type registration is not supported");
   }

   @Override
   public void unregisterNodeType(String name)
         throws UnsupportedRepositoryOperationException, NoSuchNodeTypeException,
         RepositoryException
   {
      throw new UnsupportedRepositoryOperationException(
            "Dynamic node type unregistration is not supported");
   }

   @Override
   public void unregisterNodeTypes(String[] names)
         throws UnsupportedRepositoryOperationException, NoSuchNodeTypeException,
         RepositoryException
   {
      throw new UnsupportedRepositoryOperationException(
            "Dynamic node type unregistration is not supported");
   }

   /**
    * Set the repositories
    *
    * @param rep the repository, never <code>null</code>
    */
   @Transactional(propagation=Propagation.NOT_SUPPORTED)
   public void setRepository(IPSContentRepository rep)
   {
      if (rep == null)
      {
         throw new IllegalArgumentException("rep may not be null");
      }
      m_repository = rep;
   }

   @Transactional(propagation=Propagation.NOT_SUPPORTED)
   public List<Node> findItemsByPath(Session sess, List<String> paths,
         PSContentMgrConfig config) throws RepositoryException
   {
      return m_repository.loadByPath(paths, config);
   }

   @Transactional(propagation=Propagation.NOT_SUPPORTED)
   public List<Node> findItemsByGUID(List<IPSGuid> guids,
         PSContentMgrConfig config) throws RepositoryException
   {
      return m_repository.loadByGUID(guids, config);
   }

   @Transactional
   public IPSNodeDefinition createNodeDefinition()
   {
      PSNodeDefinition nodeDef = new PSNodeDefinition();
      IPSGuidManager gmgr = PSGuidManagerLocator.getGuidMgr();
      nodeDef.setGUID(gmgr.createGuid(PSTypeEnum.NODEDEF));
      return nodeDef;
   }

   public List<IPSNodeDefinition> loadNodeDefinitions(List<IPSGuid> typeids)
         throws RepositoryException
   {

      try
      {
         org.hibernate.Session session = getSession();
         //  Force initialize at the moment to prevent problems with callers not in session
         List<IPSNodeDefinition> defs =typeids.stream().map(tid -> session.get(PSNodeDefinition.class,tid.longValue())).filter(Objects::nonNull).map(nd -> {Hibernate.initialize(nd);return nd;})
                 .collect(Collectors.toList());

         if (defs.isEmpty())
         {
            throw new NoSuchNodeTypeException("Specified defs not found");
         }
         else
         {
            return defs;
         }
      }
      catch (Exception e)
      {
         throw new RepositoryException("Problem loading definitions", e);
      }
   }

   @Transactional
   public void saveNodeDefinitions(List<IPSNodeDefinition> defs)
         throws RepositoryException
   {
      try
      {
         org.hibernate.Session session = getSession();
         defs.forEach(def -> session.merge(def));
      }
      catch (Exception e)
      {
         throw new RepositoryException("Problem saving definitions", e);
      }
   }

   @Transactional
   public void deleteNodeDefinitions(List<IPSNodeDefinition> defs)
         throws RepositoryException
   {
      org.hibernate.Session s = getSession();
      try
      {
         for (IPSNodeDefinition def : defs)
         {
            PSNodeDefinition realDef = (PSNodeDefinition) def;
            Set<PSContentTemplateDesc> descSet = realDef.getCvDescriptors();
            if (descSet != null)
            {
               for (PSContentTemplateDesc desc : descSet)
               {
                  s.remove(desc);
               }
            }
            // Remove the object
            s.remove(def);
         }
         s.flush();
      }
      catch (Exception e)
      {
         throw new RepositoryException("Problem deleting definitions", e);
      }

   }

   /* (non-Javadoc)
    * @see com.percussion.services.contentmgr.IPSContentTypeMgr#findNodeDefinitionByName(java.lang.String)
    */
   public IPSNodeDefinition findNodeDefinitionByName(String name)
      throws RepositoryException
   {
      List<IPSNodeDefinition> defs = findNodeDefinitionsByName(name);

      if (defs.isEmpty())
      {
         throw new NoSuchNodeTypeException("Did not find " + name);
      }
      else if (defs.size() > 1)
      {
         throw new RepositoryException("Not a unique name - " + name);
      }
      else
      {
         return defs.get(0);
      }
   }


   public List<IPSNodeDefinition> findNodeDefinitionsByName(String name)
         throws RepositoryException
   {
      org.hibernate.Session s = getSession();
      try
      {
         name = PSContentUtils.internalizeName(name).toLowerCase();
         org.hibernate.query.Query<PSNodeDefinition> cquery = s.createQuery("from PSNodeDefinition where lower(m_name) like :name", PSNodeDefinition.class);
         cquery.setParameter("name", name);
         cquery.setCacheable(true);
         cquery.setCacheRegion(CACHE_REGION);
         List<IPSNodeDefinition> defs = cquery.list().stream().map(nd -> (IPSNodeDefinition) nd).collect(Collectors.toList());
         if (defs.size() == 0)
         {
            // Try again with spaces in case the internal name had spaces
            name = name.replace('_', ' ');
            cquery = s.createQuery("from PSNodeDefinition where lower(m_name) like :name", PSNodeDefinition.class);
            cquery.setParameter("name", name);
            cquery.setCacheable(true);
            cquery.setCacheRegion(CACHE_REGION);
            defs = cquery.list().stream().map(nd -> (IPSNodeDefinition) nd).collect(Collectors.toList());
         }

         // Make unique
         Set<IPSNodeDefinition> bdefs = new HashSet<>(defs);

         // Convert back to a list
         defs = new ArrayList<IPSNodeDefinition>(bdefs);

         return defs;
      }
      catch (Exception e)
      {
         throw new RepositoryException("Problem loading definitions", e);
      }

   }


   public List<IPSNodeDefinition> findAllItemNodeDefinitions()
         throws RepositoryException
   {
      org.hibernate.Session s = getSession();
      try
      {
         org.hibernate.query.Query<PSNodeDefinition> q = s.createQuery("from PSNodeDefinition where m_objectType = :otype", PSNodeDefinition.class)
                 .setParameter("otype", 1)
                 .setCacheable(true)
                 .setCacheRegion(CACHE_REGION);

         List<PSNodeDefinition> defs = q.list();
         //there may be an entry for every template association
         HashSet<IPSNodeDefinition> deduped = new HashSet<IPSNodeDefinition>(defs);
         return new ArrayList<>(deduped);
      }
      catch (Exception e)
      {
         throw new RepositoryException("Problem loading definitions", e);
      }

   }

   public PSContentTemplateDesc findContentTypeTemplateAssociation(
         IPSGuid tmpId, IPSGuid ctId) throws RepositoryException
   {
      if (tmpId == null)
         throw new IllegalArgumentException("tmpId may not be null");

      if (ctId == null)
         throw new IllegalArgumentException("Content Type id may not be null");

      org.hibernate.Session s = getSession();

         org.hibernate.query.Query<PSContentTemplateDesc> q = s.createQuery("from PSContentTemplateDesc where m_templateid = :tmp and m_contenttypeid = :ct", PSContentTemplateDesc.class)
                 .setParameter("tmp", tmpId.longValue())
                 .setParameter("ct", ctId.longValue())
                 .setCacheable(true)
                 .setCacheRegion(CACHE_REGION);
         return q.uniqueResult();



   }


   public List<PSContentTypeWorkflow> findContentTypeWorkflowAssociations(
         IPSGuid ctId) throws RepositoryException
   {
      if (ctId == null)
         throw new IllegalArgumentException("Content Type id may not be null");

      org.hibernate.Session s = getSession();

         org.hibernate.query.Query<PSContentTypeWorkflow> q = s.createQuery("from PSContentTypeWorkflow where m_contenttypeid = :ct", PSContentTypeWorkflow.class)
                 .setParameter("ct", ctId.longValue())
                 .setCacheable(true)
                 .setCacheRegion(CACHE_REGION);
         List<PSContentTypeWorkflow> ctwfs = q.list();
         return ctwfs;


   }


   public List<IPSNodeDefinition> findNodeDefinitionsByTemplate(
         IPSGuid templateid) throws RepositoryException
   {
      if (templateid == null)
      {
         throw new IllegalArgumentException("templateid may not be null");
      }

      org.hibernate.Session s = getSession();


         org.hibernate.query.Query<PSNodeDefinition> q = s.createQuery("select distinct nd from PSNodeDefinition nd join nd.m_cvDescriptors descriptor where descriptor.m_templateid = :templateid", PSNodeDefinition.class)
                 .setParameter("templateid", templateid.longValue())
                 .setCacheable(true)
                 .setCacheRegion(CACHE_REGION);
         List<IPSNodeDefinition> defs = q.list().stream().map(nd -> (IPSNodeDefinition) nd).collect(Collectors.toList());
         //there may be an entry for every template association
         Set<IPSNodeDefinition> deduped = new HashSet<>(defs);
         return new ArrayList<>(deduped);

   }


   public List<IPSNodeDefinition> findNodeDefinitionsByWorkflow(
         IPSGuid workflowid) throws RepositoryException
   {
      if (workflowid == null)
      {
         throw new IllegalArgumentException("workflowid may not be null");
      }

      org.hibernate.Session s = getSession();


         // Convert workflow id to int to match stored field
         int wfId = (int) workflowid.longValue();
         org.hibernate.query.Query<PSNodeDefinition> q = s.createQuery("select distinct nd from PSNodeDefinition nd join nd.m_ctWfRels ctwfrel where ctwfrel.m_workflowid = :workflowid", PSNodeDefinition.class)
                 .setParameter("workflowid", wfId)
                 .setCacheable(true)
                 .setCacheRegion(CACHE_REGION);
         List<IPSNodeDefinition> defs = q.list().stream().map(nd -> (IPSNodeDefinition) nd).collect(Collectors.toList());
         //there may be an entry for every workflow association
         Set<IPSNodeDefinition> deduped = new HashSet<>(defs);
         return new ArrayList<>(deduped);

   }

   public Collection<IPSGuid> findItemIdsByNodeDefinition(NodeDefinition def)
   {
      PSNodeDefinition psdef = (PSNodeDefinition) def;

      Collection<IPSGuid> guids = new ArrayList<>();
      String query = "select c.m_contentId, c.m_currRevision"
            + " from PSComponentSummary c where c.m_contentTypeId = :ctid";


      List<Object[]> results = getSession().createQuery(query).setParameter(
            "ctid", psdef.getRawContentType()).list();

      for (Object[] result : results)
      {
         guids.add(new PSLegacyGuid((Integer) result[0], (Integer) result[1]));
      }

      return guids;
   }

   public Query createQuery(String statement, String language)
         throws InvalidQueryException, RepositoryException
   {
      if (StringUtils.isBlank(statement))
      {
         throw new IllegalArgumentException(
               "statement may not be null or empty");
      }
      if (StringUtils.isBlank(language))
      {
         throw new IllegalArgumentException("language may not be null or empty");
      }
      Reader reader = new StringReader(statement);
      PSQuery q = null;
      CharScanner lexer = null;
      try
      {
         if (language.equals(Query.XPATH))
         {
            lexer = new XpathLexer(reader);
            XpathParser parser = new XpathParser(lexer);
            q = parser.start_rule();
         }
         else if (language.equals(Query.SQL))
         {
            lexer = new SqlLexer(reader);
            SqlParser parser = new SqlParser(lexer);
            q = parser.start_rule();

         }
         else
         {
            throw new InvalidQueryException("Language " + language
                  + " not recognized");
         }
         q.setStatement(statement);
      }
      catch (ANTLRException e)
      {
         String problem = "Encountered [" + e.getLocalizedMessage()
               + "] on line " + lexer.getLine() + " near character position "
               + lexer.getColumn() + " while parsing " + language + " query "
               + " original text: " + statement;
         throw new InvalidQueryException(problem);
      }

      return q;
   }

   /** (non-Javadoc)
    * @see javax.jcr.query.QueryManager#getQuery(javax.jcr.Node)
    */
   public Query getQuery(Node arg0) throws InvalidQueryException,
         RepositoryException
   {
      throw new UnsupportedRepositoryOperationException("Not yet implemented");
   }

   /** (non-Javadoc)
    * @see javax.jcr.query.QueryManager#getSupportedQueryLanguages()
    */
   public String[] getSupportedQueryLanguages() throws RepositoryException
   {
      return new String[]
      {Query.SQL, Query.XPATH};
   }

   @Override
   public javax.jcr.query.qom.QueryObjectModelFactory getQOMFactory()
   {
      throw new UnsupportedOperationException(
            "JCR Query Object Model (JQOM) is not supported by the CMS content manager");
   }

   /*
    * (non-Javadoc)
    *
    * @see com.percussion.services.contentmgr.IPSContentMgr#executeQuery(javax.jcr.query.Query,
    *      int, java.util.Map)
    */
   public QueryResult executeQuery(Query query, int maxresults,
         Map<String, ? extends Object> params) throws InvalidQueryException,
         RepositoryException
   {
      return executeQuery(query, maxresults, params, null);
   }

   /*
    * (non-Javadoc)
    *
    * @see com.percussion.services.contentmgr.IPSContentMgr#executeQuery(javax.jcr.query.Query,
    *      int, java.util.Map, java.lang.String)
    */
   @Transactional(propagation = Propagation.NOT_SUPPORTED)
   public QueryResult executeQuery(Query query, int maxresults,
         Map<String, ? extends Object> params, String locale)
         throws RepositoryException
   {
      return m_repository.executeInternalQuery(query, maxresults, params,
            locale);
   }

   /* (non-Javadoc)
    * @see com.percussion.services.contentmgr.IPSContentMgr#filterItemsByNodeDefinitions(java.util.Set, java.util.Collection)
    */

   public Collection<IPSGuid> filterItemsByNodeDefinitions(Set<IPSGuid> types,
         Collection<IPSGuid> ids)
   {
      if (types == null)
      {
         throw new IllegalArgumentException("types may not be null");
      }
      if (ids == null)
      {
         throw new IllegalArgumentException("ids may not be null");
      }
      // Build new collection
      Collection<IPSGuid> rval = new ArrayList<>();

      if (types.size() > 0 && ids.size() > 0)
      {
         String query = "select c.m_contentId, c.m_contentTypeId"
               + " from PSComponentSummary c where c.m_contentId in (:ids)";

         List<Integer> cids = new ArrayList<>();
         for (IPSGuid i : ids)
         {
            PSLegacyGuid lg = (PSLegacyGuid) i;
            cids.add(lg.getContentId());
         }
         List<Object[]> results = getSession().createQuery(query).setParameter(
               "ids", cids).list();
         // Build a map
         Map<Integer,IPSGuid> idToType = new HashMap<>();
         for(Object[] result : results)
         {
            Integer cid = (Integer) result[0];
            idToType.put(cid, new PSGuid(PSTypeEnum.NODEDEF, (Long) result[1]));
         }

         for (IPSGuid i : ids)
         {
            PSLegacyGuid lg = (PSLegacyGuid) i;
            IPSGuid ctype = idToType.get(lg.getContentId());
            if (types.contains(ctype))
            {
               rval.add(i);
            }
         }
      }

      return rval;
   }

   /*
    * (non-Javadoc)
    *
    * @see com.percussion.services.contentmgr.IPSContentMgr#findNodesByTitle(
    *      java.long.Long, java.lang.String)
    */

   public List<String> findNodesByTitle(Long contentTypeId, String title)
   throws RepositoryException
   {
      org.hibernate.Session s = getSession();
      List<String> contentIds = new ArrayList<>();
      try
      {
         String hql = "select c.m_contentId from PSComponentSummary c where c.m_contentTypeId = :ct and lower(c.m_name) like :title";
         org.hibernate.query.Query<Integer> q = s.createQuery(hql, Integer.class)
               .setParameter("ct", contentTypeId)
               .setParameter("title", title.toLowerCase());
         List<Integer> defs = q.list();
         for(Integer def : defs)
         {
            contentIds.add(def + "");
         }
         return contentIds;
      }
      catch (Exception e)
      {
         throw new RepositoryException("Problem loading definitions", e);
      }

   }

   @Transactional
   public Node copyItem(Node existing)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Transactional
   public Node createItem(NodeDefinition def)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Transactional
   public Node createItemRevision(Node existing)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Transactional
   public void deleteItems(List<IPSGuid> items) throws RepositoryException
   {
      // TODO Auto-generated method stub

   }

   @Transactional
   public void saveItems(List<Node> items, PSContentMgrConfig config) throws RepositoryException
   {
      // TODO Auto-generated method stub

   }

   /*
    * (non-Javadoc)
    *
    * @see com.percussion.services.contentmgr.IPSContentMgr#findItemsByLocalFieldValue(
    *      java.long.Long, java.lang.String, java.lang.String)
    */
   public List<Integer> findItemsByLocalFieldValue(long contentTypeId,
         String fieldName, String fieldValue)
   {
      List<Integer> contentIds = new ArrayList<>();
      PSItemDefManager itemDefMgr = PSItemDefManager.getInstance();
      long[] ctypeIds = new long[]{contentTypeId};
      Collection<PSField> fields = itemDefMgr.getFieldsByName(ctypeIds, fieldName);
      if(fields.isEmpty())
      {
         throw new RuntimeException("Invalid field");
      }
      PSField field = fields.iterator().next();
      IPSBackEndMapping loc = field.getLocator();
      if(!(loc instanceof PSBackEndColumn))
      {
         throw new RuntimeException("Invalid column");
      }
      PSBackEndColumn beColumn = (PSBackEndColumn)loc;
      String tableName = beColumn.getTable().getTable();
      String columnName = beColumn.getColumn();
      org.hibernate.Session sess = getSession();

      // Validate columnName to prevent SQL injection (CWE-89)
      // Note: columnName comes from PSBackEndColumn domain model
      // which is a trusted source, but we validate for defense-in-depth
      if (!isValidColumnName(columnName)) {
         throw new RuntimeException("Invalid column name: " + columnName);
      }

      String sql = null;
      try {
         sql = "SELECT DISTINCT c.CONTENTID FROM " +
                        PSSqlHelper.qualifyTableName("CONTENTSTATUS") + " c, " +
                        PSSqlHelper.qualifyTableName(tableName) +
                        " t WHERE c.CONTENTID=t.CONTENTID AND c.CURRENTREVISION=t.REVISIONID AND t." +
                        columnName + " = :fieldValue";
      } catch (SQLException e) {
         throw new RuntimeException(e);
      }
      org.hibernate.query.NativeQuery<?> query = sess.createNativeQuery(sql, Object.class);
      // Parameterize fieldValue to prevent SQL injection (CWE-89)
      query.setParameter("fieldValue", fieldValue);
      List<?> rows = query.list();

         for (Object row : rows)
         {
            contentIds.add((Integer)row);
         }


      return contentIds;
   }

   /**
    * Validates that a column name contains only safe characters (CWE-89 SQL Injection prevention).
    * Allows: alphanumeric characters, underscores, dots (for qualified names like "table.column")
    * Rejects: quotes, dashes, semicolons, comment markers, and other SQL special characters.
    *
    * @param columnName the column name to validate
    * @return true if the column name is safe, false otherwise
    */
   private static boolean isValidColumnName(String columnName) {
      if (columnName == null || columnName.trim().isEmpty()) {
         return false;
      }
      // Allow alphanumeric, underscore, and dot (for qualified names)
      // Pattern: [a-zA-Z0-9_.]+ - no spaces, no quotes, no special SQL chars
      return columnName.matches("^[a-zA-Z0-9_.]+$");
   }

}
