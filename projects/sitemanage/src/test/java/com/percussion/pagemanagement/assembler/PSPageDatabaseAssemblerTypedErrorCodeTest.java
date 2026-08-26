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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.codes.AssemblyErrorCodes;
import com.percussion.pagemanagement.assembler.impl.PSAssemblyItemBridge;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.assembly.impl.PSAssemblyJexlEvaluator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * {@link PSPageDatabaseAssembler#preProcessItemBinding} wraps context failures as typed {@link
 * AssemblyErrorCodes#UNKNOWN_ERROR} (issue #3846). Parent {@code PSAssemblerBase} static init
 * looks up the assembly service; stub the locator so this stays a unit test.
 */
@Tag("UnitTest")
class PSPageDatabaseAssemblerTypedErrorCodeTest {

  @Test
  void preProcessItemBindingWrapsFailureAsTypedUnknownError() throws Exception {
    IPSAssemblyItem pageItem = mock(IPSAssemblyItem.class);
    IPSAssemblyItem cloned = mock(IPSAssemblyItem.class);
    when(pageItem.clone()).thenReturn(cloned);

    PSAssemblyItemBridge bridge = mock(PSAssemblyItemBridge.class);
    when(bridge.getTemplateAndPage(cloned)).thenThrow(new RuntimeException("boom"));

    try (MockedStatic<PSAssemblyServiceLocator> locator =
        mockStatic(PSAssemblyServiceLocator.class)) {
      locator
          .when(PSAssemblyServiceLocator::getAssemblyService)
          .thenReturn(mock(IPSAssemblyService.class));

      PSPageDatabaseAssembler assembler = new PSPageDatabaseAssembler();
      assembler.setAssemblyItemBridge(bridge);

      PSAssemblyException thrown =
          assertThrows(
              PSAssemblyException.class,
              () ->
                  assembler.preProcessItemBinding(
                      pageItem, mock(PSAssemblyJexlEvaluator.class)));
      assertEquals(AssemblyErrorCodes.UNKNOWN_ERROR.numericCode(), thrown.getErrorCode());
      assertEquals(5, thrown.getErrorCode());
      assertInstanceOf(RuntimeException.class, thrown.getCause());
    }
  }
}
