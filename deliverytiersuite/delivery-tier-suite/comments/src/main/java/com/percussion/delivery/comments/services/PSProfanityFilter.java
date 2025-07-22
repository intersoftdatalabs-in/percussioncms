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
package com.percussion.delivery.comments.services;

import com.percussion.error.PSExceptionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Thread-safe profanity filter that checks text for forbidden words.
 * Words are loaded from a configuration file in the following order:
 * 1. /conf/perc/profanity.txt
 * 2. /webapps/profanity.txt
 * 3. /profanity.txt (classpath)
 */
public final class PSProfanityFilter {
    private static final Logger log = LogManager.getLogger(PSProfanityFilter.class);

    private static final String PROFANITY_FILE_CONF = "/conf/perc/profanity.txt";
    private static final String PROFANITY_FILE_WEBAPPS = "/webapps/profanity.txt";
    private static final String PROFANITY_FILE_CP = "/profanity.txt";

    private final Set<Pattern> profanityPatterns;

    /**
     * Creates a profanity filter using the default configuration file locations.
     * @throws IllegalStateException if no profanity file could be loaded
     */
    public PSProfanityFilter() {
        this.profanityPatterns = loadDefaultProfanityFile()
            .map(this::compileProfanityPatterns)
            .orElseThrow(() -> new IllegalStateException("No profanity file could be loaded"));
    }

    /**
     * Creates a profanity filter using the specified configuration file.
     * @param profanityFile the file containing profanity words, must not be null
     * @throws IllegalArgumentException if the file cannot be read
     */
    public PSProfanityFilter(Path profanityFile) {
        Objects.requireNonNull(profanityFile, "profanityFile must not be null");
        try {
            this.profanityPatterns = compileProfanityPatterns(profanityFile);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read profanity file: " + profanityFile, e);
        }
    }

    /**
     * Checks if the given text contains any profanity words.
     * @param text the text to check, must not be null
     * @return true if profanity is found, false otherwise
     */
    public boolean containsProfanity(String text) {
        Objects.requireNonNull(text, "text must not be null");
        return profanityPatterns.stream()
            .anyMatch(pattern -> pattern.matcher(text.toLowerCase()).find());
    }

    private Optional<Path> loadDefaultProfanityFile() {
        var catalinaBase = System.getProperty("catalina.base");
        if (catalinaBase != null) {
            var confPath = Paths.get(catalinaBase, PROFANITY_FILE_CONF);
            if (Files.isRegularFile(confPath)) {
                return Optional.of(confPath);
            }
            var webappsPath = Paths.get(catalinaBase, PROFANITY_FILE_WEBAPPS);
            if (Files.isRegularFile(webappsPath)) {
                return Optional.of(webappsPath);
            }
        }
        var cpPath = getClass().getResource(PROFANITY_FILE_CP);
        return Optional.ofNullable(cpPath)
            .map(url -> Paths.get(url.getFile()));
    }

    private Set<Pattern> compileProfanityPatterns(Path file) throws IOException {
        try (Stream<String> lines = Files.lines(file)) {
            return lines
                .filter(StringUtils::isNotBlank)
                .map(String::toLowerCase)
                .map(String::trim)
                .map(word -> Pattern.compile("(?i)\\b" + Pattern.quote(word) + "\\b"))
                .collect(Collectors.toCollection(ConcurrentHashMap::newKeySet));
        }
    }
}
