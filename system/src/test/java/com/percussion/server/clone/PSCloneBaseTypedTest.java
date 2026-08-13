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
package com.percussion.server.clone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.server.IPSRequestContext;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Behavioral coverage for typed clone exit parameter maps after #3213 Xlint cleanup.
 */
class PSCloneBaseTypedTest {

  private final TestCloneExit exit = new TestCloneExit();

  @Test
  @DisplayName("getCloneSourceId rejects a null request")
  void getCloneSourceIdRejectsNullRequest() {
    assertThrows(IllegalArgumentException.class, () -> exit.getCloneSourceId(null));
  }

  @Test
  @DisplayName("getCloneSourceId returns -1 when the HTML parameter is absent")
  void getCloneSourceIdMissingParam() throws PSParameterMismatchException {
    IPSRequestContext request = mock(IPSRequestContext.class);
    when(request.getParameter(PSCloneBase.CLONESOURCEID)).thenReturn(null);
    assertEquals(-1, exit.getCloneSourceId(request));
  }

  @Test
  @DisplayName("getCloneSourceId parses a typed integer source id")
  void getCloneSourceIdParsesInt() throws PSParameterMismatchException {
    IPSRequestContext request = mock(IPSRequestContext.class);
    when(request.getParameter(PSCloneBase.CLONESOURCEID)).thenReturn("42");
    assertEquals(42, exit.getCloneSourceId(request));
  }

  @Test
  @DisplayName("getCloneSourceId rejects a non-integer source id")
  void getCloneSourceIdRejectsNonInteger() {
    IPSRequestContext request = mock(IPSRequestContext.class);
    when(request.getParameter(PSCloneBase.CLONESOURCEID)).thenReturn("not-an-id");
    assertThrows(PSParameterMismatchException.class, () -> exit.getCloneSourceId(request));
  }

  @Test
  @DisplayName("cloneChildObjects rejects a null request")
  void cloneChildObjectsRejectsNullRequest() {
    Map<String, Object> query = new HashMap<>();
    Map<String, Object> update = new HashMap<>();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            exit.cloneChildObjects(
                null, "Key", "1", new String[] {"q"}, new String[] {"u"}, query, update));
  }

  @Test
  @DisplayName("cloneChildObjects rejects mismatched query/update resource arrays")
  void cloneChildObjectsRejectsMismatchedResources() {
    IPSRequestContext request = mock(IPSRequestContext.class);
    Map<String, Object> query = new HashMap<>();
    Map<String, Object> update = new HashMap<>();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            exit.cloneChildObjects(
                request,
                "Key",
                "1",
                new String[] {"q1", "q2"},
                new String[] {"u1"},
                query,
                update));
  }

  @Test
  @DisplayName("updateContent rejects a null request")
  void updateContentRejectsNullRequest() {
    Map<String, Object> update = new HashMap<>();
    update.put("DBActionType", "INSERT");
    assertThrows(
        IllegalArgumentException.class, () -> exit.updateContent(null, "app/update", update));
  }

  @Test
  @DisplayName("updateContent rejects an empty resource name")
  void updateContentRejectsEmptyResource() {
    IPSRequestContext request = mock(IPSRequestContext.class);
    Map<String, Object> update = new HashMap<>();
    assertThrows(IllegalArgumentException.class, () -> exit.updateContent(request, "  ", update));
  }

  @Test
  @DisplayName("updateContent reports a missing typed resource")
  void updateContentMissingResource() {
    IPSRequestContext request = mock(IPSRequestContext.class);
    when(request.getInternalRequest("missing/update", Map.of("DBActionType", "INSERT"), false))
        .thenReturn(null);
    Map<String, Object> update = new HashMap<>();
    update.put("DBActionType", "INSERT");
    assertThrows(
        PSExtensionProcessingException.class,
        () -> exit.updateContent(request, "missing/update", update));
  }

  /** Concrete clone exit so protected map helpers can be exercised. */
  private static final class TestCloneExit extends PSCloneBase {
    @Override
    public Document processResultDocument(
        Object[] params, IPSRequestContext request, Document resultDoc) {
      return resultDoc;
    }
  }
}
