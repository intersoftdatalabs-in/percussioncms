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
package com.percussion.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Behavioral coverage for typed {@link PSCleanFolderSearchResultsExit#processRows} (#2386).
 */
public class PSCleanFolderSearchResultsExitTypedTest {

  @Test
  public void processRowsClearsDisallowedFolderColumns() throws Exception {
    PSCleanFolderSearchResultsExit exit = new PSCleanFolderSearchResultsExit();

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element rowEl = doc.createElement(PSSearchResultRow.XML_NODE_NAME);
    rowEl.appendChild(column(doc, "sys_contentid", "10"));
    rowEl.appendChild(column(doc, "sys_title", "My Folder"));
    rowEl.appendChild(column(doc, "sys_contenttypeid", String.valueOf(PSFolder.FOLDER_CONTENT_TYPE_ID)));
    rowEl.appendChild(column(doc, "sys_workflowid", "5"));
    rowEl.appendChild(column(doc, "sys_communityid", "1001"));

    PSSearchResultRow row = new PSSearchResultRow(rowEl);
    List<Object> rows = new ArrayList<>();
    rows.add(row);

    List<Object> result = exit.processRows(new Object[0], rows, null);
    assertEquals(1, result.size());
    IPSSearchResultRow out = (IPSSearchResultRow) result.get(0);
    assertEquals("10", out.getColumnValue("sys_contentid"));
    assertEquals("My Folder", out.getColumnValue("sys_title"));
    assertEquals(String.valueOf(PSFolder.FOLDER_CONTENT_TYPE_ID), out.getColumnValue("sys_contenttypeid"));
    // workflow id is not in allowed folder fields — cleared
    assertEquals("", out.getColumnValue("sys_workflowid"));
    // community id is allowed for folders
    assertEquals("1001", out.getColumnValue("sys_communityid"));
  }

  @Test
  public void processRowsLeavesNonFolderRowsAlone() throws Exception {
    PSCleanFolderSearchResultsExit exit = new PSCleanFolderSearchResultsExit();

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element rowEl = doc.createElement(PSSearchResultRow.XML_NODE_NAME);
    rowEl.appendChild(column(doc, "sys_contentid", "11"));
    rowEl.appendChild(column(doc, "sys_title", "Page"));
    rowEl.appendChild(column(doc, "sys_contenttypeid", "1"));
    rowEl.appendChild(column(doc, "sys_workflowid", "5"));

    PSSearchResultRow row = new PSSearchResultRow(rowEl);
    List<Object> rows = new ArrayList<>();
    rows.add(row);

    List<Object> result = exit.processRows(new Object[0], rows, null);
    IPSSearchResultRow out = (IPSSearchResultRow) result.get(0);
    assertEquals("5", out.getColumnValue("sys_workflowid"));
    assertTrue(out.hasColumn("sys_workflowid"));
  }

  private static Element column(Document doc, String name, String value) {
    Element col = doc.createElement(PSSearchResultColumn.XML_NODE_NAME);
    col.setAttribute(PSSearchResultColumn.ATTR_NAME, name);
    col.setAttribute(PSSearchResultColumn.ATTR_DISPLAY_VALUE, value);
    col.appendChild(doc.createTextNode(value));
    return col;
  }
}
