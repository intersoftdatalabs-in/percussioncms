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
package com.percussion.services.ui.impl;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.ui.IPSUiService;
import com.percussion.services.ui.PSUiException;
import com.percussion.services.ui.data.PSHierarchyNode;
import com.percussion.services.ui.data.PSHierarchyNodeProperty;
import com.percussion.system.utils.PSBaseBean;
import com.percussion.utils.guid.IPSGuid;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import com.intsof.percussioncms.auditlog.codes.UiErrorCodes;

/**
 * Implementations for all ui services.
 */
@PSBaseBean("sys_uiService")
@Transactional
public class PSUiService implements IPSUiService
{
   @PersistenceContext
   private EntityManager entityManager;

   private Session getSession(){
      return entityManager.unwrap(Session.class);
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiService#createHierarchyNode(String, IPSGuid,
    * PSHierarchyNode.NodeType)
    */
   @Transactional
   public PSHierarchyNode createHierarchyNode(String name, IPSGuid parentId, PSHierarchyNode.NodeType type)
   {
      if (StringUtils.isBlank(name))
         throw new IllegalArgumentException("name cannot be null or empty");

      if (type == null)
         throw new IllegalArgumentException("type cannot be null");

      List<PSHierarchyNode> nodes = findHierarchyNodes(name, parentId, type);
      if (!nodes.isEmpty())
         throw new IllegalArgumentException("name must be unique in parent node");

      IPSGuidManager guidManager = PSGuidManagerLocator.getGuidMgr();

      PSHierarchyNode node = new PSHierarchyNode(name, guidManager.createGuid(PSTypeEnum.HIERARCHY_NODE), type);
      node.setParentId(parentId);

      return node;
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiService#deleteHierarchyNode(IPSGuid)
    */
   @Transactional
   public void deleteHierarchyNode(IPSGuid id)
   {
      if (id == null)
         throw new IllegalArgumentException("id cannot be null");

      try
      {
         PSHierarchyNode node = loadHierarchyNode(id);

         // delete all children recursive
         List<PSHierarchyNode> children = findHierarchyNodes("%", node.getGUID(), null);

         for (PSHierarchyNode child : children)
            deleteHierarchyNode(child.getGUID());

         // delete all properties first
         List<PSHierarchyNodeProperty> properties = loadHierarchyNodeProperties(node.getGUID());
         for (PSHierarchyNodeProperty property : properties)
            deleteHierarchyNodeProperty(property);

         // node delete the node
         getSession().remove(node);
      }
      catch (PSUiException e)
      {
         // ignore non existing node
      }
   }

   /**
    * To get all the hierarchy nodes.
    */

   public List<PSHierarchyNode> getAllHierarchyNodes()
   {
      TypedQuery<PSHierarchyNode> q = entityManager.createQuery("from PSHierarchyNode", PSHierarchyNode.class);
      return q.getResultList();
   }

   /**
    * To get the hierarchy node properties where the nodes are type guid(non
    * folders)
    */
   public List<PSHierarchyNodeProperty> getAllHierarchyNodesGuidProperties()
   {

      Session session = getSession();


      CriteriaBuilder builder = session.getCriteriaBuilder();
      CriteriaQuery<PSHierarchyNodeProperty> criteria = builder.createQuery(PSHierarchyNodeProperty.class);
      Root<PSHierarchyNodeProperty> critRoot = criteria.from(PSHierarchyNodeProperty.class);
      criteria.where(builder.equal(critRoot.get("name"),"guid"));


      return  entityManager.createQuery(criteria).getResultList();
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiService#findHierarchyNodes(String, PSHierarchyNode.NodeType)
    */

   public List<PSHierarchyNode> findHierarchyNodes(String name, PSHierarchyNode.NodeType type)
   {
      if (StringUtils.isBlank(name))
         name = "%";

      CriteriaBuilder cb = entityManager.getCriteriaBuilder();
      CriteriaQuery<PSHierarchyNode> cq = cb.createQuery(PSHierarchyNode.class);
      Root<PSHierarchyNode> root = cq.from(PSHierarchyNode.class);

      List<Predicate> predicates = new ArrayList<>();
      if (!name.equals("%")) predicates.add(cb.like(root.get("name"), name));
      if (type != null) predicates.add(cb.equal(root.get("type"), type.getOrdinal()));

      cq.select(root).where(predicates.toArray(new Predicate[0]));
      cq.orderBy(cb.asc(root.get("name")));

      TypedQuery<PSHierarchyNode> query = entityManager.createQuery(cq);
      List<PSHierarchyNode> nodes = query.getResultList();

      // then load all node properties
      for (PSHierarchyNode node : nodes)
         loadHierarchyNodeProperties(node);

      return nodes;

   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiService#findHierarchyNodes(String, IPSGuid,
    * PSHierarchyNode.NodeType)
    */

   public List<PSHierarchyNode> findHierarchyNodes(String name, IPSGuid parentId, PSHierarchyNode.NodeType type)
   {
      Session session = getSession();

      if (StringUtils.isBlank(name))
         name = "%";

      CriteriaBuilder cb = entityManager.getCriteriaBuilder();
      CriteriaQuery<PSHierarchyNode> cq = cb.createQuery(PSHierarchyNode.class);
      Root<PSHierarchyNode> root = cq.from(PSHierarchyNode.class);

      List<Predicate> predicates = new ArrayList<>();
      if (!name.equals("%")) predicates.add(cb.like(root.get("name"), name));
      if (parentId != null) predicates.add(cb.equal(root.get("parentId"), parentId.longValue()));
      if (type != null) predicates.add(cb.equal(root.get("type"), type.getOrdinal()));

      cq.select(root).where(predicates.toArray(new Predicate[0]));
      cq.orderBy(cb.asc(root.get("name")));

      TypedQuery<PSHierarchyNode> query = entityManager.createQuery(cq);
      query.setHint("org.hibernate.cacheable", Boolean.TRUE);
      List<PSHierarchyNode> nodes = query.getResultList();

      // then filter out root nodes if requested
      List<PSHierarchyNode> resultNodes = null;
      if (parentId == null)
      {
         resultNodes = new ArrayList<>();
         for (PSHierarchyNode node : nodes)
            if (node.getParentId() == null)
               resultNodes.add(node);
      }
      else
         resultNodes = nodes;

      // finally load all node properties
      for (PSHierarchyNode node : resultNodes)
         loadHierarchyNodeProperties(node);

      return resultNodes;

   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiService#loadHierarchyNode(IPSGuid)
    */
   public PSHierarchyNode loadHierarchyNode(IPSGuid id) throws PSUiException
   {
      if (id == null)
         throw new IllegalArgumentException("id cannot be null");

      Session session = getSession();

      PSHierarchyNode node = session.get(PSHierarchyNode.class, id.longValue());
      if (node == null)
         throw new PSUiException(UiErrorCodes.MISSING_HIERARCHY_NODE, id);

      loadHierarchyNodeProperties(node);
      return node;

   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiService#saveHierarchyNode(PSHierarchyNode)
    */
   @Transactional
   public void saveHierarchyNode(PSHierarchyNode node)
   {
      if (node == null)
         throw new IllegalArgumentException("node cannot be null");

      Session session = getSession();
      try
      {
         if (node.getVersion() == null)
         {
            // insert node first
            session.persist(node);

            // then insert properties
            for (String propertyName : node.getProperties().keySet())
            {
               String propertyValue = node.getProperty(propertyName);
               PSHierarchyNodeProperty property = new PSHierarchyNodeProperty(propertyName, propertyValue,
                       node.getGUID());
               saveHierarchyNodeProperty(property);
            }
         }
         else
         {
            // update node first
            session.merge(node);

            // then update properties
            List<PSHierarchyNodeProperty> existingProperties = loadHierarchyNodeProperties(node.getGUID());
            for (String propertyName : node.getProperties().keySet())
            {
               String propertyValue = node.getProperty(propertyName);

               boolean exists = false;
               for (PSHierarchyNodeProperty property : existingProperties)
               {
                  if (property.getName().equals(propertyName))
                  {
                     // update existing property
                     property.setValue(propertyValue);
                     saveHierarchyNodeProperty(property);

                     existingProperties.remove(property);
                     exists = true;
                     break;
                  }
               }

               if (!exists)
               {
                  // create new propery
                  PSHierarchyNodeProperty property = new PSHierarchyNodeProperty(propertyName, propertyValue,
                          node.getGUID());
                  saveHierarchyNodeProperty(property);
               }
            }

            // remove removed properties
            for (PSHierarchyNodeProperty property : existingProperties)
               deleteHierarchyNodeProperty(property);
         }
      }
      finally
      {
         session.flush();

      }
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiService#removeChildren(IPSGuid, List)
    */
   @Transactional
   public void removeChildren(IPSGuid parentId, List<IPSGuid> ids)
   {
      if (parentId == null)
         throw new IllegalArgumentException("parentId cannot be null");

      if (ids == null || ids.isEmpty())
         throw new IllegalArgumentException("ids cannot be null or empty");

      List<PSHierarchyNode> children = findHierarchyNodes("%", parentId, null);
      for (IPSGuid id : ids)
      {
         PSHierarchyNode node = findHierarchyNode(children, id);
         if (node != null)
            deleteHierarchyNode(id);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSUiService#moveChildren(IPSGuid, IPSGuid, List)
    */
   @Transactional
   public void moveChildren(IPSGuid sourceId, IPSGuid targetId, List<IPSGuid> ids)
   {
      if (sourceId == null)
         throw new IllegalArgumentException("sourceId cannot be null");

      if (targetId == null)
         throw new IllegalArgumentException("targetId cannot be null");

      if (ids == null || ids.isEmpty())
         throw new IllegalArgumentException("ids cannot be null or empty");

      List<PSHierarchyNode> children = findHierarchyNodes("%", sourceId, null);
      for (IPSGuid id : ids)
      {
         PSHierarchyNode node = findHierarchyNode(children, id);
         if (node != null)
         {
            node.setParentId(targetId);
            getSession().merge(node);
         }
      }
   }

   /**
    * Finds and returns the node in the supplied list for the specified id.
    *
    * @param nodes the list of nodes to search for the node with the specified
    *           id, assumed not <code>null</code>, may be empty.
    * @param id the id of the node to find, assumed not <code>null</code>.
    * @return the foundd node, may be <code>null</code> if not found.
    */
   private PSHierarchyNode findHierarchyNode(List<PSHierarchyNode> nodes, IPSGuid id)
   {
      for (PSHierarchyNode node : nodes)
      {
         if (node.getGUID().equals(id))
            return node;
      }

      return null;
   }

   /**
    * Load all hierarchy node properties for the supplied node id.
    *
    * @param nodeId the id of the node for which to load all hierarchy node
    *           properties, not <code>null</code>.
    * @return a list with all hierarchy node properties found for the supplied
    *         node, never <code>null</code>, may be empty.
    */

   private List<PSHierarchyNodeProperty> loadHierarchyNodeProperties(IPSGuid nodeId)
   {
      if (nodeId == null)
         throw new IllegalArgumentException("nodeId cannot be null");

      Session session = getSession();

      Query<PSHierarchyNodeProperty> q = session.createQuery("from PSHierarchyNodeProperty where nodeId = :nid", PSHierarchyNodeProperty.class)
            .setParameter("nid", nodeId.longValue()).setCacheable(true);
      return q.list();

   }

   /**
    * Load all hierarchy node properties into the supplied node.
    *
    * @param node the node for which to load all hierarchy node properties, not
    *           <code>null</code>.
    */
   private void loadHierarchyNodeProperties(PSHierarchyNode node)
   {
      if (node == null)
         throw new IllegalArgumentException("node cannot be null");

      List<PSHierarchyNodeProperty> properties = loadHierarchyNodeProperties(node.getGUID());

      for (PSHierarchyNodeProperty property : properties)
         node.addProperty(property.getName(), property.getValue());
   }

   /**
    * Save the supplied hierarchy node property.
    *
    * @param property the property to be saved, not <code>null</code>.
    */
   private void saveHierarchyNodeProperty(PSHierarchyNodeProperty property)
   {
      if (property == null)
         throw new IllegalArgumentException("property cannot be null");

      Session session = getSession();

      if (property.getVersion() == null)
         session.persist(property);
      else
         session.merge(property);

   }

   /**
    * Delete the suppliedd hierarchy node property.
    *
    * @param property the property to delete, not <code>null</code>.
    */
   private void deleteHierarchyNodeProperty(PSHierarchyNodeProperty property)
   {
      if (property == null)
         throw new IllegalArgumentException("property cannot be null");

      getSession().remove(property);
   }
}
