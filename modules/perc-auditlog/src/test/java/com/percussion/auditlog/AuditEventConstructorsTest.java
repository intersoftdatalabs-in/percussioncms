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
import static org.mockito.Mockito.lenient;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Behavioral coverage for concrete audit event constructors after replacing {@code this-escape}
 * suppressions with real constructor patterns (issue #2018).
 */
@ExtendWith(MockitoExtension.class)
public class AuditEventConstructorsTest {

  @Mock private HttpServletRequest request;

  @BeforeEach
  void stubRequest() {
    // Lenient: not every test exercises every stub (no-arg / final-class cases).
    lenient().when(request.getRemoteAddr()).thenReturn("203.0.113.10");
    lenient().when(request.getRemoteUser()).thenReturn("editor");
    lenient().when(request.getHeader("User-Agent")).thenReturn("JUnit-Agent/1.0");
  }

  @Test
  @DisplayName("concrete event types are final (no subclass this-escape)")
  void concreteEventTypesAreFinal() {
    assertTrue(Modifier.isFinal(PSAuthenticationEvent.class.getModifiers()));
    assertTrue(Modifier.isFinal(PSContentEvent.class.getModifiers()));
    assertTrue(Modifier.isFinal(PSUserManagementEvent.class.getModifiers()));
    assertTrue(Modifier.isFinal(PSWorkflowEvent.class.getModifiers()));
  }

  @Test
  @DisplayName("PSAuthenticationEvent no-arg sets security observer")
  void authenticationNoArgSetsObserver() {
    PSAuthenticationEvent event = new PSAuthenticationEvent();
    assertEquals(PSAuthenticationEvent.SYSTEM_SECURITY_URI, event.getObserverName());
    assertEquals(PSActionOutcome.UNKNOWN.name(), event.getOutcome());
  }

  @Test
  @DisplayName("PSAuthenticationEvent populated constructor seeds request metadata")
  void authenticationPopulatedConstructor() {
    PSAuthenticationEvent event =
        new PSAuthenticationEvent(
            PSActionOutcome.SUCCESS.name(),
            PSAuthenticationEvent.AuthenticationEventActions.login,
            request,
            "alice");
    assertEquals(PSAuthenticationEvent.SYSTEM_SECURITY_URI, event.getObserverName());
    assertEquals(PSActionOutcome.SUCCESS.name(), event.getOutcome());
    assertEquals(PSAuthenticationEvent.AuthenticationEventActions.login, event.getAction());
    assertEquals("203.0.113.10", event.getInitiatorIP());
    assertEquals("alice", event.getTargetUsername());
    assertEquals("JUnit-Agent/1.0", event.getAgentName());
  }

  @Test
  @DisplayName("PSContentEvent no-arg sets content observer")
  void contentNoArgSetsObserver() {
    PSContentEvent event = new PSContentEvent();
    assertEquals(PSContentEvent.CONTENT_OBSERVER, event.getObserverName());
  }

  @Test
  @DisplayName("PSContentEvent populated constructor seeds content and request fields")
  void contentPopulatedConstructor() {
    PSContentEvent event =
        new PSContentEvent(
            "guid-1",
            "42",
            "/Sites/demo/page",
            PSContentEvent.ContentEventActions.create,
            request,
            PSActionOutcome.SUCCESS);
    assertEquals("guid-1", event.getGuid());
    assertEquals("42", event.getContentId());
    assertEquals("/Sites/demo/page", event.getPath());
    assertEquals(PSContentEvent.ContentEventActions.create, event.getAction());
    assertEquals("editor", event.getTargetUsername());
    assertEquals("203.0.113.10", event.getInitiatorIP());
    assertEquals("JUnit-Agent/1.0", event.getAgentName());
    assertEquals(PSActionOutcome.SUCCESS.name(), event.getOutcome());
  }

  @Test
  @DisplayName("PSUserManagementEvent constructor seeds action and request fields")
  void userManagementConstructor() {
    PSUserManagementEvent event =
        new PSUserManagementEvent(
            request, PSUserManagementEvent.UserEventActions.create, PSActionOutcome.FAILURE);
    assertEquals(PSUserManagementEvent.UserEventActions.create, event.getAction());
    assertEquals("editor", event.getIniatorName());
    assertEquals("editor", event.getTargetName());
    assertEquals("203.0.113.10", event.getInitiatorIP());
    assertEquals(PSActionOutcome.FAILURE.name(), event.getOutcome());
    assertEquals("JUnit-Agent/1.0", event.getAgentName());
  }

  @Test
  @DisplayName("PSWorkflowEvent constructor seeds transition and content fields")
  void workflowConstructor() {
    PSWorkflowEvent event =
        new PSWorkflowEvent(
            "Draft",
            "Review",
            PSWorkflowEvent.WorkflowEventActions.update,
            request,
            "99",
            "guid-wf",
            PSActionOutcome.SUCCESS.name());
    assertEquals("Draft", event.getTransitionFrom());
    assertEquals("Review", event.getTransitionTo());
    assertEquals(PSWorkflowEvent.WorkflowEventActions.update, event.getAction());
    assertEquals(99, event.getContentId());
    assertEquals("guid-wf", event.getGuid());
    assertEquals("editor", event.getTargetUsername());
    assertEquals("203.0.113.10", event.getInitiatorIP());
    assertEquals(PSActionOutcome.SUCCESS.name(), event.getOutcome());
  }
}
