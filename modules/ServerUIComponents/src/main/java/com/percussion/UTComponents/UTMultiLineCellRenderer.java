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
package com.percussion.UTComponents;

import java.awt.*;
import java.io.Serializable;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

/** Cell renderer used when a {@link JTextArea} object is stored within a table or list cell. */
@SuppressWarnings({"rawtypes", "unchecked", "this-escape"})
public class UTMultiLineCellRenderer extends JTextArea
    implements TableCellRenderer, ListCellRenderer, Serializable {

  private static final long serialVersionUID = 1L;

  /** Constructs the renderer with line wrap, word-wrap, and a small empty border configured. */
  public UTMultiLineCellRenderer() {
    super();
    noFocusBorder = new EmptyBorder(1, 2, 1, 2);
    setLineWrap(true);
    setWrapStyleWord(true);
    setOpaque(true);
    setBorder(noFocusBorder);
  }

  /**
   * Sets the unselected foreground color as well as delegating to the superclass.
   *
   * @param c the new foreground color; may be {@code null}.
   */
  public void setForeground(Color c) {
    super.setForeground(c);
    unselectedForeground = c;
  }

  /**
   * Sets the unselected background color as well as delegating to the superclass.
   *
   * @param c the new background color; may be {@code null}.
   */
  public void setBackground(Color c) {
    super.setBackground(c);
    unselectedBackground = c;
  }

  /** Resets the foreground and background colors when the look-and-feel is updated. */
  public void updateUI() {
    super.updateUI();
    setForeground(null);
    setBackground(null);
  }

  /**
   * Renders the supplied table cell value using this renderer's colors and border.
   *
   * @param table the target table; must not be {@code null}.
   * @param value the cell value; may be {@code null}.
   * @param isSelected {@code true} if the cell is selected.
   * @param hasFocus {@code true} if the cell has focus.
   * @param row the cell row.
   * @param column the cell column.
   * @return this renderer.
   */
  public Component getTableCellRendererComponent(
      JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    if (isSelected) {
      super.setForeground(table.getSelectionForeground());
      super.setBackground(table.getSelectionBackground());
    } else {
      super.setForeground(
          (unselectedForeground != null) ? unselectedForeground : table.getForeground());
      super.setBackground(
          (unselectedBackground != null) ? unselectedBackground : table.getBackground());
    }

    setFont(table.getFont());

    if (hasFocus) {
      setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
      if (table.isCellEditable(row, column)) {
        super.setForeground(UIManager.getColor("Table.focusCellForeground"));
        super.setBackground(UIManager.getColor("Table.focusCellBackground"));
      }
    } else {
      setBorder(noFocusBorder);
    }

    setValue(value);

    return this;
  }

  /**
   * Renders the supplied list cell value using this renderer's colors and border.
   *
   * @param list the target list; must not be {@code null}.
   * @param value the cell value; may be {@code null}.
   * @param index the cell index.
   * @param isSelected {@code true} if the cell is selected.
   * @param cellHasFocus {@code true} if the cell has focus.
   * @return this renderer.
   */
  public Component getListCellRendererComponent(
      JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    if (isSelected) {
      super.setForeground(list.getSelectionForeground());
      super.setBackground(list.getSelectionBackground());
    } else {
      super.setForeground(
          (unselectedForeground != null) ? unselectedForeground : list.getForeground());
      super.setBackground(
          (unselectedBackground != null) ? unselectedBackground : list.getBackground());
    }

    setFont(list.getFont());

    if (cellHasFocus) {
      setBorder(UIManager.getBorder("List.focusCellHighlightBorder"));
    } else {
      setBorder(noFocusBorder);
    }

    setValue(value);

    return this;
  }

  /**
   * Renders the supplied value into the text area.
   *
   * @param value the value to render; may be {@code null}, in which case the empty string is
   *     rendered.
   */
  protected void setValue(Object value) {
    setText((value == null) ? "" : value.toString());
  }

  /**
   * {@link UIResource} variant of the renderer used by the Swing pluggable look-and-feel
   * architecture.
   */
  public static class UIResource extends UTMultiLineCellRenderer
      implements javax.swing.plaf.UIResource {

    private static final long serialVersionUID = 1L;
  }

  protected static Border noFocusBorder;

  private Color unselectedForeground;
  private Color unselectedBackground;
}
