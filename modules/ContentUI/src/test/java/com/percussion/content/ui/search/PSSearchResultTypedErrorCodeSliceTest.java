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
package com.percussion.content.ui.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.codes.SearchErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.server.IPSRequestContext;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * Issue #3741 / parent #2616 slice 3: leftover ContentUI search production {@code IPSSearchErrors}
 * / {@code IPSServerErrors} sites throw typed {@link SearchErrorCodes} / {@link ServerErrorCodes}.
 * Both leftover codes are non-auditable (dual-write skip).
 */
class PSSearchResultTypedErrorCodeSliceTest {

  @Test
  void missingSearchIdThrowsTypedHtmlSearchMissingParameter() throws Exception {
    IPSRequestContext request = mock(IPSRequestContext.class);
    when(request.getParametersIterator()).thenReturn(Collections.emptyIterator());
    when(request.getUserLocale()).thenReturn("en-us");

    PSExtensionProcessingException ex =
        assertThrows(
            PSExtensionProcessingException.class,
            () -> new PSSearchResult().getSearchResults(request));

    assertEquals(
        SearchErrorCodes.HTML_SEARCH_MISSING_PARAMETER.numericCode(), ex.getErrorCode());
    assertSame(SearchErrorCodes.HTML_SEARCH_MISSING_PARAMETER, ex.getTypedErrorCode());
    assertFalse(SearchErrorCodes.HTML_SEARCH_MISSING_PARAMETER.isAuditable());
    assertFalse(ex.isAuditable());
  }

  @Test
  void rawDumpTypedConstructionSkipsDualWrite() {
    PSExtensionProcessingException ex =
        new PSExtensionProcessingException(ServerErrorCodes.RAW_DUMP, "invalid query");

    assertEquals(ServerErrorCodes.RAW_DUMP.numericCode(), ex.getErrorCode());
    assertSame(ServerErrorCodes.RAW_DUMP, ex.getTypedErrorCode());
    assertFalse(ServerErrorCodes.RAW_DUMP.isAuditable());
    assertFalse(ex.isAuditable());
  }
}
