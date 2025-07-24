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

package com.percussion.delivery.forms.data;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Version;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang.Validate.notNull;


/**
 * This object represents a form with its fields and data. It does not contain
 * any information about rendering. A form is immutable once constructed.
 * 
 * @author PaulHoward
 */
@Entity
@Table(name = "PSX_FORM_DATA")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "FormData")
public class PSFormData implements IPSFormData {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "PSX_FORM_DATA_ID")
    @TableGenerator(name = "PSX_FORM_DATA_ID", table = "PSX_FORM_DATA_ID_GEN", pkColumnName = "GEN_KEY", valueColumnName = "GEN_VALUE", pkColumnValue = "PSX_FORM_DATA_ID", allocationSize = 1)
    @Column(name = "ID")
    private String id;

    @Basic
    @Column(name = "NAME")
    private String name;

    @Basic
    @Column(name = "CREATE_DATE")
    private Date createDate;

    @Basic
    @Column(name = "EXPORTED")
    private char exported;

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "PSX_FORM_DATA_FIELDS", joinColumns = @JoinColumn(name = "FORM_ID"))
    @MapKeyColumn(name = "FIELD_NAME")
    @Column(name = "FIELD_VALUE")
    private Map<String, String> fields = new HashMap<>();

    public PSFormData() {}
    public PSFormData(String id, String name, Date createDate, char exported, Map<String, String> fields) {
        this.id = id;
        this.name = name;
        this.createDate = createDate;
        this.exported = exported;
        this.fields = fields != null ? fields : new HashMap<>();
    }
    @Override
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    @Override
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    @Override
    public Date getCreateDate() { return createDate; }
    public void setCreateDate(Date createDate) { this.createDate = createDate; }
    @Override
    public char isExported() { return exported; }
    public void setExported(char exported) { this.exported = exported; }
    @Override
    public Set<String> getFieldNames() { return Collections.unmodifiableSet(fields.keySet()); }
    @Override
    public Map<String, String> getFields() { return Collections.unmodifiableMap(fields); }
    public void setFields(Map<String, String> fields) { this.fields = fields; }
    @Override
    public Date getCreated() { return createDate; }
    @Override
    public String toString() {
        return String.format("PSFormData[id=%s, name=%s, createDate=%s, exported=%s, fields=%s]", id, name, createDate, exported, fields);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PSFormData that = (PSFormData) o;
        return exported == that.exported &&
                java.util.Objects.equals(id, that.id) &&
                java.util.Objects.equals(name, that.name) &&
                java.util.Objects.equals(createDate, that.createDate) &&
                java.util.Objects.equals(fields, that.fields);
    }
    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, name, createDate, exported, fields);
    }
}
