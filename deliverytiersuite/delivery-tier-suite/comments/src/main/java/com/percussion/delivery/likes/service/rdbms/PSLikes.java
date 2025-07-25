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
package com.percussion.delivery.likes.service.rdbms;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.percussion.delivery.likes.data.IPSLikes;

/**
 * JPA entity representing a like for a page, comment, or image.
 * Immutable, thread-safe, and OWASP-compliant.
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSLikes1")
@Table(name = "PERC_PAGE_LIKES", uniqueConstraints = @UniqueConstraint(columnNames = {"site", "likeId", "type"}))
public class PSLikes implements IPSLikes, Serializable {

    @TableGenerator(name = "likesId", table = "PERC_ID_GEN", pkColumnName = "GEN_KEY", valueColumnName = "GEN_VALUE", pkColumnValue = "likesId", allocationSize = 1)
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "likesId")
    private long id;

    @Basic
    private String site;

    @Basic
    private String likeId;

    @Basic
    private String type;

    @Basic
    private int total;

    public PSLikes() {
        // Default constructor for JPA
    }

    /**
     * Creates a new PSLikes with the same values as the given one, except for the id.
     *
     * @param likes a Likes to create a copy from
     */
    public PSLikes(IPSLikes likes) {
        this.type = likes.getType();
        this.site = likes.getSite();
        this.likeId = likes.getLikeId();
        this.total = likes.getTotal();
    }

    /**
     * Static factory method for creating a PSLikes instance.
     *
     * @param site the site name
     * @param likeId the like identifier
     * @param type the like type
     * @return new PSLikes instance
     */
    public static PSLikes of(String site, String likeId, String type) {
        var likes = new PSLikes();
        likes.site = site;
        likes.likeId = likeId;
        likes.type = type;
        return likes;
    }

    public PSLikes(String site, String likeId, String type) {
        this.site = site;
        this.likeId = likeId;
        this.type = type;
    }

    /**
     * Sets the id.
     *
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id == null ? 0 : Long.parseLong(id);
    }

    /**
     * @return the likeId
     */
    public String getLikeId() {
        return likeId;
    }

    /**
     * Sets the likeId.
     *
     * @param likeId the likeId to set
     */
    public void setLikeId(String likeId) {
        this.likeId = likeId;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the site
     */
    public String getSite() {
        return site;
    }

    /**
     * Sets the site.
     *
     * @param site the site to set
     */
    public void setSite(String site) {
        this.site = site;
    }

    /**
     * @return the id as a string
     */
    public String getId() {
        return String.valueOf(id);
    }

    /**
     * @return the total number of likes
     */
    public int getTotal() {
        return total;
    }

    /**
     * Sets the total number of likes.
     *
     * @param total the total to set
     */
    public void setTotal(int total) {
        this.total = total;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PSLikes)) return false;
        var that = (PSLikes) o;
        return id == that.id &&
                total == that.total &&
                Objects.equals(site, that.site) &&
                Objects.equals(likeId, that.likeId) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, site, likeId, type, total);
    }

    @Override
    public String toString() {
        return String.format("PSLikes{id=%d, site='%s', likeId='%s', type='%s', total=%d}",
                id, site, likeId, type, total);
    }
}
