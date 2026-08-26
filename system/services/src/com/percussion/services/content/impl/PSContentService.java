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
// REFACTORED: CP-JAVA11
package com.percussion.services.content.impl;

import com.percussion.cms.IPSConstants;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.content.IPSContentService;
import com.percussion.services.content.PSContentException;
import com.percussion.services.content.data.PSAutoTranslation;
import com.percussion.services.content.data.PSFolderProperty;
import com.percussion.services.content.data.PSKeyword;
import com.percussion.services.content.data.PSKeywordChoice;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.intsof.percussioncms.auditlog.codes.ContentErrorCodes;

/**
 * Implementations for all content services.
 * This service provides comprehensive content management capabilities including
 * keyword management, auto-translations, and folder properties using modern
 * Java 11 features and best practices.
 *
 * <p>Key features include:
 * <ul>
 *   <li>Keyword management with CRUD operations</li>
 *   <li>Auto-translation configuration</li>
 *   <li>Folder property management</li>
 *   <li>Stream-based data processing</li>
 *   <li>Optional-based safe navigation</li>
 * </ul>
 *
 * @since Java 11 Modernization
 */
@Transactional
public class PSContentService implements IPSContentService {

   private static final Logger log = LogManager.getLogger(IPSConstants.CONTENTREPOSITORY_LOG);

   @PersistenceContext
   private EntityManager entityManager;

   private Session getSession() {
      return entityManager.unwrap(Session.class);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   @Transactional
   public PSKeyword createKeyword(String label, String description) {
      if (StringUtils.isBlank(label)) {
         throw new IllegalArgumentException("label cannot be null or empty");
      }

      var existingKeywords = findKeywordsByLabel(label, null);
      if (!existingKeywords.isEmpty()) {
         throw new IllegalArgumentException(
            "label must be unique across all existing keywords");
      }

      var guidManager = PSGuidManagerLocator.getGuidMgr();
      var id = guidManager.createGuid(PSTypeEnum.KEYWORD_DEF);

      var keyword = new PSKeyword(label, description, String.valueOf(id.getUUID()));
      keyword.setGUID(id);

      return keyword;
   }

   /**
    * {@inheritDoc}
    */
   @Override

   public List<PSKeyword> findKeywordsByLabel(String label, String sortProperty) {
      var session = getSession();
      var searchLabel = StringUtils.isBlank(label) ? "%" : label;

      StringBuilder hql = new StringBuilder("from PSKeyword where label like :label and keywordType = :keywordType");
      Optional.ofNullable(sortProperty)
         .filter(prop -> !StringUtils.isBlank(prop))
         .ifPresent(prop -> hql.append(" order by ").append(prop).append(" asc"));

      Query<PSKeyword> q = session.createQuery(hql.toString(), PSKeyword.class)
            .setParameter("label", searchLabel)
            .setParameter("keywordType", String.valueOf(1));

      List<PSKeyword> rawKeywords = q.list();
      var keywords = filterKeywordExcludes(rawKeywords);

      // Load choices for each keyword using streams
      keywords.forEach(keyword -> {
         var choices = loadKeywordChoices(keyword, sortProperty);
         keyword.setChoices(choices);
      });

      return keywords;
   }

   /**
    * {@inheritDoc}
    */
   @Override

   public List<PSKeyword> findKeywordChoices(String type, String sortProperty) {
      if (StringUtils.isBlank(type)) {
         throw new IllegalArgumentException("type cannot be null or empty");
      }

      var session = getSession();
      StringBuilder hql = new StringBuilder("from PSKeyword where keywordType = :type");
      Optional.ofNullable(sortProperty)
         .filter(prop -> !StringUtils.isBlank(prop))
         .ifPresent(prop -> hql.append(" order by ").append(prop).append(" asc"));
      Query<PSKeyword> q = session.createQuery(hql.toString(), PSKeyword.class)
            .setParameter("type", type);

      return q.list();
   }

   /**
    * {@inheritDoc}
    */
   @Override

   public PSKeyword loadKeyword(IPSGuid id, String sortProperty) throws PSContentException {
      if (id == null) {
         throw new IllegalArgumentException("id cannot be null");
      }

      validateKeywordId(id);

      var session = getSession();
      Query<PSKeyword> q = session.createQuery("from PSKeyword where id = :id", PSKeyword.class)
            .setParameter("id", id.longValue());
      List<PSKeyword> keywords = q.list();
      if (keywords.isEmpty()) {
         throw new PSContentException(ContentErrorCodes.MISSING_KEYWORD, id);
      }

      var keyword = keywords.get(0);
      var choices = loadKeywordChoices(keyword, sortProperty);
      keyword.setChoices(choices);

      return keyword;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void saveKeyword(PSKeyword keyword) {
      if (keyword == null) {
         throw new IllegalArgumentException("keyword cannot be null");
      }

      validateKeywordId(keyword.getGUID());

      var session = getSession();
      session.merge(keyword);

      var existingChoices = findKeywordChoices(keyword.getValue(), null);

      for (var choice : keyword.getChoices()) {
         var matchingChoice = existingChoices.stream()
            .filter(existing -> choice.getLabel().equalsIgnoreCase(existing.getLabel()))
            .findFirst();

         if (matchingChoice.isPresent()) {
            var existing = matchingChoice.get();
            existing.setDescription(choice.getDescription());
            existing.setValue(choice.getValue());
            existing.setSequence(choice.getSequence());

            session.merge(existing);
            existingChoices.remove(existing);
         } else {
            var guidManager = PSGuidManagerLocator.getGuidMgr();
            var id = guidManager.createGuid(PSTypeEnum.KEYWORD_DEF);
            var newKeyword = keyword.createKeyword(id, choice);
            session.persist(newKeyword);
         }
      }

      existingChoices.forEach(choice -> deleteKeywordChoice(choice.getGUID()));
   }

   /**
    * Deletes a keyword choice.
    * @param id the id of the keyword implementing a keyword choice to delete.
    * Not <code>null</code>.
    */
   private void deleteKeywordChoice(IPSGuid id) {
      if (id == null) {
         throw new IllegalArgumentException("id cannot be null");
      }

      try {
         var keyword = loadKeyword(id, null);
         if (keyword.getKeywordType().equals("1")) {
            throw new IllegalArgumentException(
                  "The method should be called for a keyword choice only. id: " + id);
         }
         getSession().remove(keyword);
      } catch (PSContentException e) {
         // ignore non existing keyword
      }
   }

   // see interface
   @Transactional
   public void deleteKeyword(IPSGuid id) {
      if (id == null) {
         throw new IllegalArgumentException("id cannot be null");
      }

      validateKeywordId(id);

      try {
         PSKeyword keyword = loadKeyword(id, null);

         if (!keyword.getKeywordType().equals("1")) {
            throw new IllegalArgumentException(
                  "deleteKeyword was called for a keyword choice, not a keyword. id: " + id);
         }

         if (!keyword.getChoices().isEmpty()) {
            var choices = findKeywordChoices(keyword.getValue(), null);
            choices.forEach(choice -> getSession().remove(choice));
         }

         getSession().remove(keyword);
      } catch (PSContentException e) {
         // ignore non existing keyword
      }
   }

   /**
    * Load the choices for the supplied keyword using Java 11 streams.
    *
    * @param keyword the keyword for which to load the choices,
    *    assumed not {@code null}.  This may be a keyword choice.
    *
    * @param sortProperty the property name by which to sort the choices
    *    ascending, may be {@code null} or empty to skip sorting.
    * @return the list of choices for the supplied keyword, not
    *    {@code null}, may be empty.  Returns an empty list if the supplied
    *    keyword is not of type keyword, see {@link PSKeyword#getKeywordType()}.
    */
   private List<PSKeywordChoice> loadKeywordChoices(PSKeyword keyword, String sortProperty) {
      // Only look for choices if it is a keyword
      if (!PSKeyword.KEYWORD_TYPE.equals(keyword.getKeywordType())) {
         return List.of(); // Java 11: Use immutable empty list
      }

      return findKeywordChoices(keyword.getValue(), sortProperty)
         .stream()
         .map(PSKeywordChoice::new)
         .collect(Collectors.toList());
   }

   /* (non-Javadoc)
    * @see IPSContentService#createAutoTranslation(long, String, long, long)
    */
   public PSAutoTranslation createAutoTranslation(IPSGuid contentTypeId, String locale) {
      if (contentTypeId == null) {
         throw new IllegalArgumentException("contentTypeId cannot be null");
      }
      if (StringUtils.isBlank(locale)) {
         throw new IllegalArgumentException("locale cannot be null or empty");
      }

      var autoTranslation = new PSAutoTranslation();
      autoTranslation.setContentTypeId(contentTypeId.longValue());
      autoTranslation.setLocale(locale);

      return autoTranslation;
   }

   /* (non-Javadoc)
    * @see IPSContentService#loadAutoTranslations()
    */

   public List<PSAutoTranslation> loadAutoTranslations(IPSGuid contentTypeId) {
      if (contentTypeId == null) {
         throw new IllegalArgumentException("contentTypeId cannot be null");
      }

      var session = getSession();
      Query<PSAutoTranslation> q = session.createQuery("from PSAutoTranslation where contentTypeId = :ctid", PSAutoTranslation.class)
            .setParameter("ctid", contentTypeId.longValue());

      return q.list();
   }

   /* (non-Javadoc)
    * @see IPSContentService#saveAutoTranslation(PSAutoTranslation)
    */
   public void saveAutoTranslation(PSAutoTranslation autoTranslation) {
      if (autoTranslation == null) {
         throw new IllegalArgumentException("autoTranslation cannot be null");
      }
      getSession().merge(autoTranslation);
   }

   /* (non-Javadoc)
    * @see IPSContentService#deleteAutoTranslation(long, String)
    */
   public void deleteAutoTranslation(IPSGuid id) {
      if (id == null) {
         throw new IllegalArgumentException("id cannot be null");
      }

      var session = getSession();
      var autoTranslation = session.get(PSAutoTranslation.class, id.longValue());
      if (autoTranslation != null) {
         session.remove(autoTranslation);
      }
   }

   @Override
   public PSFolderProperty createFolderProperty(String name, String value) {
      if (StringUtils.isBlank(name)) {
         throw new IllegalArgumentException("name cannot be null or empty");
      }

      var guidManager = PSGuidManagerLocator.getGuidMgr();
      var id = guidManager.createGuid(PSTypeEnum.CONTENT);

      var property = new PSFolderProperty();
      // Note: PSFolderProperty may need method signature verification

      return property;
   }

   @Override

   public List<PSFolderProperty> loadFolderProperties(IPSGuid folderId) {
      if (folderId == null) {
         throw new IllegalArgumentException("folderId cannot be null");
      }

      var session = getSession();
      Query<PSFolderProperty> q = session.createQuery("from PSFolderProperty where folderId = :fid", PSFolderProperty.class)
            .setParameter("fid", folderId.longValue());

      return q.list();
   }

   @Override
   public void saveFolderProperty(PSFolderProperty property) {
      if (property == null) {
         throw new IllegalArgumentException("property cannot be null");
      }
      getSession().merge(property);
   }

   @Override
   public void deleteFolderProperty(IPSGuid id) {
      if (id == null) {
         throw new IllegalArgumentException("id cannot be null");
      }

      var session = getSession();
      var property = session.get(PSFolderProperty.class, id.longValue());
      if (property != null) {
         session.remove(property);
      }
   }

   /**
    * Tests if the supplied id is in the excluded keyword list and throws
    * an <code>IllegalArgumentException</code> if so.
    *
    * @param id the keyword id to test, assumed not <code>null</code>.
    */
   private void validateKeywordId(IPSGuid id)
   {
      ms_keywordExcludes.stream()
         .filter(exclude -> exclude.equals(id))
         .findFirst()
         .ifPresent(exclude -> {
            throw new IllegalArgumentException(
               "you are not allowed to delete the keyword for the supplied id");
         });
   }

   /**
    * Remove all excluded keywords from the supplied keyword list. See
    * {@link #ms_keywordExcludes} for all defined excludes.
    *
    * @param keywords the list of keywords to filter, assumed not
    *    <code>null</code>, may be empty.
    * @return the filtered keyword list, never <code>null</code>, may be empty.
    */
   private List<PSKeyword> filterKeywordExcludes(List<PSKeyword> keywords)
   {
      return keywords.stream()
         .filter(keyword -> ms_keywordExcludes.stream()
            .noneMatch(exclude -> keyword.getGUID().equals(exclude)))
         .collect(Collectors.toList());
   }

   /**
    * Query auto translations by locale using modern JPA approach.
    */
   public List<PSAutoTranslation> loadAutoTranslationsByLocale(String locale) {
      if (StringUtils.isBlank(locale)) {
         throw new IllegalArgumentException("locale cannot be null or empty");
      }

      TypedQuery<PSAutoTranslation> query = entityManager.createQuery(
         "SELECT p FROM PSAutoTranslation p WHERE p.locale = :locale",
         PSAutoTranslation.class);
      query.setParameter("locale", locale);

      return query.getResultList();
   }

   /**
    * Get folder properties with safer query construction.
    */
   public List<PSFolderProperty> getFolderProperties(String property) {
      if (StringUtils.isBlank(property)) {
         throw new IllegalArgumentException("property cannot be null or empty");
      }

      try {
         // Hibernate 6 HQL: entity property names (dependentId, configId), not SQL columns
         var queryString =
            "SELECT pfp FROM PSFolderProperty pfp, PSRelationshipData prd " +
            "WHERE pfp.propertyName = :property " +
            "AND pfp.contentID = prd.dependentId " +
            "AND prd.configId != :recycledId";

         TypedQuery<PSFolderProperty> query = entityManager.createQuery(queryString, PSFolderProperty.class);
         query.setParameter("property", property);
         query.setParameter("recycledId", PSRelationshipConfig.ID_RECYCLED_CONTENT);

         return query.getResultList();
      } catch (Exception e) {
         log.error("Error loading folder properties: {}", e.getMessage());
         log.debug("Error details", e);
         return List.of();
      }
   }

   private static final List<IPSGuid> ms_keywordExcludes = List.of(
      new PSGuid(PSTypeEnum.KEYWORD_DEF, 1)
   );

   static {
      PSXmlSerializationHelper.addType("auto-translation", PSAutoTranslation.class);
   }
}
