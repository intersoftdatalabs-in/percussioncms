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
package com.percussion.services.contentchange.impl;

import com.percussion.cms.IPSConstants;
import com.percussion.cms.IPSEditorChangeListener;
import com.percussion.cms.PSEditorChangeEvent;
import com.percussion.cms.PSRelationshipChangeEvent;
import com.percussion.cms.handlers.PSContentEditorHandler;
import com.percussion.server.IPSHandlerInitListener;
import com.percussion.server.IPSRequestHandler;
import com.percussion.server.PSServer;
import com.percussion.services.contentchange.IPSContentChangeHandler;
import com.percussion.services.contentchange.IPSContentChangeService;
import com.percussion.services.contentchange.PSContentChangeServiceLocator;
import com.percussion.services.contentchange.data.PSContentChangeEvent;
import com.percussion.services.contentchange.data.PSContentChangePK;
import com.percussion.services.contentchange.data.PSContentChangeType;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.exception.PSDataServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.MutationQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Modern Java 11 implementation of the Content Change Service.
 *
 * <p>This service provides comprehensive content change tracking capabilities with:
 * <ul>
 *   <li>Thread-safe event handling and handler management</li>
 *   <li>Stream-based data processing for efficiency</li>
 *   <li>Enhanced error handling and logging</li>
 *   <li>Modern Spring and Hibernate integration</li>
 * </ul>
 *
 * <p>The service implements multiple interfaces to integrate with the CMS
 * editor change notification system and provide change tracking for
 * incremental publishing scenarios.
 *
 * @author JaySeletz
 * @since Java 11 Modernization
 */
@Service("sys_contentChangeService")
@Transactional
public final class PSContentChangeService implements IPSContentChangeService,
        IPSEditorChangeListener, IPSHandlerInitListener, IPSNotificationListener {

    private static final Logger log = LogManager.getLogger(IPSConstants.CONTENTREPOSITORY_LOG);

    /**
     * Constant for the GUID manager key used to generate IDs.
     */
    private static final String GUID_MGR_KEY = "PSX_CONTENTCHANGEEVENT";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired(required = false)
    private IPSGuidManager guidManager;

    /**
     * Thread-safe list of content change handlers.
     */
    private final List<IPSContentChangeHandler> changeHandlers = new CopyOnWriteArrayList<>();

    /**
     * Cache for efficient content ID lookups.
     */
    private final Set<String> contentChangeCache = ConcurrentHashMap.newKeySet();

    /**
     * Constructs a new content change service and registers with the server.
     */
    public PSContentChangeService() {
        PSServer.addInitListener(this);
    }

    @Override
    @Transactional
    public void contentChanged(PSContentChangeEvent changeEvent) throws PSDataServiceException {
        Objects.requireNonNull(changeEvent, "Change event cannot be null");

        var session = getSession();
        try {
            var primaryKey = new PSContentChangePK(
                changeEvent.getContentId(),
                changeEvent.getSiteId(),
                changeEvent.getChangeType().name()
            );

            var existingEvent = session.get(PSContentChangeEvent.class, primaryKey);

            if (existingEvent == null) {
                session.merge(changeEvent);
                updateCache(changeEvent);
                log.debug("Saved content change event: {}", changeEvent);
            } else {
                log.debug("Content change event already exists: {}", changeEvent);
            }
        } catch (HibernateException e) {
            var msg = "Database error while saving content change event: " + e.getMessage();
            log.error(msg, e);
            throw new PSDataServiceException(msg, e);
        }
    }

    @Override
    public List<Integer> getChangedContent(long siteId, PSContentChangeType changeType) {
        Objects.requireNonNull(changeType, "Change type cannot be null");

        var session = getSession();

        try {
            var query = session.createQuery(
                "SELECT ce.contentId FROM PSContentChangeEvent ce " +
                "WHERE ce.changeType = :changeType AND ce.siteId = :siteId",
                Integer.class
            );
            query.setParameter("changeType", changeType.name());
            query.setParameter("siteId", siteId);

            var results = query.getResultList();
            log.debug("Found {} changed content items for site {} and type {}",
                     results.size(), siteId, changeType);

            return results.stream()
                .distinct()
                .collect(Collectors.toUnmodifiableList());

        } catch (HibernateException e) {
            log.error("Error retrieving changed content for site {} and type {}: {}",
                     siteId, changeType, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    @Transactional
    public void deleteChangeEvents(long siteId, int contentId, PSContentChangeType changeType) {
        Objects.requireNonNull(changeType, "Change type cannot be null");

        var session = getSession();

        try {
            MutationQuery query = session.createMutationQuery(deleteChangeEventsHql(siteId != -1));
            query.setParameter("contentId", contentId);
            query.setParameter("changeType", changeType.name());

            if (siteId != -1) {
                query.setParameter("siteId", siteId);
            }

            var deletedCount = query.executeUpdate();
            removeFromCache(contentId, siteId, changeType);

            log.debug("Deleted {} change events for content {} on site {} with type {}",
                     deletedCount, contentId, siteId, changeType);

        } catch (HibernateException e) {
            log.error("Error deleting change events for content {} on site {}: {}",
                     contentId, siteId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete change events", e);
        }
    }

    @Override
    @Transactional
    public void deleteChangeEventsForSite(long siteId) {
        var session = getSession();

        try {
            MutationQuery query = session.createMutationQuery(DELETE_CHANGE_EVENTS_FOR_SITE_HQL);
            query.setParameter("siteId", siteId);

            var deletedCount = query.executeUpdate();
            clearCacheForSite(siteId);

            log.debug("Deleted {} change events for site {}", deletedCount, siteId);

        } catch (HibernateException e) {
            log.error("Error deleting change events for site {}: {}", siteId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete change events for site", e);
        }
    }

    @Override
    @Transactional
    public void deleteChangeEventsForSite(long siteId, PSContentChangeType changeType) {
        Objects.requireNonNull(changeType, "Change type cannot be null");

        var session = getSession();

        try {
            MutationQuery query = session.createMutationQuery(DELETE_CHANGE_EVENTS_FOR_SITE_TYPE_HQL);
            query.setParameter("siteId", siteId);
            query.setParameter("changeType", changeType.name());

            var deletedCount = query.executeUpdate();
            clearCacheForSite(siteId, changeType);

            log.debug("Deleted {} change events for site {} with type {}",
                     deletedCount, siteId, changeType);

        } catch (HibernateException e) {
            log.error("Error deleting change events for site {} with type {}: {}",
                     siteId, changeType, e.getMessage(), e);
            throw new RuntimeException("Failed to delete change events for site and type", e);
        }
    }

    @Override
    public void addContentChangeHandler(IPSContentChangeHandler handler) {
        Objects.requireNonNull(handler, "Handler cannot be null");

        if (!changeHandlers.contains(handler)) {
            changeHandlers.add(handler);
            log.debug("Added content change handler: {}", handler.getClass().getSimpleName());
        }
    }

    @Override
    public boolean removeContentChangeHandler(IPSContentChangeHandler handler) {
        Objects.requireNonNull(handler, "Handler cannot be null");

        var removed = changeHandlers.remove(handler);
        if (removed) {
            log.debug("Removed content change handler: {}", handler.getClass().getSimpleName());
        }
        return removed;
    }

    @Override
    public Set<IPSContentChangeHandler> getContentChangeHandlers() {
        return Set.copyOf(changeHandlers);
    }

    @Override
    @Transactional
    public void editorChanged(PSEditorChangeEvent event) {
        Objects.requireNonNull(event, "Editor change event cannot be null");

        try {
            changeHandlers.parallelStream()
                .forEach(handler -> {
                    try {
                        handler.handleEvent(event);
                    } catch (Exception e) {
                        log.error("Handler {} failed to process editor change event: {}",
                                 handler.getClass().getSimpleName(), e.getMessage(), e);
                    }
                });
        } catch (Exception e) {
            // Don't fail the entire transaction due to handler errors
            log.error("Failed to handle editor change event: {}", e.getMessage(), e);
        }
    }

    @Override
    public void notifyEvent(PSNotificationEvent notification) throws PSDataServiceException, PSNotFoundException {
        Objects.requireNonNull(notification, "Notification cannot be null");

        var target = notification.getTarget();
        if (target instanceof PSRelationshipChangeEvent) {
            PSRelationshipChangeEvent relationshipEvent = (PSRelationshipChangeEvent) target;
            changeHandlers.parallelStream()
                .forEach(handler -> {
                    try {
                        handler.handleEvent(relationshipEvent);
                    } catch (Exception e) {
                        log.error("Handler {} failed to process relationship change event: {}",
                                 handler.getClass().getSimpleName(), e.getMessage(), e);
                    }
                });
        }
    }

    @Override
    public void initHandler(IPSRequestHandler requestHandler) {
        if (requestHandler instanceof PSContentEditorHandler) {
            PSContentEditorHandler contentEditorHandler = (PSContentEditorHandler) requestHandler;
            // Use Spring proxy to handle transaction annotations
            var serviceProxy = PSContentChangeServiceLocator.getContentChangeService();
            // The proxy may implement IPSEditorChangeListener as well; cast to satisfy the compiler
            contentEditorHandler.addEditorChangeListener((IPSEditorChangeListener) serviceProxy);
            log.debug("Registered content change service with editor handler");
        }
    }

    @Override
    public void shutdownHandler(IPSRequestHandler requestHandler) {
        // No cleanup needed
    }

    /**
     * Sets the GUID manager for dependency injection.
     *
     * @param guidManager the GUID manager to set
     */
    public void setGuidManager(IPSGuidManager guidManager) {
        this.guidManager = guidManager;
    }

    /**
     * Sets the notification service and registers for relationship events.
     *
     * @param notificationService the notification service to register with
     */
    @Autowired(required = false)
    public void setNotificationService(IPSNotificationService notificationService) {
        if (notificationService != null) {
            notificationService.addListener(EventType.RELATIONSHIP_CHANGED, this);
            log.debug("Registered with notification service for relationship changes");
        }
    }

    /**
     * Gets the Hibernate session from the entity manager.
     */
    private Session getSession() {
        return entityManager.unwrap(Session.class);
    }

    /**
     * Updates the content change cache with a new event.
     */
    private void updateCache(PSContentChangeEvent event) {
        var cacheKey = createCacheKey(event.getContentId(), event.getSiteId(), event.getChangeType());
        contentChangeCache.add(cacheKey);
    }

    /**
     * Removes a content change from the cache.
     */
    private void removeFromCache(int contentId, long siteId, PSContentChangeType changeType) {
        var cacheKey = createCacheKey(contentId, siteId, changeType);
        contentChangeCache.remove(cacheKey);
    }

    /**
     * Clears cache entries for a specific site.
     */
    private void clearCacheForSite(long siteId) {
        contentChangeCache.removeIf(key -> key.contains(":" + siteId + ":"));
    }

    /**
     * Clears cache entries for a specific site and change type.
     */
    private void clearCacheForSite(long siteId, PSContentChangeType changeType) {
        var pattern = ":" + siteId + ":" + changeType.name();
        contentChangeCache.removeIf(key -> key.contains(pattern));
    }

    /**
     * Creates a cache key for content change events.
     */
    private String createCacheKey(int contentId, long siteId, PSContentChangeType changeType) {
        return String.format("%d:%d:%s", contentId, siteId, changeType.name());
    }

    /** HQL for typed unit tests (issue #3265). */
    public static final String DELETE_CHANGE_EVENTS_HQL =
          "DELETE FROM PSContentChangeEvent WHERE contentId = :contentId AND changeType = :changeType";

    public static final String DELETE_CHANGE_EVENTS_FOR_SITE_HQL =
          "DELETE FROM PSContentChangeEvent WHERE siteId = :siteId";

    public static final String DELETE_CHANGE_EVENTS_FOR_SITE_TYPE_HQL =
          "DELETE FROM PSContentChangeEvent WHERE siteId = :siteId AND changeType = :changeType";

    public static String deleteChangeEventsHql(boolean includeSite) {
        if (includeSite) {
            return DELETE_CHANGE_EVENTS_HQL + " AND siteId = :siteId";
        }
        return DELETE_CHANGE_EVENTS_HQL;
    }
}
