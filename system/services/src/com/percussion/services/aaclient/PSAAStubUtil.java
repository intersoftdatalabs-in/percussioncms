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
// REFACTORED: CP-JAVA11
package com.percussion.services.aaclient;

import com.percussion.server.PSServer;
import com.percussion.util.PSStringTemplate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Utility class for retrieving and caching Active Assembly template files.
 * The retrieved template strings are cached for performance, and the cache can be
 * reset by calling the {@link #reset()} method. This is particularly useful for
 * debugging when the assembly servlet calls reset (e.g., with sys_reinit=true parameter).
 *
 * <p>This class is thread-safe and uses concurrent caching for optimal performance.</p>
 *
 * @author Percussion Software
 */
public final class PSAAStubUtil {

    private static final Logger log = LogManager.getLogger(PSAAStubUtil.class);

    /** Base path for HTML template files */
    private static final String HTMLBASE_PATH = "sys_resources" + File.separator + "html" + File.separator;

    /** Template file paths */
    private static final String HEADER_FILE_PATH = HTMLBASE_PATH + "sys_aaPageHeader.html";
    private static final String ACTIONBAR_FILE_PATH = HTMLBASE_PATH + "sys_aaPageActionBar.html";
    private static final String PAGEFOOTER_FILE_PATH = HTMLBASE_PATH + "sys_aaPageFooter.html";
    private static final String AB_FILE_PAGE_ACTIONS_PATH = HTMLBASE_PATH + "sys_aaPageActions.html";
    private static final String AB_FILE_SLOT_ACTIONS_PATH = HTMLBASE_PATH + "sys_aaSlotActions.html";
    private static final String AB_FILE_SNIPPET_ACTIONS_PATH = HTMLBASE_PATH + "sys_aaSnippetActions.html";

    /** Thread-safe cache for template contents */
    private static final ConcurrentHashMap<String, Object> templateCache = new ConcurrentHashMap<>();

    /** Read-write lock for cache operations */
    private static final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    // Private constructor to prevent instantiation
    private PSAAStubUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets the Active Assembly page header template.
     *
     * @return the page header template, never null
     */
    public static PSStringTemplate getAaPageHeader() {
        return getTemplate(HEADER_FILE_PATH, PSStringTemplate.class);
    }

    /**
     * Gets the Active Assembly page action bar template.
     *
     * @return the action bar template, never null
     */
    public static PSStringTemplate getAaPageActionBar() {
        return getTemplate(ACTIONBAR_FILE_PATH, PSStringTemplate.class);
    }

    /**
     * Gets the page actions template.
     *
     * @return the page actions template, never null
     */
    public static PSStringTemplate getPageActions() {
        return getTemplate(AB_FILE_PAGE_ACTIONS_PATH, PSStringTemplate.class);
    }

    /**
     * Gets the slot actions template.
     *
     * @return the slot actions template, never null
     */
    public static PSStringTemplate getSlotActions() {
        return getTemplate(AB_FILE_SLOT_ACTIONS_PATH, PSStringTemplate.class);
    }

    /**
     * Gets the snippet actions template.
     *
     * @return the snippet actions template, never null
     */
    public static PSStringTemplate getSnippetActions() {
        return getTemplate(AB_FILE_SNIPPET_ACTIONS_PATH, PSStringTemplate.class);
    }

    /**
     * Gets the Active Assembly page footer content.
     *
     * @return the page footer content, never null
     */
    public static String getAaPageFooter() {
        return getTemplate(PAGEFOOTER_FILE_PATH, String.class);
    }

    /**
     * Generic method to get cached templates with type safety.
     *
     * @param filePath the path to the template file
     * @param returnType the expected return type
     * @param <T> the type parameter
     * @return the cached template of the specified type
     */

    private static <T> T getTemplate(String filePath, Class<T> returnType) {
        cacheLock.readLock().lock();
        try {
            var cachedTemplate = templateCache.get(filePath);
            if (cachedTemplate != null) {
                return (T) cachedTemplate;
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        // Template not in cache, load it
        cacheLock.writeLock().lock();
        try {
            // Double-check pattern for thread safety
            var cachedTemplate = templateCache.get(filePath);
            if (cachedTemplate != null) {
                return (T) cachedTemplate;
            }

            // Load and cache the template
            var content = readFileContent(filePath);
            Object template;

            if (returnType == PSStringTemplate.class) {
                template = new PSStringTemplate(content);
            } else {
                template = content;
            }

            templateCache.put(filePath, template);
            return (T) template;
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Resets the template cache, forcing all templates to be reloaded on next access.
     * This method is thread-safe and useful for debugging purposes.
     */
    public static void reset() {
        cacheLock.writeLock().lock();
        try {
            templateCache.clear();
            log.info("Template cache cleared - {} templates removed", templateCache.size());
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Reads the content of a template file using modern NIO.2 APIs.
     *
     * @param fileName the relative file path from the Rhythmyx root directory
     * @return the file content as a string, or empty string if file cannot be read
     */
    private static String readFileContent(String fileName) {
        try {
            var rxDir = PSServer.getRxDir();
            if (rxDir == null) {
                log.error("Rhythmyx root directory is not set - cannot load template: {}", fileName);
                return "";
            }

            var filePath = Paths.get(rxDir.getAbsolutePath(), fileName);

            if (!Files.exists(filePath)) {
                log.error("Template file does not exist: {}", filePath);
                return "";
            }

            if (!Files.isReadable(filePath)) {
                log.error("Template file is not readable: {}", filePath);
                return "";
            }

            var content = Files.readString(filePath, StandardCharsets.UTF_8);
            log.debug("Successfully loaded template file: {} ({} characters)", fileName, content.length());
            return content;

        } catch (IOException e) {
            log.error("Failed to read template file '{}': {}", fileName, e.getMessage(), e);
            log.error("Active Assembly functionality may be impaired");
            return "";
        } catch (Exception e) {
            log.error("Unexpected error reading template file '{}': {}", fileName, e.getMessage(), e);
            return "";
        }
    }

    /**
     * Gets the current cache size for monitoring purposes.
     *
     * @return the number of cached templates
     */
    public static int getCacheSize() {
        cacheLock.readLock().lock();
        try {
            return templateCache.size();
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * Checks if a specific template is cached.
     *
     * @param filePath the template file path to check
     * @return true if the template is cached, false otherwise
     */
    public static boolean isCached(String filePath) {
        cacheLock.readLock().lock();
        try {
            return templateCache.containsKey(filePath);
        } finally {
            cacheLock.readLock().unlock();
        }
    }
}
