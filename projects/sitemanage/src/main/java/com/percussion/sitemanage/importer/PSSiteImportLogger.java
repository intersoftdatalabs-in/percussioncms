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
package com.percussion.sitemanage.importer;

import com.percussion.sitemanage.importer.data.PSImportLogEntry;
import com.percussion.utils.types.PSPair;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Logger for site/template import operations.
 * Thread-safe for concurrent logging and waiting.
 */
public class PSSiteImportLogger implements IPSSiteImportLogger {

    private PSLogObjectType objectType;
    private final StringBuilder log;
    private static final String LOG_MSG_SEP = ": ";
    private List<PSPair<String, String>> errorLogMessages;
    private CountDownLatch waitingThreadCount;

    /**
     * Constructs a logger for the specified object type.
     *
     * @param objectType The type of object being imported.
     */
    public PSSiteImportLogger(PSLogObjectType objectType) {
        this.objectType = Objects.requireNonNull(objectType);
        this.log = new StringBuilder();
    }

    @Override
    public void appendLogMessage(PSLogEntryType type, String category, String message) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(category);
        Objects.requireNonNull(message);

        log.append(type)
           .append(LOG_MSG_SEP)
           .append(category)
           .append(LOG_MSG_SEP)
           .append(message)
           .append("\n");

        if (type.equals(PSLogEntryType.ERROR) && errorLogMessages != null) {
            errorLogMessages.add(new PSPair<>(category, message));
        }
    }

    /**
     * Gets the current log buffer as a String.
     */
    public String getLog() {
        return log.toString();
    }

    @Override
    public PSLogObjectType getType() {
        return objectType;
    }

    @Override
    public void logErrors() {
        errorLogMessages = new ArrayList<>();
    }

    @Override
    public List<PSImportLogEntry> getErrors(PSLogObjectType type, String objectId, String description) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(objectId);
        Objects.requireNonNull(description);

        if (errorLogMessages == null) {
            return null;
        }
        var result = new ArrayList<PSImportLogEntry>();
        for (var message : errorLogMessages) {
            result.add(new PSImportLogEntry(objectId, type.name(), new Date(), description, message.getFirst(), message.getSecond()));
        }
        return result;
    }

    @Override
    public synchronized void setWaitCount(int count) {
        if (waitingThreadCount != null) {
            throw new IllegalStateException("Wait count has already been set on this object.");
        }
        waitingThreadCount = new CountDownLatch(count);
    }

    @Override
    public void removeFromWaitCount() {
        if (waitingThreadCount != null) {
            waitingThreadCount.countDown();
        }
    }

    @Override
    public void waitForThreads(long timeoutSeconds) {
        if (waitingThreadCount == null) {
            return;
        }
        try {
            waitingThreadCount.await(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
