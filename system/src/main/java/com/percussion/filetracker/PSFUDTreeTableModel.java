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

package com.percussion.filetracker;

import javax.swing.tree.TreeModel;

/**
 * PSFUDTreeTableModel is the model used by a PSFUDJTreeTable. It extends TreeModel to add methods
 * for getting inforamtion about the set of columns each node in the PSFUDTreeTableModel may have.
 * Each column, like a column in a TableModel, has a name and a type associated with it. Each node
 * in the PSFUDTreeTableModel can return a value for each of the columns and set that value if
 * isCellEditable() returns true.
 */
public interface PSFUDTreeTableModel extends TreeModel {
  /**
   * Returns the number of available columns.
   *
   * @return the number of columns, never negative.
   */
  public int getColumnCount();

  /**
   * Returns the name for column number <code>column</code>.
   *
   * @param column the column index.
   * @return the column name, may be <code>null</code>.
   */
  public String getColumnName(int column);

  /**
   * Returns the type for column number <code>column</code>.
   *
   * @param column the column index.
   * @return the {@link Class} of values stored in the column.
   */
  public Class getColumnClass(int column);

  /**
   * Returns the value to be displayed for node <code>node</code>, at column number <code>column
   * </code>.
   *
   * @param node the tree node, may not be <code>null</code>.
   * @param column the column index.
   * @return the displayed value, may be <code>null</code>.
   */
  public Object getValueAt(Object node, int column);

  /**
   * Indicates whether the value for node <code>node</code>, at column number <code>column</code> is
   * editable.
   *
   * @param node the tree node, may not be <code>null</code>.
   * @param column the column index.
   * @return <code>true</code> when the cell can be edited.
   */
  public boolean isCellEditable(Object node, int column);

  /**
   * Sets the value for node <code>node</code>, at column number <code>column</code>.
   *
   * @param aValue the new value, may be <code>null</code>.
   * @param node the tree node, may not be <code>null</code>.
   * @param column the column index.
   */
  public void setValueAt(Object aValue, Object node, int column);
}
