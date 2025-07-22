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

import com.percussion.delivery.comments.data.IPSDefaultModerationState;
import com.percussion.delivery.comments.data.APPROVAL_STATE;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * JPA entity for storing default moderation state per site.
 * Uses Hibernate second-level cache for improved performance.
 */
@Entity
@Table(name = "PERC_DEFAULT_MODERATION_STATE")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSComments2")
public class PSDefaultModerationState implements IPSDefaultModerationState, Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @NotBlank
    @Column(nullable = false, length = 255)
    private String site;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "defaultState", nullable = false)
    private APPROVAL_STATE defaultState = APPROVAL_STATE.PENDING;

    // Required by JPA
    protected PSDefaultModerationState() {}

    /**
     * Creates a new default moderation state for a site.
     *
     * @param site site name, must not be blank
     * @param state default approval state, must not be null
     * @return new moderation state entity
     * @throws IllegalArgumentException if site is blank or state is null
     */
    public static PSDefaultModerationState create(String site, APPROVAL_STATE state) {
        var moderationState = new PSDefaultModerationState();
        moderationState.setSite(site);
        moderationState.setDefaultState(state);
        return moderationState;
    }

    @Override
    public String getSite() {
        return site;
    }

    @Override
    public void setSite(@NotBlank String site) {
        this.site = Optional.ofNullable(site)
            .filter(s -> !s.isBlank())
            .orElseThrow(() -> new IllegalArgumentException("site must not be blank"));
    }

    @Override
    public APPROVAL_STATE getDefaultState() {
        return defaultState;
    }

    @Override
    public void setDefaultState(@NotNull APPROVAL_STATE state) {
        this.defaultState = Objects.requireNonNull(state, "state must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PSDefaultModerationState)) return false;
        PSDefaultModerationState that = (PSDefaultModerationState) o;
        return Objects.equals(site, that.site);
    }

    @Override
    public int hashCode() {
        return Objects.hash(site);
    }

    @Override
    public String toString() {
        return String.format("DefaultModerationState{site='%s', state=%s}",
            site, defaultState);
    }
}
