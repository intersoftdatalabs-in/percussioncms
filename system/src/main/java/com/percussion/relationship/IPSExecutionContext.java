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
package com.percussion.relationship;

import com.percussion.design.objectstore.PSRelationship;

import java.util.Set;

/**
 * This is an interface to define the execution context needed for
 * relationship effects. Depending on where relationships are processed, the
 * execution context will be different. All context classes that can be
 * used to process relationships must implement this interface. See
 * {@code com.percussion.design.objectstore.PSRelationship} and
 * PSXRelationshipSet.dtd for a description of relationships.
 */
public interface IPSExecutionContext
{
   /**
    * Returns the effect execution context type.
    *
    * @return the effect execution context.
    */
   int getContextType();

   /**
    * Is this called before construction? See {@link #RS_PRE_CONSTRUCTION} for
    * detail info.
    *
    * @return {@code true} if it is, {@code false} otherwise.
    */
   boolean isPreConstruction();

   /**
    * @deprecated use {@link #isPreConstruction()} instead.
    */
   @Deprecated
   boolean isPostConstruction();

   /**
    * @deprecated use {@link #isPreConstruction()} instead.
    */
   @Deprecated
   boolean isConstruction();

   /**
    * Is this called before destruction? See {@link #RS_PRE_DESTRUCTION} for
    * detail info.
    *
    * @return {@code true} if it is, {@code false} otherwise.
    */
   boolean isPreDestruction();

   /**
    * @deprecated use {@link #isPreDestruction()} instead.
    */
   @Deprecated
   boolean isPostDestruction();

   /**
    * @deprecated use {@link #isPreDestruction()} instead.
    */
   @Deprecated
   boolean isDestruction();

   /**
    * Is this called before a workflow transition?
    *
    * @return {@code true} if it is, {@code false} otherwise.
    */
   boolean isPreWorkflow();

   /**
    * Is this called after a workflow transition?
    *
    * @return {@code true} if it is, {@code false} otherwise.
    */
   boolean isPostWorkflow();

   /**
    * Is this called before a checkin?
    *
    * @return {@code true} if it is, {@code false} otherwise.
    */
   boolean isPreCheckin();

   /**
    * Is this called before a checkin?
    *
    * @return {@code true} if it is, {@code false} otherwise.
    * @deprecated use {@link #isPreCheckin()} instead.
    */
   @Deprecated
   boolean isCheckin();

   /**
    * Is this called after a checkout?
    *
    * @return {@code true} if it is, {@code false} otherwise.
    */
   boolean isPostCheckout();

   /**
    * Is this called after a checkout?
    *
    * @return {@code true} if it is, {@code false} otherwise.
    * @deprecated use {@link #isPostCheckout()} instead.
    */
   @Deprecated
   boolean isCheckout();

   /**
    * Is this called before an object update? See {@link #RS_PRE_UPDATE} for
    * detail info.
    *
    * @return {@code true} if it is, {@code false} otherwise.
    */
   boolean isPreUpdate();

   /**
    * @deprecated use {@link #isPreUpdate()} instead.
    */
   @Deprecated
   boolean isPostUpdate();

   /**
    * @deprecated use {@link #isPreUpdate()} instead.
    */
   @Deprecated
   boolean isUpdate();

   /**
    * Is this called before a clone process
    *
    * @return {@code true} if it is, {@code false} otherwise.
    */
   boolean isPreClone();

   /**
    * This context is used when performing various actions that may affect
    * relationships. At any given time, exactly 1 relationship is being
    * processed. This method allows the effect write to obtain that
    * relationship.
    *
    * @return The relationship that is currently being processed, may be {@code
    *    null} if no relationship is available yet. If the current
    *    context is {@code RS_CONSTRUCTION}, then this is a newly created
    *    relationship that has not been persisted. Any changes to this object
    *    will affect what is persisted.
    */
   PSRelationship getCurrentRelationship();

   /**
    * When a relationship request is made, it may result in many relationships
    * being processed (recursion). This method will return the relationships
    * 'back to the top' of the recursion. While processing the topmost
    * relationship, this method returns an empty list.
    *
    * @return Never {@code null}, may be empty. Although each entry is the
    *    actual relationship, any changes will not affect the persisted value.
    *    However, they could have an affect on other effects that call this
    *    method. Therefore, these should be treated as read-only. They are not
    *    cloned to save processing time.
    */
   PSRelationship getOriginatingRelationship();

   /**
    * Contains the set of all relationships that have been processed up to this
    * point in time.
    * 
    * @return Never {@code null}, but may be empty if this information
    * is not tracked. The caller takes ownership of the returned set. Each entry
    * is a {@code PSRelationship}. The caller should not modify the
    * members, treat them as read-only.
    */
   Set<PSRelationship> getProcessedRelationships();

   /**
    * The end point property determines which end of the relationship (owner
    * or dependent) causes the effect to be activated. This is set when the
    * effect is added to the relationship. If the effect was configured for
    * both, this method returns the endpoint that actually activated.
    *
    * @return Exactly one of the RS_ENDPOINT_xxx values.
    */
   int getActivationEndPoint();

   /**
    * The context type used to run effects before persisting a created 
    * relationship instance into the backend repository.
    */
   int RS_PRE_CONSTRUCTION = 1;

   /**
    * @deprecated use {@link #RS_PRE_CONSTRUCTION} instead
    */
   @Deprecated
   int RS_POST_CONSTRUCTION = RS_PRE_CONSTRUCTION;

   /**
    * @deprecated use {@link #RS_PRE_CONSTRUCTION} instead
    */
   @Deprecated
   int RS_CONSTRUCTION = RS_PRE_CONSTRUCTION;

   /**
    * The context type used to run effects before removing a relationship
    * instance from the backend repository.
    */
   int RS_PRE_DESTRUCTION = 2;

   /**
    * @deprecated use {@link #RS_PRE_DESTRUCTION} instead
    */
   @Deprecated
   int RS_POST_DESTRUCTION = RS_PRE_DESTRUCTION;

   /**
    * @deprecated use {@link #RS_PRE_DESTRUCTION} instead
    */
   @Deprecated
   int RS_DESTRUCTION = RS_PRE_DESTRUCTION;

   /**
    * The context type used to run effects before a workflow transition.
    */
   int RS_PRE_WORKFLOW = 3;

   /**
    * The context type used to run effects after a workflow transition.
    */
   int RS_POST_WORKFLOW = 4;

   /**
    * The context type used to run effects before a checkin.
    */
   int RS_PRE_CHECKIN = 5;

   /**
    * @deprecated use {@link #RS_PRE_CHECKIN} instead
    */
   @Deprecated
   int RS_CHECKIN = RS_PRE_CHECKIN;

   /**
    * The context type used to run effects after a checkout.
    */
   int RS_POST_CHECKOUT = 6;

   /**
    * @deprecated use {@link #RS_POST_CHECKOUT} instead
    */
   @Deprecated
   int RS_CHECKOUT = RS_POST_CHECKOUT;

   /**
    * The context type used to run effects before an object update.
    */
   int RS_PRE_UPDATE = 7;

   /**
    * @deprecated use {@link #RS_PRE_UPDATE} instead
    */
   @Deprecated
   int RS_POST_UPDATE = RS_PRE_UPDATE;

   /**
    * @deprecated use {@link #RS_PRE_UPDATE} instead
    */
   @Deprecated
   int RS_UPDATE = RS_PRE_UPDATE;

   /**
    * The context type used to run effects before a clone process.
    */
   int RS_PRE_CLONE = 8;

   /**
    * The minimum validation context type.
    */
   int VALIDATION_MIN = RS_PRE_CONSTRUCTION;

   /**
    * The maximum validation context type.
    */
   int VALIDATION_MAX = RS_PRE_CLONE;

   /**
    * The endpoint value for the owner of the relationship.
    */
   int RS_ENDPOINT_OWNER = 1;

   /**
    * The endpoint value for the dependent of the relationship.
    */
   int RS_ENDPOINT_DEPENDENT = RS_ENDPOINT_OWNER << 1;
}
