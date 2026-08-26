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
package com.percussion.pagemanagement.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.codes.AssemblyErrorCodes;
import com.percussion.error.PSNotFoundException;
import com.percussion.extension.IPSExtensionManager;
import com.percussion.extension.PSExtensionRef;
import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.server.PSServer;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.utils.guid.IPSGuid;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * {@link PSPageUtils#widgetContents} uses typed {@link AssemblyErrorCodes#MISSING_FINDER} with the
 * finder name (issue #3846). Production swallows the exception so page assembly continues.
 */
@Tag("UnitTest")
class PSPageUtilsMissingFinderTest {

  private static final String MISSING_FINDER_NAME =
      "Java/global/percussion/widgetcontentfinder/perc_MissingFinder";

  @Test
  void missingFinderUsesFinderNameNotNullInstance() {
    PSAssemblyException ex = PSPageUtils.missingFinder(MISSING_FINDER_NAME);
    assertEquals(AssemblyErrorCodes.MISSING_FINDER.numericCode(), ex.getErrorCode());
    assertEquals(12, ex.getErrorCode());
    Object[] args = ex.getErrorArguments();
    assertEquals(1, args.length);
    assertEquals(MISSING_FINDER_NAME, args[0]);
  }

  @Test
  void widgetContentsSwallowsUnresolvableFinder() throws Exception {
    PSWidgetItem widgetItem = new PSWidgetItem();
    widgetItem.setId("42");
    PSWidgetInstance widget = new PSWidgetInstance();
    widget.setItem(widgetItem);

    IPSAssemblyItem item = mock(IPSAssemblyItem.class);
    IPSGuid guid = mock(IPSGuid.class);
    when(item.getId()).thenReturn(guid);
    when(guid.toString()).thenReturn("guid-1");

    IPSExtensionManager emgr = mock(IPSExtensionManager.class);
    when(emgr.prepareExtension(any(PSExtensionRef.class), isNull()))
        .thenThrow(new PSNotFoundException(1));

    try (MockedStatic<PSServer> server = mockStatic(PSServer.class)) {
      server.when(() -> PSServer.getExtensionManager(null)).thenReturn(emgr);
      PSPageUtils utils = new PSPageUtils();
      assertNull(
          utils.widgetContents(item, widget, MISSING_FINDER_NAME, Map.of(), false));
    }
  }
}
