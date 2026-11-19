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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.security.PSUserEntry;
import com.percussion.services.security.PSServletRequestWrapper;
import javax.security.auth.Subject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class PSUserSessionManagerRevocationTest {

  @AfterEach
  void tearDown() {
    PSUserSessionManager.clearSessionsForTest();
  }

  @Test
  void releaseSessionsForUserRemovesAllMatchingCaseInsensitively() {
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setServerName("localhost");
    req.setServerPort(9992);
    req.setScheme("http");
    PSServletRequestWrapper wrapper = new PSServletRequestWrapper(req, new Subject());

    PSUserSession alice1 = new PSUserSession(wrapper, "sess-alice-1", false);
    alice1.addAuthenticatedUserEntry(new PSUserEntry("Alice", 0, null, null, null, null));

    PSUserSession alice2 = new PSUserSession(wrapper, "sess-alice-2", false);
    alice2.addAuthenticatedUserEntry(new PSUserEntry("alice", 0, null, null, null, null));

    PSUserSession bob = new PSUserSession(wrapper, "sess-bob-1", false);
    bob.addAuthenticatedUserEntry(new PSUserEntry("bob", 0, null, null, null, null));

    PSUserSessionManager.registerSessionForTest(alice1);
    PSUserSessionManager.registerSessionForTest(alice2);
    PSUserSessionManager.registerSessionForTest(bob);

    int removed = PSUserSessionManager.releaseSessionsForUser("ALICE");

    assertEquals(2, removed);
    assertFalse(PSUserSessionManager.doesSessionExist("sess-alice-1"));
    assertFalse(PSUserSessionManager.doesSessionExist("sess-alice-2"));
    assertTrue(PSUserSessionManager.doesSessionExist("sess-bob-1"));
  }
}
