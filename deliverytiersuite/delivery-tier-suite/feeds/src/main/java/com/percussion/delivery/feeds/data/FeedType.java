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

package com.percussion.delivery.feeds.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.ws.rs.core.MediaType;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Enumeration of supported feed types with their corresponding media types.
 */
public enum FeedType {
    @JsonProperty("ATOM")
    ATOM(MediaType.APPLICATION_ATOM_XML),

    @JsonProperty("RSS1")
    RSS1("application/rss+xml"),

    @JsonProperty("RSS2")
    RSS2("application/rss+xml");

    private final String mediaType;

    FeedType(String mediaType) {
        this.mediaType = mediaType;
    }

    /**
     * Returns the media type for this feed type.
     *
     * @return the media type string
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * Finds a FeedType by its name, case-insensitive.
     *
     * @param name the name to look up
     * @return Optional containing the FeedType if found, empty otherwise
     */
    public static Optional<FeedType> fromName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Stream.of(values())
            .filter(type -> type.name().equalsIgnoreCase(name))
            .findFirst();
    }

    /**
     * Checks if the given media type is supported by any feed type.
     *
     * @param mediaType the media type to check
     * @return true if supported, false otherwise
     */
    public static boolean isSupported(String mediaType) {
        if (mediaType == null) {
            return false;
        }
        return Stream.of(values())
            .map(FeedType::getMediaType)
            .anyMatch(mt -> mt.equals(mediaType));
    }
}
