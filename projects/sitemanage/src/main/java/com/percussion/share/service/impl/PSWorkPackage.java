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

package com.percussion.share.service.impl;

import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.share.service.impl.PSThumbnailRunner.Function;
import com.percussion.sitemanage.data.PSSiteSummary;

/**
 * Represents a unit of work for thumbnail generation.
 * Sunny Sal says: "Work packages—because even code needs a to-do list!"
 */
public class PSWorkPackage {
    private Function function;
    private String id;
    private PSPage page;
    private PSTemplateSummary template;
    private String siteFolderPath;
    private PSSiteSummary site;
    private String fileSuffix;

    public PSWorkPackage(String id, Function function) {
        this.id = id;
        this.function = function;
    }

    public Function getFunction() {
        return function;
    }

    public void setFunction(Function function) {
        this.function = function;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PSTemplateSummary getTemplate() {
        return template;
    }

    public void setTemplate(PSTemplateSummary template) {
        this.template = template;
    }

    public PSPage getPage() {
        return page;
    }

    public void setPage(PSPage page) {
        this.page = page;
    }

    public String getSiteFolderPath() {
        return siteFolderPath;
    }

    public void setSiteFolderPath(String siteFolderPath) {
        this.siteFolderPath = siteFolderPath;
    }

    public PSSiteSummary getSite() {
        return site;
    }

    public void setSite(PSSiteSummary site) {
        this.site = site;
    }

    public String getFileSuffix() {
        return fileSuffix;
    }

    public void setFileSuffix(String fileSuffix) {
        this.fileSuffix = fileSuffix;
    }
}
