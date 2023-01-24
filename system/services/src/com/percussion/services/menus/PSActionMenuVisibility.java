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

package com.percussion.services.menus;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE,
        region = "RXMENUVISIBILITY")
@Table(name="RXMENUVISIBILITY")
public class PSActionMenuVisibility implements Serializable{

    @Embeddable
    public static class PSActionMenuVisibilityPK implements Serializable {

        @Column(name="ACTIONID")
        protected int actionId;

        @Column(name="VISIBILITYCONTEXT")
        protected int contextid;

        @Column(name="VALUE")
        private String value;

        @Column(name="DESCRIPTION")
        private String description;

        public PSActionMenuVisibilityPK() {}


        public PSActionMenuVisibilityPK(int actionId, int contextid, String value, String description) {
            this.actionId = actionId;
            this.contextid = contextid;
            this.value = value;
            this.description = description;
        }
        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if ( !(other instanceof PSActionMenuVisibilityPK) ) return false;

            final PSActionMenuVisibilityPK pk = (PSActionMenuVisibilityPK) other;

            if ( pk.actionId != actionId &&
                    pk.value.equals(value) &&
                    pk.description.equals(description) &&
                    pk.contextid == contextid) {
                return true;
            }else {
                return false;
            }
        }

        public int hashCode() {
            int result;
            result = value.hashCode() + description.hashCode();
            result = 31 * (result + actionId + contextid);
            return result;
        }

        public int getActionId() {
            return actionId;
        }

        public void setActionId(int actionId) {
            this.actionId = actionId;
        }

        public int getContextid() {
            return contextid;
        }

        public void setContextid(int contextid) {
            this.contextid = contextid;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    @EmbeddedId
    PSActionMenuVisibilityPK primaryKey;

    @MapsId("actionId")
    @ManyToOne
    @JoinColumn(name = "ACTIONID")
    private PSActionMenu menu;

    @MapsId("contextId")
    @ManyToOne
    @JoinColumn(name="VISIBILITYCONTEXT")
    private PSUiContext context;


    public PSActionMenuVisibility(){}

    public PSActionMenuVisibility(int actionId, int contextId, String value, String description){
        this.primaryKey = new PSActionMenuVisibilityPK(actionId,contextId,value,description);
    }

    public PSActionMenu getMenu() {
        return menu;
    }

    public void setMenu(PSActionMenu menu) {
        this.menu = menu;
    }

    public PSUiContext getVisibilityContext() {
        return context;
    }

    public void setVisibilityContext(PSUiContext visibilityContext) {
        this.context = visibilityContext;
    }

    public PSActionMenuVisibilityPK getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(PSActionMenuVisibilityPK primaryKey) {
        this.primaryKey = primaryKey;
    }

    public PSUiContext getContext() {
        return context;
    }

    public void setContext(PSUiContext context) {
        this.context = context;
    }
}
