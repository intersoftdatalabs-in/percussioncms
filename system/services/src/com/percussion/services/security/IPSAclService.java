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
    package com.percussion.services.security;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.security.data.PSUserAccessLevel;
import com.percussion.services.security.PSServiceSecurityException;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.security.IPSTypedPrincipal;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * This interface provides methods for managing object ACLs (Access Control Lists) with Java 11 modernization.
 * ACL objects require the Principal who is accessing or modifying them, which is part
 * of the user context information. This service takes user context information and
 * simplifies operations involving object ACLs, user context, and business rules.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>Optional-based safe access for ACL retrieval and validation</li>
 * <li>Stream API for efficient ACL filtering and processing</li>
 * <li>CompletableFuture support for asynchronous security operations</li>
 * <li>Enhanced validation with Objects.requireNonNull</li>
 * <li>Functional interfaces for access level predicates</li>
 * </ul>
 *
 * <p>All {@code loadAclXXX} methods return read-only objects. These are cached and may
 * be shared among threads; callers should never modify them. If changes need to be
 * made, use either {@link #loadAclsModifiable(List)} or
 * {@link #loadAclsForObjectsModifiable(List)} methods.
 *
 * @author Ram
 * @version 6.0
 */
public interface IPSAclService {

    /**
     * A convenience method that retrieves the ACL for the object with the
     * supplied ID and calls {@link #calculateUserAccessLevel(IPSAcl)}.
     *
     * @param objectGuid The GUID of the object for which the current user's
     *                   effective access level needs to be computed, not {@code null}
     * @return the user access level, never {@code null}
     * @throws IllegalArgumentException if objectGuid is null
     */
    PSUserAccessLevel getUserAccessLevel(IPSGuid objectGuid);

    /**
     * Safely get user access level, returning an Optional for enhanced error handling.
     *
     * @param objectGuid The GUID of the object to check access for, not {@code null}
     * @return an Optional containing the access level, empty if object not found or access denied
     * @throws IllegalArgumentException if objectGuid is null
     */
    default Optional<PSUserAccessLevel> getUserAccessLevelSafely(IPSGuid objectGuid) {
        Objects.requireNonNull(objectGuid, "objectGuid cannot be null");
        try {
            return Optional.of(getUserAccessLevel(objectGuid));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Asynchronously get user access level for non-blocking operations.
     *
     * @param objectGuid The GUID of the object to check access for, not {@code null}
     * @return a CompletableFuture containing the access level
     * @throws IllegalArgumentException if objectGuid is null
     */
    default CompletableFuture<PSUserAccessLevel> getUserAccessLevelAsync(IPSGuid objectGuid) {
        Objects.requireNonNull(objectGuid, "objectGuid cannot be null");
        return CompletableFuture.supplyAsync(() -> getUserAccessLevel(objectGuid));
    }

    /**
     * Computes the current user's effective access level to the object protected
     * by the supplied ACL. The effective access level is the highest permission
     * the user can get on the associated object based on all entries in the ACL.
     *
     * @param acl The ACL which will be used to compute access. If {@code null},
     *            all access is allowed. Must be an ACL previously returned by this interface
     * @return the effective access level for the current user, never {@code null}
     */
    PSUserAccessLevel calculateUserAccessLevel(IPSAcl acl);

    /**
     * Create an ACL for the specified object with enhanced validation.
     *
     * @param objGuid The GUID of the object for which the ACL will specify
     *                permissions, not {@code null}
     * @param owner The owner of the ACL, not {@code null}
     * @return The ACL, never {@code null}. This object will not have been persisted
     * @throws IllegalArgumentException if objGuid or owner is null
     */
    default IPSAcl createAcl(IPSGuid objGuid, IPSTypedPrincipal owner) {
        Objects.requireNonNull(objGuid, "objGuid cannot be null");
        Objects.requireNonNull(owner, "owner cannot be null");
        return createAclImpl(objGuid, owner);
    }

    /**
     * Internal implementation for ACL creation.
     */
    IPSAcl createAclImpl(IPSGuid objGuid, IPSTypedPrincipal owner);

    /**
     * Load ACLs for the given list of ACL GUIDs. These objects are cached and shared
     * between threads and should be treated as read-only.
     *
     * @param aclGuids list of ACL GUIDs to load ACLs for. May be {@code null}
     *                 to return all ACLs. If not {@code null}, must not be empty
     * @return an immutable list of ACL objects, never {@code null}, may be empty
     * @throws PSServiceSecurityException if any of the specified ACLs cannot be loaded
     */
    List<IPSAcl> loadAcls(List<IPSGuid> aclGuids) throws PSServiceSecurityException;

    /**
     * Load ACLs safely, returning an Optional for error handling.
     *
     * @param aclGuids list of ACL GUIDs to load ACLs for
     * @return an Optional containing the list of ACLs, empty if loading fails
     */
    default Optional<List<IPSAcl>> loadAclsSafely(List<IPSGuid> aclGuids) {
        try {
            return Optional.of(loadAcls(aclGuids));
        } catch (PSServiceSecurityException e) {
            return Optional.empty();
        }
    }

    /**
     * Load ACLs as a stream for efficient processing.
     *
     * @param aclGuids list of ACL GUIDs to load ACLs for
     * @return Stream of ACLs, never {@code null}
     * @throws PSServiceSecurityException if any of the specified ACLs cannot be loaded
     */
    default Stream<IPSAcl> streamAcls(List<IPSGuid> aclGuids) throws PSServiceSecurityException {
        return loadAcls(aclGuids).stream();
    }

    /**
     * Load ACLs for the given list of ACL GUIDs, always from persistent storage.
     * Just like {@link #loadAcls(List)}, except the objects are always retrieved
     * from persistent storage, never from cache.
     *
     * @param aclGuids list of ACL GUIDs to load ACLs for, not {@code null}
     * @return an immutable list of modifiable ACL objects, never {@code null}, may be empty
     * @throws PSServiceSecurityException if any of the specified ACLs cannot be loaded
     * @throws IllegalArgumentException if aclGuids is null
     */
    List<IPSAcl> loadAclsModifiable(List<IPSGuid> aclGuids) throws PSServiceSecurityException;

    /**
     * Load ACLs for objects with the specified GUIDs. These objects are cached
     * and shared between threads and should be treated as read-only.
     *
     * @param objectGuids list of object GUIDs to load ACLs for, not {@code null}
     * @return an immutable list of ACL objects, never {@code null}, may be empty
     * @throws PSServiceSecurityException if any of the specified ACLs cannot be loaded
     * @throws IllegalArgumentException if objectGuids is null
     */
    List<IPSAcl> loadAclsForObjects(List<IPSGuid> objectGuids) throws PSServiceSecurityException;

    /**
     * Load ACLs for objects safely, returning an Optional for error handling.
     *
     * @param objectGuids list of object GUIDs to load ACLs for, not {@code null}
     * @return an Optional containing the list of ACLs, empty if loading fails
     */
    default Optional<List<IPSAcl>> loadAclsForObjectsSafely(List<IPSGuid> objectGuids) {
        try {
            return Optional.of(loadAclsForObjects(objectGuids));
        } catch (PSServiceSecurityException e) {
            return Optional.empty();
        }
    }

    /**
     * Load modifiable ACLs for objects with the specified GUIDs.
     *
     * @param objectGuids list of object GUIDs to load ACLs for, not {@code null}
     * @return an immutable list of modifiable ACL objects, never {@code null}, may be empty
     * @throws PSServiceSecurityException if any of the specified ACLs cannot be loaded
     * @throws IllegalArgumentException if objectGuids is null
     */
    List<IPSAcl> loadAclsForObjectsModifiable(List<IPSGuid> objectGuids) throws PSServiceSecurityException;

    /**
     * Convenience method that loads the ACL for a single object GUID.
     *
     * @param objectGuid the GUID of the object, not {@code null}
     * @return the ACL for the object, or {@code null} if not found
     */
    default IPSAcl loadAclForObject(IPSGuid objectGuid) throws PSServiceSecurityException {
        Objects.requireNonNull(objectGuid, "objectGuid cannot be null");
        List<IPSAcl> list = loadAclsForObjects(List.of(objectGuid));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Backwards-compatible convenience method to load a single modifiable ACL
     * for the supplied object GUID.
     */
    default IPSAcl loadAclForObjectModifiable(IPSGuid objectGuid) throws PSServiceSecurityException {
        Objects.requireNonNull(objectGuid, "objectGuid cannot be null");
        List<IPSAcl> list = loadAclsForObjectsModifiable(List.of(objectGuid));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Save the supplied ACLs to persistent storage with enhanced validation.
     *
     * @param acls The ACLs to save, not {@code null} or empty
     * @throws PSServiceSecurityException if the save operation fails
     * @throws IllegalArgumentException if acls is null or empty
     */
    default void saveAcls(List<IPSAcl> acls) throws PSServiceSecurityException {
        Objects.requireNonNull(acls, "acls cannot be null");
        if (acls.isEmpty()) {
            throw new IllegalArgumentException("acls cannot be empty");
        }
        saveAclsImpl(acls);
    }

    /**
     * Internal implementation for ACL saving.
     */
    void saveAclsImpl(List<IPSAcl> acls) throws PSServiceSecurityException;

    /**
     * Delete ACLs for the specified object GUIDs with enhanced validation.
     *
     * @param objectGuids list of object GUIDs whose ACLs should be deleted, not {@code null}
     * @throws PSServiceSecurityException if the delete operation fails
     * @throws IllegalArgumentException if objectGuids is null
     */
    default void deleteAcls(List<IPSGuid> objectGuids) throws PSServiceSecurityException {
        Objects.requireNonNull(objectGuids, "objectGuids cannot be null");
        deleteAclsImpl(objectGuids);
    }

    /**
     * Backwards compatible convenience method to delete a single ACL GUID.
     */
    default void deleteAcl(IPSGuid aclGuid) throws PSServiceSecurityException {
        Objects.requireNonNull(aclGuid, "aclGuid cannot be null");
        deleteAcls(List.of(aclGuid));
    }

    /**
     * Clear any internal ACL caches. Default is a no-op; implementations
     * that cache ACLs should override to clear their caches.
     */
    default void clearCache() {
        // no-op by default
    }

    /**
     * Internal implementation for ACL deletion.
     */
    void deleteAclsImpl(List<IPSGuid> objectGuids) throws PSServiceSecurityException;

    /**
     * Check if the current user has the specified access level to an object.
     *
     * @param objectGuid The GUID of the object to check, not {@code null}
     * @param requiredLevel The required access level, not {@code null}
     * @return {@code true} if user has required access, {@code false} otherwise
     * @throws IllegalArgumentException if objectGuid or requiredLevel is null
     */
    default boolean hasAccess(IPSGuid objectGuid, PSUserAccessLevel requiredLevel) {
        Objects.requireNonNull(objectGuid, "objectGuid cannot be null");
        Objects.requireNonNull(requiredLevel, "requiredLevel cannot be null");

        var userLevel = getUserAccessLevel(objectGuid);
        // Compare permission sets: user must contain all required permissions
        return userLevel.getPermissions().containsAll(requiredLevel.getPermissions());
    }

    /**
     * Check access for multiple objects efficiently using streams.
     *
     * @param objectGuids list of object GUIDs to check, not {@code null}
     * @param requiredLevel The required access level, not {@code null}
     * @return Stream of object GUIDs that the user has access to, never {@code null}
     * @throws IllegalArgumentException if objectGuids or requiredLevel is null
     */
    default Stream<IPSGuid> filterAccessibleObjects(List<IPSGuid> objectGuids,
                                                   PSUserAccessLevel requiredLevel) {
        Objects.requireNonNull(objectGuids, "objectGuids cannot be null");
        Objects.requireNonNull(requiredLevel, "requiredLevel cannot be null");

        return objectGuids.stream()
            .filter(guid -> hasAccess(guid, requiredLevel));
    }

    /**
     * Find ACLs that match the specified predicate.
     *
     * @param aclGuids list of ACL GUIDs to search, not {@code null}
     * @param predicate predicate to match ACLs, not {@code null}
     * @return Stream of matching ACLs, never {@code null}
     * @throws PSServiceSecurityException if ACL loading fails
     * @throws IllegalArgumentException if aclGuids or predicate is null
     */
    default Stream<IPSAcl> findAcls(List<IPSGuid> aclGuids, Predicate<IPSAcl> predicate)
            throws PSServiceSecurityException {
        Objects.requireNonNull(aclGuids, "aclGuids cannot be null");
        Objects.requireNonNull(predicate, "predicate cannot be null");

        return streamAcls(aclGuids).filter(predicate);
    }
    /**
     * Find objects visible to named communities for a given type.
     *
     * @param communityNames List of community names, not {@code null}
     * @param type optional type filter, may be {@code null}
     * @return collection of object GUIDs visible to the communities
     */
    Collection<IPSGuid> findObjectsVisibleToCommunities(List<String> communityNames, PSTypeEnum type);

    /**
     * Count ACLs that match the specified criteria.
     *
     * @param aclGuids list of ACL GUIDs to count, not {@code null}
     * @param predicate predicate to match ACLs, not {@code null}
     * @return count of matching ACLs
     * @throws PSServiceSecurityException if ACL loading fails
     * @throws IllegalArgumentException if aclGuids or predicate is null
     */
    default long countAcls(List<IPSGuid> aclGuids, Predicate<IPSAcl> predicate)
            throws PSServiceSecurityException {
        return findAcls(aclGuids, predicate).count();
    }

    /**
     * Check if any ACLs exist for the specified object types.
     *
     * @param objectType the type of objects to check, not {@code null}
     * @return {@code true} if ACLs exist for this object type, {@code false} otherwise
     * @throws IllegalArgumentException if objectType is null
     */
    default boolean hasAclsForType(PSTypeEnum objectType) {
        Objects.requireNonNull(objectType, "objectType cannot be null");
        return hasAclsForTypeImpl(objectType);
    }

    /**
     * Internal implementation for type-based ACL existence check.
     */
    boolean hasAclsForTypeImpl(PSTypeEnum objectType);

    /**
     * Get all object GUIDs that have ACLs.
     *
     * @return Stream of object GUIDs with ACLs, never {@code null}
     */
    Stream<IPSGuid> streamObjectsWithAcls();

    /**
     * Validate ACL configuration for security compliance.
     *
     * @param acl the ACL to validate, not {@code null}
     * @return validation result, empty if valid, contains error message if invalid
     * @throws IllegalArgumentException if acl is null
     */
    default Optional<String> validateAcl(IPSAcl acl) {
        Objects.requireNonNull(acl, "acl cannot be null");
        return validateAclImpl(acl);
    }

    /**
     * Internal implementation for ACL validation.
     */
    Optional<String> validateAclImpl(IPSAcl acl);

}
