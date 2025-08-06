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
package com.percussion.sitemanage.importer;

import com.percussion.sitemanage.importer.data.PSImportLogEntry;
import java.util.Date;
import java.util.List;

/**
 * Logging interface for site or template import operations.
 */
public interface IPSSiteImportLogger {
    /**
     * Type of log entry, used in {@link #appendLogMessage(PSLogEntryType, String, String)}.
     */
    enum PSLogEntryType {
        STATUS,
        ERROR
    }

    /**
     * Types of objects for which log entries are created.
     */
    enum PSLogObjectType {
        SITE,
        TEMPLATE,
        PAGE,
        SITE_ERROR
    }

    /**
     * Appends an entry to the current import log.
     *
     * @param type     The type of entry, not null.
     * @param category The category, not null or empty.
     * @param message  The message to log, not null or empty.
     */
    void appendLogMessage(PSLogEntryType type, String category, String message);

    /**
     * Gets the log built for the current import.
     *
     * @return The log as a String, never null but may be empty.
     */
    String getLog();

    /**
     * Gets the type of log.
     *
     * @return The type, never null.
     */
    PSLogObjectType getType();

    /**
     * Collects errors when {@link #appendLogMessage(PSLogEntryType, String, String)} is called with
     * {@link PSLogEntryType#ERROR}. Errors can be retrieved via {@link #getErrors(PSLogObjectType, String, String)}.
     */
    void logErrors();

    /**
     * Gets the list of error log entries collected.
     *
     * @param errorObjectType The error type for the log entries, not null.
     * @param errorObjectId   The object id for the log entries, not blank.
     * @param description     Description of the object being imported.
     * @return The list, may be empty; null if {@link #logErrors()} has not been called.
     */
    List<PSImportLogEntry> getErrors(PSLogObjectType errorObjectType, String errorObjectId, String description);

    /**
     * Sets the count of threads that must complete before the log is saved.
     *
     * @param count The number of threads to wait for.
     */
    void setWaitCount(int count);

    /**
     * Removes a thread from the wait count.
     */
    void removeFromWaitCount();

    /**
     * Waits for threads to complete before saving the log.
     *
     * @param timeoutSeconds The number of seconds to wait before continuing without reaching zero.
     */
    void waitForThreads(long timeoutSeconds);
}
