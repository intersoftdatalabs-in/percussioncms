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
package com.percussion.services.notification;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A single notification event with Java 11 modernization. This object is immutable after construction
 * and provides comprehensive event information for the notification system.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>Immutable design with enhanced validation</li>
 * <li>Optional-based safe access for target objects</li>
 * <li>Modern time handling with Instant</li>
 * <li>AtomicLong for thread-safe message ID generation</li>
 * <li>Factory methods for type-safe event creation</li>
 * </ul>
 *
 * @author dougrand
 */
public final class PSNotificationEvent implements Serializable {

    /**
     * Serial version UID for serialization compatibility.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Message ID generator to identify messages being sent.
     */
    private static final AtomicLong MESSAGE_ID_GENERATOR = new AtomicLong();

    /**
     * The unique message ID for this event.
     */
    private final long messageId;

    /**
     * The type of this notification event.
     */
    private final EventType eventType;

    /**
     * The target object associated with this event.
     */
    private final Object target;

    /**
     * The timestamp when this event was created.
     */
    private final Instant timestamp;

    /**
     * Optional source identifier for the event.
     */
    private final String source;

    /**
     * Optional server type associated with the event. Mutable for compatibility with legacy callers.
     */
    private String serverType = null;

    /**
     * An enumeration of event types that may occur with enhanced documentation.
     */
    public enum EventType {
        /**
         * A file has been modified. The target object of the notification will be
         * of type {@link java.io.File}.
         */
        FILE(true, "File modification event"),

        /**
         * A cached in-memory object has been invalidated, either because it has
         * been deleted or because it has been modified. This event's target is
         * the modified object's key for the object cache. This uses a queue as
         * the actual invalidation is propagated by ehcache.
         */
        OBJECT_INVALIDATION(false, "Object cache invalidation event"),

        /**
         * An event that is signaled when a content item is changed. The target
         * will be the GUID of the changed item.
         */
        CONTENT_CHANGED(false, "Content item modification event"),

        /**
         * An event that is signaled when a site is deleted. The target
         * will be the GUID of the deleted site.
         */
        SITE_DELETED(false, "Site deletion event"),

        /**
         * An event that is signaled when a site is renamed. The target
         * will be the IPSSite object.
         */
        SITE_RENAMED(false, "Site rename event"),

        /**
         * The CMS (Core) Server has completed its initialization.
         * No Solutions have been initialized yet, and this is a signal that
         * it is OK to do so.
         */
        CORE_SERVER_INITIALIZED(false, "Core server initialization complete"),

        /**
         * This is the final event in server initialization, called after all notifications
         * for {@link #CORE_SERVER_INITIALIZED} have been processed.
         */
        CORE_SERVER_POST_INIT(false, "Core server post-initialization complete"),

        /**
         * Signaling the CMS (Core) Server is in the shutdown process.
         */
        CORE_SERVER_SHUTDOWN(false, "Core server shutdown initiated"),

        /**
         * A set of relationships have been modified. The target object of the
         * notification is {@link com.percussion.cms.PSRelationshipChangeEvent}.
         */
        RELATIONSHIP_CHANGED(false, "Relationship modification event"),

        /**
         * A workflow state transition has occurred. The target object contains
         * workflow transition information.
         */
        WORKFLOW_TRANSITION(false, "Workflow state transition event"),

        /**
         * A template has been modified or updated. The target object is the
         * template identifier or template object.
         */
        TEMPLATE_CHANGED(false, "Template modification event"),

        /**
         * User session events such as login/logout. The target object contains
         * session information.
         */
        USER_SESSION(false, "User session event"),

        /**
         * Security-related events such as permission changes. The target object
         * contains security context information.
         */
        SECURITY_EVENT(false, "Security-related event"),

        /**
         * Workflow folder assignment queueing notification.
         */
        WORKFLOW_FOLDER_ASSIGNMENT_QUEUEING(true, "Workflow folder assignment queueing"),

        /**
         * Workflow folder assignment processing notification.
         */
        WORKFLOW_FOLDER_ASSIGNMENT_PROCESSING(true, "Workflow folder assignment processing"),

        /**
         * A JMS error occurred that should be treated as a notification event.
         */
        JMS_ERROR(false,
                "JMS error event"),
                
        SEARCH_INDEX_ITEM_PROCESSED(true, 
        "Search index item processed"), 
        SEARCH_INDEX_STATUS_CHANGE(true, 
        "Search index status change"), 
        SEARCH_INDEX_ITEM_QUEUED(true, 
        "Search index item queued");

        private final boolean usesQueue;
        private final String description;

        EventType(boolean usesQueue, String description) {
            this.usesQueue = usesQueue;
            this.description = Objects.requireNonNull(description, "description cannot be null");
        }

        /**
         * Check if this event type uses a queue for processing.
         *
         * @return {@code true} if uses queue, {@code false} otherwise
         */
        public boolean usesQueue() {
            return usesQueue;
        }

        /**
         * Get a human-readable description of this event type.
         *
         * @return the description, never {@code null}
         */
        public String getDescription() {
            return description;
        }
    }

    /**
     * Private constructor for creating notification events.
     *
     * @param eventType the type of event, not {@code null}
     * @param target the target object, may be {@code null}
     * @param source the source identifier, may be {@code null}
     */
    public PSNotificationEvent(EventType eventType, Object target, String source) {
        this.messageId = MESSAGE_ID_GENERATOR.incrementAndGet();
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        this.target = target;
        this.timestamp = Instant.now();
        this.source = source;
    }

    /**
     * Factory method to create a notification event.
     *
     * @param eventType the type of event, not {@code null}
     * @param target the target object, may be {@code null}
     * @return a new notification event
     * @throws IllegalArgumentException if eventType is null
     */
    public static PSNotificationEvent create(EventType eventType, Object target) {
        return new PSNotificationEvent(eventType, target, null);
    }

    /**
     * Factory method to create a notification event with source information.
     *
     * @param eventType the type of event, not {@code null}
     * @param target the target object, may be {@code null}
     * @param source the source identifier, may be {@code null}
     * @return a new notification event
     * @throws IllegalArgumentException if eventType is null
     */
    public static PSNotificationEvent create(EventType eventType, Object target, String source) {
        return new PSNotificationEvent(eventType, target, source);
    }

    /**
     * Factory method for file events.
     *
     * @param file the file that was modified, not {@code null}
     * @return a new file notification event
     * @throws IllegalArgumentException if file is null
     */
    public static PSNotificationEvent createFileEvent(java.io.File file) {
        Objects.requireNonNull(file, "file cannot be null");
        return create(EventType.FILE, file);
    }

    /**
     * Factory method for content change events.
     *
     * @param contentGuid the GUID of the changed content, not {@code null}
     * @return a new content change notification event
     * @throws IllegalArgumentException if contentGuid is null
     */
    public static PSNotificationEvent createContentChangeEvent(Object contentGuid) {
        Objects.requireNonNull(contentGuid, "contentGuid cannot be null");
        return create(EventType.CONTENT_CHANGED, contentGuid);
    }

    /**
     * Factory method for object invalidation events.
     *
     * @param cacheKey the cache key of the invalidated object, not {@code null}
     * @return a new object invalidation notification event
     * @throws IllegalArgumentException if cacheKey is null
     */
    public static PSNotificationEvent createObjectInvalidationEvent(Object cacheKey) {
        Objects.requireNonNull(cacheKey, "cacheKey cannot be null");
        return create(EventType.OBJECT_INVALIDATION, cacheKey);
    }

    /**
     * Factory method for server lifecycle events.
     *
     * @param eventType the server event type, must be a server-related event
     * @return a new server lifecycle notification event
     * @throws IllegalArgumentException if eventType is null or not server-related
     */
    public static PSNotificationEvent createServerEvent(EventType eventType) {
        Objects.requireNonNull(eventType, "eventType cannot be null");
        if (!isServerEvent(eventType)) {
            throw new IllegalArgumentException("Event type must be server-related: " + eventType);
        }
        return create(eventType, null);
    }

    /**
     * Check if an event type is server-related.
     */
    private static boolean isServerEvent(EventType eventType) {
        return eventType == EventType.CORE_SERVER_INITIALIZED ||
               eventType == EventType.CORE_SERVER_POST_INIT ||
               eventType == EventType.CORE_SERVER_SHUTDOWN;
    }

    /**
     * Get the unique message ID for this event.
     *
     * @return the message ID
     */
    public long getMessageId() {
        return messageId;
    }

    /**
     * Get the event type.
     *
     * @return the event type, never {@code null}
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * Backwards compatible alias for {@link #getEventType()} used by older call sites.
     *
     * @return the event type, never {@code null}
     */
    public EventType getType() {
        return getEventType();
    }

    /**
     * Public two-argument constructor retained for backward compatibility.
     *
     * @param eventType the type of event, not {@code null}
     * @param target the target object, may be {@code null}
     */
    public PSNotificationEvent(EventType eventType, Object target) {
        this(eventType, target, null);
    }

    /**
     * Get the target object associated with this event.
     *
     * @return the target object, may be {@code null}
     */
    public Object getTarget() {
        return target;
    }

    /**
     * Get the target object safely wrapped in an Optional.
     *
     * @return an Optional containing the target object, empty if null
     */
    public Optional<Object> getTargetSafely() {
        return Optional.ofNullable(target);
    }

    /**
     * Get the target object cast to a specific type safely.
     *
     * @param <T> the expected type
     * @param targetClass the class to cast to, not {@code null}
     * @return an Optional containing the cast target, empty if null or wrong type
     * @throws IllegalArgumentException if targetClass is null
     */
    public <T> Optional<T> getTargetAs(Class<T> targetClass) {
        Objects.requireNonNull(targetClass, "targetClass cannot be null");
        return getTargetSafely()
            .filter(targetClass::isInstance)
            .map(targetClass::cast);
    }

    /**
     * Get the timestamp when this event was created.
     *
     * @return the timestamp, never {@code null}
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Get the source identifier for this event.
     *
     * @return an Optional containing the source, empty if not specified
     */
    public Optional<String> getSource() {
        return Optional.ofNullable(source);
    }

    /**
     * Get the server type associated with this event, if any.
     *
     * @return an Optional containing the server type, empty if not specified
     */
    public Optional<String> getServerType() {
        return Optional.ofNullable(serverType);
    }

    /**
     * Sets the server type for this event. This exists for compatibility with older call sites
     * that expect to be able to set a server type on an event.
     *
     * @param serverType the server type to set, may be {@code null}
     */
    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    /**
     * Check if this event uses queue processing.
     *
     * @return {@code true} if uses queue, {@code false} otherwise
     */
    public boolean usesQueue() {
        return eventType.usesQueue();
    }

    /**
     * Get a human-readable description of this event.
     *
     * @return the event description, never {@code null}
     */
    public String getDescription() {
        return eventType.getDescription();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PSNotificationEvent)) return false;
        var other = (PSNotificationEvent) obj;
        return messageId == other.messageId &&
               eventType == other.eventType &&
               Objects.equals(target, other.target) &&
               Objects.equals(timestamp, other.timestamp) &&
               Objects.equals(source, other.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, eventType, target, timestamp, source);
    }

    @Override
    public String toString() {
        return "PSNotificationEvent{" +
            "messageId=" + messageId +
            ", eventType=" + eventType +
            ", target=" + target +
            ", timestamp=" + timestamp +
            ", source='" + source + '\'' +
            '}';
    }
}
