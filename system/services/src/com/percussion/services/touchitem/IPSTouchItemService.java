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

package com.percussion.services.touchitem;

import com.percussion.design.objectstore.PSRelationship;
import com.percussion.server.IPSRequestContext;
import com.percussion.utils.guid.IPSGuid;

/**
 * Touches Items for relationship changes particularly folder relationships.
 * Item's need to be touched so that incremental publishing works properly.
 * <p>
 * This service is called by Relationship and Workflow Extensions.
 *
 * @author agent
 * @see PSTouchItemConfiguration
 */
public interface IPSTouchItemService {

   /**
    * Touches the items that may be impacted by the
    * {@link PSRelationship#getDependent() dependent item} of the
    * {@code relationship}. This could be an item or folder.
    * Impacted items are based on {@link PSTouchItemConfiguration}.
    * 
    * @param context the request context, not {@code null}.
    * @param relationship the relationship, not {@code null}.
    * @return the number of items that were touched.
    * @see PSTouchItemConfiguration
    */
   int touchItems(IPSRequestContext context, PSRelationship relationship);
   
   /**
    * Touches items that may be impacted by the given item (id).
    * The determination of which items are impacted (and thus touched) are based on 
    * {@link PSTouchItemConfiguration}.
    * 
    * @param id item id can be any item type except folder, never {@code null}.
    * @return the number of items that were touched.
    * @see PSTouchItemConfiguration
    */
   int touchItems(IPSGuid id);
   
   /**
    * Updates site items based on a folder relationship change.
    * This is to make incremental publishing pick up folder changes.
    * 
    * @param requestContext the request context, not {@code null}.
    * @param relationship should be a folder relationship and not {@code null}.
    * @see PSTouchItemConfiguration
    */
   void updateSiteItems(IPSRequestContext requestContext, PSRelationship relationship);
   
   /**
    * Gets the current configuration for the touch item service.
    *
    * @return the configuration, never {@code null}.
    * @see PSTouchItemConfiguration
    */
   PSTouchItemConfiguration getConfiguration();

   /**
    * Sets the configuration for the touch item service.
    *
    * @param config the configuration, never {@code null}.
    * @see #getConfiguration()
    */
   void setConfiguration(PSTouchItemConfiguration config);
}
