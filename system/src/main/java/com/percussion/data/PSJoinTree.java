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

package com.percussion.data;

import com.percussion.design.objectstore.PSBackEndJoin;
import com.percussion.design.objectstore.PSBackEndTable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The PSJoinTree class is used to determine what the connections are
 * between the tables being joined.
 *
 * @author      Tas Giakouminakis
 * @version    1.0
 * @since      1.0
 */
public class PSJoinTree {
  public PSJoinTree(List<PSBackEndJoin> joins) {
    super();
    if (joins == null) {
      return;
    }
    int joinCount = joins.size();
    for (int i = 0; i < joinCount; i++) {
      PSBackEndJoin join = joins.get(i);
      // store the tree node for each table in the map
      PSBackEndTable ltab = join.getLeftColumn().getTable();
      PSBackEndTable rtab = join.getRightColumn().getTable();

      PSJoinTreeTableNode lnode = m_nodeMap.get(ltab);
      if (lnode == null) {
        lnode = new PSJoinTreeTableNode(ltab);
        m_nodeMap.put(ltab, lnode);
      }

      PSJoinTreeTableNode rnode = m_nodeMap.get(rtab);
      if (rnode == null) {
        rnode = new PSJoinTreeTableNode(rtab);
        m_nodeMap.put(rtab, rnode);
      }

      lnode.addJoin(join, rnode);
      rnode.addJoin(join, lnode);

      // if we don't have a root yet, make the first left node the root
      if (m_root == null) m_root = lnode;
    }
  }

  public PSJoinTreeTableNode getRoot() {
    return m_root;
  }

  public PSJoinTreeTableNode getTableNode(PSBackEndTable tab) {
    return (PSJoinTreeTableNode) m_nodeMap.get(tab);
  }

  public boolean hasRoute(PSBackEndTable tab1, PSBackEndTable tab2) {
    // keeping a full list of visited nodes to avoid recursion problems
    List<PSJoinTreeTableNode> visited = new ArrayList<>();
    PSJoinTreeTableNode node = m_nodeMap.get(tab1);
    return node.hasRoute(tab2, visited);
  }

  class PSJoinTreeTableNode {
    PSJoinTreeTableNode(PSBackEndTable nodeTable) {
      super();
      m_table = nodeTable;
    }

    PSBackEndTable getTable() {
      return m_table;
    }

    void addJoin(PSBackEndJoin join, PSJoinTreeTableNode joinNode) {
      PSJoinTreeTableNode l, r;
      if (join.getLeftColumn().getTable().equals(m_table)) {
        l = this;
        r = joinNode;
      } else {
        l = joinNode;
        r = this;
      }
      m_joins.add(new PSJoinTreeJoinNode(join, l, r));
    }

    PSJoinTreeJoinNode getJoin(PSBackEndJoin join) {
      int size = m_joins.size();
      for (int i = 0; i < size; i++) {
        PSJoinTreeJoinNode node = m_joins.get(i);
        if (join.equals(node.getJoin())) return node;
      }
      return null;
    }

    List<PSJoinTreeJoinNode> getJoins() {
      return m_joins;
    }

    boolean hasRoute(PSBackEndTable tab, List<PSJoinTreeTableNode> visited) {
      // keeping a full list of visited nodes to avoid recursion problems
      if (visited.contains(this)) return false;
      try {
        visited.add(this);
        // recursively check for a link
        for (int i = 0; i < m_joins.size(); i++) {
          PSJoinTreeJoinNode j = m_joins.get(i);
          PSJoinTreeTableNode t = j.getLeftTableNode();
          if (t.m_table.equals(tab)) return true;
          if (t.hasRoute(tab, visited)) return true;
          t = j.getRightTableNode();
          if (t.m_table.equals(tab)) return true;
          if (t.hasRoute(tab, visited)) return true;
        }
        return false; // no luck if we've gotten here
      } finally {
        visited.remove(this);
      }
    }

    private PSBackEndTable m_table;
    private List<PSJoinTreeJoinNode> m_joins = new ArrayList<>();
  }

  class PSJoinTreeJoinNode {
    PSJoinTreeJoinNode(
        PSBackEndJoin join, PSJoinTreeTableNode leftNode, PSJoinTreeTableNode rightNode) {
      super();
      m_join = join;
      m_leftNode = leftNode;
      m_rightNode = rightNode;
    }

    PSJoinTreeTableNode getLeftTableNode() {
      return m_leftNode;
    }

    PSJoinTreeTableNode getRightTableNode() {
      return m_rightNode;
    }

    PSBackEndJoin getJoin() {
      return m_join;
    }

    private PSBackEndJoin m_join;
    private PSJoinTreeTableNode m_leftNode;
    private PSJoinTreeTableNode m_rightNode;
  }

  private Map<PSBackEndTable, PSJoinTreeTableNode> m_nodeMap = new HashMap<>();
  private PSJoinTreeTableNode m_root = null;
}
