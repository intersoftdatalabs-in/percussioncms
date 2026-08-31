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
package com.percussion.services.content;

import com.percussion.services.content.data.PSAutoTranslation;
import com.percussion.services.content.data.PSFolderProperty;
import com.percussion.services.content.data.PSKeyword;
import com.percussion.utils.guid.IPSGuid;

import java.util.List;
import java.util.Optional;

/**
 * This interface defines various services used for content and content
 * design objects. Provides comprehensive content management capabilities
 * including keyword management, auto-translation, and folder properties.
 */
public interface IPSContentService {

    /**
     * Create a new keyword with an empty choice list.
     *
     * @param label the label of the new keyword, not {@code null} or empty,
     *              must be unique against all existing keywords
     * @param description a description for the new keyword, may be {@code null} or empty
     * @return the new keyword created, never {@code null}
     * @throws IllegalArgumentException if label is null or empty
     */
    PSKeyword createKeyword(String label, String description);

    /**
     * Find all keywords for the supplied label.
     *
     * @param label the label for which to find the keywords, may be {@code null}
     *              or empty to get all keywords, SQL type (%) wildcards are supported
     * @param sortProperty the name of the property by which to sort the results
     *                     ascending, may be {@code null} or empty to skip sorting
     * @return all found keywords for the supplied label, never {@code null}, may be empty
     */
    List<PSKeyword> findKeywordsByLabel(String label, String sortProperty);

    /**
     * Finds all keyword choices for the supplied keyword type.
     *
     * @param type the keyword type for which to find all choices, not {@code null} or empty
     * @param sortProperty the name of the property by which to sort the results
     *                     ascending, may be {@code null} or empty to skip sorting
     * @return all keywords which represent choices for the specified keyword
     *         type, never {@code null}, may be empty
     * @throws IllegalArgumentException if type is null or empty
     */
    List<PSKeyword> findKeywordChoices(String type, String sortProperty);

    /**
     * Load the keyword for the supplied id.
     *
     * @param id the id of the keyword to load, not {@code null}
     * @param sortProperty the name of the property by which to sort the
     *                     keyword choices ascending, may be {@code null} or empty to skip sorting
     * @return the loaded keyword, never {@code null}
     * @throws PSContentException if the identified keyword does not exist
     * @throws IllegalArgumentException if id is null
     */
    PSKeyword loadKeyword(IPSGuid id, String sortProperty) throws PSContentException;

    /**
     * Load the keyword for the supplied id, returning an Optional.
     *
     * @param id the id of the keyword to load, not {@code null}
     * @param sortProperty the name of the property by which to sort the
     *                     keyword choices ascending, may be {@code null} or empty to skip sorting
     * @return an Optional containing the keyword if found, empty otherwise
     * @throws IllegalArgumentException if id is null
     */
    default Optional<PSKeyword> findKeyword(IPSGuid id, String sortProperty) {
        try {
            return Optional.of(loadKeyword(id, sortProperty));
        } catch (PSContentException e) {
            return Optional.empty();
        }
    }

    /**
     * Insert or update the supplied keyword.
     *
     * @param keyword the keyword to save, not {@code null}
     * @throws IllegalArgumentException if keyword is null
     */
    void saveKeyword(PSKeyword keyword);

    /**
     * Delete the supplied keyword and all its choices.
     *
     * @param id the keyword to be deleted, not {@code null}
     * @throws IllegalArgumentException if id is null
     */
    void deleteKeyword(IPSGuid id);

    /**
     * Create a new auto translation definition.
     *
     * @param contentTypeId The content type for which the auto translation is defined
     * @param locale The language string of the locale for which the auto translation is defined
     * @return the new auto translation definition, never {@code null}
     * @throws IllegalArgumentException if contentTypeId or locale is null
     */
    PSAutoTranslation createAutoTranslation(IPSGuid contentTypeId, String locale);

    /**
     * Load all auto translation definitions for the specified content type.
     *
     * @param contentTypeId the content type id, not {@code null}
     * @return list of auto translation definitions, never {@code null}, may be empty
     * @throws IllegalArgumentException if contentTypeId is null
     */
    List<PSAutoTranslation> loadAutoTranslations(IPSGuid contentTypeId);

    /**
     * Save the supplied auto translation definition.
     *
     * @param autoTranslation the auto translation to save, not {@code null}
     * @throws IllegalArgumentException if autoTranslation is null
     */
    void saveAutoTranslation(PSAutoTranslation autoTranslation);

    /**
     * Delete the specified auto translation definition.
     *
     * @param id the id of the auto translation to delete, not {@code null}
     * @throws IllegalArgumentException if id is null
     */
    void deleteAutoTranslation(IPSGuid id);

    /**
     * Delete the auto-translation row for the given content type and locale
     * (composite primary key).
     *
     * @param contentTypeId content type id (UUID or typed long)
     * @param locale language string, not {@code null} or empty
     * @throws IllegalArgumentException if locale is null or empty
     */
    void deleteAutoTranslation(long contentTypeId, String locale);

    /**
     * Create a new folder property.
     *
     * @param name the name of the property, not {@code null} or empty
     * @param value the value of the property, may be {@code null}
     * @return the new folder property, never {@code null}
     * @throws IllegalArgumentException if name is null or empty
     */
    PSFolderProperty createFolderProperty(String name, String value);

    /**
     * Load folder properties for the specified folder.
     *
     * @param folderId the folder id, not {@code null}
     * @return list of folder properties, never {@code null}, may be empty
     * @throws IllegalArgumentException if folderId is null
     */
    List<PSFolderProperty> loadFolderProperties(IPSGuid folderId);

    /**
     * Find folder properties by property name across all folders.
     *
     * @param property the property name to search for, not {@code null} or empty
     * @return list of folder properties matching the property name, never {@code null}, may be empty
     * @throws IllegalArgumentException if property is null or empty
     */
    List<PSFolderProperty> getFolderProperties(String property);

    /**
     * Save the supplied folder property.
     *
     * @param property the folder property to save, not {@code null}
     * @throws IllegalArgumentException if property is null
     */
    void saveFolderProperty(PSFolderProperty property);

    /**
     * Delete the specified folder property.
     *
     * @param id the id of the folder property to delete, not {@code null}
     * @throws IllegalArgumentException if id is null
     */
    void deleteFolderProperty(IPSGuid id);
}
