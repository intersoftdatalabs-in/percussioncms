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

package com.percussion.cms;

import com.percussion.data.PSConditionalEvaluator;
import com.percussion.data.PSExecutionData;
import com.percussion.data.PSInternalRequestCallException;
import com.percussion.design.objectstore.PSSystemValidationException;
import com.percussion.security.PSAuthenticationFailedException;
import com.percussion.security.PSAuthorizationException;
import com.percussion.server.PSServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Each request handled by the modify command handler will be processed by a
 * particular plan.  Each plan consists of a set of steps, which are executed
 * in sequence.
 */
public class PSModifyPlan
{
   /**
    * Constructs an empty plan.
    *
    * @param type One of the plan types, specified by:
    * <ul>
    * <li>{@link #TYPE_INSERT_PLAN}</li>
    * <li>{@link #TYPE_UPDATE_PLAN}</li>
    * <li>{@link #TYPE_UPDATE_NO_BIN_PLAN}</li>
    * <li>{@link #TYPE_UPDATE_SEQUENCE}</li>
    * <li>{@link #TYPE_DELETE_COMPLEX_CHILD}</li>
    * </ul>
    * It is not enforced that type is one of these values.
    */
   public PSModifyPlan(int type)
   {
      m_type = type;
   }

   /**
    * Returns the type of this plan.  See {@link #PSModifyPlan(int)} for plan
    * types.
    *
    * @return the type.
    */
   public int getType()
   {
      return m_type;
   }

   /**
    * Adds a step to this plan.
    *
    * @param step The step to add, may not be <code>null</code>.
    *
    * @throws IllegalArgumentException if step is <code>null</code>.
    */
   public void addModifyStep(IPSModifyStep step)
   {
      if (step == null)
         throw new IllegalArgumentException("step may not be null");

      m_steps.add(step);
   }

   final Object stepLock = new Object();

   /**
    * Executes each step of the plan
    *
    * @param data The execution data.  May not be <code>null</code>.
    * @param appName The name of the app this step will execute against.  May
    * not be <code>null</code>.
    *
    * @return The number of steps executed.
    *
    * @throws PSAuthorizationException if the user is not authorized to
    * perform a step.
    * @throws PSAuthenticationFailedException if the user cannot be
    * authenticated.
    * @throws PSSystemValidationException if the step does any validation and the
    * validation fails.
    * @throws PSInternalRequestCallException if there are any other errors.
    */
   public int execute(PSExecutionData data, String appName)
      throws PSInternalRequestCallException, PSAuthorizationException,
      PSAuthenticationFailedException, PSSystemValidationException
   {
      if (data == null || appName == null)
         throw new IllegalArgumentException(
            "data and appName must be supplied");

      int stepCount = 0;
      for (IPSModifyStep step : m_steps) {
         // need to synchronize on this in case another thread is checking
         synchronized (stepLock) {
            if (step.getHandler() == null)
               step.setHandler(PSServer.getInternalRequestHandler(appName +
                       "/" + step.getName()));
         }
         step.execute(data);
         stepCount++;
      }

      return stepCount;
   }

   /**
    * Returns an Iterator over <code>zero</code> or more PSModifyStep objects,
    * which is all the steps that have been added to this plan.
    *
    * @return The Iterator, never <code>null</code>, may be empty.
    */
   public Iterator getAllSteps()
   {
      return m_steps.iterator();
   }

   /**
    * Appends all modify steps found in the supplied plan to this plan.
    *
    * @param modifyPlan The plan whose steps are to be appended to this plan.
    * May not be <code>null</code>.
    *
    * @throws IllegalArgumentException if modifyPlan is <code>null</code>.
    */
   public void addAllSteps(PSModifyPlan modifyPlan)
   {
      Iterator steps = modifyPlan.getAllSteps();
      while (steps.hasNext())
      {
         IPSModifyStep step = (IPSModifyStep)steps.next();
         addModifyStep(step);
      }
   }
   
   /**
    * Sets a map of binary field names that may be processed by this plan.  This 
    * method takes ownership of the supplied map and changes should not be made 
    * to the map after this call returns.
    * 
    * @param binFields A Map of binary field names possibly updated by this 
    * plan, never <code>null</code>, may be empty.  Key is the field name as a
    * <code>String</code>, value is a <code>List</code> of 
    * <code>PSConditionalEvaluator</code> objects used to determine if the field 
    * is modified. If any of the evaluators return <code>true</code>, the
    * field should be considered modified.  If the list of evaluators is 
    * <code>null</code>, then the field should always be considered to have
    * been modified.
    */
   public void setBinaryFields(Map<String, List<PSConditionalEvaluator>> binFields)
   {
      if (binFields == null)
         throw new IllegalArgumentException("binFields may not be null");

      m_binFields = binFields;      
   }
   
   /**
    * Get a Map of binary field names processed by this plan.  See 
    * {@link #setBinaryFields(Map)} for more info.
    * 
    * @return The map of field names and evaluators, never <code>null</code>, 
    * may be empty.  The returned map should be treated read-only as it is the 
    * copy owned by this class.
    */
   public Map<String, List<PSConditionalEvaluator>> getBinaryFields()
   {
      return m_binFields;
   }
   
   /**
    * Determines if the specified plan type may update item data.
    * 
    * @param planType The plan type to check, should be one of the 
    * <code>TYPE_xxx</code> constants.
    * 
    * @return <code>true</code> if the type would modify the item's field data, 
    * <code>false</code> otherwise. 
    */
   public static boolean updatesItemData(int planType)
   {
      boolean updatesData = false;
      switch (planType)
      {
         case TYPE_INSERT_PLAN :
         case TYPE_UPDATE_PLAN :
         case TYPE_UPDATE_NO_BIN_PLAN :
            updatesData = true;
            break;

         default :
            break;
      }
      
      return updatesData;
   }

   /**
    * Constant specifying a plan used to process an insert.
    */
   public static final int TYPE_INSERT_PLAN = 0;

   /**
    * Constant specifying a plan used to process a full update.
    */
   public static final int TYPE_UPDATE_PLAN = 1;

   /**
    * Constant specifying a plan used to process a update that excludes any
    * binary fields.
    */
   public static final int TYPE_UPDATE_NO_BIN_PLAN = 2;

   /**
    * Constant specifying a plan used to process an update of a complex child's
    * sequences.
    */
   public static final int TYPE_UPDATE_SEQUENCE = 3;

   /**
    * Constant specifying a plan used to process a delete of a complex child
    * row.
    */
   public static final int TYPE_DELETE_COMPLEX_CHILD = 4;

   /**
    * Constant specifying a plan used to process a delete of an entire item.
    */
   public static final int TYPE_DELETE_ITEM = 5;

   /**
    * The type of this plan.  Set in the constructor.
    */
   private int m_type;

   /**
    * The list of steps, never <code>null</code>, may be empty.  Steps are added
    * by a call to {@link #addModifyStep(IPSModifyStep) addModifyStep()}
    */
   private ArrayList<IPSModifyStep> m_steps = new ArrayList<>();

   /**
    * A Map of binary field names possibly updated by this 
    * plan, never <code>null</code>, may be empty.  Modified by calls to 
    * {@link #setBinaryFields(Map)}, see that method for more info.
    */
   private Map<String, List<PSConditionalEvaluator>> m_binFields = new HashMap<>();

}
