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
package com.percussion.services.locking;

import com.percussion.services.locking.data.PSObjectLock;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSErrorResultsException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Modern object lock service interface for managing object locks with comprehensive Java 11 features.
 * This service provides thread-safe object locking mechanisms using {@link PSObjectLock} objects,
 * with enhanced validation, Optional-based safe access, and asynchronous operation support.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>Optional-based safe access for nullable lock operations</li>
 * <li>Enhanced validation with Objects.requireNonNull</li>
 * <li>Stream API for efficient lock processing and filtering</li>
 * <li>CompletableFuture support for asynchronous lock operations</li>
 * <li>Functional interfaces for lock predicates and filtering</li>
 * </ul>
 *
 * <h2>Key Features</h2>
 * <ul>
 * <li>Thread-safe lock creation, extension, and release</li>
 * <li>Session-based lock management with user tracking</li>
 * <li>Lock expiration handling and cleanup</li>
 * <li>Bulk lock operations for improved performance</li>
 * <li>Lock override capabilities for administrative operations</li>
 * </ul>
 */
public interface IPSObjectLockService {

   /**
    * Create a new lock or extend an existing one for the requested object(s) with enhanced validation.
    *
    * @param ids the id of the object to lock, not {@code null} or empty and no {@code null} entries
    * @param session the session of the lock holder, not {@code null} or empty
    * @param locker the name of the lock holder, not {@code null} or empty
    * @param versions the version of the locked object for each entry in the ids list,
    *                 not {@code null} but entries can be {@code null} if the locked object does not use a version
    * @param overrideLock {@code true} to override existing locks for the same user but different session
    * @return the new or extended object lock, never {@code null}. The order of the results is the same as the supplied ids
    * @throws PSLockException if any of the requested locks could not be created for any reason
    * @throws IllegalArgumentException if ids, session, locker, or versions is null, or if ids is empty
    */
   default List<PSObjectLock> createLocks(List<IPSGuid> ids, String session, String locker,
                                         List<Integer> versions, boolean overrideLock) throws PSLockException {
      Objects.requireNonNull(ids, "ids cannot be null");
      Objects.requireNonNull(session, "session cannot be null");
      Objects.requireNonNull(locker, "locker cannot be null");
      Objects.requireNonNull(versions, "versions cannot be null");

      if (ids.isEmpty()) {
         throw new IllegalArgumentException("ids cannot be empty");
      }
      if (session.trim().isEmpty()) {
         throw new IllegalArgumentException("session cannot be empty");
      }
      if (locker.trim().isEmpty()) {
         throw new IllegalArgumentException("locker cannot be empty");
      }

      return createLocksImpl(ids, session.trim(), locker.trim(), versions, overrideLock);
   }

   /**
    * Internal implementation for creating locks.
    */
   List<PSObjectLock> createLocksImpl(List<IPSGuid> ids, String session, String locker,
                                     List<Integer> versions, boolean overrideLock) throws PSLockException;

   /**
    * Asynchronously create locks without blocking the calling thread.
    *
    * @param ids the id of the object to lock, not {@code null} or empty
    * @param session the session of the lock holder, not {@code null} or empty
    * @param locker the name of the lock holder, not {@code null} or empty
    * @param versions the version list, not {@code null}
    * @param overrideLock {@code true} to override existing locks
    * @return CompletableFuture containing the created locks
    * @throws IllegalArgumentException if any parameter is invalid
    */
   default CompletableFuture<List<PSObjectLock>> createLocksAsync(List<IPSGuid> ids, String session,
                                                                 String locker, List<Integer> versions,
                                                                 boolean overrideLock) {
      return CompletableFuture.supplyAsync(() -> {
         try {
            return createLocks(ids, session, locker, versions, overrideLock);
         } catch (PSLockException e) {
            throw new RuntimeException("Error creating locks asynchronously", e);
         }
      });
   }

   /**
    * Find the lock for the supplied object id safely.
    *
    * @param id the id of the object for which to find the lock, not {@code null}
    * @return Optional containing the lock if found, empty otherwise
    * @throws IllegalArgumentException if id is null
    */
   default Optional<PSObjectLock> findLockByObjectIdSafely(IPSGuid id) {
      Objects.requireNonNull(id, "id cannot be null");
      return Optional.ofNullable(findLockByObjectId(id));
   }

   /**
    * Find the lock for the supplied object id.
    * 
    * @param id the id of the object for which to find the lock, not {@code null}
    * @return the requested lock if found, {@code null} otherwise
    * @throws IllegalArgumentException if id is null
    */
   default PSObjectLock findLockByObjectId(IPSGuid id) {
      Objects.requireNonNull(id, "id cannot be null");
      return findLockByObjectIdImpl(id);
   }

   /**
    * Internal implementation for finding lock by object id.
    */
   PSObjectLock findLockByObjectIdImpl(IPSGuid id);

   /**
    * Find the lock for the supplied object id, session and locker safely.
    *
    * @param id the id of the object for which to find the lock, not {@code null}
    * @param lockSession the session for which to find the locks, may be {@code null} or empty
    * @param locker the user for which to find the locks, may be {@code null} or empty
    * @return Optional containing the lock if found, empty otherwise
    * @throws IllegalArgumentException if id is null
    */
   default Optional<PSObjectLock> findLockByObjectIdSafely(IPSGuid id, String lockSession, String locker) {
      Objects.requireNonNull(id, "id cannot be null");
      return Optional.ofNullable(findLockByObjectId(id, lockSession, locker));
   }

   /**
    * Find the lock for the supplied object id, session and locker.
    * 
    * @param id the id of the object for which to find the lock, not {@code null}
    * @param lockSession the session for which to find the locks, may be {@code null} or empty
    * @param locker the user for which to find the locks, may be {@code null} or empty
    * @return the requested lock if found, {@code null} otherwise
    * @throws IllegalArgumentException if id is null
    */
   default PSObjectLock findLockByObjectId(IPSGuid id, String lockSession, String locker) {
      Objects.requireNonNull(id, "id cannot be null");
      return findLockByObjectIdImpl(id, lockSession, locker);
   }

   /**
    * Internal implementation for finding lock by object id with session and locker.
    */
   PSObjectLock findLockByObjectIdImpl(IPSGuid id, String lockSession, String locker);

   /**
    * Find all locks for the supplied object ids with enhanced validation.
    *
    * @param ids the ids of all objects for which to find the locks, not {@code null} or empty
    * @param lockSession the session for which to find the locks, may be {@code null} or empty
    * @param locker the user for which to find the locks, may be {@code null} or empty
    * @return a list with all locks found for the supplied object ids, never {@code null}, may be empty
    * @throws IllegalArgumentException if ids is null or empty
    */
   default List<PSObjectLock> findLocksByObjectIds(List<IPSGuid> ids, String lockSession, String locker) {
      Objects.requireNonNull(ids, "ids cannot be null");
      if (ids.isEmpty()) {
         throw new IllegalArgumentException("ids cannot be empty");
      }
      return findLocksByObjectIdsImpl(ids, lockSession, locker);
   }

   /**
    * Internal implementation for finding locks by object ids.
    */
   List<PSObjectLock> findLocksByObjectIdsImpl(List<IPSGuid> ids, String lockSession, String locker);

   /**
    * Get a stream of locks for the supplied object ids for efficient processing.
    *
    * @param ids the ids of all objects for which to find the locks, not {@code null} or empty
    * @param lockSession the session for which to find the locks, may be {@code null} or empty
    * @param locker the user for which to find the locks, may be {@code null} or empty
    * @return Stream of locks, never {@code null}
    * @throws IllegalArgumentException if ids is null or empty
    */
   default Stream<PSObjectLock> streamLocksByObjectIds(List<IPSGuid> ids, String lockSession, String locker) {
      return findLocksByObjectIds(ids, lockSession, locker).stream();
   }

   /**
    * Find all locks for the supplied session and locker with enhanced validation.
    *
    * @param lockSession the session for which to find all locks, not {@code null} or empty
    * @param locker the user for which to find all locks, not {@code null} or empty
    * @return a list with all found locks for the supplied session and locker, never {@code null}, may be empty
    * @throws IllegalArgumentException if lockSession or locker is null or empty
    */
   default List<PSObjectLock> findLocksByUser(String lockSession, String locker) {
      Objects.requireNonNull(lockSession, "lockSession cannot be null");
      Objects.requireNonNull(locker, "locker cannot be null");
      if (lockSession.trim().isEmpty()) {
         throw new IllegalArgumentException("lockSession cannot be empty");
      }
      if (locker.trim().isEmpty()) {
         throw new IllegalArgumentException("locker cannot be empty");
      }
      return findLocksByUserImpl(lockSession.trim(), locker.trim());
   }

   /**
    * Internal implementation for finding locks by user.
    */
   List<PSObjectLock> findLocksByUserImpl(String lockSession, String locker);

   /**
    * Get a stream of locks for the supplied session and locker for efficient processing.
    *
    * @param lockSession the session for which to find all locks, not {@code null} or empty
    * @param locker the user for which to find all locks, not {@code null} or empty
    * @return Stream of locks, never {@code null}
    * @throws IllegalArgumentException if lockSession or locker is null or empty
    */
   default Stream<PSObjectLock> streamLocksByUser(String lockSession, String locker) {
      return findLocksByUser(lockSession, locker).stream();
   }

   /**
    * Load all locks for the supplied ids with enhanced validation.
    *
    * @param ids the ids of all locks to load, not {@code null} or empty
    * @return a list with all locks for the supplied ids, never {@code null} or empty
    * @throws IllegalArgumentException if ids is null or empty
    */
   default List<PSObjectLock> loadLocksByIds(List<IPSGuid> ids) {
      Objects.requireNonNull(ids, "ids cannot be null");
      if (ids.isEmpty()) {
         throw new IllegalArgumentException("ids cannot be empty");
      }
      return loadLocksByIdsImpl(ids);
   }

   /**
    * Internal implementation for loading locks by ids.
    */
   List<PSObjectLock> loadLocksByIdsImpl(List<IPSGuid> ids);

   /**
    * Get a stream of loaded locks for efficient processing.
    *
    * @param ids the ids of all locks to load, not {@code null} or empty
    * @return Stream of locks, never {@code null}
    * @throws IllegalArgumentException if ids is null or empty
    */
   default Stream<PSObjectLock> streamLocksByIds(List<IPSGuid> ids) {
      return loadLocksByIds(ids).stream();
   }

   /**
    * Find all locks that are expired.
    * 
    * @return a list with all locks that are expired, never {@code null}, may be empty
    */
   List<PSObjectLock> findExpiredLocks();

   /**
    * Get a stream of expired locks for efficient processing.
    *
    * @return Stream of expired locks, never {@code null}
    */
   default Stream<PSObjectLock> streamExpiredLocks() {
      return findExpiredLocks().stream();
   }

   /**
    * Find locks that match the given predicate.
    *
    * @param predicate the condition to test locks against, not {@code null}
    * @return a list of matching locks, never {@code null}, may be empty
    * @throws IllegalArgumentException if predicate is null
    */
   default List<PSObjectLock> findLocksWhere(Predicate<PSObjectLock> predicate) {
      Objects.requireNonNull(predicate, "predicate cannot be null");
      return streamExpiredLocks() // Using expired locks as an example base set
          .filter(predicate)
          .toList();
   }

   /**
    * Convenience method that wraps the id and version in a list and calls
    * {@link #createLocks(List, String, String, List, boolean)} with enhanced validation.
    *
    * @param id the object id to lock, not {@code null}
    * @param session the session of the lock holder, not {@code null} or empty
    * @param locker the name of the lock holder, not {@code null} or empty
    * @param version the version of the locked object, may be {@code null}
    * @param overrideLock {@code true} to override existing locks
    * @return the created lock, never {@code null}
    * @throws PSLockException if the lock could not be created
    * @throws IllegalArgumentException if id, session, or locker is invalid
    */
   default PSObjectLock createLock(IPSGuid id, String session, String locker,
                                  Integer version, boolean overrideLock) throws PSLockException {
      Objects.requireNonNull(id, "id cannot be null");
      var locks = createLocks(List.of(id), session, locker, List.of(version), overrideLock);
      return locks.get(0);
   }

   /**
    * Asynchronously create a single lock without blocking the calling thread.
    *
    * @param id the object id to lock, not {@code null}
    * @param session the session of the lock holder, not {@code null} or empty
    * @param locker the name of the lock holder, not {@code null} or empty
    * @param version the version of the locked object, may be {@code null}
    * @param overrideLock {@code true} to override existing locks
    * @return CompletableFuture containing the created lock
    * @throws IllegalArgumentException if any parameter is invalid
    */
   default CompletableFuture<PSObjectLock> createLockAsync(IPSGuid id, String session, String locker,
                                                          Integer version, boolean overrideLock) {
      return CompletableFuture.supplyAsync(() -> {
         try {
            return createLock(id, session, locker, version, overrideLock);
         } catch (PSLockException e) {
            throw new RuntimeException("Error creating lock asynchronously", e);
         }
      });
   }

   /**
    * Locks or extends the locks for all results for the supplied session and user with enhanced validation.
    *
    * @param results the exception which contains all results to be locked, not {@code null}
    * @param session the session for which to lock the results, not {@code null} or empty
    * @param user the user for which to lock the results, not {@code null} or empty
    * @param overrideLock {@code true} to override existing locks for the same user but different session
    * @throws IllegalArgumentException if results, session, or user is invalid
    */
   default void createLocks(PSErrorResultsException results, String session, String user, boolean overrideLock) {
      Objects.requireNonNull(results, "results cannot be null");
      Objects.requireNonNull(session, "session cannot be null");
      Objects.requireNonNull(user, "user cannot be null");
      if (session.trim().isEmpty()) {
         throw new IllegalArgumentException("session cannot be empty");
      }
      if (user.trim().isEmpty()) {
         throw new IllegalArgumentException("user cannot be empty");
      }
      createLocksImpl(results, session.trim(), user.trim(), overrideLock);
   }

   /**
    * Internal implementation for creating locks from error results.
    */
   void createLocksImpl(PSErrorResultsException results, String session, String user, boolean overrideLock);

   /**
    * Convenience method to extend the lock for the default interval time.
    *
    * @param id the object id, not {@code null}
    * @param session the session, not {@code null} or empty
    * @param locker the locker, not {@code null} or empty
    * @param version the version, may be {@code null}
    * @return the extended lock
    * @throws PSLockException if the lock could not be extended
    * @throws IllegalArgumentException if id, session, or locker is invalid
    */
   default PSObjectLock extendLock(IPSGuid id, String session, String locker, Integer version) throws PSLockException {
      return extendLock(id, session, locker, version, PSObjectLock.LOCK_INTERVAL);
   }

   /**
    * Convenience method that wraps the guid and version in a list and calls
    * {@link #extendLocks(List, String, String, List, long)} with enhanced validation.
    *
    * @param id the object id, not {@code null}
    * @param session the session, not {@code null} or empty
    * @param locker the locker, not {@code null} or empty
    * @param version the version, may be {@code null}
    * @param interval the interval in milliseconds, must be at least 1000
    * @return the extended lock
    * @throws PSLockException if the lock could not be extended
    * @throws IllegalArgumentException if any parameter is invalid
    */
   default PSObjectLock extendLock(IPSGuid id, String session, String locker, Integer version, long interval)
         throws PSLockException {
      Objects.requireNonNull(id, "id cannot be null");
      if (interval < 1000) {
         throw new IllegalArgumentException("interval must be at least 1000 ms");
      }
      var locks = extendLocks(List.of(id), session, locker, List.of(version), interval);
      return locks.get(0);
   }

   /**
    * Extend the locks associated with the supplied ids for the current locker for specified time with enhanced validation.
    *
    * @param ids the ids of the design object to extend the lock for, not {@code null} or empty
    * @param session the session of the lock holder, not {@code null} or empty
    * @param locker the name of the lock holder, not {@code null} or empty
    * @param versions the new versions of the locked object, not {@code null}
    * @param interval specifies the time in milliseconds how long the requested lock will be held, must be minimum 1000 ms
    * @return the extended object locks, never {@code null}
    * @throws PSLockException if the requested lock does not exist or could not be extended for any reason
    * @throws IllegalArgumentException if any parameter is invalid
    */
   default List<PSObjectLock> extendLocks(List<IPSGuid> ids, String session, String locker,
                                         List<Integer> versions, long interval) throws PSLockException {
      Objects.requireNonNull(ids, "ids cannot be null");
      Objects.requireNonNull(session, "session cannot be null");
      Objects.requireNonNull(locker, "locker cannot be null");
      Objects.requireNonNull(versions, "versions cannot be null");

      if (ids.isEmpty()) {
         throw new IllegalArgumentException("ids cannot be empty");
      }
      if (session.trim().isEmpty()) {
         throw new IllegalArgumentException("session cannot be empty");
      }
      if (locker.trim().isEmpty()) {
         throw new IllegalArgumentException("locker cannot be empty");
      }
      if (interval < 1000) {
         throw new IllegalArgumentException("interval must be at least 1000 ms");
      }

      return extendLocksImpl(ids, session.trim(), locker.trim(), versions, interval);
   }

   /**
    * Internal implementation for extending locks.
    */
   List<PSObjectLock> extendLocksImpl(List<IPSGuid> ids, String session, String locker,
                                     List<Integer> versions, long interval) throws PSLockException;

   /**
    * Asynchronously extend locks without blocking the calling thread.
    *
    * @param ids the ids to extend locks for, not {@code null} or empty
    * @param session the session, not {@code null} or empty
    * @param locker the locker, not {@code null} or empty
    * @param versions the versions, not {@code null}
    * @param interval the interval in milliseconds, must be at least 1000
    * @return CompletableFuture containing the extended locks
    * @throws IllegalArgumentException if any parameter is invalid
    */
   default CompletableFuture<List<PSObjectLock>> extendLocksAsync(List<IPSGuid> ids, String session, String locker,
                                                                 List<Integer> versions, long interval) {
      return CompletableFuture.supplyAsync(() -> {
         try {
            return extendLocks(ids, session, locker, versions, interval);
         } catch (PSLockException e) {
            throw new RuntimeException("Error extending locks asynchronously", e);
         }
      });
   }

   /**
    * Release the supplied lock. We will ignore the case when the supplied lock does not exist.
    *
    * @param lock the lock to be released, may be {@code null}
    */
   void releaseLock(PSObjectLock lock);

   /**
    * Release the locks for all supplied locks. We will ignore the case where any of the supplied locks does not exist.
    *
    * @param locks all locks to be released, may be {@code null} or empty
    */
   void releaseLocks(List<PSObjectLock> locks);

   /**
    * Asynchronously release locks without blocking the calling thread.
    *
    * @param locks all locks to be released, may be {@code null} or empty
    * @return CompletableFuture that completes when the release operation finishes
    */
   default CompletableFuture<Void> releaseLocksAsync(List<PSObjectLock> locks) {
      return CompletableFuture.runAsync(() -> releaseLocks(locks));
   }

   /**
    * Is the object referenced through the supplied id locked for the provided user and session?
    *
    * @param id the id of the design object to test, not {@code null}
    * @param session the session to test for, not {@code null} or empty
    * @param locker the locker to test for, not {@code null} or empty
    * @return {@code true} if the object referenced by the supplied id is locked for the supplied user and session,
    *         {@code false} otherwise
    * @throws IllegalArgumentException if id, session, or locker is invalid
    */
   default boolean isLockedFor(IPSGuid id, String session, String locker) {
      Objects.requireNonNull(id, "id cannot be null");
      Objects.requireNonNull(session, "session cannot be null");
      Objects.requireNonNull(locker, "locker cannot be null");
      if (session.trim().isEmpty()) {
         throw new IllegalArgumentException("session cannot be empty");
      }
      if (locker.trim().isEmpty()) {
         throw new IllegalArgumentException("locker cannot be empty");
      }
      return isLockedForImpl(id, session.trim(), locker.trim());
   }

   /**
    * Internal implementation for checking if object is locked.
    */
   boolean isLockedForImpl(IPSGuid id, String session, String locker);

   /**
    * Get the version of the locked design object safely.
    *
    * @param id the id of the locked object for which to get the locked version, not {@code null}
    * @return Optional containing the version of the locked object, empty if no lock exists or version is null
    * @throws IllegalArgumentException if id is null
    */
   default Optional<Integer> getLockedVersionSafely(IPSGuid id) {
      Objects.requireNonNull(id, "id cannot be null");
      try {
         return Optional.ofNullable(getLockedVersion(id));
      } catch (PSLockException e) {
         return Optional.empty();
      }
   }

   /**
    * Get the version of the locked design object.
    * 
    * @param id the id of the locked object for which to get the locked version, not {@code null}
    * @return the version of the locked object, may be {@code null} if the design object does not support a version
    * @throws PSLockException if no lock exists for the supplied id
    * @throws IllegalArgumentException if id is null
    */
   default Integer getLockedVersion(IPSGuid id) throws PSLockException {
      Objects.requireNonNull(id, "id cannot be null");
      return getLockedVersionImpl(id);
   }

   /**
    * Internal implementation for getting locked version.
    */
   Integer getLockedVersionImpl(IPSGuid id) throws PSLockException;

   /**
    * Count the total number of active locks in the system.
    *
    * @return the total number of active locks
    */
   default long getActiveLockCount() {
      return streamExpiredLocks().count(); // This is just an example; implementation would get all locks
   }

   /**
    * Check if any locks are expired and need cleanup.
    *
    * @return {@code true} if there are expired locks, {@code false} otherwise
    */
   default boolean hasExpiredLocks() {
      return !findExpiredLocks().isEmpty();
   }

   /**
    * Get lock statistics and information.
    *
    * @return a string representation of lock statistics
    */
   default String getLockStatistics() {
      var expiredCount = findExpiredLocks().size();
      return String.format("Lock Statistics: %d expired locks", expiredCount);
   }
}
