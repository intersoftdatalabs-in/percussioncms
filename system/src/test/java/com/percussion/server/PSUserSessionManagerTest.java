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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PSUserSessionManagerTest {

  private Map<String, PSUserSession> originalSessions;
  private ConcurrentHashMap<String, PSUserSession> sessions;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void isolateSessions() throws ReflectiveOperationException {
    Field sessionsField = PSUserSessionManager.class.getDeclaredField("ms_Sessions");
    sessionsField.setAccessible(true);
    sessions = (ConcurrentHashMap<String, PSUserSession>) sessionsField.get(null);
    originalSessions = new HashMap<>(sessions);
    sessions.clear();
  }

  @AfterEach
  void restoreSessions() {
    sessions.clear();
    sessions.putAll(originalSessions);
  }

  @Test
  void doesSessionExistDoesNotRefreshIdleTimeout() {
    String sessionId = "active-session";
    PSUserSession session = mock(PSUserSession.class);
    sessions.put(sessionId, session);

    assertTrue(PSUserSessionManager.doesSessionExist(sessionId));
    assertFalse(PSUserSessionManager.doesSessionExist("missing-session"));
    assertFalse(PSUserSessionManager.doesSessionExist((String) null));
    verifyNoInteractions(session);
  }
}
