/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cx.objectstore.PSNode;
import com.percussion.util.PSEntrySet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed {@link PSDisplayFormatTableModel} after rawtypes cleanup (#2875).
 * Uses headless applet; does not exercise live ActionManager / display-format catalog.
 */
public class PSDisplayFormatTableModelTest {

  @Test
  public void constructorRejectsNullApplet() {
    assertThrows(IllegalArgumentException.class, () -> new PSDisplayFormatTableModel(null));
  }

  @Test
  public void setRootRejectsNull() {
    PSDisplayFormatTableModel model = newModel();
    assertThrows(IllegalArgumentException.class, () -> model.setRoot(null));
  }

  @Test
  public void getRootNullUntilSet() {
    PSDisplayFormatTableModel model = newModel();
    assertNull(model.getRoot());
  }

  @Test
  public void getSysTitleIndexNullNode() {
    assertEquals(-1, PSDisplayFormatTableModel.getSysTitleIndex(null, null));
  }

  @Test
  public void getSysTitleIndexMissingDisplayFormatId() {
    PSNode node = folder("p", "Parent");
    assertEquals(-1, PSDisplayFormatTableModel.getSysTitleIndex(node, null));
  }

  @Test
  public void getDataOnEmptyModelReturnsEmptyIterator() {
    PSDisplayFormatTableModel model = newModel();
    Iterator<Object> data = model.getData();
    assertFalse(data.hasNext());
  }

  @Test
  public void setRootWithoutDisplayFormatUsesNameColumnAndNodeData() {
    PSDisplayFormatTableModel model = newModel();
    PSNode parent = folder("p", "Parent");
    PSNode child = item("c1", "Child One");
    parent.addChild(child);

    model.setRoot(parent);

    assertSame(parent, model.getRoot());
    assertEquals(1, model.getRowCount());
    assertEquals(1, model.getColumnCount());
    assertSame(child, model.getData(0));
    assertFalse(model.isCellEditable(0, 0));

    Iterator<Object> data = model.getData();
    assertTrue(data.hasNext());
    assertSame(child, data.next());
    assertFalse(data.hasNext());
  }

  @Test
  public void setRootWithDisplayFormatConvertsNumberAndTextColumns() {
    PSDisplayFormatTableModel model = newModel();
    PSNode parent = folder("p", "Parent");

    List<Map.Entry<String, String>> defs = new ArrayList<>();
    defs.add(new PSEntrySet<>("sys_contentid", PSNode.DATA_TYPE_NUMBER));
    defs.add(new PSEntrySet<>("sys_title", PSNode.DATA_TYPE_TEXT));
    parent.setChildrenDisplayFormat(defs.iterator());

    PSNode child = item("c1", "Child One");
    Map<String, Object> row = new HashMap<>();
    row.put("sys_contentid", "4242");
    row.put("sys_title", "Hello");
    child.setRowData(row);
    parent.addChild(child);

    model.setRoot(parent);

    // default Name column + 2 display format columns
    assertEquals(3, model.getColumnCount());
    assertEquals(1, model.getRowCount());
    assertSame(child, model.getData(0));
    assertEquals(Integer.valueOf(4242), model.getValueAt(0, 1));
    assertEquals("Hello", model.getValueAt(0, 2));
    assertEquals(Integer.class, model.getColumnClass(1));
    assertEquals(String.class, model.getColumnClass(2));
  }

  @Test
  public void convertCellValueNumberAndNonNumeric() {
    assertEquals(Integer.valueOf(7), PSDisplayFormatTableModel.convertCellValue("7", PSNode.DATA_TYPE_NUMBER));
    assertEquals("abc", PSDisplayFormatTableModel.convertCellValue("abc", PSNode.DATA_TYPE_NUMBER));
    assertNull(PSDisplayFormatTableModel.convertCellValue(null, PSNode.DATA_TYPE_NUMBER));
    assertEquals("x", PSDisplayFormatTableModel.convertCellValue("x", PSNode.DATA_TYPE_TEXT));
  }

  @Test
  public void convertCellValueDateParsesString() {
    Object converted =
        PSDisplayFormatTableModel.convertCellValue("2020-01-15 00:00:00.000", PSNode.DATA_TYPE_DATE);
    assertNotNull(converted);
    assertInstanceOf(Date.class, converted);
  }

  @Test
  public void setLocaleEmptyUsesDefault() {
    PSDisplayFormatTableModel model = newModel();
    model.setLocale(null);
    assertNotNull(model.getLocale());
    model.setLocale("");
    assertNotNull(model.getLocale());
  }

  private static PSDisplayFormatTableModel newModel() {
    return new PSDisplayFormatTableModel(new PSContentExplorerApplet(true));
  }

  private static PSNode folder(String name, String label) {
    return new PSNode(name, label, PSNode.TYPE_FOLDER, "url", null, false, 1);
  }

  private static PSNode item(String name, String label) {
    return new PSNode(name, label, PSNode.TYPE_ITEM, "", null, false, -1);
  }
}
