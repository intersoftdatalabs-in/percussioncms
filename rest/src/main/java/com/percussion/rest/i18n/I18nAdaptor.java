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

import com.percussion.i18n.PSTmxResourceBundle;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class I18nAdaptor implements II18nAdaptor {

    private final PSTmxResourceBundle resourceBundle;

    public I18nAdaptor() {
        this.resourceBundle = PSTmxResourceBundle.getInstance();
    }

    @Override
    public Translations getTranslations(String locale, List<String> prefixes) {
        if (locale == null || locale.isEmpty()) {
            locale = PSTmxResourceBundle.ms_DefaultLanguage;
        }

        Map<String, String> translations = new HashMap<>();
        Iterator<String> keys = resourceBundle.getKeys(locale);

        if (keys != null) {
            while (keys.hasNext()) {
                String key = keys.next();
                if (shouldInclude(key, prefixes)) {
                    String value = resourceBundle.getString(key, locale);
                    translations.put(key, value);
                }
            }
        }

        return new Translations(locale, translations);
    }

    @Override
    public List<String> getAvailableLocales() {
        Set<String> languages = resourceBundle.getLanguages();
        return List.of(languages.toArray(new String[0]));
    }

    private boolean shouldInclude(String key, List<String> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) {
            return true;
        }

        for (String prefix : prefixes) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
