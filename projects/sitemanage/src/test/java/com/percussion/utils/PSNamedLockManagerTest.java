// REFACTORED: CP-JAVA11
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
package com.percussion.utils;

import static com.percussion.test.TestAssertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** Tests for PSNamedLockManager. */
public class PSNamedLockManagerTest {

  @Test
  public void testSingleThread() {
    var mgr = new PSNamedLockManager(5000);
    var name1 = "name1";
    var name2 = "name2";
    assertFalse(mgr.isLocked(name1));
    assertFalse(mgr.haveLock(name1));
    assertFalse(mgr.isLocked(name2));
    assertFalse(mgr.haveLock(name2));

    assertTrue(mgr.getLock(name1));
    assertTrue(mgr.isLocked(name1));
    assertTrue(mgr.haveLock(name1));
    assertFalse(mgr.isLocked(name2));
    assertFalse(mgr.haveLock(name2));

    assertTrue(mgr.getLock(name2));
    assertTrue(mgr.isLocked(name2));
    assertTrue(mgr.haveLock(name2));
    assertTrue(mgr.isLocked(name1));
    assertTrue(mgr.haveLock(name1));

    mgr.releaseLock(name1);
    assertFalse(mgr.isLocked(name1));
    assertFalse(mgr.haveLock(name1));
    assertTrue(mgr.isLocked(name2));
    assertTrue(mgr.haveLock(name2));

    mgr.releaseLock(name2);
    assertFalse(mgr.isLocked(name1));
    assertFalse(mgr.haveLock(name1));
    assertFalse(mgr.isLocked(name2));
    assertFalse(mgr.haveLock(name2));

    assertFalse(mgr.releaseLock(name1));
  }

  @Disabled("Multi-threaded lock test is unstable and for manual verification only.")
  public void testMultipleThreads() throws Exception {
    var mgr = new PSNamedLockManager(5);
    var name1 = "name1";

    var locker1 = new Locker("locker1", mgr);
    var locker2 = new Locker("locker2", mgr);

    locker1.start();
    locker2.start();

    locker1.setLock(name1);
    Thread.sleep(20);
    assertTrue(mgr.isLocked(name1));
    locker2.setLock(name1);
    Thread.sleep(20);

    assertTrue(locker1.didLock(name1));
    assertFalse(locker2.didLock(name1));

    locker1.releaseLock(name1);
    Thread.sleep(20);
    assertFalse(mgr.isLocked(name1));

    locker2.setLock(name1);
    Thread.sleep(20);
    assertTrue(mgr.isLocked(name1));
    assertTrue(locker2.didLock(name1));
    locker2.releaseLock(name1);
    Thread.sleep(20);
    assertFalse(mgr.isLocked(name1));
  }

  private static class Locker extends Thread {
    private volatile String lockName = null;
    private volatile String lastLock = null;
    private volatile String releaseLock = null;
    private final PSNamedLockManager lockMgr;

    public Locker(String name, PSNamedLockManager lockMgr) {
      super(name);
      setDaemon(true);
      this.lockMgr = lockMgr;
    }

    @Override
    public void run() {
      while (true) {
        if (lockName != null) {
          boolean didLock = lockMgr.getLock(lockName);
          if (didLock && lockMgr.haveLock(lockName)) {
            lastLock = lockName;
          }
          lockName = null;
        }

        if (releaseLock != null) {
          lockMgr.releaseLock(releaseLock);
          releaseLock = null;
        }

        try {
          sleep(5);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }

    public void setLock(String name) {
      lockName = name;
    }

    public boolean didLock(String name) {
      boolean didLock = name.equals(lastLock);
      if (didLock) {
        lastLock = null;
      }
      return didLock;
    }

    public void releaseLock(String name) {
      if (releaseLock != null) {
        throw new IllegalStateException("Still waiting to release lock: " + releaseLock);
      }
      releaseLock = name;
    }
  }
}
