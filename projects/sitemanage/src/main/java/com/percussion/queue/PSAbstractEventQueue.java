// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

package com.percussion.queue;

import com.percussion.queue.impl.PSSiteQueue;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.utils.types.PSPair;

/**
 * A generic base class used for managing a queue. The queue can be either in memory (light weight) or
 * persistent (heavy weight) queue.
 * <p>
 * This class is inspired by PSSearchIndexEventQueue. It is possible to reuse this code
 * with PSSearchIndexEventQueue in the future, so that there is no copy and paste code.
 *
 * @author YuBingChen
 * @param <T> the queue set
 */
public abstract class PSAbstractEventQueue<T> {

    protected abstract String getQueueName();
    protected abstract void preStart();
    protected abstract boolean doRun();
    protected abstract void preShutdown();
    protected abstract PSPair<PSSiteQueue, Integer> getNextEvent();

    /**
     * Causes any persisted events to be restored from the repository, and starts
     * the thread that processes queued events.
     *
     * @throws IllegalStateException if the queue has already been started or is
     * shutting down.
     */
    public void start() {
        synchronized (runMonitor) {
            if (run) {
                throw new IllegalStateException("Index queue is already running");
            }
            if (shutdown) {
                throw new IllegalStateException("Index queue is shutting down");
            }
            preStart();
            queueThread = new Thread(getQueueName()) {
                @Override
                public void run() {
                    PSRequestInfoBase.initRequestInfo(null);
                    while (!shutdown) {
                        if (!doRun()) {
                            break;
                        }
                    }
                    PSRequestInfoBase.resetRequestInfo();
                    run = false;
                    synchronized (runMonitor) {
                        runMonitor.notifyAll();
                    }
                }
            };
            queueThread.setDaemon(true);
            queueThread.start();
            run = true;
        }
    }

    /**
     * Stops processing queued events and shuts down the queue.  Will not return
     * until it has completed any work in progress.  Calling this multiple times
     * is safe.
     */
    public void shutdown() {
        synchronized (shutdownMonitor) {
            if (shutdown) {
                return;
            }
            shutdown = true;
        }
        synchronized (queueMonitor) {
            queueMonitor.notifyAll();
        }
        synchronized (runMonitor) {
            while (run) {
                try {
                    runMonitor.wait(5000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            preShutdown();
            shutdown = false;
        }
    }

    /**
     * Get next event which is returned by {@link #getNextEvent()}.
     * If there is nothing in the queue (hence the {@link #getNextEvent()}
     * return null), then this will wait for a limited time before
     * returning, but will not process any new events that are ready after
     * waiting. In this case, the caller in different thread may call
     * {@link #notifyEventQueue()} to wake up this and return null.
     *
     * @param timeOut The number of milliseconds to wait before returning.
     *                Pass 0 to wait indefinitely, negative is the same as 0.
     * @return The next set of events to process, may be null if
     * there are no events in the queue or if we waited.
     * @throws InterruptedException if interrupted while waiting on the queue
     * monitor.
     */
    protected PSPair<PSSiteQueue, Integer> getNextQueueEvent(long timeOut) throws InterruptedException {
        timeOut = timeOut <= 0 ? 100 : timeOut;
        PSPair<PSSiteQueue, Integer> eventSet = null;
        synchronized (queueMonitor) {
            if (!isShutdown()) {
                while (eventSet == null) {
                    eventSet = getNextEvent();
                    queueMonitor.wait(timeOut);
                }
            }
        }
        return eventSet;
    }

    protected void notifyEventQueue() {
        if (run) {
            synchronized (queueMonitor) {
                queueMonitor.notifyAll();
            }
        }
    }

    protected boolean isShutdown() {
        return shutdown;
    }

    private final Object runMonitor = new Object();
    private final Object queueMonitor = new Object();
    private final Object shutdownMonitor = new Object();
    private boolean run = false;
    private Thread queueThread;
    private boolean shutdown = false;
}
