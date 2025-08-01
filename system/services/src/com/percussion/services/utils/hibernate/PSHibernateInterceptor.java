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
package com.percussion.services.utils.hibernate;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.notification.PSNotificationHelper;
import com.percussion.services.security.data.PSAccessLevelImpl;
import com.percussion.services.security.data.PSAclEntryImpl;
import com.percussion.utils.guid.IPSGuid;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.type.Type;

/**
 * Handle update events that should notify the memory subsystem to evict
 * in-memory cached objects. This interceptor uses modern Java 11 features
 * for enhanced performance and type safety.
 * <p>
 * The interceptor tracks entity changes during Hibernate transactions and
 * sends notifications when transactions complete to ensure cache consistency.
 *
 * @author dougrand
 */
public class PSHibernateInterceptor extends EmptyInterceptor {

    /**
     * Serialization global id
     */
    private static final long serialVersionUID = 1L;

    /**
     * Logger for this class
     */
    private static final Logger ms_log = LogManager.getLogger(PSHibernateInterceptor.class);

    /**
     * Event types that can be reported
     */
    public enum EventType {
        LOAD("load"),
        PERSIST("persist"),
        DELETE("delete");

        private final String eventName;

        EventType(String eventName) {
            this.eventName = eventName;
        }

        public String getEventName() {
            return eventName;
        }

        public static Optional<EventType> fromString(String eventName) {
            return Arrays.stream(values())
                .filter(type -> type.eventName.equals(eventName))
                .findFirst();
        }
    }

    /**
     * Configuration for reporting events
     */
    private final Set<EventType> reportedEvents = EnumSet.noneOf(EventType.class);

    /**
     * When a transaction starts, a set is pushed on the stack. The methods such
     * as {@code onSave} add guids to this set. When the transaction
     * finishes, it is popped off the stack and notifications are sent for each
     * noted changes. A stack is required because transactions can be nested. We
     * use thread local storage because transactions are bound to threads.
     */
    private static final ThreadLocal<Stack<Set<IPSGuid>>> ms_pendingChanges =
        ThreadLocal.withInitial(Stack::new);

    /**
     * Cache for reflection methods to improve performance
     */
    private static final Map<Class<?>, Optional<Method>> methodCache = new ConcurrentHashMap<>();

    /**
     * Constructor with modern Java 11 collection handling
     *
     * @param eventsString the events configured, may be {@code null} or empty
     */
    public PSHibernateInterceptor(List<String> eventsString) {
        if (eventsString != null && !eventsString.isEmpty()) {
            eventsString.stream()
                .map(EventType::fromString)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(reportedEvents::add);
        }

        ms_log.info("PSHibernateInterceptor initialized with events: {}", reportedEvents);
    }

    /**
     * Constructor with no events (all disabled)
     */
    public PSHibernateInterceptor() {
        this(Collections.emptyList());
    }

    @Override
    public void onDelete(Object entity,
                        @SuppressWarnings("unused") Serializable id,
                        @SuppressWarnings("unused") Object[] state,
                        @SuppressWarnings("unused") String[] propertyNames,
                        @SuppressWarnings("unused") Type[] types) {

        if (reportedEvents.contains(EventType.DELETE)) {
            reportEvent(EventType.DELETE, entity.getClass().getName());
        }

        getGuidFromObject(entity).ifPresent(this::addGuid);
    }

    /**
     * Add a guid to the notification list with null safety
     *
     * @param guid guid, must not be {@code null}
     * @throws IllegalStateException if no pending changes stack exists
     */
    private void addGuid(IPSGuid guid) {
        Objects.requireNonNull(guid, "GUID cannot be null");

        var stack = ms_pendingChanges.get();
        if (stack.isEmpty()) {
            ms_log.warn("Attempting to add GUID {} but no transaction is active", guid);
            return;
        }

        var current = stack.peek();
        current.add(guid);
        ms_log.debug("Added GUID {} to pending changes", guid);
    }

    @Override
    public boolean onSave(Object entity,
                         @SuppressWarnings("unused") Serializable id,
                         @SuppressWarnings("unused") Object[] state,
                         @SuppressWarnings("unused") String[] propertyNames,
                         @SuppressWarnings("unused") Type[] types) {

        if (reportedEvents.contains(EventType.PERSIST)) {
            reportEvent(EventType.PERSIST, entity.getClass().getName());
        }

        getGuidFromObject(entity).ifPresent(this::addGuid);
        return false; // Don't veto the save
    }

    @Override
    public boolean onLoad(Object entity,
                         @SuppressWarnings("unused") Serializable id,
                         @SuppressWarnings("unused") Object[] state,
                         @SuppressWarnings("unused") String[] propertyNames,
                         @SuppressWarnings("unused") Type[] types) {

        if (reportedEvents.contains(EventType.LOAD)) {
            reportEvent(EventType.LOAD, entity.getClass().getName());
        }

        return false; // Don't modify the entity
    }

    @Override
    public void afterTransactionBegin(Transaction tx) {
        super.afterTransactionBegin(tx);

        var stack = ms_pendingChanges.get();
        stack.push(new HashSet<>());
        ms_log.debug("Transaction begun, pushed new change set. Stack depth: {}", stack.size());
    }

    @Override
    public void afterTransactionCompletion(Transaction tx) {
        super.afterTransactionCompletion(tx);

        var stack = ms_pendingChanges.get();
        if (stack.isEmpty()) {
            ms_log.warn("Transaction completed but no pending changes found");
            return;
        }

        var changes = stack.pop();
        ms_log.debug("Transaction completed. Stack depth: {}, Changes: {}", stack.size(), changes.size());

        // Only send notifications if transaction was successful
        if (tx.getStatus() == TransactionStatus.COMMITTED && !changes.isEmpty()) {
            sendNotifications(changes);
        }

        // Clean up thread local if stack is empty
        if (stack.isEmpty()) {
            ms_pendingChanges.remove();
        }
    }

    /**
     * Send notifications for all changed GUIDs
     *
     * @param changes the set of changed GUIDs, not {@code null}
     */
    private void sendNotifications(Set<IPSGuid> changes) {
        Objects.requireNonNull(changes, "Changes set cannot be null");

        ms_log.debug("Sending {} cache invalidation notifications", changes.size());

        changes.forEach(guid -> {
            try {
                PSNotificationHelper.notifyMemoryEvent(guid);
                ms_log.trace("Sent notification for GUID: {}", guid);
            } catch (Exception e) {
                ms_log.error("Failed to send notification for GUID: {}", guid, e);
            }
        });
    }

    /**
     * Report an event with modern logging
     *
     * @param eventType the type of event
     * @param className the class name involved in the event
     */
    private void reportEvent(EventType eventType, String className) {
        ms_log.info("Hibernate event: {} on class: {}", eventType.getEventName(), className);
    }

    /**
     * Extract GUID from an object using cached reflection for performance
     *
     * @param obj the object to extract GUID from, may be {@code null}
     * @return Optional containing the GUID if found, empty otherwise
     */
    private Optional<IPSGuid> getGuidFromObject(Object obj) {
        if (obj == null) {
            return Optional.empty();
        }

        // Handle known special cases first for performance
        if (obj instanceof PSAccessLevelImpl accessLevel) {
            return Optional.ofNullable(accessLevel.getGUID());
        }

        if (obj instanceof PSAclEntryImpl aclEntry) {
            return Optional.ofNullable(aclEntry.getGUID());
        }

        // Use cached reflection for other objects
        var objClass = obj.getClass();
        var method = methodCache.computeIfAbsent(objClass, this::findGuidMethod);

        if (method.isEmpty()) {
            ms_log.trace("No GUID method found for class: {}", objClass.getName());
            return Optional.empty();
        }

        try {
            var guid = method.get().invoke(obj);
            return Optional.ofNullable((IPSGuid) guid);
        } catch (IllegalAccessException | InvocationTargetException e) {
            ms_log.debug("Failed to invoke GUID method on {}: {}", objClass.getName(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Find the GUID method for a given class using modern method resolution
     *
     * @param clazz the class to search for GUID method
     * @return Optional containing the method if found, empty otherwise
     */
    private Optional<Method> findGuidMethod(Class<?> clazz) {
        // Try common GUID method names
        var methodNames = List.of("getGUID", "getGuid", "getId");

        for (var methodName : methodNames) {
            try {
                var method = clazz.getMethod(methodName);
                if (IPSGuid.class.isAssignableFrom(method.getReturnType())) {
                    ms_log.debug("Found GUID method {} for class {}", methodName, clazz.getName());
                    return Optional.of(method);
                }
            } catch (NoSuchMethodException e) {
                // Continue searching
            }
        }

        ms_log.debug("No suitable GUID method found for class: {}", clazz.getName());
        return Optional.empty();
    }

    /**
     * Get the current set of reported events
     *
     * @return an unmodifiable set of reported event types
     */
    public Set<EventType> getReportedEvents() {
        return Collections.unmodifiableSet(reportedEvents);
    }

    /**
     * Check if a specific event type is being reported
     *
     * @param eventType the event type to check
     * @return {@code true} if the event type is being reported, {@code false} otherwise
     */
    public boolean isEventReported(EventType eventType) {
        return reportedEvents.contains(eventType);
    }

    /**
     * Get statistics about the current pending changes
     *
     * @return a map containing statistics about pending changes
     */
    public Map<String, Object> getPendingChangesStats() {
        var stack = ms_pendingChanges.get();
        var stats = new HashMap<String, Object>();

        stats.put("stackDepth", stack.size());
        stats.put("totalPendingChanges",
            stack.stream().mapToInt(Set::size).sum());
        stats.put("reportedEvents", reportedEvents.size());

        return Collections.unmodifiableMap(stats);
    }

    @Override
    public String toString() {
        return String.format("PSHibernateInterceptor[reportedEvents=%s, pendingChanges=%s]",
            reportedEvents, getPendingChangesStats());
    }
}
