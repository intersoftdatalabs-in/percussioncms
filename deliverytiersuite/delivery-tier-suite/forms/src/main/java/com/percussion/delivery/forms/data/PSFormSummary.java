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

/**
 * Class to hold form summary info.
 * 
 * @author leonardohildt
 * 
 */
public class PSFormSummary {
    private String name;
    private Long totalForms;
    private Long exportedForms;

    public PSFormSummary() {}
    public PSFormSummary(String name, Long totalForms, Long exportedForms) {
        this.name = name;
        this.totalForms = totalForms;
        this.exportedForms = exportedForms;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Long getTotalForms() {
        return totalForms;
    }
    public void setTotalForms(Long totalForms) {
        this.totalForms = totalForms;
    }
    public Long getExportedForms() {
        return exportedForms;
    }
    public void setExportedForms(Long exportedForms) {
        this.exportedForms = exportedForms;
    }
    @Override
    public String toString() {
        return String.format("PSFormSummary[name=%s, totalForms=%d, exportedForms=%d]", name, totalForms, exportedForms);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PSFormSummary that = (PSFormSummary) o;
        return java.util.Objects.equals(name, that.name) &&
               java.util.Objects.equals(totalForms, that.totalForms) &&
               java.util.Objects.equals(exportedForms, that.exportedForms);
    }
    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, totalForms, exportedForms);
    }
}
