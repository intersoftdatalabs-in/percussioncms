/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.server;

import static org.junit.jupiter.api.Assertions.*;

public class PSServerLockManagerTest {
  /**
   * Construct this unit test
   *
   * @param name The name of this test.
   */

  /**
   * Tests all lock mgr functionality
   *
   * @throws Exception if there are any errors.
   */
  public void testAll() throws Exception {
    boolean didThrow = false;

    // test get before create
    try {
      PSServerLockManager.getInstance();
    } catch (IllegalStateException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    // now create
    PSServerLockManager lockMgr = PSServerLockManager.createInstance();
    assertNotNull(lockMgr);

    lockMgr = PSServerLockManager.getInstance();
    assertNotNull(lockMgr);

    // test 2nd create
    didThrow = false;
    try {
      PSServerLockManager.createInstance();
    } catch (IllegalStateException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    // test acquire
    assertFalse(lockMgr.isLocked(PSServerLockManager.RESOURCE_PUBLISHER));
    PSServerLockResult result =
        lockMgr.acquireLock(PSServerLockManager.RESOURCE_PUBLISHER, "testLocker");

    assertNotNull(result);
    PSServerLock lock = result.getLock();
    assertNotNull(lock);
    int[] lockedResources = lock.getLockedResources();
    for (int i = 0; i < lockedResources.length; i++) {
      System.err.println("locked: " + lockedResources[i]);
    }

    assertTrue(result.wasLockAcquired());
    assertTrue(lock.getLockId() != -1);
    assertTrue(lock.getLocker().equals("testLocker"));
    assertFalse(result.getConflicts().hasNext());
    assertTrue(lock.isResourceLocked(PSServerLockManager.RESOURCE_PUBLISHER));
    assertTrue(lockMgr.isLocked(PSServerLockManager.RESOURCE_PUBLISHER));

    // test failed acquire
    PSServerLockResult result2 =
        lockMgr.acquireLock(PSServerLockManager.RESOURCE_PUBLISHER, "testLocker2");

    assertNotNull(result2);
    PSServerLock lock2 = result2.getLock();
    assertNotNull(lock2);
    assertFalse(result2.wasLockAcquired());
    assertTrue(lock2.getLockId() == -1);
    assertTrue(lock2.getLocker().equals("testLocker2"));
    assertTrue(result2.getConflicts().hasNext());
    PSServerLock conflict = (PSServerLock) result2.getConflicts().next();
    assertTrue(conflict.getLockId() == result.getLock().getLockId());
    assertTrue(conflict.isResourceLocked(PSServerLockManager.RESOURCE_PUBLISHER));

    // test release
    assertFalse(lockMgr.releaseLock(lock2.getLockId()));
    assertTrue(lockMgr.releaseLock(lock.getLockId()));
    assertTrue(
        lockMgr
            .acquireLock(PSServerLockManager.RESOURCE_PUBLISHER, "testLocker2")
            .wasLockAcquired());
  }

  // collect all tests into a TestSuite and return it

}
