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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.intsof.percussioncms.auditlog.codes.CmsErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ContentExplorerErrorCodes;
import com.percussion.cms.PSCmsException;
import com.percussion.cx.catalogers.PSCommunityCataloger;
import com.percussion.cx.error.PSContentExplorerException;
import com.percussion.wizard.PSWizardValidationError;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Issue #3741 / parent #2616 slice 3: leftover Desktop Content Explorer production {@code
 * IPSContentExplorerErrors} / {@code IPSCmsErrors} sites throw typed {@link
 * ContentExplorerErrorCodes} / {@link CmsErrorCodes}. Catalog codes are non-auditable (dual-write
 * skip).
 */
class PSContentExplorerTypedErrorCodeSliceTest {

  @Test
  void typedContentExplorerExceptionSkipsDualWrite() {
    PSContentExplorerException ex =
        new PSContentExplorerException(ContentExplorerErrorCodes.SEARCH_ERROR, "no hits");

    assertEquals(ContentExplorerErrorCodes.SEARCH_ERROR.numericCode(), ex.getErrorCode());
    assertSame(ContentExplorerErrorCodes.SEARCH_ERROR, ex.getTypedErrorCode());
    assertFalse(ContentExplorerErrorCodes.SEARCH_ERROR.isAuditable());
    assertFalse(ex.isAuditable());
  }

  @Test
  void typedWizardValidationErrorSkipsDualWrite() {
    PSWizardValidationError ex =
        new PSWizardValidationError(ContentExplorerErrorCodes.WIZARD_VALIDATION_ERROR, "blank");

    assertEquals(
        ContentExplorerErrorCodes.WIZARD_VALIDATION_ERROR.numericCode(), ex.getErrorCode());
    assertSame(ContentExplorerErrorCodes.WIZARD_VALIDATION_ERROR, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void cmsUnexpectedErrorFromRelationshipsIsNonAuditable() {
    PSCmsException ex = new PSCmsException(CmsErrorCodes.UNEXPECTED_ERROR, "boom");

    assertEquals(CmsErrorCodes.UNEXPECTED_ERROR.numericCode(), ex.getErrorCode());
    assertSame(CmsErrorCodes.UNEXPECTED_ERROR, ex.getTypedErrorCode());
    assertFalse(CmsErrorCodes.UNEXPECTED_ERROR.isAuditable());
    assertFalse(ex.isAuditable());
  }

  @Test
  void columnWidthsFromXmlWrongRootThrowsTypedOptionsError() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = doc.createElement("not-column-widths");
    PSColumnWidthsOption option = new PSColumnWidthsOption();

    PSContentExplorerException ex =
        assertThrows(PSContentExplorerException.class, () -> option.fromXml(wrong));

    assertEquals(
        ContentExplorerErrorCodes.MISC_PROCESSING_OPTIONS_ERROR.numericCode(), ex.getErrorCode());
    assertSame(ContentExplorerErrorCodes.MISC_PROCESSING_OPTIONS_ERROR, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void communityCatalogerMissingResourceThrowsTypedCatalogError(@TempDir Path tmp)
      throws Exception {
    Path base = tmp.resolve("cx-catalog-base");
    Files.createDirectories(base);
    URL urlBase = base.toUri().toURL();

    PSCmsException ex =
        assertThrows(PSCmsException.class, () -> new PSCommunityCataloger(urlBase));

    assertEquals(ContentExplorerErrorCodes.CATALOG_ERROR.numericCode(), ex.getErrorCode());
    assertSame(ContentExplorerErrorCodes.CATALOG_ERROR, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }
}
