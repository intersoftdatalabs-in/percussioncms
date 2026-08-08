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
package com.percussion.cms.objectstore;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.cms.PSCmsException;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for typed property/column iterators on {@link PSDisplayFormat} (issue #2401
 * cms.objectstore rawtypes batch 2e).
 */
public class PSDisplayFormatGenericsTest {

  @Test
  public void testTypedPropertyApis() throws PSCmsException {
    PSDisplayFormat format = new PSDisplayFormat();
    format.setProperty("customProp", "alpha");
    format.setProperty("customProp", "beta", true);

    assertTrue(format.hasProperty("customProp"));
    assertTrue(format.doesPropertyHaveValue("customProp", "alpha"));
    assertTrue(format.doesPropertyHaveValue("customProp", "beta"));
    assertEquals("alpha", format.getPropertyValue("customProp"));

    Iterator<PSDFMultiProperty> props = format.getProperties();
    assertNotNull(props);
    boolean found = false;
    while (props.hasNext()) {
      PSDFMultiProperty prop = props.next();
      assertNotNull(prop.getName());
      if ("customProp".equalsIgnoreCase(prop.getName())) {
        found = true;
        assertTrue(prop.contains("alpha"));
        assertTrue(prop.contains("beta"));
      }
    }
    assertTrue(found, "customProp multiproperty must be present");

    format.removeProperty("customProp", "beta", true);
    assertFalse(format.doesPropertyHaveValue("customProp", "beta"));
    assertTrue(format.doesPropertyHaveValue("customProp", "alpha"));
  }

  @Test
  public void testTypedColumnIteratorIncludesSystemTitle() throws PSCmsException {
    PSDisplayFormat format = new PSDisplayFormat();

    Iterator<PSDisplayColumn> columns = format.getColumns();
    assertNotNull(columns);
    assertTrue(columns.hasNext(), "default format must include sys_title column");

    boolean foundTitle = false;
    while (columns.hasNext()) {
      PSDisplayColumn column = columns.next();
      assertNotNull(column.getSource());
      if ("sys_title".equalsIgnoreCase(column.getSource())) {
        foundTitle = true;
      }
    }
    assertTrue(foundTitle);

    int titleIndex = format.getColumnIndex("sys_title");
    assertTrue(titleIndex >= 0);
  }

  @Test
  public void testValidForFolderWithDefaultColumns() throws PSCmsException {
    PSDisplayFormat format = new PSDisplayFormat();
    // default has only sys_title → valid for folders and not for related content
    assertTrue(format.isValidForFolder());
    assertFalse(format.isValidForRelatedContent());
    assertTrue(format.isValidForViewsAndSearches());
  }
}
