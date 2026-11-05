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
package com.percussion.services.ui;

import com.percussion.error.IPSErrorCode;
import com.percussion.utils.exceptions.PSBaseException;
import com.percussion.utils.guid.IPSGuid;

import java.util.Objects;
import java.util.Optional;
import com.intsof.percussioncms.auditlog.codes.UiErrorCodes;

/**
 * Exception thrown when UI service operations fail with modern Java 11 patterns.
 * Provides enhanced error context, factory methods for common scenarios, and
 * Optional-based safe access to error details.
 *
 * @author Percussion Software
 */
public class PSUiException extends PSBaseException {

   /**
    * Compiler generated serial version ID used for serialization.
    */
   private static final long serialVersionUID = -6886987313402394369L;

   /**
    * Optional context information about the failed operation.
    */
   private final Optional<String> operationContext;

   /**
    * Optional node ID that caused the exception.
    */
   private final Optional<IPSGuid> nodeId;

   /**
    * Create a UI exception with the specified message code.
    *
    * @param msgCode the error message code
    */
   public PSUiException(int msgCode) {
      super(msgCode);
      this.operationContext = Optional.empty();
      this.nodeId = Optional.empty();
   }

   /**
    * Create a UI exception with message code and arguments.
    *
    * @param msgCode the error message code
    * @param arrayArgs the message arguments, may be null
    */
   public PSUiException(int msgCode, Object... arrayArgs) {
      super(msgCode, arrayArgs);
      this.operationContext = Optional.empty();
      this.nodeId = Optional.empty();
   }

   /**
    * Create a UI exception with message code, cause, and arguments.
    *
    * @param msgCode the error message code
    * @param cause the underlying cause, may be null
    * @param arrayArgs the message arguments, may be null
    */
   public PSUiException(int msgCode, Throwable cause, Object... arrayArgs) {
      super(msgCode, cause, arrayArgs);
      this.operationContext = Optional.empty();
      this.nodeId = Optional.empty();
   }

   /**
    * Typed construction from a catalogued {@link IPSErrorCode}.
    *
    * @param code catalogued error code, never {@code null}
    * @param arrayArgs the message arguments, may be null
    */
   public PSUiException(IPSErrorCode code, Object... arrayArgs) {
      super(code, arrayArgs);
      this.operationContext = Optional.empty();
      this.nodeId = Optional.empty();
   }

   /**
    * Typed construction with a cause.
    *
    * @param code catalogued error code, never {@code null}
    * @param cause the underlying cause, may be null
    * @param arrayArgs the message arguments, may be null
    */
   public PSUiException(IPSErrorCode code, Throwable cause, Object... arrayArgs) {
      super(code, cause, arrayArgs);
      this.operationContext = Optional.empty();
      this.nodeId = Optional.empty();
   }

   /**
    * Create a UI exception with enhanced context information.
    *
    * @param msgCode the error message code
    * @param operationContext optional context about the failed operation
    * @param nodeId optional node ID that caused the exception
    * @param arrayArgs the message arguments, may be null
    */
   public PSUiException(int msgCode, Optional<String> operationContext, Optional<IPSGuid> nodeId,
         Object... arrayArgs) {
      super(msgCode, arrayArgs);
      this.operationContext = operationContext != null ? operationContext : Optional.empty();
      this.nodeId = nodeId != null ? nodeId : Optional.empty();
   }

   /**
    * Create a UI exception with enhanced context information and cause.
    *
    * @param msgCode the error message code
    * @param cause the underlying cause, may be null
    * @param operationContext optional context about the failed operation
    * @param nodeId optional node ID that caused the exception
    * @param arrayArgs the message arguments, may be null
    */
   public PSUiException(int msgCode, Throwable cause, Optional<String> operationContext,
         Optional<IPSGuid> nodeId, Object... arrayArgs) {
      super(msgCode, cause, arrayArgs);
      this.operationContext = operationContext != null ? operationContext : Optional.empty();
      this.nodeId = nodeId != null ? nodeId : Optional.empty();
   }

   /**
    * Typed construction with enhanced context information.
    *
    * @param code catalogued error code, never {@code null}
    * @param operationContext optional context about the failed operation
    * @param nodeId optional node ID that caused the exception
    * @param arrayArgs the message arguments, may be null
    */
   public PSUiException(IPSErrorCode code, Optional<String> operationContext, Optional<IPSGuid> nodeId,
         Object... arrayArgs) {
      super(code, arrayArgs);
      this.operationContext = operationContext != null ? operationContext : Optional.empty();
      this.nodeId = nodeId != null ? nodeId : Optional.empty();
   }

   /**
    * Typed construction with enhanced context information and cause.
    *
    * @param code catalogued error code, never {@code null}
    * @param cause the underlying cause, may be null
    * @param operationContext optional context about the failed operation
    * @param nodeId optional node ID that caused the exception
    * @param arrayArgs the message arguments, may be null
    */
   public PSUiException(IPSErrorCode code, Throwable cause, Optional<String> operationContext,
         Optional<IPSGuid> nodeId, Object... arrayArgs) {
      super(code, cause, arrayArgs);
      this.operationContext = operationContext != null ? operationContext : Optional.empty();
      this.nodeId = nodeId != null ? nodeId : Optional.empty();
   }

   /**
    * Get the operation context that caused this exception.
    *
    * @return Optional containing the operation context, empty if not available
    */
   public Optional<String> getOperationContext() {
      return operationContext;
   }

   /**
    * Get the node ID that caused this exception.
    *
    * @return Optional containing the node ID, empty if not available
    */
   public Optional<IPSGuid> getNodeId() {
      return nodeId;
   }

   // Factory methods for common UI exception scenarios

   /**
    * Create an exception for a node not found scenario.
    *
    * @param nodeId the ID of the node that was not found, never null
    * @return PSUiException with appropriate error context
    */
   public static PSUiException nodeNotFound(IPSGuid nodeId) {
      Objects.requireNonNull(nodeId, "Node ID cannot be null");
      return new PSUiException(
         UiErrorCodes.MISSING_HIERARCHY_NODE,
         Optional.of("Node lookup operation"),
         Optional.of(nodeId),
         nodeId.toString()
      );
   }

   /**
    * Create an exception for a duplicate node name scenario.
    *
    * @param nodeName the name that already exists, never null
    * @param parentId the parent node ID, may be null for root nodes
    * @return PSUiException with appropriate error context
    */
   public static PSUiException duplicateNodeName(String nodeName, IPSGuid parentId) {
      Objects.requireNonNull(nodeName, "Node name cannot be null");
      return new PSUiException(
         UiErrorCodes.DUPLICATE_NODE_NAME,
         Optional.of("Node creation operation"),
         Optional.ofNullable(parentId),
         nodeName, parentId != null ? parentId.toString() : "root"
      );
   }

   /**
    * Create an exception for invalid node hierarchy operations.
    *
    * @param operation the operation that failed, never null
    * @param nodeId the node ID involved, may be null
    * @param reason the reason for the failure, never null
    * @return PSUiException with appropriate error context
    */
   public static PSUiException invalidHierarchyOperation(String operation, IPSGuid nodeId, String reason) {
      Objects.requireNonNull(operation, "Operation cannot be null");
      Objects.requireNonNull(reason, "Reason cannot be null");
      return new PSUiException(
         UiErrorCodes.INVALID_HIERARCHY_OPERATION,
         Optional.of(operation),
         Optional.ofNullable(nodeId),
         operation, reason
      );
   }

   /**
    * Create an exception for node type mismatch scenarios.
    *
    * @param nodeId the node ID with wrong type, never null
    * @param expectedType the expected node type, never null
    * @param actualType the actual node type, never null
    * @return PSUiException with appropriate error context
    */
   public static PSUiException nodeTypeMismatch(IPSGuid nodeId, String expectedType, String actualType) {
      Objects.requireNonNull(nodeId, "Node ID cannot be null");
      Objects.requireNonNull(expectedType, "Expected type cannot be null");
      Objects.requireNonNull(actualType, "Actual type cannot be null");
      return new PSUiException(
         UiErrorCodes.NODE_TYPE_MISMATCH,
         Optional.of("Node type validation"),
         Optional.of(nodeId),
         nodeId.toString(), expectedType, actualType
      );
   }

   /**
    * Create an exception for general UI operation failures.
    *
    * @param operation the operation that failed, never null
    * @param cause the underlying cause, may be null
    * @param details additional error details, never null
    * @return PSUiException with appropriate error context
    */
   public static PSUiException operationFailed(String operation, Throwable cause, String details) {
      Objects.requireNonNull(operation, "Operation cannot be null");
      Objects.requireNonNull(details, "Details cannot be null");
      return new PSUiException(
         UiErrorCodes.OPERATION_FAILED,
         cause,
         Optional.of(operation),
         Optional.empty(),
         operation, details
      );
   }

   @Override
   public String toString() {
      var sb = new StringBuilder(super.toString());

      operationContext.ifPresent(context ->
         sb.append(" [Operation: ").append(context).append("]"));

      nodeId.ifPresent(id ->
         sb.append(" [Node: ").append(id).append("]"));

      return sb.toString();
   }

   @Override
   protected String getResourceBundleBaseName() {
      return "com.percussion.services.ui.PSUiErrorStringBundle";
   }
}
