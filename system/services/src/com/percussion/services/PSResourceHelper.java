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
package com.percussion.services;

import java.io.File;
import java.util.Objects;

/**
 * Allows the initialization servlet to store the resource directory in
 * a place the code can access. This utility class provides thread-safe
 * access to the resource directory configuration.
 *
 * @author dougrand
 */
public final class PSResourceHelper {

    /**
     * Default value used in unit testing
     */
    private static volatile File ms_resourceDir = new File("ear");

    // Private constructor to prevent instantiation
    private PSResourceHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets the current resource directory.
     *
     * @return the resource directory, never {@code null}
     */
    public static File getResourceDir() {
        return ms_resourceDir;
    }

    /**
     * Sets the resource directory path.
     *
     * @param path the new resource directory path, must not be {@code null}
     * @throws IllegalArgumentException if path is {@code null}
     */
    public static synchronized void setResourceDir(File path) {
        ms_resourceDir = Objects.requireNonNull(path, "Resource directory path cannot be null");
    }
}
