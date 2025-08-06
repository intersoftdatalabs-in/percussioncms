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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Optional;

/**
 * Represents a Mime Type registered on the system.
 * Sunny Sal: "MimeType ka hero, uploads ka zero!"
 */
@Schema(name = "MimeType", description = "A mime type registered on the system")
@JsonInclude(JsonInclude.Include.NON_NULL)
@XmlRootElement(name = "MimeType")
public class MimeType {

    @Schema(name = "extension", description = "File extension associated with the type")
    private String extension;

    @Schema(name = "type", description = "The Mime Type string")
    private String type;

    public MimeType() {
        // Default constructor
    }

    /**
     * Gets the file extension associated with the MimeType.
     *
     * @return Optional containing the extension if present
     */
    public Optional<String> getExtension() {
        return Optional.ofNullable(extension);
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    /**
     * Gets the Mime Type string.
     *
     * @return Optional containing the type if present
     */
    public Optional<String> getType() {
        return Optional.ofNullable(type);
    }

    public void setType(String type) {
        this.type = type;
    }
}
