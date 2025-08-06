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
package com.percussion.services.utils.orm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides various utility methods related to ORM processing with modern Java 11 features
 * for enhanced performance, type safety, and maintainability.
 * <p>
 * This class includes efficient caching mechanisms, null-safe operations, and comprehensive
 * error handling for robust ORM operations in enterprise environments.
 *
 * @author dougrand
 */
public final class PSORMUtils {

    private static final Logger ms_log = LogManager.getLogger(PSORMUtils.class);

    /**
     * Cache for reflection methods to improve performance
     */
    private static final Map<Class<?>, Optional<Method>> VERSION_METHOD_CACHE = new ConcurrentHashMap<>();

    /**
     * Cache for reflection methods to improve performance
     */
    private static final Map<Class<?>, Optional<Method>> ID_METHOD_CACHE = new ConcurrentHashMap<>();

    /**
     * Common method names for version retrieval
     */
    private static final List<String> VERSION_METHOD_NAMES = List.of("getVersion", "version", "getVersionNumber");

    /**
     * Common method names for ID retrieval
     */
    private static final List<String> ID_METHOD_NAMES = List.of("getId", "id", "getKey", "getPrimaryKey");

    /**
     * Private constructor to enforce static use of this class
     */
    private PSORMUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Get the value of the version used by the ORM framework with enhanced caching and error handling.
     * This method looks for common version method names and attempts to execute them.
     *
     * @param object The object from which to obtain the version, may be {@code null}
     * @return Optional containing the version if found, empty otherwise
     */
    public static Optional<Integer> getVersion(Object object) {
        if (object == null) {
            ms_log.trace("Cannot get version from null object");
            return Optional.empty();
        }

        var objectClass = object.getClass();
        var method = VERSION_METHOD_CACHE.computeIfAbsent(objectClass, PSORMUtils::findVersionMethod);

        if (method.isEmpty()) {
            ms_log.debug("No version method found for class: {}", objectClass.getName());
            return Optional.empty();
        }

        try {
            var result = method.get().invoke(object);
            if (result instanceof Integer) {
                Integer intValue = (Integer) result;
                ms_log.trace("Retrieved version {} from {}", intValue, objectClass.getSimpleName());
                return Optional.of(intValue);
            } else if (result instanceof Number) {
                Number numberValue = (Number) result;
                var version = numberValue.intValue();
                ms_log.trace("Converted version {} from {} to Integer", numberValue, objectClass.getSimpleName());
                return Optional.of(version);
            } else {
                ms_log.debug("Version method returned non-numeric value: {} for class: {}",
                    result, objectClass.getName());
                return Optional.empty();
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            ms_log.debug("Failed to invoke version method on {}: {}",
                objectClass.getName(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get the ID value from an ORM entity with enhanced type safety
     *
     * @param object The object from which to obtain the ID, may be {@code null}
     * @return Optional containing the ID if found, empty otherwise
     */
    public static Optional<Object> getId(Object object) {
        if (object == null) {
            ms_log.trace("Cannot get ID from null object");
            return Optional.empty();
        }

        var objectClass = object.getClass();
        var method = ID_METHOD_CACHE.computeIfAbsent(objectClass, PSORMUtils::findIdMethod);

        if (method.isEmpty()) {
            ms_log.debug("No ID method found for class: {}", objectClass.getName());
            return Optional.empty();
        }

        try {
            var result = method.get().invoke(object);
            ms_log.trace("Retrieved ID {} from {}", result, objectClass.getSimpleName());
            return Optional.ofNullable(result);
        } catch (IllegalAccessException | InvocationTargetException e) {
            ms_log.debug("Failed to invoke ID method on {}: {}",
                objectClass.getName(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Extract IDs from a collection of ORM entities
     *
     * @param entities the collection of entities, may be {@code null} or empty
     * @return a list of IDs, never {@code null}
     */
    public static List<Object> extractIds(Collection<?> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream()
            .filter(Objects::nonNull)
            .map(PSORMUtils::getId)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    /**
     * Extract versions from a collection of ORM entities
     *
     * @param entities the collection of entities, may be {@code null} or empty
     * @return a list of versions, never {@code null}
     */
    public static List<Integer> extractVersions(Collection<?> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream()
            .filter(Objects::nonNull)
            .map(PSORMUtils::getVersion)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    /**
     * Group entities by their ID values
     *
     * @param entities the collection of entities to group
     * @return a map of ID to entity, never {@code null}
     */
    public static Map<Object, Object> groupById(Collection<?> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyMap();
        }

        return entities.stream()
            .filter(Objects::nonNull)
            .filter(entity -> getId(entity).isPresent())
            .collect(Collectors.toMap(
                entity -> getId(entity).orElseThrow(),
                Function.identity(),
                (existing, replacement) -> {
                    ms_log.warn("Duplicate ID found, keeping existing entity: {}", existing);
                    return existing;
                }
            ));
    }

    /**
     * Check if an entity has a valid ID
     *
     * @param entity the entity to check, may be {@code null}
     * @return {@code true} if entity has a valid ID, {@code false} otherwise
     */
    public static boolean hasValidId(Object entity) {
        return getId(entity).isPresent();
    }

    /**
     * Check if an entity has a valid version
     *
     * @param entity the entity to check, may be {@code null}
     * @return {@code true} if entity has a valid version, {@code false} otherwise
     */
    public static boolean hasValidVersion(Object entity) {
        return getVersion(entity).isPresent();
    }

    /**
     * Find the version method for a given class using cached lookup
     *
     * @param clazz the class to search for version method
     * @return Optional containing the method if found, empty otherwise
     */
    private static Optional<Method> findVersionMethod(Class<?> clazz) {
        return findMethodByNames(clazz, VERSION_METHOD_NAMES, "version");
    }

    /**
     * Find the ID method for a given class using cached lookup
     *
     * @param clazz the class to search for ID method
     * @return Optional containing the method if found, empty otherwise
     */
    private static Optional<Method> findIdMethod(Class<?> clazz) {
        return findMethodByNames(clazz, ID_METHOD_NAMES, "ID");
    }

    /**
     * Generic method finder that searches for methods by a list of possible names
     *
     * @param clazz the class to search
     * @param methodNames the list of method names to try
     * @param purpose the purpose of the method (for logging)
     * @return Optional containing the method if found, empty otherwise
     */
    private static Optional<Method> findMethodByNames(Class<?> clazz, List<String> methodNames, String purpose) {
        for (var methodName : methodNames) {
            try {
                var method = clazz.getMethod(methodName);
                // Ensure method takes no parameters and returns something
                if (method.getParameterCount() == 0 && !void.class.equals(method.getReturnType())) {
                    ms_log.debug("Found {} method '{}' for class: {}", purpose, methodName, clazz.getName());
                    return Optional.of(method);
                }
            } catch (NoSuchMethodException e) {
                // Continue searching
            }
        }

        ms_log.debug("No {} method found for class: {}", purpose, clazz.getName());
        return Optional.empty();
    }

    /**
     * Clear the method caches - useful for testing or when class definitions change
     */
    public static void clearCaches() {
        VERSION_METHOD_CACHE.clear();
        ID_METHOD_CACHE.clear();
        ms_log.info("Cleared ORM utility method caches");
    }

    /**
     * Get cache statistics for monitoring and debugging
     *
     * @return a map containing cache statistics
     */
    public static Map<String, Object> getCacheStats() {
        var stats = new HashMap<String, Object>();
        stats.put("versionMethodCacheSize", VERSION_METHOD_CACHE.size());
        stats.put("idMethodCacheSize", ID_METHOD_CACHE.size());
        stats.put("totalCachedMethods", VERSION_METHOD_CACHE.size() + ID_METHOD_CACHE.size());

        return Collections.unmodifiableMap(stats);
    }

    /**
     * Validate that an entity is suitable for ORM operations
     *
     * @param entity the entity to validate
     * @return {@code true} if entity is valid for ORM operations, {@code false} otherwise
     */
    public static boolean isValidEntity(Object entity) {
        if (entity == null) {
            return false;
        }

        // Check if entity has at least one identifiable property
        return hasValidId(entity) || hasValidVersion(entity);
    }

    /**
     * Create a summary of an entity's ORM properties
     *
     * @param entity the entity to summarize
     * @return a map containing ORM property information
     */
    public static Map<String, Object> createEntitySummary(Object entity) {
        var summary = new HashMap<String, Object>();

        if (entity == null) {
            summary.put("valid", false);
            summary.put("reason", "Entity is null");
            return Collections.unmodifiableMap(summary);
        }

        summary.put("className", entity.getClass().getName());
        summary.put("hasId", hasValidId(entity));
        summary.put("hasVersion", hasValidVersion(entity));
        summary.put("valid", isValidEntity(entity));

        getId(entity).ifPresent(id -> summary.put("id", id));
        getVersion(entity).ifPresent(version -> summary.put("version", version));

        return Collections.unmodifiableMap(summary);
    }
}
