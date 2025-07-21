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
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object implementation for Widget Builder Definition operations.
 * This Spring Repository provides CRUD operations for managing widget builder definitions
 * using JPA/Hibernate for persistence.
 *
 * @author matthewernewein
 */
@Transactional
@Repository
@PSBaseBean("sys_widgetBuilderDefinitionDao")
public class PSWidgetBuilderDefinitionDao implements IPSWidgetBuilderDefinitionDao {

    private static final Logger logger = LogManager.getLogger(PSWidgetBuilderDefinitionDao.class);

    /**
     * Constant for the key used to generate widget builder definition IDs.
     */
    private static final String WIDGET_BUILDER_DEFINITION_ID_KEY = "PSX_WIDGETBUILDERDEFINITIONID";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private IPSGuidManager guidManager;

    /**
     * Gets the current Hibernate session from the EntityManager.
     *
     * @return the current Hibernate session
     */
    private Session getSession() {
        return entityManager.unwrap(Session.class);
    }

    @Override
    @Transactional
    public PSWidgetBuilderDefinition save(PSWidgetBuilderDefinition definition) throws IPSGenericDao.SaveException {
        Validate.notNull(definition, "Widget builder definition cannot be null");

        // Generate new ID if this is a new entity
        if (definition.getWidgetBuilderDefinitionId() == -1) {
            var newId = guidManager.createId(WIDGET_BUILDER_DEFINITION_ID_KEY);
            definition.setWidgetBuilderDefinitionId(newId);
        }

        var session = getSession();
        try {
            session.saveOrUpdate(definition);
            session.flush();
            logger.debug("Successfully saved widget builder definition with ID: {}",
                        definition.getWidgetBuilderDefinitionId());
            return definition;
        } catch (HibernateException e) {
            var errorMsg = "Database error while saving widget builder definition: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new IPSGenericDao.SaveException(errorMsg, e);
        }
    }

    @Override
    public PSWidgetBuilderDefinition find(long definitionId) {
        Validate.isTrue(definitionId > 0, "Definition ID must be positive");

        var session = getSession();
        try {
            var query = session.createQuery(
                "FROM PSWidgetBuilderDefinition WHERE widgetBuilderDefinitionId = :widgetBuilderDefinitionId",
                PSWidgetBuilderDefinition.class);
            query.setParameter("widgetBuilderDefinitionId", definitionId);

            var result = query.uniqueResult();
            logger.debug("Found widget builder definition with ID {}: {}", definitionId, result != null);
            return result;
        } catch (HibernateException e) {
            logger.error("Error finding widget builder definition with ID: {}", definitionId, e);
            return null;
        }
    }

    @Override
    @Transactional
    public void delete(long definitionId) {
        Validate.isTrue(definitionId > 0, "Definition ID must be positive");

        var definition = find(definitionId);
        if (definition != null) {
            var session = getSession();
            try {
                session.delete(definition);
                session.flush();
                logger.debug("Successfully deleted widget builder definition with ID: {}", definitionId);
            } catch (HibernateException e) {
                logger.error("Error deleting widget builder definition with ID: {}", definitionId, e);
                throw new RuntimeException("Failed to delete widget builder definition", e);
            }
        } else {
            logger.warn("Attempted to delete non-existent widget builder definition with ID: {}", definitionId);
        }
    }

    @Override
    public List<PSWidgetBuilderDefinition> getAll() {
        var session = getSession();
        try {
            var query = session.createQuery(
                "FROM PSWidgetBuilderDefinition ORDER BY label",
                PSWidgetBuilderDefinition.class);
            var results = query.list();
            logger.debug("Retrieved {} widget builder definitions", results.size());
            return results;
        } catch (HibernateException e) {
            logger.error("Error retrieving all widget builder definitions", e);
            return List.of(); // Return empty list instead of null
        }
    }

    /**
     * Finds a widget builder definition by prefix.
     *
     * @param prefix the prefix to search for
     * @return the widget builder definition if found, otherwise empty Optional
     */
    public Optional<PSWidgetBuilderDefinition> findByPrefix(String prefix) {
        Validate.notBlank(prefix, "Prefix cannot be blank");

        var session = getSession();
        try {
            var query = session.createQuery(
                "FROM PSWidgetBuilderDefinition WHERE prefix = :prefix",
                PSWidgetBuilderDefinition.class);
            query.setParameter("prefix", prefix);

            var result = Optional.ofNullable(query.uniqueResult());
            logger.debug("Found widget builder definition with prefix {}: {}", prefix, result.isPresent());
            return result;
        } catch (HibernateException e) {
            logger.error("Error finding widget builder definition with prefix: {}", prefix, e);
            return Optional.empty();
        }
    }
}
