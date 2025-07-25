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
package com.percussion.generickey.utils.data.rdbms.impl;

import com.percussion.generickey.data.IPSGenericKey;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.*;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * JPA entity for generic key.
 * Sunny Sal: "Keys are like passwords - keep them unique and expiring!"
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSGenericKey")
@Table(name = "PERC_GENERIC_KEY")
public class PSGenericKey implements IPSGenericKey {

    @TableGenerator(
            name = "genericKeyId",
            table = "PERC_ID_GEN",
            pkColumnName = "GEN_KEY",
            valueColumnName = "GEN_VALUE",
            pkColumnValue = "genericKeyId",
            allocationSize = 1)
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "genericKeyId")
    private long id;

    @Basic
    @Column(length = 4000)
    private String genericKey;

    @Basic
    private Date expirationDate;

    public PSGenericKey() {
        // Required by JPA
    }

    /**
     * Creates a new genericKey with the same values as the given one, except for the id.
     *
     * @param genericKey A generic key to create a copy from, not null.
     */
    public PSGenericKey(IPSGenericKey genericKey) {
        Objects.requireNonNull(genericKey, "genericKey may not be null");
        this.expirationDate = genericKey.getExpirationDate();
        this.genericKey = genericKey.getGenericKey();
    }

    @Override
    public String getResetKeyId() {
        return String.valueOf(id);
    }

    @Override
    public void setResetKeyId(String genericKeyId) {
        Objects.requireNonNull(genericKeyId, "genericKeyId may not be null or empty");
        this.id = Long.parseLong(genericKeyId);
    }

    @Override
    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    @Override
    public Date getExpirationDate() {
        return expirationDate;
    }

    public Optional<Date> getExpirationDateOptional() {
        return Optional.ofNullable(expirationDate);
    }

    @Override
    public String getGenericKey() {
        return genericKey;
    }

    @Override
    public void setGenericKey(String genericKey) {
        this.genericKey = genericKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PSGenericKey)) return false;
        PSGenericKey that = (PSGenericKey) o;
        return id == that.id &&
               Objects.equals(genericKey, that.genericKey) &&
               Objects.equals(expirationDate, that.expirationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, genericKey, expirationDate);
    }
}
