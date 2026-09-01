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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.StringReader;
import java.util.Iterator;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Regression for GH-2712: Explorer Search failed while deserializing default {@code View_All}
 * because {@link PSSFields}/{@link PSSProperties} Element ctors expected class-derived roots
 * ({@code PSXSFields}/{@code PSXSProperties}) after the this-escape residual (#2612) instead of the
 * wire names {@code PSX_FIELDS}/{@code PSX_PROPERTIES}.
 */
public class PSSearchXmlRoundTripTest {

  /**
   * Minimal View_All-shaped document matching server XML from issue #2712 / getSearches result
   * (SEARCHID=3, INTERNALNAME=View_All, PSX_FIELDS + PSX_PROPERTIES).
   */
  private static final String VIEW_ALL_XML =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
          + "<PSXSearch state=\"db_unmodified\">\n"
          + "   <PSXKey isPersisted=\"yes\">\n"
          + "      <SEARCHID>3</SEARCHID>\n"
          + "   </PSXKey>\n"
          + "   <DISPLAYNAME>All</DISPLAYNAME>\n"
          + "   <DISPLAYFORMAT>1</DISPLAYFORMAT>\n"
          + "   <PARENTCATEGORY>3</PARENTCATEGORY>\n"
          + "   <TYPE>View</TYPE>\n"
          + "   <MAXIMUMITEMS>-1</MAXIMUMITEMS>\n"
          + "   <CASESENSITIVE>0</CASESENSITIVE>\n"
          + "   <INTERNALNAME>View_All</INTERNALNAME>\n"
          + "   <DESCRIPTION>All content for all communities</DESCRIPTION>\n"
          + "   <VERSION>0</VERSION>\n"
          + "   <PSX_FIELDS className=\"com.percussion.cms.objectstore.PSSearchField\""
          + " ordered=\"yes\" state=\"db_unmodified\">\n"
          + "      <PSXSearchField state=\"db_unmodified\">\n"
          + "         <PSXKey isPersisted=\"yes\">\n"
          + "            <FIELDNAME>sys_contentid</FIELDNAME>\n"
          + "            <SEARCHID>3</SEARCHID>\n"
          + "         </PSXKey>\n"
          + "         <FIELDTYPE>Number</FIELDTYPE>\n"
          + "         <FIELDVALUE>0</FIELDVALUE>\n"
          + "         <FIELDDESCRIPTION>All content</FIELDDESCRIPTION>\n"
          + "         <FIELDLABEL>ContentId</FIELDLABEL>\n"
          + "         <OPERATOR>greaterthan</OPERATOR>\n"
          + "         <EXTOPERATOR/>\n"
          + "      </PSXSearchField>\n"
          + "   </PSX_FIELDS>\n"
          + "   <PSX_PROPERTIES className=\"com.percussion.cms.objectstore.PSSearchMultiProperty\""
          + " ordered=\"no\" state=\"db_unmodified\">\n"
          + "      <PSXSearchMultiProperty className=\"com.percussion.cms.objectstore.PSSProperty\""
          + " ordered=\"no\" propName=\"aadNewSearch\" state=\"db_unmodified\">\n"
          + "         <PSXSProperty propName=\"aadNewSearch\" state=\"db_unmodified\">\n"
          + "            <PSXKey isPersisted=\"yes\" needGenerateId=\"no\">\n"
          + "               <PROPERTYNAME>aadNewSearch</PROPERTYNAME>\n"
          + "               <PROPERTYVALUE>n</PROPERTYVALUE>\n"
          + "               <SEARCHID>3</SEARCHID>\n"
          + "            </PSXKey>\n"
          + "            <Value>n</Value>\n"
          + "            <Description/>\n"
          + "         </PSXSProperty>\n"
          + "      </PSXSearchMultiProperty>\n"
          + "   </PSX_PROPERTIES>\n"
          + "   <CUSTOMURL/>\n"
          + "</PSXSearch>\n";

  @Test
  public void pssFieldsElementCtorAcceptsWireRootPsxFields() throws Exception {
    Document doc =
        PSXmlDocumentBuilder.createXmlDocument(new StringReader(VIEW_ALL_XML), false);
    Element searchRoot = doc.getDocumentElement();
    Element fieldsEl =
        (Element) searchRoot.getElementsByTagName(PSSFields.XML_NODE_NAME).item(0);
    assertNotNull(fieldsEl);
    assertEquals(PSSFields.XML_NODE_NAME, fieldsEl.getNodeName());

    PSSFields fields = new PSSFields(fieldsEl);
    assertEquals(1, fields.size());
    PSSearchField field = (PSSearchField) fields.get(0);
    assertEquals("sys_contentid", field.getFieldName());
  }

  @Test
  public void pssPropertiesElementCtorAcceptsWireRootPsxProperties() throws Exception {
    Document doc =
        PSXmlDocumentBuilder.createXmlDocument(new StringReader(VIEW_ALL_XML), false);
    Element searchRoot = doc.getDocumentElement();
    Element propsEl =
        (Element) searchRoot.getElementsByTagName(PSSProperties.XML_NODE_NAME).item(0);
    assertNotNull(propsEl);
    assertEquals(PSSProperties.XML_NODE_NAME, propsEl.getNodeName());

    PSSProperties props = new PSSProperties(propsEl);
    assertEquals(1, props.size());
  }

  /**
   * Merged getSearches.xml StandardSearch row (Default_Search) must deserialize
   * as a non-view so {@code findSearches} can catalog UI-06 creates.
   */
  private static final String DEFAULT_SEARCH_XML =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
          + "<PSXSearch state=\"db_unmodified\">\n"
          + "   <PSXKey isPersisted=\"yes\">\n"
          + "      <SEARCHID>0</SEARCHID>\n"
          + "   </PSXKey>\n"
          + "   <DISPLAYNAME>Default CX New Search</DISPLAYNAME>\n"
          + "   <DISPLAYFORMAT>0</DISPLAYFORMAT>\n"
          + "   <PARENTCATEGORY>5</PARENTCATEGORY>\n"
          + "   <TYPE>StandardSearch</TYPE>\n"
          + "   <MAXIMUMITEMS>200</MAXIMUMITEMS>\n"
          + "   <CASESENSITIVE>0</CASESENSITIVE>\n"
          + "   <INTERNALNAME>Default_Search</INTERNALNAME>\n"
          + "   <DESCRIPTION>Content Explorer Search</DESCRIPTION>\n"
          + "   <VERSION>0</VERSION>\n"
          + "</PSXSearch>\n";

  @Test
  public void standardSearchXmlIsNotAView() throws Exception {
    Document doc =
        PSXmlDocumentBuilder.createXmlDocument(new StringReader(DEFAULT_SEARCH_XML), false);
    PSSearch search = new PSSearch(doc.getDocumentElement());
    assertEquals("Default_Search", search.getInternalName());
    assertEquals(PSSearch.TYPE_STANDARDSEARCH, search.getType());
    assertFalse(search.isView());
    assertTrue(search.isStandardSearch());
  }

  @Test
  public void viewAllXmlDeserializesViaPsSearchElementCtor() throws Exception {
    Document doc =
        PSXmlDocumentBuilder.createXmlDocument(new StringReader(VIEW_ALL_XML), false);
    PSSearch search = new PSSearch(doc.getDocumentElement());

    assertEquals("View_All", search.getInternalName());
    assertEquals("All", search.getDisplayName());
    assertTrue(search.isView());
    assertEquals(3, search.getParentCategory());

    Iterator<PSSearchField> fields = search.getFields();
    assertTrue(fields.hasNext());
    PSSearchField field = fields.next();
    assertEquals("sys_contentid", field.getFieldName());
    assertEquals(PSSearchField.TYPE_NUMBER, field.getFieldType());
    assertFalse(fields.hasNext());
  }

  @Test
  public void searchRoundTripPreservesWireFieldAndPropertyRoots() throws Exception {
    Document doc =
        PSXmlDocumentBuilder.createXmlDocument(new StringReader(VIEW_ALL_XML), false);
    PSSearch original = new PSSearch(doc.getDocumentElement());

    Document out = PSXmlDocumentBuilder.createXmlDocument();
    Element xml = original.toXml(out);
    assertNotNull(xml.getElementsByTagName(PSSFields.XML_NODE_NAME).item(0));
    assertNotNull(xml.getElementsByTagName(PSSProperties.XML_NODE_NAME).item(0));

    PSSearch restored = new PSSearch(xml);
    assertEquals(original.getInternalName(), restored.getInternalName());
    assertEquals(original.getDisplayName(), restored.getDisplayName());
    assertEquals(original.isView(), restored.isView());

    Iterator<PSSearchField> fields = restored.getFields();
    assertTrue(fields.hasNext());
    assertEquals("sys_contentid", fields.next().getFieldName());
  }
}
