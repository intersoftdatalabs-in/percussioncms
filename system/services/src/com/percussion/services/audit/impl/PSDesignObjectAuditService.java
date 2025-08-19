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

package com.percussion.services.audit.impl;

import com.percussion.services.audit.IPSDesignObjectAuditConfig;
import com.percussion.services.audit.IPSDesignObjectAuditService;
import com.percussion.services.audit.data.PSAuditLogEntry;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.PSGuidManagerLocator;

import org.hibernate.Session;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;

/**
 * Implementation of the design object audit service using modern Java 11 features.
 *
 * <p>This service provides comprehensive audit logging functionality with support
 * for both legacy Date and modern LocalDateTime APIs. The implementation uses
 * JPA TypedQuery for type safety and Stream API for efficient data processing.</p>
 *
 * @author Percussion Software
 * @since 6.0
 */
@Transactional
public class PSDesignObjectAuditService implements IPSDesignObjectAuditService {

   @PersistenceContext
   private EntityManager entityManager;

   /**
    * Configuration of this service, injected by Spring framework.
    */
   private IPSDesignObjectAuditConfig config;

   /**
    * Get Hibernate session from EntityManager.
    *
    * @return The Hibernate session, never null.
    */
   private Session getSession() {
      return entityManager.unwrap(Session.class);
   }

   /**
    * Set the audit configuration on this class, usually called by Spring
    * framework via dependency injection.
    * 
    * @param config The config, may not be <code>null</code>.
    */
   public void setConfig(IPSDesignObjectAuditConfig config) {
      this.config = Objects.requireNonNull(config, "config may not be null");
   }

   @Override
   public IPSDesignObjectAuditConfig getConfig() {
      return config;
   }

   @Override
   @Transactional
   public PSAuditLogEntry createAuditLogEntry() {
      var entry = new PSAuditLogEntry();
      var guidMgr = PSGuidManagerLocator.getGuidMgr();
      entry.setGUID(guidMgr.createGuid(PSTypeEnum.INTERNAL));
      
      return entry;
   }

   @Override
   @Transactional
   public void saveAuditLogEntry(PSAuditLogEntry entry) {
      Objects.requireNonNull(entry, "entry may not be null");
      getSession().save(entry);
   }

   @Override
   @Transactional
   @Deprecated
   public void deleteAuditLogEntriesByDate(Date beforeDate) {
      Objects.requireNonNull(beforeDate, "beforeDate may not be null");

      // Use modern JPA TypedQuery instead of deprecated Criteria API
      TypedQuery<PSAuditLogEntry> query = entityManager.createQuery(
         "SELECT e FROM PSAuditLogEntry e WHERE e.auditDate < :beforeDate",
         PSAuditLogEntry.class);
      query.setParameter("beforeDate", beforeDate);

      var entries = query.getResultList();

      // Use Stream API for efficient processing
      entries.forEach(entityManager::remove);
   }

   @Override
   @Transactional
   public void deleteAuditLogEntriesByLocalDateTime(LocalDateTime beforeDateTime) {
      Objects.requireNonNull(beforeDateTime, "beforeDateTime may not be null");

      // Convert LocalDateTime to Date for JPA compatibility
      var beforeDate = Date.from(beforeDateTime.atZone(ZoneOffset.UTC).toInstant());
      deleteAuditLogEntriesByDate(beforeDate);
   }

   @Override
   @Transactional
   public void saveAuditLogEntries(Collection<PSAuditLogEntry> entries) {
      Objects.requireNonNull(entries, "entries may not be null");

      if (entries.isEmpty()) {
         throw new IllegalArgumentException("entries may not be empty");
      }

      var session = getSession();

      // Use Stream API for efficient batch processing
      entries.forEach(session::save);
   }

   @Override
   public Collection<PSAuditLogEntry> findAuditLogEntries() {
      // Use modern JPA TypedQuery instead of deprecated Criteria API
      TypedQuery<PSAuditLogEntry> query = entityManager.createQuery(
         "SELECT e FROM PSAuditLogEntry e ORDER BY e.auditDate DESC",
         PSAuditLogEntry.class);

      return query.getResultList();
   }
}
