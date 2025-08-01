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

package com.percussion.services.widgetbuilder;

import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.util.PSBaseBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Data Access Object implementation for Widget Builder Definition operations with modern Java 11 patterns.
 * This Spring Repository provides comprehensive CRUD operations for managing widget builder definitions
 * using JPA/Hibernate for persistence with enhanced error handling and Optional-based safe access.
 *
 * @author matthewernewein
 */
@Transactional
@Repository
@PSBaseBean("sys_widgetBuilderDefinitionDao")
public class PSWidgetBuilderDefinitionDao implements IPSWidgetBuilderDefinitionDao {

    private static final Logger log = LogManager.getLogger(PSWidgetBuilderDefinitionDao.class);

    /**
     * Constant for the key used to generate widget builder definition IDs.
     */
    private static final String WIDGET_BUILDER_DEFINITION_ID_KEY = "PSX_WIDGETBUILDERDEFINITIONID";

    /**
     * Entity manager for JPA operations.
     */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * GUID manager for generating unique identifiers.
     */
    @Autowired
    private IPSGuidManager guidManager;

    /**
     * Gets the current Hibernate session from the EntityManager with enhanced error handling.
     *
     * @return the current Hibernate session, never null
     * @throws IllegalStateException if the session cannot be obtained
     */
    private Session getSession() {
        try {
            return entityManager.unwrap(Session.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to obtain Hibernate session", e);
        }
    }

    @Override
    @Transactional
    public PSWidgetBuilderDefinition save(PSWidgetBuilderDefinition definition) throws IPSGenericDao.SaveException {
        Objects.requireNonNull(definition, "Widget builder definition cannot be null");

        // Generate new ID if this is a new entity
        if (definition.getWidgetBuilderDefinitionId() == -1) {
            var newId = guidManager.createId(WIDGET_BUILDER_DEFINITION_ID_KEY);
            definition.setWidgetBuilderDefinitionId(newId);
            log.debug("Generated new ID {} for widget builder definition", newId);
        }

        var session = getSession();
        try {
            session.saveOrUpdate(definition);
            session.flush();
            log.debug("Successfully saved widget builder definition with ID: {}",
                     definition.getWidgetBuilderDefinitionId());
            return definition;
        } catch (HibernateException e) {
            var errorMsg = String.format("Database error while saving widget builder definition with ID %d: %s",
                                       definition.getWidgetBuilderDefinitionId(), e.getMessage());
            log.error(errorMsg, e);
            throw new IPSGenericDao.SaveException(errorMsg, e);
        }
    }

    @Override
    public Optional<PSWidgetBuilderDefinition> find(long definitionId) {
        validateDefinitionId(definitionId);

        var session = getSession();
        try {
            var query = session.createQuery(
                "FROM PSWidgetBuilderDefinition WHERE widgetBuilderDefinitionId = :definitionId",
                PSWidgetBuilderDefinition.class);
            query.setParameter("definitionId", definitionId);

            var result = Optional.ofNullable(query.uniqueResult());
            log.debug("Found widget builder definition with ID {}: {}", definitionId, result.isPresent());
            return result;
        } catch (HibernateException e) {
            log.error("Error finding widget builder definition with ID: {}", definitionId, e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public void delete(long definitionId) {
        validateDefinitionId(definitionId);

        var definition = find(definitionId);
        if (definition.isPresent()) {
            var session = getSession();
            try {
                session.delete(definition.get());
                session.flush();
                log.debug("Successfully deleted widget builder definition with ID: {}", definitionId);
            } catch (HibernateException e) {
                var errorMsg = String.format("Error deleting widget builder definition with ID: %d", definitionId);
                log.error(errorMsg, e);
                throw new RuntimeException(errorMsg, e);
            }
        } else {
            log.warn("Attempted to delete non-existent widget builder definition with ID: {}", definitionId);
        }
    }

    @Override
    public List<PSWidgetBuilderDefinition> getAll() {
        var session = getSession();
        try {
            var query = session.createQuery(
                "FROM PSWidgetBuilderDefinition ORDER BY label ASC",
                PSWidgetBuilderDefinition.class);
            var results = query.list();
            log.debug("Retrieved {} widget builder definitions", results.size());
            return Collections.unmodifiableList(results);
        } catch (HibernateException e) {
            log.error("Error retrieving all widget builder definitions", e);
            return Collections.emptyList();
        }
    }

    /**
     * Validates that a definition ID is valid (positive).
     *
     * @param definitionId the ID to validate
     * @throws IllegalArgumentException if the ID is invalid
     */
    @Override
    public void validateDefinitionId(long definitionId) {
        if (definitionId <= 0) {
            throw new IllegalArgumentException("Definition ID must be positive, got: " + definitionId);
        }
    }

    /**
     * Gets the entity manager for advanced operations.
     *
     * @return the entity manager, never null
     */
    protected EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * Gets the GUID manager for ID generation.
     *
     * @return the GUID manager, never null
     */
    protected IPSGuidManager getGuidManager() {
        return guidManager;
    }
}
