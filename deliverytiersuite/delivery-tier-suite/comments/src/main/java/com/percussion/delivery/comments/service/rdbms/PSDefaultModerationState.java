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
package com.percussion.delivery.comments.service.rdbms;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.percussion.delivery.comments.data.IPSDefaultModerationState;


/**
 * Simple entity to store default moderation state for comments service.
 * @author erikserating
 *
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSComments2")
@Table(name = "PERC_DEFAULT_MODERATION_STATE")
public class PSDefaultModerationState implements IPSDefaultModerationState
{
   
   @Id
   private String site;
   
   @Basic
   private String defaultState;
   
   public PSDefaultModerationState()
   {
      // No-arg constructor
   }
   
   public PSDefaultModerationState(String site, String defaultState)
   {
      this.site = site;
      this.defaultState = defaultState;
   }

   public String getSite()
   {
      return site;
   }

   public void setSite(String site)
   {
      this.site = site;
   }

   public String getDefaultState()
   {
      return defaultState;
   }

   public void setDefaultState(String defaultState)
   {
      this.defaultState = defaultState;
   }   
   

}
