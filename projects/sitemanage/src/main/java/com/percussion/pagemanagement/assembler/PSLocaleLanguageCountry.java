/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.pagemanagement.assembler;

/**
 * Represents a locale with language and country codes.
 * Immutable value object.
 */
public class PSLocaleLanguageCountry {

    private String language;
    private String country;

    public PSLocaleLanguageCountry() {
        // Default constructor
    }

    public PSLocaleLanguageCountry(String language, String country) {
        this.language = language;
        this.country = country;
    }

    /**
     * Gets the language code.
     * @return the language code, may be null
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Sets the language code.
     * @param language the language code to set
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Gets the country code.
     * @return the country code, may be null
     */
    public String getCountry() {
        return country;
    }

    /**
     * Sets the country code.
     * @param country the country code to set
     */
    public void setCountry(String country) {
        this.country = country;
    }
}
