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

import com.percussion.services.PSBaseServiceLocator;
import com.percussion.error.PSMissingBeanConfigurationException;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe service locator for the ACL service with comprehensive Java 11 modernization.
 * This utility class provides access to the ACL service implementation through Spring bean
 * configuration using modern concurrency patterns and Optional-based safe access.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>AtomicReference for thread-safe service access</li>
 * <li>Optional-based safe service retrieval</li>
 * <li>Enhanced validation with Objects.requireNonNull</li>
 * <li>Utility class design with private constructor</li>
 * </ul>
 *
 * @author dougrand
 */
public final class PSAclServiceLocator {

    /**
     * Thread-safe reference to the ACL service instance.
     */
    private static final AtomicReference<IPSAclService> aclService =
        new AtomicReference<>();

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private PSAclServiceLocator() {
        // Utility class - no instances allowed
    }

    /**
     * Gets the ACL service using atomic reference for thread safety.
     *
     * @return the ACL service, never {@code null}
     * @throws PSMissingBeanConfigurationException if there's a problem with the
     *         Spring configuration or the bean cannot be found
     */
    public static IPSAclService getAclService() throws PSMissingBeanConfigurationException {
        return aclService.updateAndGet(current -> {
            if (current == null) {
                var service = (IPSAclService) PSBaseServiceLocator.getBean("sys_aclService");
                return Objects.requireNonNull(service,
                    "ACL service bean 'sys_aclService' is not configured");
            }
            return current;
        });
    }

    /**
     * Gets the ACL service safely, returning an Optional for null-safe access.
     * This method will not throw exceptions if the service is not configured.
     *
     * @return an Optional containing the ACL service if available, empty otherwise
     */
    public static Optional<IPSAclService> getAclServiceSafely() {
        try {
            return Optional.of(getAclService());
        } catch (Exception e) {
            // Log error if needed, but return empty Optional for safe access
            return Optional.empty();
        }
    }

    /**
     * Check if the ACL service is available and properly configured.
     *
     * @return {@code true} if the service is available, {@code false} otherwise
     */
    public static boolean isAclServiceAvailable() {
        return getAclServiceSafely().isPresent();
    }

    /**
     * Reset the cached service instance. This method is primarily for testing purposes
     * and should be used with caution in production environments.
     */
    public static void resetService() {
        aclService.set(null);
    }

    /**
     * Get the ACL service with a fallback if not available.
     *
     * @param fallback the fallback service to use if the primary service is not available
     * @return the primary service if available, otherwise the fallback service
     * @throws IllegalArgumentException if fallback is null
     */
    public static IPSAclService getAclServiceWithFallback(IPSAclService fallback) {
        Objects.requireNonNull(fallback, "fallback service cannot be null");
        return getAclServiceSafely().orElse(fallback);
    }

    /**
     * Execute an operation with the ACL service if available.
     *
     * @param operation the operation to execute with the ACL service, not {@code null}
     * @throws IllegalArgumentException if operation is null
     */
    public static void withAclService(AclServiceOperation operation) {
        Objects.requireNonNull(operation, "operation cannot be null");
        getAclServiceSafely().ifPresent(service -> {
            try {
                operation.execute(service);
            } catch (Exception e) {
                throw new RuntimeException("Error executing ACL service operation", e);
            }
        });
    }

    /**
     * Execute an operation with the ACL service and return a result.
     *
     * @param <T> the type of the result
     * @param operation the operation to execute with the ACL service, not {@code null}
     * @return an Optional containing the result, empty if service not available or operation fails
     * @throws IllegalArgumentException if operation is null
     */
    public static <T> Optional<T> withAclServiceResult(AclServiceFunction<T> operation) {
        Objects.requireNonNull(operation, "operation cannot be null");
        return getAclServiceSafely().flatMap(service -> {
            try {
                return Optional.ofNullable(operation.apply(service));
            } catch (Exception e) {
                return Optional.empty();
            }
        });
    }

    /**
     * Functional interface for ACL service operations that don't return a value.
     */
    @FunctionalInterface
    public interface AclServiceOperation {
        /**
         * Execute an operation with the ACL service.
         *
         * @param aclService the ACL service instance, never {@code null}
         * @throws Exception if the operation fails
         */
        void execute(IPSAclService aclService) throws Exception;
    }

    /**
     * Functional interface for ACL service operations that return a value.
     *
     * @param <T> the type of the result
     */
    @FunctionalInterface
    public interface AclServiceFunction<T> {
        /**
         * Apply a function to the ACL service and return a result.
         *
         * @param aclService the ACL service instance, never {@code null}
         * @return the result of the operation, may be {@code null}
         * @throws Exception if the operation fails
         */
        T apply(IPSAclService aclService) throws Exception;
    }
}
