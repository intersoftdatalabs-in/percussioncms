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
package com.percussion.taxonomy.repository;

import com.percussion.taxonomy.domain.Attribute;
import com.percussion.taxonomy.domain.Node;
import com.percussion.taxonomy.domain.Node_editor;
import com.percussion.taxonomy.domain.Related_node;
import com.percussion.taxonomy.domain.Value;
import java.util.Collection;
import java.util.Map;

/**
 * Service interface for managing Node entities.
 * Provides CRUD operations, search, and relationship management for taxonomy nodes.
 */
public interface NodeServiceInf {

  /**
   * Retrieves all nodes for a given taxonomy and language.
   *
   * @param taxID the unique identifier of the taxonomy
   * @param langID the unique identifier of the language
   * @return a collection of all Node entities matching the criteria
   */
  public Collection<Node> getAllNodes(int taxID, int langID);

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
      int taxID, int langID, String search_string, boolean exclude_disabled);

  /**
   * Retrieves a specific node by its ID and language.
   *
   * @param nodeID the unique identifier of the node
   * @param langID the unique identifier of the language
   * @return the Node entity, or null if not found
   */
  public Node getNode(int nodeID, int langID);

  /**
   * Retrieves multiple nodes by their unique identifiers.
   *
   * @param ids a collection of node unique identifiers
   * @return a collection of Node entities corresponding to the provided IDs
   */
  public Collection<Node> getSomeNodes(Collection<Integer> ids);

  /**
   * Retrieves all direct child nodes of a given parent node.
   *
   * @param nodeID the unique identifier of the parent node
   * @return a collection of child Node entities, or an empty collection if none exist
   */
  public Collection<Node> getChildNodes(int nodeID);

  /**
   * Finds nodes that have the specified attribute with associated values.
   *
   * @param attribute the Attribute to search for; must not be null
   * @return a collection of Node entities that have the given attribute
   */
  public Collection<Node> findNodesByAttribute(Attribute attribute);

  /**
   * Removes the specified node from the system.
   *
   * @param node the Node entity to remove; must not be null
   */
  public void removeNode(Node node);

  /**
   * Saves or updates the specified node in the system.
   *
   * @param node the Node entity to save; must not be null
   */
  public void saveNode(Node node);

  /**
   * Changes the parent of a node (moves the node to a new location in the hierarchy).
   *
   * @param nodeID the unique identifier of the node to move
   * @param newParentID the unique identifier of the new parent node
   */
  public void changeParent(int nodeID, int newParentID);

  /**
   * If the node doesn't have children, delete it and its relations.
   *
   * @param nodeID the unique identifier of the node to delete
   * @param taxonomyID the unique identifier of the taxonomy containing the node
   * @return a map of error messages if any, or null if the deletion was successful
   */
  public Map<String, String> deleteNodeAndFriends(int nodeID, int taxonomyID);

  /**
   * Retrieves all node names (and their IDs) for a given taxonomy and language.
   * Useful for dropdown selections.
   *
   * @param taxonomyID the unique identifier of the taxonomy
   * @param langID the unique identifier of the language
   * @return a collection of Object arrays containing [nodeId, nodeName] pairs
   */
  public Collection<Object[]> getAllNodeNames(int taxonomyID, int langID);

  /**
   * Retrieves names for a subset of nodes by their IDs.
   *
   * @param ids a collection of node unique identifiers
   * @param langID the unique identifier of the language
   * @return a collection of Object arrays containing [nodeId, nodeName] pairs
   */
  public Collection<Object[]> getSomeNodeNames(Collection<Integer> ids, int langID);

  /**
   * Retrieves the name of a specific node.
   *
   * @param nodeID the unique identifier of the node
   * @param langID the unique identifier of the language
   * @return a collection of node names (typically contains one entry)
   */
  public Collection<String> getNodeName(int nodeID, int langID);

  /**
   * Retrieves all values associated with a specific node.
   *
   * @param nodeID the unique identifier of the node
   * @param langID the unique identifier of the language
   * @return a collection of Value entities associated with the node
   */
  public Collection<Value> getValuesForNode(int nodeID, int langID);

  /**
   * Retrieves values for a specific node that are associated with a particular attribute.
   *
   * @param nodeID the unique identifier of the node
   * @param attrID the unique identifier of the attribute
   * @param langID the unique identifier of the language
   * @return a collection of Value entities matching the criteria
   */
  public Collection<Value> getSpecificValuesForNode(int nodeID, int attrID, int langID);

  /**
   * Retrieves all editors associated with a given node.
   *
   * @param nodeID the unique identifier of the node
   * @return a collection of Node_editor entities, or an empty collection if none exist
   */
  public Collection<Node_editor> getNodeEditors(int nodeID);

  /**
   * Retrieves all nodes related to the specified node (as defined by relationships).
   *
   * @param nodeID the unique identifier of the source node
   * @return a collection of Related_node entities
   */
  public Collection<Related_node> getRelatedNodes(int nodeID);

  /**
   * Finds nodes that are similar to the specified node based on attribute values.
   *
   * @param nodeID the unique identifier of the source node
   * @return a collection of Related_node entities representing similar nodes
   */
  public Collection<Related_node> getSimilarNodes(int nodeID);

  /**
   * Retrieves all references to the specified node from other nodes.
   *
   * @param nodeID the unique identifier of the target node
   * @return a collection of Related_node entities that reference this node
   */
  public Collection<Related_node> getRelatedNodeReferences(int nodeID);

  /**
   * Retrieves titles for all nodes in a given taxonomy.
   *
   * @param taxonomyID the unique identifier of the taxonomy
   * @param languageID the unique identifier of the language
   * @return a collection of Object arrays containing [nodeId, title] pairs
   */
  public Collection<Object[]> getTitlesForNodes(int taxonomyID, int languageID);

}
