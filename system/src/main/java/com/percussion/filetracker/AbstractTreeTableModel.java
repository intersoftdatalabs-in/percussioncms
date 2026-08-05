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

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

/**
 * An abstract implementation of the PSFUDTreeTableModel interface, handling the list of listeners.
 */
public abstract class AbstractTreeTableModel implements PSFUDTreeTableModel {
  protected Object root;
  protected EventListenerList listenerList = new EventListenerList();

  public AbstractTreeTableModel(Object root) {
    this.root = root;
  }

  //
  // Default implmentations for methods in the TreeModel interface.
  //

  /**
   * Default implementation for the method in the TreeModel interface.
   *
   * @return the root node of the tree, may be <code>null</code>.
   */
  public Object getRoot() {
    return root;
  }

  /**
   * Default implementation for the method in the TreeModel interface.
   *
   * @param node the node to test.
   * @return <code>true</code> when the supplied node has no children.
   */
  public boolean isLeaf(Object node) {
    return getChildCount(node) == 0;
  }

  /**
   * Default implementation for the method in the TreeModel interface.
   *
   * @param path the tree path whose value changed, may be <code>null</code>.
   * @param newValue the new value, may be <code>null</code>.
   */
  public void valueForPathChanged(TreePath path, Object newValue) {}

  /**
   * Default implementation for the method in the TreeModel interface.
   *
   * <p>This is not called in the JTree's default mode: use a native implementation.
   *
   * @param parent the parent node to search within, may not be <code>null</code>.
   * @param child the child node to locate.
   * @return the index of the child within the parent, or <code>-1</code> if not found.
   */
  public int getIndexOfChild(Object parent, Object child) {
    for (int i = 0; i < getChildCount(parent); i++) {
      if (getChild(parent, i).equals(child)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Add the TreeModelListener object to the model's listeners list
   *
   * @param l as TreeModelListener
   */
  public void addTreeModelListener(TreeModelListener l) {
    listenerList.add(TreeModelListener.class, l);
  }

  /**
   * Remove the specified TreeModelListener object from the listeners list
   *
   * @param l as TreeModelListener
   */
  public void removeTreeModelListener(TreeModelListener l) {
    listenerList.remove(TreeModelListener.class, l);
  }

  /**
   * Notify all listeners that have registered interest for notification on this event type. The
   * event instance is lazily created using the parameters passed into the fire method.
   *
   * @see EventListenerList
   * @param source the source of the event, may be <code>null</code>.
   * @param path the path to the parent of the modified nodes.
   * @param childIndices the indices of the modified children.
   * @param children the modified child nodes.
   */
  protected void fireTreeNodesChanged(
      Object source, Object[] path, int[] childIndices, Object[] children) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == TreeModelListener.class) {
        // Lazily create the event:
        if (e == null) e = new TreeModelEvent(source, path, childIndices, children);

        ((TreeModelListener) listeners[i + 1]).treeNodesChanged(e);
      }
    }
  }

  /**
   * Notify all listeners that have registered interest for notification on this event type. The
   * event instance is lazily created using the parameters passed into the fire method.
   *
   * @see EventListenerList
   * @param source the source of the event, may be <code>null</code>.
   * @param path the path to the parent of the inserted nodes.
   * @param childIndices the indices of the inserted children.
   * @param children the inserted child nodes.
   */
  protected void fireTreeNodesInserted(
      Object source, Object[] path, int[] childIndices, Object[] children) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == TreeModelListener.class) {
        // Lazily create the event:
        if (e == null) e = new TreeModelEvent(source, path, childIndices, children);
        ((TreeModelListener) listeners[i + 1]).treeNodesInserted(e);
      }
    }
  }

  /**
   * Notify all listeners that have registered interest for notification on this event type. The
   * event instance is lazily created using the parameters passed into the fire method.
   *
   * @see EventListenerList
   * @param source the source of the event, may be <code>null</code>.
   * @param path the path to the parent of the removed nodes.
   * @param childIndices the indices of the removed children.
   * @param children the removed child nodes.
   */
  protected void fireTreeNodesRemoved(
      Object source, Object[] path, int[] childIndices, Object[] children) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == TreeModelListener.class) {
        // Lazily create the event:
        if (e == null) e = new TreeModelEvent(source, path, childIndices, children);
        ((TreeModelListener) listeners[i + 1]).treeNodesRemoved(e);
      }
    }
  }

  /**
   * Notify all listeners that have registered interest for notification on this event type. The
   * event instance is lazily created using the parameters passed into the fire method.
   *
   * @see EventListenerList
   * @param source the source of the event, may be <code>null</code>.
   * @param path the path to the parent of the structurally changed nodes.
   * @param childIndices the indices of the affected children.
   * @param children the affected child nodes.
   */
  protected void fireTreeStructureChanged(
      Object source, Object[] path, int[] childIndices, Object[] children) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == TreeModelListener.class) {
        // Lazily create the event:
        if (e == null) e = new TreeModelEvent(source, path, childIndices, children);
        ((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
      }
    }
  }

  /**
   * Default implementation for methods in the PSFUDTreeTableModel interface.
   *
   * @param column the column index.
   * @return the class of values in the column, defaults to {@link Object}.
   */
  public Class getColumnClass(int column) {
    return Object.class;
  }

  /**
   * By default, make the column with the Tree in it the only editable one. Making this column
   * editable causes the JTable to forward mouse and keyboard events in the Tree column to the
   * underlying JTree.
   */
  public boolean isCellEditable(Object node, int column) {
    return getColumnClass(column) == PSFUDTreeTableModel.class;
  }

  /**
   * Default implementation for the {@link PSFUDTreeTableModel#setValueAt(Object, Object, int)}
   * method. Subclasses should override when column values are editable.
   *
   * @param aValue the new value, may be <code>null</code>.
   * @param node the node being edited, may be <code>null</code>.
   * @param column the column index being edited.
   */
  public void setValueAt(Object aValue, Object node, int column) {}

  // Left to be implemented in the subclass:

  /*
   *   public Object getChild(Object parent, int index)
   *   public int getChildCount(Object parent)
   *   public int getColumnCount()
   *   public String getColumnName(Object node, int column)
   *   public Object getValueAt(Object node, int column)
   */

}
