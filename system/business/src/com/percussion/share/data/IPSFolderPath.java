// REFACTORED: CP-JAVA11
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
package com.percussion.share.data;

/**
 * Java 11 refactored: Interface for folder path data objects.
 * <p>
 * Implementations must be immutable and thread-safe.
 * <p>
 * <b>Contract:</b> The folder path must never be {@code null} or empty.
 *
 * @author adamgent
 */
public interface IPSFolderPath {
    /**
     * Gets the folder path.
     *
     * @return the folder path, never {@code null} or empty
     */
    String getFolderPath();

    /**
     * Sets the folder path.
     *
     * @param path the folder path, never {@code null} or empty
     */
    void setFolderPath(String path);
}
