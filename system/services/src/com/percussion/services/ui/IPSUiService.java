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
package com.percussion.services.ui;

import com.percussion.services.ui.data.PSHierarchyNode;
import com.percussion.services.ui.data.PSHierarchyNodeProperty;
import com.percussion.utils.guid.IPSGuid;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Provides comprehensive functionality for creating, updating, and deleting UI objects
 * in the repository with modern Java 11 patterns. This service manages hierarchical
 * UI structures with enhanced type safety, Optional-based safe access, and Stream API
 * integration for efficient UI component management.
 *
 * @author Percussion Software
 */
public interface IPSUiService {

   /**
    * Create a new hierarchy node for the supplied name, parent and type with enhanced validation.
    *
    * @param name the name for the new node, never null or empty, must be unique for the specified parent
    * @param parentId the id of the parent node, may be null if this is a new root node
    * @param type the node type, never null
    * @return the newly created node, never null. The node is complete including the correct id,
    *         but not persisted to the repository yet
    * @throws IllegalArgumentException if name is null/empty or type is null
    * @throws PSUiException if a node with the same name already exists under the parent
    */
   PSHierarchyNode createHierarchyNode(String name, IPSGuid parentId,
      PSHierarchyNode.NodeType type) throws PSUiException;

   /**
    * Get all hierarchy nodes with enhanced collection support.
    *
    * @return unmodifiable list of all hierarchy nodes, never null but may be empty
    */
   List<PSHierarchyNode> getAllHierarchyNodes();

   /**
    * Get all hierarchy nodes as a stream for efficient processing.
    *
    * @return stream of all hierarchy nodes, never null
    */
   default Stream<PSHierarchyNode> getAllHierarchyNodesAsStream() {
      return getAllHierarchyNodes().stream();
   }

   /**
    * Get all node properties for nodes that have GUID as their name.
    *
    * @return unmodifiable list of hierarchy node properties, never null but may be empty
    */
   List<PSHierarchyNodeProperty> getAllHierarchyNodesGuidProperties();

   /**
    * Find hierarchy nodes by name with Optional-based filtering and enhanced search capabilities.
    *
    * @param name the name of the node to find, may be null or empty. Finds all nodes if null or empty,
    *             SQL wildcard patterns (%) are supported
    * @param type the node type filter, may be null to ignore this filter
    * @return unmodifiable list of found nodes, never null but may be empty
    */
   List<PSHierarchyNode> findHierarchyNodes(String name, PSHierarchyNode.NodeType type);

   /**
    * Find hierarchy nodes by name with Optional-based type filtering.
    *
    * @param name the name pattern to search for
    * @param type the optional node type filter
    * @return unmodifiable list of found nodes, never null but may be empty
    */
   default List<PSHierarchyNode> findHierarchyNodes(String name, Optional<PSHierarchyNode.NodeType> type) {
      return findHierarchyNodes(name, type.orElse(null));
   }

   /**
    * Find hierarchy nodes by name and parent with enhanced filtering capabilities.
    *
    * @param name the name of the node to find, may be null or empty. Finds all nodes for the specified
    *             parent if null or empty, SQL wildcard patterns (%) are supported
    * @param parentId the parent for which to find all nodes, may be null to find all root nodes
    * @param type the node type filter, may be null to ignore this filter
    * @return unmodifiable list of found nodes, never null but may be empty
    */
   List<PSHierarchyNode> findHierarchyNodes(String name, IPSGuid parentId, PSHierarchyNode.NodeType type);

   /**
    * Find hierarchy nodes as a stream for efficient processing and filtering.
    *
    * @param name the name pattern to search for
    * @param parentId the parent node id, may be null for root nodes
    * @param type the node type filter, may be null
    * @return stream of matching nodes, never null
    */
   default Stream<PSHierarchyNode> findHierarchyNodesAsStream(String name, IPSGuid parentId,
         PSHierarchyNode.NodeType type) {
      return findHierarchyNodes(name, parentId, type).stream();
   }

   /**
    * Load the hierarchy node for the specified id with enhanced error handling.
    *
    * @param id the id of hierarchy node to load, never null
    * @return the loaded hierarchy node, never null
    * @throws PSUiException if no hierarchy node was found for the specified id
    * @throws IllegalArgumentException if id is null
    */
   PSHierarchyNode loadHierarchyNode(IPSGuid id) throws PSUiException;

   /**
    * Find hierarchy node by id with safe access using Optional.
    *
    * @param id the id of the hierarchy node to find, never null
    * @return Optional containing the node if found, empty otherwise
    * @throws IllegalArgumentException if id is null
    */
   default Optional<PSHierarchyNode> findHierarchyNode(IPSGuid id) {
      Objects.requireNonNull(id, "Node ID cannot be null");
      try {
         return Optional.of(loadHierarchyNode(id));
      } catch (PSUiException e) {
         return Optional.empty();
      }
   }

   /**
    * Save the supplied hierarchy node to the repository with enhanced validation.
    *
    * @param node the hierarchy node to be saved, never null
    * @throws IllegalArgumentException if node is null
    * @throws PSUiException if the save operation fails
    */
   void saveHierarchyNode(PSHierarchyNode node) throws PSUiException;

   /**
    * Save multiple hierarchy nodes efficiently.
    *
    * @param nodes the collection of hierarchy nodes to save, never null
    * @throws IllegalArgumentException if nodes is null or contains null elements
    * @throws PSUiException if any save operation fails
    */
   default void saveHierarchyNodes(Collection<PSHierarchyNode> nodes) throws PSUiException {
      Objects.requireNonNull(nodes, "Nodes collection cannot be null");
      for (var node : nodes) {
         Objects.requireNonNull(node, "Node cannot be null");
         saveHierarchyNode(node);
      }
   }

   /**
    * Delete the referenced hierarchy node with enhanced safety.
    * Cases where the node for the supplied id does not exist are ignored.
    *
    * @param id the id of the hierarchy node to be deleted, never null
    * @throws IllegalArgumentException if id is null
    */
   void deleteHierarchyNode(IPSGuid id);

   /**
    * Delete multiple hierarchy nodes efficiently.
    *
    * @param ids the collection of node ids to delete, never null
    * @throws IllegalArgumentException if ids is null or contains null elements
    */
   default void deleteHierarchyNodes(Collection<IPSGuid> ids) {
      Objects.requireNonNull(ids, "IDs collection cannot be null");
      ids.forEach(id -> {
         Objects.requireNonNull(id, "Node ID cannot be null");
         deleteHierarchyNode(id);
      });
   }

   /**
    * Remove the specified children from the provided parent with enhanced validation.
    * Children which are not found in the parent will be ignored.
    *
    * @param parentId the id of the parent from which to remove the specified children, never null
    * @param ids the ids of all children to remove from the specified parent, never null or empty
    * @throws IllegalArgumentException if parentId is null or ids is null/empty
    */
   void removeChildren(IPSGuid parentId, List<IPSGuid> ids);

   /**
    * Remove children using a collection for flexibility.
    *
    * @param parentId the parent node id, never null
    * @param ids the collection of child ids to remove, never null
    * @throws IllegalArgumentException if parentId is null or ids is null
    */
   default void removeChildren(IPSGuid parentId, Collection<IPSGuid> ids) {
      Objects.requireNonNull(parentId, "Parent ID cannot be null");
      Objects.requireNonNull(ids, "Child IDs collection cannot be null");
      if (!ids.isEmpty()) {
         removeChildren(parentId, List.copyOf(ids));
      }
   }

   /**
    * Move all specified children from the source to the target with enhanced validation.
    * Children which are not found in the source will be ignored.
    *
    * @param sourceId the id of the source node from which to move the specified children, never null
    * @param targetId the id of the target to which to move the specified children, never null
    * @param ids the ids of all children to move from the source to the target, never null or empty
    * @throws IllegalArgumentException if any parameter is null or ids is empty
    */
   void moveChildren(IPSGuid sourceId, IPSGuid targetId, List<IPSGuid> ids);

   /**
    * Move children using a collection for flexibility.
    *
    * @param sourceId the source node id, never null
    * @param targetId the target node id, never null
    * @param ids the collection of child ids to move, never null
    * @throws IllegalArgumentException if any parameter is null
    */
   default void moveChildren(IPSGuid sourceId, IPSGuid targetId, Collection<IPSGuid> ids) {
      Objects.requireNonNull(sourceId, "Source ID cannot be null");
      Objects.requireNonNull(targetId, "Target ID cannot be null");
      Objects.requireNonNull(ids, "Child IDs collection cannot be null");
      if (!ids.isEmpty()) {
         moveChildren(sourceId, targetId, List.copyOf(ids));
      }
   }

   /**
    * Check if a hierarchy node exists with the given id.
    *
    * @param id the node id to check, never null
    * @return true if the node exists, false otherwise
    * @throws IllegalArgumentException if id is null
    */
   default boolean hierarchyNodeExists(IPSGuid id) {
      return findHierarchyNode(id).isPresent();
   }

   /**
    * Get the count of all hierarchy nodes.
    *
    * @return the total number of hierarchy nodes
    */
   default long getHierarchyNodeCount() {
      return getAllHierarchyNodes().size();
   }

   /**
    * Get the count of hierarchy nodes by type.
    *
    * @param type the node type to count, may be null to count all
    * @return the number of nodes of the specified type
    */
   default long getHierarchyNodeCountByType(PSHierarchyNode.NodeType type) {
      return getAllHierarchyNodesAsStream()
         .filter(node -> type == null || type.equals(node.getType()))
         .count();
   }
}
