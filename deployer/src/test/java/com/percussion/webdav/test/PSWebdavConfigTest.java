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
package com.percussion.webdav.test;

import static org.junit.jupiter.api.Assertions.*;

import com.intsof.percussioncms.auditlog.codes.WebdavErrorCodes;
import com.percussion.webdav.error.PSWebdavException;
import com.percussion.webdav.objectstore.IPSRxWebDavDTD;
import com.percussion.webdav.objectstore.PSWebdavConfigDef;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.InputStream;
import java.util.Iterator;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Unit test class for the <code>PSWebdavConfig</code> class. */
public class PSWebdavConfigTest {

  /**
   * Test loading a good configuration
   *
   * @throws Exception for any error
   */
  @Test
  public void testGoodConfig() throws Exception {
    PSWebdavConfigDef config = new PSWebdavConfigDef(loadXmlResource(CONFIG_GOOD));
    assertEquals("default", config.getCommunityName());
    assertEquals("/Site/mysite", config.getRootPath());
    Iterator it = config.getContentTypes();
    int typesCount = 0;
    while (it.hasNext()) {
      typesCount++;
      it.next();
    }
    assertEquals(2, typesCount);
    assertEquals("image", config.getDefaultContentType().getName());
    // test to/from XML
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element targetEl = config.toXml(doc);
    PSWebdavConfigDef target = new PSWebdavConfigDef(targetEl);
    assertTrue(config.equals(target));
  }

  /**
   * Test loading a configuration missing a required community attribute.
   *
   * @throws Exception for any error
   */
  @Test
  public void testMissingCommunity() throws Exception {
    String errorMsg = "";
    try {
      new PSWebdavConfigDef(loadXmlResource(CONFIG_MISSING_COMM));
    } catch (PSWebdavException e) {
      errorMsg = e.getMessage();
    }
    assertEquals(
        getAttributeErrorMsg(IPSRxWebDavDTD.ATTR_COMMUNITY_NAME, IPSRxWebDavDTD.ELEM_CONFIG),
        errorMsg);
  }

  /**
   * Test loading a configuration missing a required contentfield attribute.
   *
   * @throws Exception for any error
   */
  @Test
  public void testMissingContentField() throws Exception {
    String errorMsg = "";
    try {
      new PSWebdavConfigDef(loadXmlResource(CONFIG_MISSING_CONTENTFIELD));
    } catch (PSWebdavException e) {
      errorMsg = e.getMessage();
    }
    assertEquals(
        getAttributeErrorMsg(IPSRxWebDavDTD.ATTR_CONTENTFIELD, IPSRxWebDavDTD.ELEM_CONTENTTYPE),
        errorMsg);
  }

  /**
   * Test loading a configuration missing a required property name attribute.
   *
   * @throws Exception for any error
   */
  @Test
  public void testMissingPropName() throws Exception {
    String errorMsg = "";
    try {
      new PSWebdavConfigDef(loadXmlResource(CONFIG_MISSING_PROPNAME));
    } catch (PSWebdavException e) {
      errorMsg = e.getMessage();
    }
    assertEquals(
        getAttributeErrorMsg(IPSRxWebDavDTD.ATTR_NAME, IPSRxWebDavDTD.ELEM_PROPERTYFIELD_MAPPING),
        errorMsg);
  }

  /**
   * Test loading a configuration missing a required root attribute.
   *
   * @throws Exception for any error
   */
  @Test
  public void testMissingRoot() throws Exception {
    String errorMsg = "";
    try {
      new PSWebdavConfigDef(loadXmlResource(CONFIG_MISSING_ROOT));
    } catch (PSWebdavException e) {
      errorMsg = e.getMessage();
    }
    assertEquals(
        getAttributeErrorMsg(IPSRxWebDavDTD.ATTR_ROOT, IPSRxWebDavDTD.ELEM_CONFIG), errorMsg);
  }

  /**
   * Test loading a configuration missing a required content type id attribute.
   *
   * @throws Exception for any error
   */
  @Test
  public void testMissingTypeID() throws Exception {
    String errorMsg = "";
    try {
      new PSWebdavConfigDef(loadXmlResource(CONFIG_MISSING_TYPEID));
    } catch (PSWebdavException e) {
      errorMsg = e.getMessage();
    }
    assertEquals(
        getAttributeErrorMsg(IPSRxWebDavDTD.ATTR_ID, IPSRxWebDavDTD.ELEM_CONTENTTYPE), errorMsg);
  }

  /**
   * Test loading a configuration missing a required content type name attribute.
   *
   * @throws Exception for any error
   */
  @Test
  public void testMissingTypeName() throws Exception {
    String errorMsg = "";
    try {
      new PSWebdavConfigDef(loadXmlResource(CONFIG_MISSING_TYPENAME));
    } catch (PSWebdavException e) {
      errorMsg = e.getMessage();
    }
    assertEquals(
        getAttributeErrorMsg(IPSRxWebDavDTD.ATTR_NAME, IPSRxWebDavDTD.ELEM_CONTENTTYPE), errorMsg);
  }

  /**
   * Test loading a configuration missing a required field name element data
   *
   * @throws Exception for any error
   */
  @Test
  public void testMissingFieldName() throws Exception {
    PSWebdavException expectedEx = null;
    try {
      new PSWebdavConfigDef(loadXmlResource(CONFIG_MISSING_FIELDNAME));
    } catch (PSWebdavException e) {
      expectedEx = e;
    }
    assertEquals(
        WebdavErrorCodes.FIELDNAME_CANNOT_BE_EMPTY_OR_MISSING.numericCode(),
        expectedEx.getErrorCode());
    assertSame(WebdavErrorCodes.FIELDNAME_CANNOT_BE_EMPTY_OR_MISSING, expectedEx.getTypedErrorCode());
    assertFalse(expectedEx.isAuditable());
  }

  /**
   * Test loading a configuration missing required properties
   *
   * @throws Exception for any error
   */
  @Test
  public void testMissingProps() throws Exception {
    PSWebdavException expectedEx = null;
    try {
      new PSWebdavConfigDef(loadXmlResource(CONFIG_MISSING_PROPS));
    } catch (PSWebdavException e) {
      expectedEx = e;
    }
    assertEquals(
        WebdavErrorCodes.MISSING_REQUIRED_PROPERTY.numericCode(), expectedEx.getErrorCode());
    assertSame(WebdavErrorCodes.MISSING_REQUIRED_PROPERTY, expectedEx.getTypedErrorCode());
    assertFalse(expectedEx.isAuditable());
  }

  /**
   * Test loading a configuration with duplicate content types
   *
   * @throws Exception for any error
   */
  @Test
  public void testDuplicateTypes() throws Exception {
    PSWebdavException expectedEx = null;
    try {
      new PSWebdavConfigDef(loadXmlResource(CONFIG_DUP_TYPES));
    } catch (PSWebdavException e) {
      expectedEx = e;
    }
    assertEquals(
        WebdavErrorCodes.DUPLICATE_CONTENTTYPE_NAMES.numericCode(), expectedEx.getErrorCode());
    assertSame(WebdavErrorCodes.DUPLICATE_CONTENTTYPE_NAMES, expectedEx.getTypedErrorCode());
    assertFalse(expectedEx.isAuditable());
  }

  /**
   * Test loading a configuration missing required properties
   *
   * @throws Exception for any error
   */
  @Test
  public void testDuplicateProps() throws Exception {
    PSWebdavException expectedEx = null;
    try {
      new PSWebdavConfigDef(loadXmlResource(CONFIG_DUP_PROPS));
    } catch (PSWebdavException e) {
      expectedEx = e;
    }
    assertEquals(
        WebdavErrorCodes.CANNOT_HAVE_DUPLICATE_PROPERTIES.numericCode(),
        expectedEx.getErrorCode());
    assertSame(WebdavErrorCodes.CANNOT_HAVE_DUPLICATE_PROPERTIES, expectedEx.getTypedErrorCode());
    assertFalse(expectedEx.isAuditable());
  }

  /**
   * Test loading a configuration missing mimetypes if default is false
   *
   * @throws Exception for any error
   */
  @Test
  public void testMissingMimetypes() throws Exception {
    PSWebdavException expectedEx = null;
    try {
      new PSWebdavConfigDef(loadXmlResource(CONFIG_MISSING_MIMES));
    } catch (PSWebdavException e) {
      expectedEx = e;
    }
    assertEquals(WebdavErrorCodes.MIMETYPES_REQUIRED.numericCode(), expectedEx.getErrorCode());
    assertSame(WebdavErrorCodes.MIMETYPES_REQUIRED, expectedEx.getTypedErrorCode());
    assertFalse(expectedEx.isAuditable());
  }

  /**
   * Test loading a configuration that has more then one default content type.
   *
   * @throws Exception for any error
   */
  @Test
  public void testMoreThenOneDefaultCT() throws Exception {
    PSWebdavException expectedEx = null;
    try {
      new PSWebdavConfigDef(loadXmlResource(CONFIG_DEFAULT));
    } catch (PSWebdavException e) {
      expectedEx = e;
    }
    assertEquals(
        WebdavErrorCodes.CAN_ONLY_HAVE_ONE_DEFAULT_CONTENTTYPE.numericCode(),
        expectedEx.getErrorCode());
    assertSame(
        WebdavErrorCodes.CAN_ONLY_HAVE_ONE_DEFAULT_CONTENTTYPE, expectedEx.getTypedErrorCode());
    assertFalse(expectedEx.isAuditable());
  }

  /**
   * Loads the xml resource into an xml document and returns the root element.
   *
   * @param name name of the resource,. Cannot be <code>null</code>.
   * @return root element of the xml document
   * @throws Exception on any error
   */
  private Element loadXmlResource(String name) throws Exception {
    InputStream in = getClass().getResourceAsStream("/com/percussion/webdav/test/" + name);
    Document doc = PSXmlDocumentBuilder.createXmlDocument(in, false);
    return doc.getDocumentElement();
  }

  private String getAttributeErrorMsg(String attr, String elem) {
    return "Attribute '" + attr + "' must be specified for element '" + elem + "'.";
  }

  // Various configuration files for testing
  private static final String CONFIG_GOOD = "WebDavConfig_Good.xml";
  private static final String CONFIG_DUP_PROPS = "WebDavConfig_dupProps.xml";
  private static final String CONFIG_DUP_TYPES = "WebDavConfig_dupTypes.xml";
  private static final String CONFIG_MISSING_COMM = "WebDavConfig_missingCommunity.xml";
  private static final String CONFIG_MISSING_CONTENTFIELD = "WebDavConfig_missingContentField.xml";
  private static final String CONFIG_MISSING_FIELDNAME = "WebDavConfig_missingFieldName.xml";
  private static final String CONFIG_MISSING_MIMES = "WebDavConfig_missingMimes.xml";
  private static final String CONFIG_MISSING_PROPNAME = "WebDavConfig_missingPropName.xml";
  private static final String CONFIG_MISSING_PROPS = "WebDavConfig_missingProps.xml";
  private static final String CONFIG_MISSING_ROOT = "WebDavConfig_missingRoot.xml";
  private static final String CONFIG_MISSING_TYPEID = "WebDavConfig_missingTypeID.xml";
  private static final String CONFIG_MISSING_TYPENAME = "WebDavConfig_missingTypeName.xml";
  private static final String CONFIG_DEFAULT = "WebDavConfig_Defaults.xml";
}
