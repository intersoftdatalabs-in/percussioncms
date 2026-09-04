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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.design.objectstore.PSApplication;
import com.percussion.design.objectstore.PSDataSet;
import com.percussion.rest.pipelines.ApplicationValidationProblem;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class CollectingApplicationValidatorTest {

  @Test
  void collectsErrorAndWarningWithoutThrowing() throws Exception {
    CollectingApplicationValidator validator = new CollectingApplicationValidator(null);
    PSApplication app = mock(PSApplication.class);
    when(app.getName()).thenReturn("sys_cmpDocuments");
    when(app.getId()).thenReturn(7);
    validator.setContainer(app);

    PSDataSet ds = mock(PSDataSet.class);
    when(ds.getName()).thenReturn("contenteditor");
    when(ds.getId()).thenReturn(11);

    validator.pushParent(ds);
    validator.validationError(ds, 1301, new Object[] {"Missing requestor"});
    validator.validationWarning(ds, 1400, new Object[] {"Slow mapping"});
    validator.popParent();

    assertEquals(2, validator.getProblems().size());
    ApplicationValidationProblem error = validator.getProblems().get(0);
    assertEquals(CollectingApplicationValidator.SEVERITY_ERROR, error.getSeverity());
    assertEquals("1301", error.getCode());
    assertEquals("contenteditor", error.getResource());
    assertTrue(error.getPath().contains("PSDataSet"));
    assertTrue(error.getPath().contains("contenteditor"));

    ApplicationValidationProblem warning = validator.getProblems().get(1);
    assertEquals(CollectingApplicationValidator.SEVERITY_WARNING, warning.getSeverity());
    assertEquals("1400", warning.getCode());
  }

  @Test
  void resolveResourceFallsBackToApplicationName() {
    CollectingApplicationValidator validator = new CollectingApplicationValidator(null);
    PSApplication app = mock(PSApplication.class);
    when(app.getName()).thenReturn("sys_foo");
    validator.setContainer(app);

    assertEquals("sys_foo", validator.resolveResourceName(null));
    assertNull(new CollectingApplicationValidator(null).resolveResourceName(null));
  }
}
