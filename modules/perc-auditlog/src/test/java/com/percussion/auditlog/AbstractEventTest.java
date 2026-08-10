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
package com.percussion.auditlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for {@link AbstractEvent} constructor defaults after this-escape remediation
 * (issue #2018).
 */
public class AbstractEventTest {

  @Test
  @DisplayName("no-arg constructor seeds UNKNOWN outcome and system observer/target")
  void noArgConstructorSeedsDefaults() {
    AbstractEvent event = new AbstractEvent();
    assertEquals(PSActionOutcome.UNKNOWN.name(), event.getOutcome());
    assertEquals("service/bss/cms", event.getObserverName());
    assertEquals("service/bss/cms", event.getTargetName());
  }

  @Test
  @DisplayName("setOutcome remains usable after construction and is final")
  void setOutcomeIsFinalAndWorks() throws Exception {
    assertTrue(
        Modifier.isFinal(AbstractEvent.class.getMethod("setOutcome", String.class).getModifiers()));
    AbstractEvent event = new AbstractEvent();
    event.setOutcome(PSActionOutcome.SUCCESS.name());
    assertEquals(PSActionOutcome.SUCCESS.name(), event.getOutcome());
  }
}
