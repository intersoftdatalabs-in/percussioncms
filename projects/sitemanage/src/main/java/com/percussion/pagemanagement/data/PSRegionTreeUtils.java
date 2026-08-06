// REFACTORED: CP-JAVA11
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
package com.percussion.pagemanagement.data;

import static com.percussion.pagemanagement.data.PSRegionTreeUtils.getWidgetRegions;
import static org.apache.commons.lang3.Validate.*;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Utilities for {@link PSRegionNode} trees.
 *
 * @author adamgent
 */
public class PSRegionTreeUtils {

  /**
   * Will visit the nodes defined by the order of the iterator.
   *
   * @param it order of the nodes, never {@code null}.
   * @param visitor never {@code null}.
   */
  public static void visitNodes(Iterator<PSRegionNode> it, IPSRegionNodeVisitor visitor) {
    notNull(it);
    notNull(visitor);
    while (it.hasNext()) {
      var node = it.next();
      node.accept(visitor);
    }
  }

  /**
   * Will visit the region nodes in natural order of the {@link PSRegionNode} tree. The order is
   * preorder, depth first search traversal. See: <a
   * href="http://en.wikipedia.org/wiki/Tree_traversal">Tree traversal</a>
   *
   * @param rootNode the root node
   * @param visitor the visitor
   */
  public static void visitNodes(PSRegionNode rootNode, IPSRegionNodeTreeVisitor visitor) {
    notNull(rootNode);
    notNull(visitor);
    var it = new PSRegionNodeWrapperIterator(rootNode);
    while (it.hasNext()) {
      var nw = it.next();
      if (nw.type == PSRegionNodeWrapper.Type.START) {
        nw.node.accept(visitor.getStartRegionNodeVisitor());
      } else if (nw.type == PSRegionNodeWrapper.Type.END) {
        nw.node.accept(visitor.getEndRegionNodeVisitor());
      } else {
        isTrue(false);
      }
    }
  }

  public static Map<String, PSRegion> regionMap(PSRegionNode rootNode) {
    var regions = iterateRegions(rootNode);
    var map = new HashMap<String, PSRegion>();
    while (regions.hasNext()) {
      var r = regions.next();
      map.put(r.getRegionId(), r);
    }
    return map;
  }

  public static Iterator<PSRegion> iterateRegions(PSRegionNode rootNode) {
    var it = new PSRegionNodeWrapperIterator(rootNode);
    var regions = new ArrayList<PSRegion>();
    while (it.hasNext()) {
      var nw = it.next();
      if (nw.node instanceof PSRegion && nw.type == PSRegionNodeWrapper.Type.START) {
        regions.add((PSRegion) nw.node);
      }
    }
    return regions.iterator();
  }

  public static List<? extends PSRegionNode> getChildren(PSRegionNode node) {
    if (node instanceof PSAbstractRegion) {
      var r = (PSAbstractRegion) node;
      if (r.getChildren() != null && !r.getChildren().isEmpty()) {
        return r.getChildren();
      }
    }
    return new ArrayList<>();
  }

  @SuppressWarnings("unchecked")
  public static <T extends PSAbstractRegion> List<T> getChildRegions(PSRegionNode node) {
    var regions = new ArrayList<T>();
    var children = getChildren(node);
    for (var child : children) {
      if (child instanceof PSAbstractRegion) {
        regions.add((T) child);
      }
    }
    return regions;
  }

  public static boolean isLeaf(PSRegionNode node) {
    if (node instanceof PSAbstractRegion) {
      var nodes = ((PSAbstractRegion) node).getChildren();
      if (nodes != null && !nodes.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  protected static class PSRegionNodeWrapper {
    protected enum Type {
      START,
      END
    }

    protected PSRegionNode node;
    protected Type type;

    public PSRegionNodeWrapper(PSRegionNode node, Type type) {
      super();
      this.node = node;
      this.type = type;
    }
  }

  /**
   * Converts a region Abstract Syntax Tree back into template code.
   *
   * @param rootNode never {@code null}.
   * @return never {@code null}.
   */
  public static String treeToString(PSAbstractRegion rootNode) {
    var sw = new StringWriter();
    var tw = new PSRegionTreeWriter(sw);
    tw.write(rootNode);
    return sw.toString();
  }

  /**
   * Retrieves a {@link Set} containing the leaf regions of the template. A leaf region is a region
   * that does not contain other regions.
   *
   * @param regionTree {@link PSRegionTree} object, cannot be {@code null}.
   * @return {@link Set}&lt;{@link PSRegion}&gt; never {@code null}.
   */
  public static Set<PSRegion> getWidgetRegions(PSRegionTree regionTree) {
    notNull(regionTree);

    if (regionTree.getRootRegion() == null) {
      return new HashSet<>();
    }

    var leafRegions = new HashSet<PSRegion>();
    var nodes = getChildRegions(regionTree.getRootRegion());
    for (var region : nodes) {
      getWidgetRegionsFromChilds(leafRegions, region);
    }
    return leafRegions;
  }

  /**
   * Recursively iterates over the nodes and gets the leaf regions.
   *
   * @param leafRegions {@link Set}<{@link PSRegion}> to save the leaf nodes. Must not be {@code
   *     null}.
   * @param node {@link PSRegion} representing the current node.
   */
  private static void getWidgetRegionsFromChilds(Set<PSRegion> leafRegions, PSAbstractRegion node) {
    if (!(node instanceof PSRegion region)) {
      return; // Only process PSRegion instances
    }
    if (isWidgetRegion(region)) {
      leafRegions.add(region);
      return;
    }
    var nodes = getChildRegions(region);
    for (var child : nodes) {
      getWidgetRegionsFromChilds(leafRegions, child);
    }
  }

  /**
   * Gets the leaf regions of the template and returns those that don't have widgets in them.
   *
   * @param regionTree the region tree
   * @return {@link Set}&lt;{@link PSRegion}&gt; never {@code null}.
   */
  public static Set<PSRegion> getEmptyWidgetRegions(PSRegionTree regionTree) {
    notNull(regionTree);

    var emptyLeafs = new HashSet<PSRegion>();
    var leafs = getWidgetRegions(regionTree);
    var notEmptyRegions = regionTree.getRegionWidgetsMap().keySet();

    for (var region : leafs) {
      if (!notEmptyRegions.contains(region.getRegionId())) {
        emptyLeafs.add(region);
      }
    }
    return emptyLeafs;
  }

  /**
   * A {@link PSRegion} is a leaf if:
   *
   * <ul>
   *   <li>its children collection is empty
   *   <li>its children collection is not empty, but the children are instances of {@link
   *       PSRegionCode}
   * </ul>
   *
   * @param region {@link PSRegion} object, must not be {@code null}
   * @return {@code true} if the region is a leaf, {@code false} otherwise.
   */
  private static boolean isWidgetRegion(PSRegion region) {
    if (isEmpty(region.getChildren())) {
      return true;
    }
    if (region.getChildren().size() == 1 && region.getChildren().get(0) instanceof PSRegionCode) {
      return true;
    }
    return false;
  }

  protected static class PSRegionNodeWrapperIterator implements Iterator<PSRegionNodeWrapper> {
    private final Stack<PSRegionNodeWrapper> nodeStack = new Stack<>();

    public PSRegionNodeWrapperIterator(PSRegionNode rootNode) {
      super();
      nodeStack.push(new PSRegionNodeWrapper(rootNode, PSRegionNodeWrapper.Type.START));
    }

    @Override
    public boolean hasNext() {
      return !nodeStack.isEmpty();
    }

    @Override
    public PSRegionNodeWrapper next() {
      var nodeWrapper = nodeStack.pop();
      var node = nodeWrapper.node;
      if (nodeWrapper.type == PSRegionNodeWrapper.Type.START) {
        nodeStack.push(new PSRegionNodeWrapper(nodeWrapper.node, PSRegionNodeWrapper.Type.END));
        if (node instanceof PSAbstractRegion) {
          var r = (PSAbstractRegion) node;
          if (r.getChildren() != null && !r.getChildren().isEmpty()) {
            var children = new ArrayList<>(r.getChildren());
            Collections.reverse(children);
            for (var child : children) {
              nodeStack.push(new PSRegionNodeWrapper(child, PSRegionNodeWrapper.Type.START));
            }
          }
        }
      }
      return nodeWrapper;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("remove is not yet supported");
    }
  }
}
