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
package com.percussion.monitor.process;

import com.percussion.monitor.service.IPSMonitor;
import com.percussion.monitor.service.PSMonitorService;
import com.percussion.search.PSSearchIndexEventQueue;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.services.notification.PSNotificationServiceLocator;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Monitor the status and size of the search index queue. Sunny Sal says: "Indexing? I'm on it, like
 * a librarian with a barcode scanner!"
 *
 * <p>Final so the constructor may publish {@code this} to the notification service without {@code
 * this-escape} (intentional register-on-construct).
 */
public final class PSSearchIndexProcessMonitor implements IPSNotificationListener {

  private static final String STATUS_MSG_SOME = " items in queue";
  private static final String STATUS_MSG_ONE = " item in queue";
  private static final String STATUS_MSG_NONE = ", no items in queue";

  private static IPSMonitor monitor;
  private static final AtomicInteger curCount = new AtomicInteger(0);
  private static String status;

  private ScheduledExecutorService executor;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final Object changeMonitor = new Object();
  private final AtomicBoolean changed = new AtomicBoolean(false);

  public PSSearchIndexProcessMonitor() {
    monitor = PSMonitorService.registerMonitor("SearchIndex", "Search indexing");
    IPSNotificationService notificationService =
        PSNotificationServiceLocator.getNotificationService();
    notificationService.addListener(EventType.SEARCH_INDEX_ITEM_PROCESSED, this);
    notificationService.addListener(EventType.SEARCH_INDEX_ITEM_QUEUED, this);
    notificationService.addListener(EventType.SEARCH_INDEX_STATUS_CHANGE, this);
    monitor.setMessage("Index queue not initialized");
  }

  /** Get the current status. */
  public static String getStatus() {
    return status;
  }

  /** Get the current count. */
  public static int getCount() {
    return curCount.get();
  }

  private void updateStatusMessage() {
    int count = curCount.get();
    var tmpStatus = status;
    var buf = new StringBuilder();
    buf.append(tmpStatus);
    if (count == 0) {
      buf.append(STATUS_MSG_NONE);
    } else {
      buf.append(", ");
      buf.append(count);
      buf.append(count == 1 ? STATUS_MSG_ONE : STATUS_MSG_SOME);
    }
    monitor.setMessage(buf.toString());
  }

  @Override
  public void notifyEvent(PSNotificationEvent notification) {
    var type = notification.getType();
    switch (type) {
      case SEARCH_INDEX_STATUS_CHANGE:
        var indexQueue = PSSearchIndexEventQueue.getInstance();
        status = indexQueue.getStatus();
        synchronized (changeMonitor) {
          if ("Running".equals(indexQueue.getStatus()) || "Paused".equals(indexQueue.getStatus())) {
            if (running.compareAndSet(false, true)) {
              if (executor == null) {
                executor = Executors.newScheduledThreadPool(1);
                executor.scheduleAtFixedRate(updater, 0, 5, TimeUnit.SECONDS);
                changed.set(true);
              }
            }
          } else {
            if (running.compareAndSet(true, false)) {
              executor.shutdown();
              executor = null;
              running.set(false);
            }
          }
        }
        // Same as item queued/processed: mark monitor dirty so the timer refreshes the message.
        changed.compareAndSet(false, true);
        break;
      case SEARCH_INDEX_ITEM_QUEUED:
      case SEARCH_INDEX_ITEM_PROCESSED:
        changed.compareAndSet(false, true);
        break;
      default:
        break;
    }
  }

  private final TimerTask updater =
      new TimerTask() {
        @Override
        public void run() {
          if (changed.compareAndSet(true, false)) {
            var indexQueue = PSSearchIndexEventQueue.getInstance();
            curCount.set(indexQueue.size());
            updateStatusMessage();
          }
        }
      };
}
