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

import com.intsof.percussioncms.auditlog.codes.LockErrorCodes;
import com.percussion.error.IPSErrorCode;
import com.percussion.services.locking.data.PSObjectLock;
import com.percussion.utils.exceptions.PSBaseException;
import com.percussion.utils.guid.IPSGuid;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Modern lock exception for handling locking problems with comprehensive Java 11 features.
 * This exception supports both single exception cases and multi-object operation exceptions
 * with enhanced validation, Optional-based safe access, and static factory methods.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>Optional-based safe access for nullable properties</li>
 * <li>Enhanced validation with Objects.requireNonNull</li>
 * <li>Static factory methods for common lock error scenarios</li>
 * <li>Immutable collections for results and errors</li>
 * </ul>
 *
 * <p>For multi-object operations, use {@link #getResults()} and {@link #getErrors()}.
 * For single operations, use the individual property accessors.</p>
 */
public class PSLockException extends PSBaseException {

   /**
    * Compiler generated serial version ID used for serialization.
    */
   private static final long serialVersionUID = 8098402028890822664L;

   /**
    * Constructor for bulk operations that fail with enhanced validation.
    *
    * @param successes An entry for each attempt. Either a valid lock, or {@code null}
    *                  if a lock could not be obtained. May be {@code null} or empty
    * @param errors The exception that occurred for each id that has a {@code null} entry
    *               in the successes list, not {@code null} or empty
    * @throws IllegalArgumentException if errors is null or empty
    */
   public PSLockException(List<PSObjectLock> successes, Map<IPSGuid, PSLockException> errors) {
      super(LockErrorCodes.MULTI_OPERATION);
      Objects.requireNonNull(errors, "errors cannot be null");
      if (errors.isEmpty()) {
         throw new IllegalArgumentException("errors cannot be empty");
      }

      // successes is 1:1 with the requested ids and uses null for failures.
      // List.copyOf rejects null elements and NPEs the bulk path (#4039).
      this.results = copyResultsAllowingNulls(successes);
      this.errors = Map.copyOf(errors);
      // This constructor represents a multi-operation failure; mark single-op fields as unset
      this.id = -1L;
      this.locker = null;
      this.remainingTime = -1L;
   }

   /**
    * Convenience constructor for simple lock failures.
    *
    * @param msgCode the error code
    * @param id the object id for which the lock failed
    */
   public PSLockException(int msgCode, long id) {
      this(msgCode, id, null, -1);
   }

   /**
    * Convenience constructor for lock conflicts.
    *
    * @param msgCode the error code
    * @param id the object id for which the lock failed
    * @param locker the name of the user who has the object locked, may be {@code null}
    * @param remainingTime the remaining time of the current lock, -1 if not locked
    */
   public PSLockException(int msgCode, long id, String locker, long remainingTime) {
      this(msgCode, id, locker, remainingTime, null);
   }

   /**
    * Constructs a new lock exception for the supplied parameters with enhanced validation.
    *
    * @param msgCode the error code
    * @param id the object id for which the lock failed
    * @param locker the name of the user who has the requested object locked,
    *               may be {@code null} if the object is not locked, never empty
    * @param remainingTime the remaining time of the current lock, -1 if the
    *                      requested object is not locked
    * @param cause the original exception that caused this exception to be thrown, may be {@code null}
    * @throws IllegalArgumentException if locker is empty or remainingTime is invalid
    */
   public PSLockException(int msgCode, long id, String locker, long remainingTime, Throwable cause) {
      super(msgCode, cause, new Object[] {id, locker, remainingTime});

      if (locker != null && locker.trim().isEmpty()) {
         throw new IllegalArgumentException("locker cannot be empty");
      }

      if (locker != null && !locker.trim().isEmpty() && remainingTime <= 0) {
         throw new IllegalArgumentException("remainingTime must be > 0 when locker is specified");
      }

      this.id = id;
      this.locker = locker != null ? locker.trim() : null;
      this.remainingTime = remainingTime;
      // Single-op constructor: mark multi-op fields as null
      this.results = null;
      this.errors = null;
   }

   /**
    * Typed convenience constructor for simple lock failures.
    *
    * @param code catalogued error code, never {@code null}
    * @param id the object id for which the lock failed
    */
   public PSLockException(IPSErrorCode code, long id) {
      this(code, id, null, -1);
   }

   /**
    * Typed convenience constructor for lock conflicts.
    *
    * @param code catalogued error code, never {@code null}
    * @param id the object id for which the lock failed
    * @param locker the name of the user who has the object locked, may be {@code null}
    * @param remainingTime the remaining time of the current lock, -1 if not locked
    */
   public PSLockException(IPSErrorCode code, long id, String locker, long remainingTime) {
      this(code, id, locker, remainingTime, null);
   }

   /**
    * Typed construction for a single-object lock failure.
    *
    * @param code catalogued error code, never {@code null}
    * @param id the object id for which the lock failed
    * @param locker the name of the user who has the requested object locked, may be {@code null}
    * @param remainingTime the remaining time of the current lock, -1 if not locked
    * @param cause the original exception that caused this exception, may be {@code null}
    */
   public PSLockException(
         IPSErrorCode code, long id, String locker, long remainingTime, Throwable cause) {
      super(code, cause, new Object[] {id, locker, remainingTime});

      if (locker != null && locker.trim().isEmpty()) {
         throw new IllegalArgumentException("locker cannot be empty");
      }

      if (locker != null && !locker.trim().isEmpty() && remainingTime <= 0) {
         throw new IllegalArgumentException("remainingTime must be > 0 when locker is specified");
      }

      this.id = id;
      this.locker = locker != null ? locker.trim() : null;
      this.remainingTime = remainingTime;
      this.results = null;
      this.errors = null;
   }

   /**
    * Creates a lock exception for object already locked scenarios.
    *
    * @param objectId the ID of the locked object
    * @param currentLocker the user who currently holds the lock, not null
    * @param remainingTime the remaining lock time in milliseconds
    * @return a new PSLockException instance
    * @throws IllegalArgumentException if currentLocker is null or empty, or remainingTime is less than or equal to 0
    */
   public static PSLockException objectAlreadyLocked(long objectId, String currentLocker, long remainingTime) {
      Objects.requireNonNull(currentLocker, "currentLocker cannot be null");
      if (currentLocker.trim().isEmpty()) {
         throw new IllegalArgumentException("currentLocker cannot be empty");
      }
      if (remainingTime <= 0) {
         throw new IllegalArgumentException("remainingTime must be > 0");
      }
      return new PSLockException(LockErrorCodes.OBJECT_ALREADY_LOCKED, objectId, currentLocker, remainingTime);
   }

   /**
    * Creates a lock exception for lock not found scenarios.
    *
    * @param objectId the ID of the object for which no lock was found
    * @return a new PSLockException instance
    */
   public static PSLockException lockNotFound(long objectId) {
      return new PSLockException(LockErrorCodes.LOCK_NOT_FOUND, objectId);
   }

   /**
    * Creates a lock exception for expired lock scenarios.
    *
    * @param objectId the ID of the object with expired lock
    * @return a new PSLockException instance
    */
   public static PSLockException lockExpired(long objectId) {
      return new PSLockException(LockErrorCodes.LOCK_EXPIRED, objectId);
   }

   /**
    * Creates a lock exception for permission denied scenarios.
    *
    * @param objectId the ID of the object
    * @param attemptedUser the user who attempted the lock operation
    * @return a new PSLockException instance
    * @throws IllegalArgumentException if attemptedUser is null or empty
    */
   public static PSLockException permissionDenied(long objectId, String attemptedUser) {
      Objects.requireNonNull(attemptedUser, "attemptedUser cannot be null");
      if (attemptedUser.trim().isEmpty()) {
         throw new IllegalArgumentException("attemptedUser cannot be empty");
      }
      return new PSLockException(LockErrorCodes.PERMISSION_DENIED, objectId, attemptedUser, -1);
   }

   /**
    * Creates a bulk operation lock exception.
    *
    * @param successes the successful lock operations, may be {@code null} or empty
    * @param errors the failed operations mapped by GUID, not {@code null} or empty
    * @return a new PSLockException instance
    * @throws IllegalArgumentException if errors is null or empty
    */
   public static PSLockException bulkOperationFailed(List<PSObjectLock> successes,
                                                    Map<IPSGuid, PSLockException> errors) {
      return new PSLockException(successes, errors);
   }

   /**
    * Get the results of a multi-operation safely.
    *
    * @return Optional containing the results if this is a multi-operation exception, empty otherwise
    */
   public Optional<List<PSObjectLock>> findResults() {
      return Optional.ofNullable(results).map(Collections::unmodifiableList);
   }

   /**
    * Contains an entry for each id supplied to the original operation.
    *
    * @return If this is a multi-operation exception, then a non-{@code null} list is returned,
    *         otherwise the return is {@code null}
    */
   public List<PSObjectLock> getResults() {
      return results != null ? Collections.unmodifiableList(results) : null;
   }

   /**
    * Get the errors of a multi-operation safely.
    *
    * @return Optional containing the errors if this is a multi-operation exception, empty otherwise
    */
   public Optional<Map<IPSGuid, PSLockException>> findErrors() {
      return Optional.ofNullable(errors).map(Collections::unmodifiableMap);
   }

   /**
    * Contains an entry for every entry that failed to obtain a lock.
    *
    * @return If this is a multi-operation exception, then a non-{@code null},
    *         non empty map is returned, otherwise the return is {@code null}
    */
   public Map<IPSGuid, PSLockException> getErrors() {
      return errors != null ? Collections.unmodifiableMap(errors) : null;
   }

   /**
    * Get the id of the object for which this exception was thrown.
    *
    * @return the id of the object for which this exception was thrown if this
    *         is a single-op error, otherwise -1
    */
   public long getId() {
      return id;
   }

   /**
    * Get the locker name safely.
    *
    * @return Optional containing the locker name if present, empty otherwise
    */
   public Optional<String> findLocker() {
      return Optional.ofNullable(locker).filter(name -> !name.trim().isEmpty());
   }

   /**
    * Get the name of the user who locks the requested object.
    *
    * @return the name of the user who locks the requested object, may be {@code null}
    *         if the requested object is not locked. If this is a multi-op error, it is {@code null}
    */
   public String getLocker() {
      return locker;
   }

   /**
    * Get the remaining lock time safely.
    *
    * @return Optional containing the remaining time if > 0, empty otherwise
    */
   public Optional<Long> findRemainingTime() {
      return remainingTime > 0 ? Optional.of(remainingTime) : Optional.empty();
   }

   /**
    * Get the remaining lock time.
    *
    * @return the remaining lock time, -1 if the requested object is not locked.
    *         If this is a multi-op error, -1
    */
   public long getRemainingTime() {
      return remainingTime;
   }

   /**    * Backwards-compatible misspelled accessor retained for legacy code.
    * @deprecated use {@link #getRemainingTime()} instead
    */
   @Deprecated
   public long getRemainigTime() {
       return getRemainingTime();
   }

   /**    * Check if this is a multi-operation exception.
    *
    * @return {@code true} if this exception represents multiple operation failures
    */
   public boolean isMultiOperation() {
      return results != null || errors != null;
   }

   /**
    * Check if this exception indicates an object is currently locked.
    *
    * @return {@code true} if the object is locked by another user
    */
   public boolean isObjectLocked() {
      return findLocker().isPresent() && findRemainingTime().isPresent();
   }

   /**
    * Get the number of successful operations in a multi-operation exception.
    *
    * @return the number of successful operations, 0 if not a multi-operation
    */
   public int getSuccessCount() {
      return results != null ? (int) results.stream().filter(Objects::nonNull).count() : 0;
   }

   /**
    * Get the number of failed operations in a multi-operation exception.
    *
    * @return the number of failed operations, 0 if not a multi-operation
    */
   public int getErrorCount() {
      return errors != null ? errors.size() : 0;
   }

   /**
    * Get a summary of this lock exception.
    *
    * @return a formatted string containing exception details
    */
   public String getSummary() {
      if (isMultiOperation()) {
         return String.format("LockException[multiOp: %d successes, %d errors]",
             getSuccessCount(), getErrorCount());
      } else {
         return String.format("LockException[id=%d, locker='%s', remaining=%d]",
             getId(), findLocker().orElse("none"), getRemainingTime());
      }
   }

   @Override
   protected String getResourceBundleBaseName() {
      return "com.percussion.services.locking.PSLockErrorStringBundle";
   }

   /**
    * Copy a bulk-lock results list, preserving {@code null} slots for failed ids.
    * {@link List#copyOf(java.util.Collection)} throws NPE on null elements.
    */
   private static List<PSObjectLock> copyResultsAllowingNulls(List<PSObjectLock> successes) {
      if (successes == null || successes.isEmpty()) {
         return List.of();
      }
      return Collections.unmodifiableList(new java.util.ArrayList<>(successes));
   }

   /**
    * The id of the object we tried to lock, extend or release.
    */
   private final long id;

   /**
    * The name of the user who has the requested object locked.
    * May be {@code null} if the requested object is not locked.
    */
   private final String locker;

   /**
    * The remaining time of the existing lock, -1 if the requested object is not locked.
    */
   private final long remainingTime;

   /**
    * Immutable list of results for multi-operation exceptions.
    * {@code null} unless this object was created for bulk operations.
    */
   private final List<PSObjectLock> results;

   /**
    * Immutable map of errors for multi-operation exceptions.
    * {@code null} unless this object was created for bulk operations.
    */
   private final Map<IPSGuid, PSLockException> errors;
}
