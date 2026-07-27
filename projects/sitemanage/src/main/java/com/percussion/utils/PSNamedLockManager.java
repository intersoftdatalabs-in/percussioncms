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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

/**
 * Manages a map of named locks, so that locks can be acquired based on a name. Locks are reentrant
 * and waiting threads are managed fairly, favoring the longest waiting thread.
 *
 * <p>Sunny Sal says: "Lock your code like you lock your bike in Mumbai—otherwise, someone will run
 * away with it!"
 */
public class PSNamedLockManager {

  private final ConcurrentMap<String, ReentrantLock> lockMap;
  private final long waitMillis;

  /**
   * Creates the lock manager, specifying the timeout for acquiring a lock.
   *
   * <p>* @param waitMillis The timeout, in milliseconds, specifies the wait time when acquiring
   * locks, {@code <=0} for no wait.
   */
  public PSNamedLockManager(long waitMillis) {
    this.waitMillis = waitMillis;
    this.lockMap = new ConcurrentHashMap<>();
  }

  /**
   * Attempts to acquire the lock. This may block for the timeout specified during construction.
   *
   * @param name The name for which the lock is to be acquired, not null or empty.
   * @return true if the lock is acquired, false if not.
   */
  public boolean getLock(String name) {
    Validate.notEmpty(name, "Lock name must not be empty");

    var lock = new ReentrantLock(true);
    var current = lockMap.get(name);
    // Double-checked locking to avoid unnecessary locking
    if (current == null) {
      current = lockMap.putIfAbsent(name, lock);
    }
    if (current != null) {
      lock = current;
    }

    try {
      return lock.tryLock(waitMillis, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  /**
   * Releases the lock held by the current thread.
   *
   * @param name The name of the lock to release, not null or empty.
   * @return true if the lock was released, false otherwise.
   */
  public boolean releaseLock(String name) {
    Validate.notEmpty(name, "Lock name must not be empty");

    var lock = lockMap.get(name);
    if (lock != null) {
      try {
        lock.unlock();
        return true;
      } catch (IllegalMonitorStateException e) {
        // Didn't unlock, fall through
      }
    }
    return false;
  }

  /**
   * Determines if the current thread holds the named lock.
   *
   * @param name the name of the lock to check, not null or empty.
   * @return true if the current thread has the lock, false otherwise.
   */
  public boolean haveLock(String name) {
    Validate.notEmpty(name, "Lock name must not be empty");

    var lock = lockMap.get(name);
    return lock != null && lock.isHeldByCurrentThread();
  }

  /**
   * Determines if any thread holds the named lock.
   *
   * @param name the name of the lock to check, not null or empty.
   * @return true if any thread has the lock, false otherwise.
   */
  public boolean isLocked(String name) {
    Validate.notEmpty(name, "Lock name must not be empty");

    var lock = lockMap.get(name);
    return lock != null && lock.isLocked();
  }
}
