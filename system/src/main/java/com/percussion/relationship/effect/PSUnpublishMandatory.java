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

package com.percussion.relationship.effect;

import com.percussion.extension.IPSExtensionErrors;
import com.percussion.relationship.PSEffectResult;
import com.percussion.server.IPSRequestContext;
import org.w3c.dom.Element;

/**
 * This effect is to get the following behavior:
 * <p>
 * The current item cannot be transitioned to an unpublish (ContentValid != y
 * or i) state from a public state unless the other end of this relationship is
 * already unpublished. If the forceTransition parameter is yes, and a
 * transition by the supplied name is found or a default transition to an
 * unpublished state is present, the item will be transitioned with this item.
 * If the other item cannot be put into an unpublished state, an exception is
 * thrown and the item being processed is not allowed to transition. The effect
 * will return immediately for any context except RS_PRE_WORKFLOW.
 * <p>This effect takes three parameters as described below:
 * <p>
 * params[0] is a boolean flag (either "yes" or "no" value) that controls
 * whether the item at the other end of the relationship is forced to
 * transition if it is possible. If "no" and the item is already in a public
 * state, the operation will fail.
 * <p>
 * params[1] is the internal name of the transition to use if the owner needs
 * to be transitioned. If not supplied, the first transition with the "default"
 * property (in alpha order) is used.
 * <p>
 * params[2] is the internal name of the transition to use if the dependent
 * needs to be transitioned. If not supplied, the first transition with the
 * "default" property (in alpha order) is used.
 *
 * @author Ram
 * @version 1.0
 */
public class PSUnpublishMandatory extends PSPublishUnpublishMandatory
{
   /**
    * Returns mode name.
    * @return the name of the effect mode: "unpublish",
    * never <code>null</code>.
    */
   protected String getModeName()
   {
      return MODE_UNPUBLISH;
   }
   
   /**
    * Determines if the given WF state is a desired state or not.
    * @param elem element with a WF state, may be <code>null</code>.
    * @return <code>true</code> if the item is in desired WF state
    * <code>false</code> otherwise.
    */
   protected boolean isItemInDesiredWFState(Element elem)
   {
      return elem != null
         && !elem.getAttribute("isPublic").equalsIgnoreCase("y")
         && !elem.getAttribute("isPublic").equalsIgnoreCase("i");
   }

   /**
    * This method is used to determine whether the item is
    * transitioning into a WF state which should trigger
    * the relationship engine to execute attempt on this
    * effect. It is up to the derived class to decide if so.
    *
    * @param isCurrentlyPublic <code>true</code> indicates
    * that the item is in the public state, <code>false</code>
    * otherwise.
    * @param isToPublic <code>true</code> indicates that this
    * item is transitioning into a public state from a non
    * public state, <code>false</code> otherwise.
    * @param isToOutOfPublic <code>true</code> indicates that
    * this item is transitioning out of a public state from
    * a public state, <code>false</code> otherwise.
    * @param result result to set, never <code>null</code>.
    *
    * @return <code>true</code> indicates that the trigger
    * condition has been met, <code>false</code> otherwise.
    */
   protected boolean isTransitioningIntoTriggerState(
      IPSRequestContext request,
      boolean isCurrentlyPublic,
      boolean isToPublic,
      boolean isToOutOfPublic,
      PSEffectResult result)
   {
      if (isCurrentlyPublic && isToOutOfPublic)
      {
         return true;
      }
      else
      {
         String[] args = {m_name, "public", "non-public"};
         //unpublish mode
         result.setWarning(request.getUserLocale(),
            IPSExtensionErrors.INVALID_TRANSITION_FOR_EFFECT, args);
         result.setRecurseDependents(false);
         return false;
      }
   }
   
   
}
