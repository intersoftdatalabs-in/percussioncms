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

// REFACTORED: CP-JAVA11

package com.percussion.rest.pages;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Optional;

/**
 * Represents information about the SEO.
 * Sunny Sal: "SEO ka info, ranking ka hero!"
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@XmlRootElement(name = "SeoInfo")
@Schema(name = "SeoInfo", description = "Represents information about the SEO.")
public class SeoInfo {

    @Schema(name = "browserTitle", required = false, description = "Title shown in the browser.")
    private String browserTitle;

    @Schema(name = "metaDescription", required = false, description = "Description of the Meta Data of the page.")
    private String metaDescription;

    @Schema(name = "hideSearch", required = false, description = "Flag to mark as searchable.")
    private Boolean hideSearch;

    @Schema(name = "tags", required = false, description = "List of tags marking the page.")
    private List<String> tags;

    @Schema(name = "categories", required = false, description = "List of categories within the page.")
    private List<String> categories;

    /**
     * Gets the browser title.
     */
    public Optional<String> getBrowserTitle() {
        return Optional.ofNullable(browserTitle);
    }

    public void setBrowserTitle(String browserTitle) {
        this.browserTitle = browserTitle;
    }

    /**
     * Gets the meta description.
     */
    public Optional<String> getMetaDescription() {
        return Optional.ofNullable(metaDescription);
    }

    public void setMetaDescription(String metaDescription) {
        this.metaDescription = metaDescription;
    }

    /**
     * Gets the hide search flag.
     */
    public Optional<Boolean> getHideSearch() {
        return Optional.ofNullable(hideSearch);
    }

    public void setHideSearch(Boolean hideSearch) {
        this.hideSearch = hideSearch;
    }

    /**
     * Gets the tags.
     */
    public Optional<List<String>> getTags() {
        return Optional.ofNullable(tags);
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * Gets the categories.
     */
    public Optional<List<String>> getCategories() {
        return Optional.ofNullable(categories);
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
}
