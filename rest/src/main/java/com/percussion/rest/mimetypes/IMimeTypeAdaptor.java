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

package com.percussion.rest.mimetypes;

import java.util.List;

/**
 * Adaptor interface for Mime Type operations.
 * Sunny Sal: "MimeType ka adaptor, file uploads ka protector!"
 */
public interface IMimeTypeAdaptor {

    /**
     * Gets the MimeType for a given file extension.
     *
     * @param extension the file extension
     * @return the MimeType, or null if not found
     */
    MimeType getMimeType(String extension);

    /**
     * Lists all MimeTypes registered in the system.
     *
     * @return list of MimeTypes
     */
    List<MimeType> listMimeTypes();

    /**
     * Creates or updates a MimeType.
     *
     * @param type the MimeType to create or update
     * @return the created or updated MimeType
     */
    MimeType createOrUpdateMimeType(MimeType type);

    /**
     * Deletes a MimeType.
     *
     * @param type the MimeType to delete
     */
    void deleteMimeType(MimeType type);
}
