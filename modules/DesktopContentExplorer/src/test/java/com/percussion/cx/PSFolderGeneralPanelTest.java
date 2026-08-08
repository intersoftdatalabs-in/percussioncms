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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSDisplayFormat;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pure comparator helper for {@link PSFolderGeneralPanel} after rawtypes cleanup. */
public class PSFolderGeneralPanelTest {

  @Test
  public void displayFormatByNameOrdersByToString() throws PSCmsException {
    PSDisplayFormat a = new PSDisplayFormat();
    a.setName("Alpha");
    PSDisplayFormat z = new PSDisplayFormat();
    z.setName("Zulu");

    List<PSDisplayFormat> formats = new ArrayList<>();
    formats.add(z);
    formats.add(a);
    formats.sort(PSFolderGeneralPanel.DISPLAY_FORMAT_BY_NAME);

    assertTrue(
        formats.get(0).toString().compareTo(formats.get(1).toString()) <= 0,
        "display formats should sort by toString()");
  }

  @Test
  public void displayFormatByNameNullElementsLastNoNpe() throws PSCmsException {
    PSDisplayFormat a = new PSDisplayFormat();
    a.setName("Alpha");
    PSDisplayFormat z = new PSDisplayFormat();
    z.setName("Zulu");

    List<PSDisplayFormat> formats = new ArrayList<>();
    formats.add(null);
    formats.add(z);
    formats.add(null);
    formats.add(a);
    formats.sort(PSFolderGeneralPanel.DISPLAY_FORMAT_BY_NAME);

    assertEquals("Alpha", formats.get(0).getName());
    assertEquals("Zulu", formats.get(1).getName());
    assertNull(formats.get(2));
    assertNull(formats.get(3));
    assertEquals(0, PSFolderGeneralPanel.DISPLAY_FORMAT_BY_NAME.compare(null, null));
    assertTrue(PSFolderGeneralPanel.DISPLAY_FORMAT_BY_NAME.compare(a, null) < 0);
    assertTrue(PSFolderGeneralPanel.DISPLAY_FORMAT_BY_NAME.compare(null, a) > 0);
  }
}
