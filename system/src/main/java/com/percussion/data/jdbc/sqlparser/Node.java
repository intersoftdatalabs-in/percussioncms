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

package com.percussion.data.jdbc.sqlparser;

/* All AST nodes must implement this interface.  It provides basic
machinery for constructing the parent and child relationships
between nodes. */

public interface Node {

  /**
   * This method is called after the node has been made the current node. It indicates that child
   * nodes can now be added to it.
   */
  public void jjtOpen();

  /** This method is called after all the child nodes have been added. */
  public void jjtClose();

  /**
   * Informs the node of its parent in the AST.
   *
   * @param n the new parent node, may be <code>null</code> when detaching.
   */
  public void jjtSetParent(Node n);

  /**
   * Returns the parent of this node in the AST.
   *
   * @return the parent node, or <code>null</code> if this node has no parent.
   */
  public Node jjtGetParent();

  /**
   * Tells the node to add its argument to the node's list of children.
   *
   * @param n the child node to add, may not be <code>null</code>.
   * @param i the zero-based index at which to add the child.
   */
  public void jjtAddChild(Node n, int i);

  /**
   * Returns a child node. The children are numbered from zero, left to right.
   *
   * @param i the zero-based index of the child to return.
   * @return the child node at the requested index.
   */
  public Node jjtGetChild(int i);

  /**
   * Returns the number of children the node has.
   *
   * @return the number of children this node holds, never negative.
   */
  public int jjtGetNumChildren();

  /** Accept the visitor. * */
  public Object jjtAccept(SQLParserVisitor visitor, Object data);
}
