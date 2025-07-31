// REFACTORED: CP-JAVA11
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
package com.percussion.membership.data.rdbms.impl;

import com.percussion.membership.data.IPSMembership;
import com.percussion.membership.data.IPSMembership.PSMemberStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * JPA entity for membership accounts.
 * Sunny Sal: "Membership is like a Bollywood club - exclusive, secure, and always fun!"
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSMembership")
@Table(name = "PERC_MEMBERSHIP")
public class PSMembership implements IPSMembership {

    @TableGenerator(
            name = "membershipId",
            table = "PERC_ID_GEN",
            pkColumnName = "GEN_KEY",
            valueColumnName = "GEN_VALUE",
            pkColumnValue = "membershipId",
            allocationSize = 1)
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "membershipId")
    private long id;

    @Basic
    @Column(length = 254, unique = true)
    private String userId;

    @Basic
    @Column(length = 254)
    private String emailAddress;

    @Basic
    @Column(length = 4000)
    private String password;

    @Basic
    private Date lastAccessed;

    @Basic
    @Column(length = 4000)
    private String sessionId;

    @Basic
    @Column(length = 4000)
    private String pwdResetKey;

    @Basic
    private Date createdDate;

    @Basic
    @Column(length = 4000)
    private PSMemberStatus status = PSMemberStatus.UNCONFIRMED;

    @Basic
    @Column(length = 4000)
    private String groups;

    public PSMembership() {
        // Required by JPA
    }

    /**
     * Creates a new membership with the same values as the given one, except for the id.
     *
     * @param membership A membership to create a copy from, not null.
     */
    public PSMembership(IPSMembership membership) {
        Validate.notNull(membership, "membership may not be null");
        this.userId = membership.getUserId();
        this.emailAddress = membership.getEmailAddress().orElse(null);
        this.password = membership.getPassword();
        this.lastAccessed = membership.getLastAccessed().orElse(null);
        this.sessionId = membership.getSessionId().orElse(null);
        this.pwdResetKey = membership.getPwdResetKey().orElse(null);
        this.createdDate = membership.getCreatedDate().orElse(null);
        this.status = membership.getStatus();
        this.groups = membership.getGroups().orElse(null);
    }

    @Override
    public String getId() {
        return String.valueOf(id);
    }

    @Override
    public void setId(String accountId) {
        Validate.notBlank(accountId, "accountId may not be null or empty");
        this.id = Long.parseLong(accountId);
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public void setUserId(String userId) {
        Validate.notBlank(userId, "userId may not be null or empty");
        this.userId = userId;
    }

    @Override
    public Optional<String> getEmailAddress() {
        return Optional.ofNullable(emailAddress).filter(StringUtils::isNotBlank);
    }

    @Override
    public void setEmailAddress(String email) {
        Validate.notBlank(email, "The email may not be null or empty");
        this.emailAddress = email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        Validate.notBlank(password, "The password may not be null or empty");
        this.password = password;
    }

    @Override
    public Optional<Date> getLastAccessed() {
        return Optional.ofNullable(lastAccessed);
    }

    @Override
    public void setLastAccessed(Date lastAccessed) {
        Validate.notNull(lastAccessed, "The last accessed date may not be null");
        this.lastAccessed = lastAccessed;
    }

    @Override
    public Optional<String> getSessionId() {
        return Optional.ofNullable(sessionId).filter(StringUtils::isNotBlank);
    }

    @Override
    public void setSessionId(String sessionId) {
        Validate.notNull(sessionId, "sessionId may not be null");
        this.sessionId = sessionId;
    }

    @Override
    public Optional<String> getPwdResetKey() {
        return Optional.ofNullable(pwdResetKey).filter(StringUtils::isNotBlank);
    }

    @Override
    public void setPwdResetKey(String pwdResetKey) {
        if (pwdResetKey != null) {
            Validate.notBlank(pwdResetKey, "The pwdResetKey may not be empty");
        }
        this.pwdResetKey = pwdResetKey;
    }

    @Override
    public Optional<Date> getCreatedDate() {
        return Optional.ofNullable(createdDate);
    }

    @Override
    public void setCreatedDate(Date createdDate) {
        Validate.notNull(createdDate, "createdDate may not be null");
        this.createdDate = createdDate;
    }

    @Override
    public PSMemberStatus getStatus() {
        return status != null ? status : PSMemberStatus.UNCONFIRMED;
    }

    @Override
    public void setStatus(PSMemberStatus status) {
        this.status = status;
    }

    @Override
    public Optional<String> getGroups() {
        return Optional.ofNullable(groups).filter(StringUtils::isNotBlank);
    }

    @Override
    public void setGroups(String groups) {
        this.groups = groups;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IPSMembership)) {
            return false;
        }
        var other = (IPSMembership) obj;
        return Objects.equals(this.emailAddress, other.getEmailAddress().orElse(null))
                && Objects.equals(this.groups, other.getGroups().orElse(null))
                && Objects.equals(this.password, other.getPassword())
                && Objects.equals(this.pwdResetKey, other.getPwdResetKey().orElse(null))
                && Objects.equals(this.sessionId, other.getSessionId().orElse(null))
                && Objects.equals(this.status, other.getStatus())
                && Objects.equals(this.userId, other.getUserId())
                && compareDates(this.createdDate, other.getCreatedDate().orElse(null))
                && compareDates(this.lastAccessed, other.getLastAccessed().orElse(null));
    }

    private boolean compareDates(Date d1, Date d2) {
        if (d1 == null || d2 == null) {
            return Objects.equals(d1, d2);
        }
        return DateUtils.truncate(d1, Calendar.SECOND).equals(DateUtils.truncate(d2, Calendar.SECOND));
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, password);
    }
}
