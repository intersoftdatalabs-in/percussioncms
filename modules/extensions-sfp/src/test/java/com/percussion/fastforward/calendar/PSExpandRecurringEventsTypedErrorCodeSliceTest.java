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
package com.percussion.fastforward.calendar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.intsof.percussioncms.auditlog.codes.ExtensionErrorCodes;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.server.IPSRequestContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Leftover sfp calendar production throw sites now construct typed {@link ExtensionErrorCodes}
 * (issue #3770).
 */
@Tag("UnitTest")
class PSExpandRecurringEventsTypedErrorCodeSliceTest {

  @Test
  void missingCalendarStartThrowsTypedMismatch() throws Exception {
    PSExpandRecurringEvents exit = new PSExpandRecurringEvents();
    IPSRequestContext request = mock(IPSRequestContext.class);
    Document doc = mock(Document.class);

    PSParameterMismatchException ex =
        assertThrows(
            PSParameterMismatchException.class,
            () -> exit.processResultDocument(new Object[] {null, "2026-01-31"}, request, doc));
    assertSame(ExtensionErrorCodes.EXT_MISSING_REQUIRED_PARAMETER_ERROR, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void makeCalendarNullDateThrowsTypedMismatch() throws Exception {
    PSMakeCalendar calendar = new PSMakeCalendar();
    IPSRequestContext request = mock(IPSRequestContext.class);
    Document doc = mock(Document.class);

    PSParameterMismatchException ex =
        assertThrows(
            PSParameterMismatchException.class,
            () -> calendar.processResultDocument(new Object[] {null}, request, doc));
    assertSame(ExtensionErrorCodes.EXT_MISSING_REQUIRED_PARAMETER_ERROR, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }
}
