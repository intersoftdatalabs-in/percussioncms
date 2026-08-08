/*
 * Copyright (c) 2023 Intersoft Data Labs, Inc.
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

package com.percussion.cx;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.cms.objectstore.PSFolderProperty;
import com.percussion.cx.guitools.UTPropertiesTablePanel;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

/**
 * Properties Panel on the Cx Folder Properties Dialog. Allows users to change custom folder
 * properties.
 */
@SuppressWarnings("serial")
public class PSFolderPropertiesPanel extends UTPropertiesTablePanel {
  /**
   * The only constructor.
   *
   * @param folder shared instance of the PSFolder, never <code>null</code>.
   * @param editable <code>true</code> if any data can be entered, <code>false</code> otherwise.
   */
  /**
   * The only constructor.
   *
   * @param folder shared instance of the PSFolder, never <code>null</code>.
   * @param editable <code>true</code> if any data can be entered, <code>false</code> otherwise.
   * @param applet the content explorer applet, may not be <code>null</code>.
   */
  public PSFolderPropertiesPanel(
      PSFolder folder, boolean editable, PSContentExplorerApplet applet) {
    if (folder == null) throw new IllegalArgumentException("folder may not be null");

    if (applet == null) throw new IllegalArgumentException("applet may not be null");

    m_folder = folder;
    m_editable = editable;
    m_applet = applet;

    String columnNames[] =
        new String[] {
          m_applet.getResourceString(getClass(), "Name"),
          m_applet.getResourceString(getClass(), "Value"),
          m_applet.getResourceString(getClass(), "Description")
        };

    // Add focus highlights
    PSDisplayOptions dispOptions =
        (PSDisplayOptions) UIManager.getDefaults().get(PSContentExplorerConstants.DISPLAY_OPTIONS);
    setTableCellFocusColor(dispOptions.getFocusColor());
    setTableUseFocusHighlight(true);

    // init properties table layout
    init(columnNames, 3, m_editable);

    setCellEditor();

    setScrollPaneSize(new Dimension(0, 250));

    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    loadTableData();
  }

  /** Cell editor for the custom properties table. */
  private void setCellEditor() {
    JTable table = getTable();

    // create the combo box editor with the known property name,
    // sys_pubFilename
    JComboBox<String> comboBox = new JComboBox<>(new String[] {PSFolder.PROPERTY_PUB_FILE_NAME});
    comboBox.setEditable(true);
    DefaultCellEditor editor = new DefaultCellEditor(comboBox);

    // assign the editor to the 1st column
    TableColumnModel tcm = table.getColumnModel();
    tcm.getColumn(0).setCellEditor(editor);
  }

  /** loads folder properties into the table. */
  private void loadTableData() {
    Iterator<?> iter = m_folder.getProperties();

    DefaultTableModel model = (DefaultTableModel) getTableModel();

    clearAllRows();

    while (iter.hasNext()) {
      Object next = iter.next();
      if (!(next instanceof PSFolderProperty)) {
        continue;
      }
      PSFolderProperty property = (PSFolderProperty) next;

      // skip properties which are handled in other tab's
      if (PSFolder.isDisplayFormatProperty(property)
          || PSFolder.isFolderPublishProperty(property)
          || PSFolder.isFolderGlobalTemplateProperty(property)) continue;

      Vector<String> vRow = new Vector<>();

      vRow.add(property.getName());
      vRow.add(property.getValue());
      vRow.add(property.getDescription());

      model.addRow(vRow);

      // remember loaded properties
      m_mapLoadedProperties.put(property.getName(), property);
    }

    for (int i = 0; i < 3; i++) addRow(); // always add several empty rows
  }

  /**
   * Folder Dialog calls this method when user pushes OK button to save changes. Depending on the
   * folder ACL and user permissions the data may or may not be saved.
   *
   * @return <code>true</code> if success, <code>false</code> otherwise.
   */
  public boolean onOk() {
    if (!m_editable) return false;

    if (!validateData()) return false;

    DefaultTableModel model = (DefaultTableModel) getTableModel();

    @SuppressWarnings("unchecked")
    Vector<Vector> vRows = model.getDataVector();

    for (int i = 0; i < vRows.size(); i++) {
      Vector<?> vColumns = vRows.elementAt(i);

      String name = stringCell(vColumns, 0);
      String value = stringCell(vColumns, 1);
      String desc = stringCell(vColumns, 2);

      if (name == null || name.trim().length() <= 0) continue;

      if (value == null) continue;

      PSFolderProperty prop = new PSFolderProperty(name, value, desc);

      // skip properties which are handled in other tab's
      if (PSFolder.isDisplayFormatProperty(prop)
          || PSFolder.isFolderPublishProperty(prop)
          || PSFolder.isFolderGlobalTemplateProperty(prop)) continue;

      m_folder.setProperty(prop);

      // once set we don't need to remember about it anymore
      m_mapLoadedProperties.remove(name);
    }

    // if any properties were removed, remove them from the PSFolder
    if (!m_mapLoadedProperties.isEmpty()) {
      for (Map.Entry<String, PSFolderProperty> entry : m_mapLoadedProperties.entrySet()) {
        // delete this property
        m_folder.deleteProperty(entry.getKey());
      }
    }

    return true;
  }

  /**
   * Reads a table cell as a string (null-safe). Package-visible for unit tests.
   *
   * @param row row vector, may be <code>null</code>
   * @param index column index
   * @return string value or <code>null</code>
   */
  static String stringCell(Vector<?> row, int index) {
    if (row == null || index < 0 || index >= row.size()) {
      return null;
    }
    Object cell = row.elementAt(index);
    return cell == null ? null : cell.toString();
  }

  /**
   * <code>true</code> indicates that user can make and save modifications to any control on this
   * panel, <code>false</code> otherwise.
   */
  private boolean m_editable;

  /**
   * The folder to create or edit, initialized in the ctor and modified in <code>onOk()</code> as
   * per user selections. Never <code>null</code> after that.
   */
  private PSFolder m_folder;

  /**
   * Remembers which properties where loaded into the table. On save allows to detect and delete
   * properties that were removed.
   */
  private Map<String, PSFolderProperty> m_mapLoadedProperties = new HashMap<>();

  /** A reference back to the applet. */
  private PSContentExplorerApplet m_applet;
}
