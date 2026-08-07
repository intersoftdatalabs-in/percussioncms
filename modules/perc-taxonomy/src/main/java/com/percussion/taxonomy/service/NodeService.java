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
package com.percussion.taxonomy.service;

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.IPSBackEndMapping;
import com.percussion.design.objectstore.PSBackEndColumn;
import com.percussion.design.objectstore.PSContentEditorMapper;
import com.percussion.design.objectstore.PSContentEditorPipe;
import com.percussion.design.objectstore.PSContentEditorSharedDef;
import com.percussion.design.objectstore.PSControlRef;
import com.percussion.design.objectstore.PSDisplayMapper;
import com.percussion.design.objectstore.PSDisplayMapping;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSSharedFieldGroup;
import com.percussion.design.objectstore.PSUISet;
import com.percussion.server.PSServer;
import com.percussion.taxonomy.domain.Attribute;
import com.percussion.taxonomy.domain.Node;
import com.percussion.taxonomy.domain.Node_editor;
import com.percussion.taxonomy.domain.Related_node;
import com.percussion.taxonomy.domain.Value;
import com.percussion.taxonomy.repository.NodeDAO;
import com.percussion.taxonomy.repository.NodeServiceInf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;

/**
 * General Service for performing CRUD operations on NODE objects
 *
 * @author rxengineer
 */
public class NodeService implements NodeServiceInf {

  protected static final Logger logger = LogManager.getLogger(NodeService.class);

  /////////////////////////////////////////////////////////

  public NodeDAO nodeDAO;

  /////////////////////////////////////////////////////////

  /**
   * Retrieves all nodes for a given taxonomy and language.
   *
   * @param taxID the unique identifier of the taxonomy
   * @param langID the unique identifier of the language
   * @return a collection of all Node entities matching the criteria
   */
  public Collection<Node> getAllNodes(int taxID, int langID) {
    Collection<Node> nodes = null;
    try {
      nodes = this.nodeDAO.getAllNodes(taxID, langID);
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
    return nodes;
  }

  /**
   * Retrieves a specific node by its unique identifier and language.
   *
   * @param nodeID the unique identifier of the node
   * @param langID the unique identifier of the language
   * @return the Node entity, or null if not found
   */
  public Node getNode(int nodeID, int langID) {
    Node node = null;
    try {
      node = this.nodeDAO.getNode(nodeID, langID);
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
    return node;
  }

  /**
   * Retrieves multiple nodes by their unique identifiers.
   *
   * @param ids a collection of node unique identifiers
   * @return a collection of Node entities corresponding to the provided IDs
   */
  public Collection<Node> getSomeNodes(Collection<Integer> ids) {
    Collection<Node> nodes = null;
    if (ids != null) {
      try {
        nodes = this.nodeDAO.getSomeNodes(ids);
      } catch (HibernateException e) {
        throw new HibernateException(e);
      }
    }
    return nodes;
  }

  /**
   * Searches for nodes matching the specified search string within a taxonomy and language.
   *
   * @param taxID the unique identifier of the taxonomy to search in
   * @param langID the unique identifier of the language
   * @param search_string the search string to match against node names or values
   * @param exclude_disabled if true, excludes disabled nodes from results
   * @return a collection of Node entities matching the search criteria
   */
  public Collection<Node> getNodesFromSearch(
      int taxID, int langID, String search_string, boolean exclude_disabled) {
    Collection<Node> nodes = null;
    try {
      nodes = this.nodeDAO.getNodesFromSearch(taxID, langID, search_string, exclude_disabled);
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
    return nodes;
  }

  /**
   * Retrieves all direct child nodes of a given parent node.
   *
   * @param nodeID the unique identifier of the parent node
   * @return a collection of child Node entities, or an empty collection if none exist
   */
  public Collection<Node> getChildNodes(int nodeID) {
    Collection<Node> children = null;
    try {
      children = this.nodeDAO.getChildNodes(nodeID);
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
    return children;
  }

  /**
   * Finds nodes that have the specified attribute with associated values.
   *
   * @param attribute the Attribute to search for; must not be null
   * @return a collection of Node entities that have the given attribute
   */
  public Collection<Node> findNodesByAttribute(Attribute attribute) {
    Collection<Node> nodes = new ArrayList<Node>();
    if (attribute != null) {
      nodes = this.nodeDAO.findNodesByAttribute(attribute);
    }
    return nodes;
  }

  /////////////////////////////////////////////////////////

  /**
   * Removes the specified node from the system.
   *
   * @param node the Node entity to remove; must not be null
   */
  public void removeNode(Node node) {
    if (node != null) {
      try {
        this.nodeDAO.removeNode(node);
      } catch (HibernateException e) {
        throw new HibernateException(e);
      }
    }
  }

  /**
   * Saves or updates the specified node in the system.
   *
   * @param node the Node entity to save; must not be null
   */
  public void saveNode(Node node) {
    if (node != null) {
      try {
        this.nodeDAO.saveNode(node);
      } catch (HibernateException e) {
        throw new HibernateException(e);
      }
    }
  }

  /////////////////////////////////////////////////////////

  /**
   * Retrieves all node names (and their IDs) for a given taxonomy and language. Useful for dropdown
   * selections.
   *
   * @param taxonomyID the unique identifier of the taxonomy
   * @param langID the unique identifier of the language
   * @return a collection of Object arrays containing [nodeId, nodeName] pairs
   */
  public Collection<Object[]> getAllNodeNames(int taxonomyID, int langID) {
    Collection<Object[]> names = null;
    try {
      names = nodeDAO.getAllNodeNames(taxonomyID, langID);
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
    return names;
  }

  /**
   * Retrieves names for a subset of nodes by their IDs.
   *
   * @param ids a collection of node unique identifiers
   * @param langID the unique identifier of the language
   * @return a collection of Object arrays containing [nodeId, nodeName] pairs
   */
  public Collection<Object[]> getSomeNodeNames(Collection<Integer> ids, int langID) {
    Collection<Object[]> names = null;
    try {
      names = this.nodeDAO.getSomeNodeNames(ids, langID);
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
    return names;
  }

  /**
   * Retrieves the name of a specific node.
   *
   * @param nodeID the unique identifier of the node
   * @param langID the unique identifier of the language
   * @return a collection of node names (typically contains one entry)
   */
  public Collection<String> getNodeName(int nodeID, int langID) {
    Collection<String> names = null;
    try {
      names = this.nodeDAO.getNodeName(nodeID, langID);
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
    return names;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Retrieves all values associated with a specific node.
   *
   * @param nodeID the unique identifier of the node
   * @param langID the unique identifier of the language
   * @return a collection of Value entities associated with the node
   */
  public Collection<Value> getValuesForNode(int nodeID, int langID) {
    Collection<Value> values = null;
    try {
      values = this.nodeDAO.getValuesForNode(nodeID, langID);
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
    return values;
  }

  /**
   * Retrieves values for a specific node that are associated with a particular attribute.
   *
   * @param nodeID the unique identifier of the node
   * @param attrID the unique identifier of the attribute
   * @param langID the unique identifier of the language
   * @return a collection of Value entities matching the criteria
   */
  public Collection<Value> getSpecificValuesForNode(int nodeID, int attrID, int langID) {
    Collection<Value> values = null;
    try {
      values = this.nodeDAO.getSpecificValuesForNode(nodeID, attrID, langID);
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
    return values;
  }

  //////////////////////////////////////////////////////////////////////////////////////

  /**
   * Retrieves all nodes related to the specified node (as defined by relationships).
   *
   * @param nodeID the unique identifier of the source node
   * @return a collection of Related_node entities
   */
  public Collection<Related_node> getRelatedNodes(int nodeID) {
    Collection<Related_node> nodes = null;
    try {
      nodes = this.nodeDAO.getRelatedNodes(nodeID);
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
    return nodes;
  }

  /**
   * Retrieves all references to the specified node from other nodes.
   *
   * @param nodeID the unique identifier of the target node
   * @return a collection of Related_node entities that reference this node
   */
  public Collection<Related_node> getRelatedNodeReferences(int nodeID) {
    Collection<Related_node> relatedNodes = null;
    try {
      relatedNodes = this.nodeDAO.getRelatedNodeReferences(nodeID);
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
    return relatedNodes;
  }

  /**
   * Finds nodes that are similar to the specified node based on attribute values.
   *
   * @param nodeID the unique identifier of the source node
   * @return a collection of Related_node entities representing similar nodes
   */
  public Collection<Related_node> getSimilarNodes(int nodeID) {
    Collection<Related_node> relatedNodes = null;
    try {
      relatedNodes = this.nodeDAO.getSimilarNodes(nodeID);
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
    return relatedNodes;
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  /**
   * Retrieves all editors associated with a given node.
   *
   * @param nodeID the unique identifier of the node
   * @return a collection of Node_editor entities, or an empty collection if none exist
   */
  public Collection<Node_editor> getNodeEditors(int nodeID) {
    Collection<Node_editor> editors = null;
    try {
      editors = this.nodeDAO.getNodeEditors(nodeID);
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
    return editors;
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  /**
   * Changes the parent of a node (moves the node to a new location in the hierarchy).
   *
   * @param nodeID the unique identifier of the node to move
   * @param newParentID the unique identifier of the new parent node
   */
  public void changeParent(int nodeID, int newParentID) {
    try {
      this.nodeDAO.changeParent(nodeID, newParentID);
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
  }

  /**
   * Retrieves titles for all nodes in a given taxonomy.
   *
   * @param taxonomyID the unique identifier of the taxonomy
   * @param languageID the unique identifier of the language
   * @return a collection of Object arrays containing [nodeId, title] pairs
   */
  public Collection<Object[]> getTitlesForNodes(int taxonomyID, int languageID) {
    Collection<Object[]> titles = null;
    try {
      titles = this.nodeDAO.getTitlesForNodes(taxonomyID, languageID);
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
    return titles;
  }

  /**
   * If the node doesn't have children, deletes it and its relations.
   *
   * @param nodeID the unique identifier of the node to delete
   * @param taxonomyID the unique identifier of the taxonomy containing the node
   * @return a map of error messages if any, or null if the deletion was successful
   */
  public Map<String, String> deleteNodeAndFriends(int nodeID, int taxonomyID) {
    Map<String, String> results = null;
    try {
      results = this.nodeDAO.deleteNodeAndFriends(nodeID, taxonomyID);
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
    return results;
  }

  /**
   * Sets the NodeDAO instance for this service.
   *
   * @param nodeDAO the NodeDAO to use for database operations
   */
  public void setNodeDAO(NodeDAO nodeDAO) {
    this.nodeDAO = nodeDAO;
  }

  /**
   * Finds items that use the specified node in their content.
   *
   * @param table the database table name containing the taxonomy field
   * @param column the database column name of the taxonomy field
   * @param node the Node to search for
   * @param maxItems the maximum number of items to return (-1 for unlimited)
   * @param remove if true, removes the node reference from found items
   * @return a list of PSLocator objects pointing to the found items
   */
  public List<PSLocator> findItemsUsingNode(
      String table, String column, Node node, int maxItems, boolean remove) {
    return this.nodeDAO.findItemsUsingNode(table, column, node, maxItems, remove);
  }

  /**
   * Gets all database locations where the specified node is in use.
   *
   * @param node the Node to search for
   * @return a list of PSLocator objects representing database locations
   */
  public List<PSLocator> getDbInUse(Node node) {
    return findItemsForAllTables(node, -1, false);
  }

  /**
   * Checks if the specified node is in use in any content.
   *
   * @param node the Node to check
   * @return true if the node is in use, false otherwise
   */
  public boolean checkDbInUse(Node node) {

    return findItemsForAllTables(node, 1, false).size() > 0;
  }

  /**
   * Deletes the specified node from all content items that reference it.
   *
   * @param node the Node to remove from content
   */
  public void deleteNodeFromContent(Node node) {
    findItemsForAllTables(node, -1, true);
  }

  private List<PSLocator> findItemsForAllTables(Node node, int maxItems, boolean remove) {
    HashMap<String, HashSet<String>> fieldCols =
        getColumnsForControls(Collections.singletonList("sys_TaxonomyAccordion"));
    List<PSLocator> locators = new ArrayList<>();
    for (Entry<String, HashSet<String>> entry : fieldCols.entrySet()) {
      String table = entry.getKey();

      HashSet<String> columns = entry.getValue();

      for (String column : columns) {
        logger.debug("found Taxonomy column " + table + "." + column);
        locators.addAll(
            findItemsUsingNode(table, column, node, maxItems - locators.size(), remove));
        if (locators.size() >= maxItems) return locators;
      }
    }
    return locators;
  }

  private HashMap<String, HashSet<String>> getColumnsForControls(List<String> controlNames) {
    PSItemDefManager defMgr = PSItemDefManager.getInstance();
    HashSet<PSField> fields = new HashSet<>();
    HashMap<String, HashSet<String>> tableColMap = new HashMap<>();
    // load system and shared def
    // PSContentEditorSystemDef m_systemDef = PSServer.getContentEditorSystemDef();
    PSContentEditorSharedDef m_sharedDef = PSServer.getContentEditorSharedDef();

    Iterator<?> groupsItt = m_sharedDef.getFieldGroups();

    while (groupsItt.hasNext()) {
      PSSharedFieldGroup group = (PSSharedFieldGroup) groupsItt.next();
      PSFieldSet fieldSet = group.getFieldSet();
      PSDisplayMapper displayMapper = group.getUIDefinition().getDisplayMapper();
      fields.addAll(getControlFields(fieldSet, displayMapper, controlNames));
    }

    long[] typeIds = defMgr.getAllContentTypeIds(-1);

    List<PSBackEndColumn> retCols = new ArrayList<PSBackEndColumn>();

    for (int i = 0; i < typeIds.length; i++) {
      PSItemDefinition itemDef;

      try {
        itemDef = defMgr.getItemDef(typeIds[i], -1);

        PSContentEditorPipe pipe = (PSContentEditorPipe) itemDef.getContentEditor().getPipe();
        PSContentEditorMapper mapper = pipe.getMapper();
        PSFieldSet fieldSet = mapper.getFieldSet();

        PSDisplayMapper dispMapper = mapper.getUIDefinition().getDisplayMapper();

        fields.addAll(getControlFields(fieldSet, dispMapper, controlNames));

      } catch (PSInvalidContentTypeException e) {
        logger.debug("Skipping invalid content type ", e);
      }
    }

    for (PSField field : fields) {
      IPSBackEndMapping locator = field.getLocator();
      if (locator instanceof PSBackEndColumn) {
        PSBackEndColumn column = (PSBackEndColumn) locator;
        String table = column.getTable().getTable();
        String columnStr = column.getColumn();
        HashSet<String> tableEntry = tableColMap.get(table);
        if (tableEntry == null) {
          tableEntry = new HashSet<>();
          tableColMap.put(table, tableEntry);
        }
        tableEntry.add(columnStr);
      }
    }
    return tableColMap;
  }

  private List<PSField> getControlFields(
      PSFieldSet fieldSet, PSDisplayMapper mapper, List<String> controlNames) {
    List<PSField> fields = new ArrayList<PSField>();
    Iterator<?> mappings = mapper.iterator();
    while (mappings.hasNext()) {
      PSDisplayMapping mapping = (PSDisplayMapping) mappings.next();

      String fieldName = mapping.getFieldRef();

      PSUISet uiSet = mapping.getUISet();
      PSControlRef control = uiSet.getControl();
      if (control != null) {
        String controlName = control.getName();
        if (controlNames.contains(controlName)) {

          Object o = fieldSet.get(fieldName);
          /**
           * If the field reference is not found in this fieldset, then check whether it is
           * multiproperty simple child field
           */
          if (o == null) {
            o = fieldSet.getChildField(fieldName, PSFieldSet.TYPE_MULTI_PROPERTY_SIMPLE_CHILD);
          }

          /* If field reference is field set, then it might be simplechild or
           * complexchild. In case of simple child, we have to show the mapping
           * in parent mapper only, so get the field reference from it's mapper
           * and get the field.
           */
          if (o instanceof PSFieldSet) {
            PSFieldSet childFs = (PSFieldSet) o;
            if (childFs.getType() == PSFieldSet.TYPE_SIMPLE_CHILD) {
              PSDisplayMapper childMapper = mapping.getDisplayMapper();
              Iterator<?> childMappings = childMapper.iterator();
              while (childMappings.hasNext()) {
                PSDisplayMapping childMapping = (PSDisplayMapping) childMappings.next();
                fieldName = childMapping.getFieldRef();
                o = fieldSet.getChildField(fieldName, PSFieldSet.TYPE_SIMPLE_CHILD);
              }
            } else
              // don't recurse into complex child field sets
              continue;
          }

          if (o instanceof PSField) {
            fields.add((PSField) o);
          }
        }
      }
    }
    return fields;
  }

  ///////////////////////////////////////////////////////////////////////////////

}
