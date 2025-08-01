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
package com.percussion.services.security;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * This interface contains the authentication for a logged in user with Java 11 modernization.
 * Provides comprehensive user authentication capabilities with enhanced security patterns
 * and modern Java features for safe access and validation.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>Optional-based safe access for user information</li>
 * <li>Stream API for efficient role processing and validation</li>
 * <li>Enhanced validation with Objects.requireNonNull</li>
 * <li>Immutable collections for role management</li>
 * <li>Default methods for common authentication patterns</li>
 * </ul>
 *
 * @author dougrand
 */
public interface IPSAuthentication {

    /**
     * Get the name of the logged in user.
     *
     * @return the user name, never {@code null}
     */
    String getUserName();

    /**
     * Get the user name safely wrapped in an Optional for enhanced null safety.
     *
     * @return an Optional containing the user name, never empty for valid authentication
     */
    default Optional<String> getUserNameSafely() {
        try {
            var userName = getUserName();
            return userName != null && !userName.trim().isEmpty()
                ? Optional.of(userName.trim())
                : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Check to see if the user has the given role with enhanced validation.
     *
     * @param roleName the name of a role, must never be {@code null} or empty
     * @return {@code true} if the user is in the given role, {@code false} otherwise
     * @throws IllegalArgumentException if roleName is null or empty
     */
    default boolean isUserInRole(String roleName) {
        Objects.requireNonNull(roleName, "roleName cannot be null");
        if (roleName.trim().isEmpty()) {
            throw new IllegalArgumentException("roleName cannot be empty");
        }
        return isUserInRoleImpl(roleName.trim());
    }

    /**
     * Internal implementation for role checking.
     */
    boolean isUserInRoleImpl(String roleName);

    /**
     * Check if the user has any of the specified roles.
     *
     * @param roleNames collection of role names to check, not {@code null}
     * @return {@code true} if user has any of the roles, {@code false} otherwise
     * @throws IllegalArgumentException if roleNames is null
     */
    default boolean hasAnyRole(Collection<String> roleNames) {
        Objects.requireNonNull(roleNames, "roleNames cannot be null");
        return roleNames.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(role -> !role.isEmpty())
            .anyMatch(this::isUserInRole);
    }

    /**
     * Check if the user has all of the specified roles.
     *
     * @param roleNames collection of role names to check, not {@code null}
     * @return {@code true} if user has all of the roles, {@code false} otherwise
     * @throws IllegalArgumentException if roleNames is null
     */
    default boolean hasAllRoles(Collection<String> roleNames) {
        Objects.requireNonNull(roleNames, "roleNames cannot be null");
        return roleNames.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(role -> !role.isEmpty())
            .allMatch(this::isUserInRole);
    }

    /**
     * Get all roles for the current user.
     *
     * @return an immutable set of role names, never {@code null}, may be empty
     */
    Set<String> getUserRoles();

    /**
     * Get user roles as a stream for efficient processing.
     *
     * @return Stream of role names, never {@code null}
     */
    default Stream<String> streamUserRoles() {
        return getUserRoles().stream();
    }

    /**
     * Filter roles that match the specified criteria.
     *
     * @param prefix the prefix to match role names against, not {@code null}
     * @return Stream of matching role names, never {@code null}
     * @throws IllegalArgumentException if prefix is null
     */
    default Stream<String> filterRolesByPrefix(String prefix) {
        Objects.requireNonNull(prefix, "prefix cannot be null");
        var normalizedPrefix = prefix.trim().toLowerCase();
        return streamUserRoles()
            .filter(role -> role.toLowerCase().startsWith(normalizedPrefix));
    }

    /**
     * Count the number of roles the user has.
     *
     * @return the count of user roles
     */
    default long getRoleCount() {
        return getUserRoles().size();
    }

    /**
     * Check if the user has any roles assigned.
     *
     * @return {@code true} if user has roles, {@code false} otherwise
     */
    default boolean hasRoles() {
        return !getUserRoles().isEmpty();
    }

    /**
     * Check if the user is authenticated (has a valid user name).
     *
     * @return {@code true} if user is authenticated, {@code false} otherwise
     */
    default boolean isAuthenticated() {
        return getUserNameSafely().isPresent();
    }

    /**
     * Get a safe display name for the user, never null.
     *
     * @return the user name or "Anonymous" if not authenticated
     */
    default String getDisplayName() {
        return getUserNameSafely().orElse("Anonymous");
    }

    /**
     * Check if this is an anonymous (unauthenticated) user.
     *
     * @return {@code true} if user is anonymous, {@code false} otherwise
     */
    default boolean isAnonymous() {
        return !isAuthenticated();
    }

    /**
     * Validate that the user has sufficient privileges for an operation.
     *
     * @param requiredRoles roles required for the operation, not {@code null}
     * @param requireAll if {@code true}, user must have all roles; if {@code false}, any role is sufficient
     * @return {@code true} if user has sufficient privileges, {@code false} otherwise
     * @throws IllegalArgumentException if requiredRoles is null
     */
    default boolean hasPrivileges(Collection<String> requiredRoles, boolean requireAll) {
        Objects.requireNonNull(requiredRoles, "requiredRoles cannot be null");
        if (requiredRoles.isEmpty()) {
            return true; // No roles required
        }
        return requireAll ? hasAllRoles(requiredRoles) : hasAnyRole(requiredRoles);
    }

    /**
     * Get authentication context information as a formatted string.
     *
     * @return formatted authentication context, never {@code null}
     */
    default String getAuthenticationContext() {
        var context = new StringBuilder();
        context.append("User: ").append(getDisplayName());
        if (hasRoles()) {
            context.append(", Roles: ").append(getUserRoles());
        }
        context.append(", Authenticated: ").append(isAuthenticated());
        return context.toString();
    }
}
