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

// REFACTORED: CP-JAVA11
package com.percussion.searchmanagement.service.impl;

import com.percussion.design.objectstore.PSLocator;
import com.percussion.search.PSSearchIndexEventQueue;
import com.percussion.security.error.PSExceptionUtils;
import jakarta.inject.Singleton;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Assists in loading locators into the search index queue using a background thread.
 *
 * <p>Final so the constructor may start a daemon thread with {@code this} as the runnable without
 * {@code this-escape}.
 */
@Component("indexHelper")
@Singleton
public final class PSIndexHelper implements Runnable {
  private static final Logger log = LogManager.getLogger(PSIndexHelper.class);

  private final PSSearchIndexEventQueue queue = PSSearchIndexEventQueue.getInstance();
  private final CopyOnWriteArrayList<PSLocator> ids = new CopyOnWriteArrayList<>();
  private static final Object lock = new Object();
  private final Thread thread;

  public PSIndexHelper() {
    thread = new Thread(this);
    thread.setDaemon(true);
    thread.start();
  }

  /** Add items to the concurrent data structure for background processing. */
  // TODO: Remove me @SuppressFBWarnings("NN_NAKED_NOTIFY")
  public void addItemsForIndex(Set<PSLocator> locas) {
    try {
      ids.addAll(locas);
    } catch (Exception e) {
      log.warn(
          "Could not add Item ids to be indexed: {} Error: {}",
          getClass().getName(),
          PSExceptionUtils.getMessageForLog(e));
    } finally {
      synchronized (lock) {
        lock.notifyAll();
      }
    }
  }

  /** The real work of the background process. Adds the locators into the search index queue. */
  public void index() throws InterruptedException {
    synchronized (lock) {
      while (ids.isEmpty()) {
        try {
          lock.wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }

    try {
      for (var locator : ids) {
        queue.indexItem(locator);
        ids.remove(locator);
      }
    } catch (Exception e) {
      log.warn(
          "Trouble adding content to search index queue - {} Error: {}",
          PSIndexHelper.class.getName(),
          PSExceptionUtils.getMessageForLog(e));
    }
  }

  @Override
  public void run() {
    do {
      try {
        index();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    } while (!Thread.currentThread().isInterrupted());
  }
}
