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

package com.percussion.rest.i18n;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@XmlRootElement(name = "translations")
@Schema(description = "Translation strings response")
public class Translations {

    @JsonProperty("locale")
    @XmlElement
    private String locale;

    @JsonProperty("translations")
    @XmlElement
    private Map<String, String> translations;

    @JsonProperty("count")
    @XmlElement
    private int count;

    public Translations() {
    }

    public Translations(String locale, Map<String, String> translations) {
        this.locale = locale;
        this.translations = translations;
        this.count = translations != null ? translations.size() : 0;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public Map<String, String> getTranslations() {
        return translations;
    }

    public void setTranslations(Map<String, String> translations) {
        this.translations = translations;
        this.count = translations != null ? translations.size() : 0;
    }

    public int getCount() {
        return count;
    }
}
