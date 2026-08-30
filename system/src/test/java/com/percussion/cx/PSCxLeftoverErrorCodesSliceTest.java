/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.codes.ContentExplorerErrorCodes;
import com.percussion.cx.error.IPSContentExplorerErrors;
import com.percussion.cx.error.PSContentExplorerException;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.awt.Font;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Issue #4013 (parent #2616 leftover): {@code com.percussion.cx} production sites throw typed
 * {@link ContentExplorerErrorCodes} (not bare {@code IPSContentExplorerErrors} ints). Catalog
 * codes are non-auditable (dual-write skip).
 */
@Tag("UnitTest")
class PSCxLeftoverErrorCodesSliceTest {

  @Test
  void leftoverCatalogsMatchLegacyIntsAndSkipDualWrite() {
    assertEquals(
        IPSContentExplorerErrors.PSCLASS_INSTANTIATION_ERROR,
        ContentExplorerErrorCodes.PSCLASS_INSTANTIATION_ERROR.numericCode());
    assertEquals(
        IPSContentExplorerErrors.MISC_PROCESSING_OPTIONS_ERROR,
        ContentExplorerErrorCodes.MISC_PROCESSING_OPTIONS_ERROR.numericCode());
    assertFalse(ContentExplorerErrorCodes.PSCLASS_INSTANTIATION_ERROR.isAuditable());
    assertFalse(ContentExplorerErrorCodes.MISC_PROCESSING_OPTIONS_ERROR.isAuditable());
  }

  @Test
  void fontFromXmlWrongRootThrowsTypedOptionsError() throws Exception {
    PSFont font = new PSFont(new Font(Font.DIALOG, Font.PLAIN, 12));
    leftoverNonAuditable(
        assertThrows(PSContentExplorerException.class, () -> font.fromXml(wrongRoot("NotFont"))),
        ContentExplorerErrorCodes.MISC_PROCESSING_OPTIONS_ERROR);
  }

  @Test
  void optionFromXmlWrongRootThrowsTypedOptionsError() throws Exception {
    leftoverNonAuditable(
        assertThrows(PSContentExplorerException.class, () -> new PSOption(wrongRoot("NotOption"))),
        ContentExplorerErrorCodes.MISC_PROCESSING_OPTIONS_ERROR);
  }

  @Test
  void optionsFromXmlWrongRootThrowsTypedOptionsError() throws Exception {
    leftoverNonAuditable(
        assertThrows(
            PSContentExplorerException.class, () -> new PSOptions(wrongRoot("NotOptions"))),
        ContentExplorerErrorCodes.MISC_PROCESSING_OPTIONS_ERROR);
  }

  @Test
  void userOptionsFromXmlWrongRootThrowsTypedOptionsError() throws Exception {
    leftoverNonAuditable(
        assertThrows(
            PSContentExplorerException.class, () -> new PSUserOptions(wrongRoot("NotUserOptions"))),
        ContentExplorerErrorCodes.MISC_PROCESSING_OPTIONS_ERROR);
  }

  @Test
  void optionInvalidPsxChildWrapsInstantiationAsTypedOptionsError() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element option = PSXmlDocumentBuilder.createRoot(doc, PSOption.ELEM_OPTION);
    option.setAttribute(PSOption.ATTR_CONTEXT, "display");
    option.setAttribute(PSOption.ATTR_OPTIONID, "font");
    option.setAttribute(PSOption.ATTR_CLASSNAME, "java.lang.String");
    PSXmlDocumentBuilder.addElement(doc, option, "PSXBogus", "x");

    leftoverNonAuditable(
        assertThrows(PSContentExplorerException.class, () -> new PSOption(option)),
        ContentExplorerErrorCodes.MISC_PROCESSING_OPTIONS_ERROR);
  }

  @Test
  void optionMakeObjectThrowsTypedClassInstantiationError() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element source = PSXmlDocumentBuilder.createRoot(doc, "PSXBogus");
    PSOption option = new PSOption("display", "font", "plain");
    Method makeObject =
        PSOption.class.getDeclaredMethod("makeObject", Element.class, String.class);
    makeObject.setAccessible(true);

    InvocationTargetException wrapped =
        assertThrows(
            InvocationTargetException.class,
            () -> makeObject.invoke(option, source, "java.lang.String"));
    assertTrue(wrapped.getCause() instanceof PSContentExplorerException);
    leftoverNonAuditable(
        (PSContentExplorerException) wrapped.getCause(),
        ContentExplorerErrorCodes.PSCLASS_INSTANTIATION_ERROR);
  }

  private static Element wrongRoot(String name) throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    return PSXmlDocumentBuilder.createRoot(doc, name);
  }

  private static void leftoverNonAuditable(
      PSContentExplorerException ex, ContentExplorerErrorCodes expected) {
    assertSame(expected, ex.getTypedErrorCode(), expected.toString());
    assertEquals(expected.numericCode(), ex.getErrorCode(), expected.toString());
    assertFalse(ex.isAuditable(), expected.toString());
    assertFalse(expected.isAuditable(), expected.toString());
  }
}
