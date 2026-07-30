/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.category.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.percussion.server.PSUserSession;
import com.percussion.server.PSUserSessionManager;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PSCategoryLockInfoStaleTest {

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
  void isLockStaleWhenSessionMissing() throws Exception {
    var json = new JSONObject();
    json.put("sessionId", "missing-session");
    json.put("userName", "tester");

    assertTrue(PSCategoryLockInfo.isLockStale(json));
  }

  @Test
  void isLockActiveWithoutRefreshingSession() throws Exception {
    String sessionId = "active-session";
    PSUserSession session = mock(PSUserSession.class);
    sessions.put(sessionId, session);
    var json = new JSONObject();
    json.put("sessionId", sessionId);
    json.put("userName", "tester");

    assertFalse(PSCategoryLockInfo.isLockStale(json));
    verifyNoInteractions(session);
  }

  @Test
  void isLockStaleFalseForNullInput() throws Exception {
    // A null lock entry is treated as "no lock present" rather than a stale lock
    // — callers (getLockInfo) only call isLockStale on a non-null parsed payload.
    assertFalse(PSCategoryLockInfo.isLockStale(null));
  }

  @Test
  void isLockStaleWhenSessionIdIsBlank() throws Exception {
    // GH-1566: blank sessionId entries were previously ignored and could only be
    // cleared by an explicit overwrite. Treat them as stale so getLockInfo()
    // cleans them up automatically on the next read.
    var json = new JSONObject();
    json.put("sessionId", "");
    json.put("userName", "tester");
    assertTrue(PSCategoryLockInfo.isLockStale(json));
  }

  @Test
  void isLockStaleWhenSessionIdIsWhitespace() throws Exception {
    var json = new JSONObject();
    json.put("sessionId", "   ");
    json.put("userName", "tester");
    assertTrue(PSCategoryLockInfo.isLockStale(json));
  }

  @Test
  void isLockStaleWhenSessionIdMissing() throws Exception {
    // No "sessionId" key at all — malformed record. GH-1566.
    var json = new JSONObject();
    json.put("userName", "tester");
    assertTrue(PSCategoryLockInfo.isLockStale(json));
  }

  @Test
  void isLockStaleWhenSessionIdIsWrongType() throws Exception {
    // sessionId is a JSON number rather than a string — malformed record. GH-1566.
    var json = new JSONObject();
    json.put("sessionId", 42);
    json.put("userName", "tester");
    assertTrue(PSCategoryLockInfo.isLockStale(json));
  }
}