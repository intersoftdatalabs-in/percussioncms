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
package com.percussion.delivery.metadata.rdbms.impl;

import com.percussion.delivery.metadata.IPSBlogPostVisit;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import java.util.Optional;

/**
 * Represents a page visit object for blog posts.
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSBlogPostVisit")
@Table(name = "BLOG_POST_VISIT")
public class PSDbBlogPostVisit implements IPSBlogPostVisit, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VISIT_ID")
    private long visitId;

    @Column(length = 2000)
    private String pagepath;

    @Basic
    @Temporal(TemporalType.DATE)
    private Date hitDate;

    @Basic
    private BigInteger hitCount;

    public PSDbBlogPostVisit() {}

    public PSDbBlogPostVisit(String pagepath, Date hitDate, BigInteger hitCount) {
        if (pagepath == null || pagepath.isEmpty())
            throw new IllegalArgumentException("pagepath cannot be null or empty");
        if (hitDate == null)
            throw new IllegalArgumentException("hitDate cannot be null");
        if (hitCount == null)
            throw new IllegalArgumentException("hitCount cannot be null");

        setHitCount(hitCount);
        setHitDate(hitDate);
        setPagepath(pagepath);
    }

    @Override
    public String getPagepath() {
        return pagepath;
    }

    @Override
    public void setPagepath(String path) {
        this.pagepath = path;
    }

    @Override
    public Date getHitDate() {
        return Optional.ofNullable(hitDate)
            .map(Date::getTime)
            .map(Date::new)
            .orElse(null);
    }

    @Override
    public void setHitDate(Date hitDate) {
        this.hitDate = Optional.ofNullable(hitDate)
            .map(Date::getTime)
            .map(Date::new)
            .orElse(null);
    }

    @Override
    public BigInteger getHitCount() {
        return hitCount;
    }

    @Override
    public void setHitCount(BigInteger hitCount) {
        this.hitCount = hitCount;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().getName().equals(obj.getClass().getName()))
            return false;
        var visits = (PSDbBlogPostVisit) obj;
        return new EqualsBuilder()
            .append(hitDate, visits.hitDate)
            .append(hitCount, visits.hitCount)
            .append(pagepath, visits.pagepath)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(hitDate)
            .append(hitCount)
            .append(pagepath)
            .toHashCode();
    }
}
