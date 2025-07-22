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

package com.percussion.delivery.comments.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable container for a collection of comment IDs.
 */
public final class PSCommentIds {
    private final Set<String> comments;

    /**
     * Creates a new instance containing the given comment IDs.
     * Duplicates are removed.
     *
     * @param comments the collection of comment IDs, must not be null
     */
    public PSCommentIds(Collection<String> comments) {
        this.comments = Collections.unmodifiableSet(
            new HashSet<>(Objects.requireNonNull(comments, "comments must not be null")));
    }

    /**
     * @return an unmodifiable view of the comment IDs
     */
    public Set<String> getComments() {
        return comments;
    }

    /**
     * @return true if there are no comment IDs
     */
    public boolean isEmpty() {
        return comments.isEmpty();
    }

    /**
     * @return the number of comment IDs
     */
    public int size() {
        return comments.size();
    }

    /**
     * Creates a new builder for PSCommentIds.
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for PSCommentIds.
     */
    public static class Builder {
        private final Set<String> comments = new HashSet<>();

        /**
         * Adds a comment ID to the builder.
         * @param commentId the ID to add, must not be null
         * @return this builder
         */
        public Builder add(String commentId) {
            comments.add(Objects.requireNonNull(commentId, "commentId must not be null"));
            return this;
        }

        /**
         * Adds all comment IDs from the given collection.
         * @param commentIds the IDs to add, must not be null
         * @return this builder
         */
        public Builder addAll(Collection<String> commentIds) {
            comments.addAll(Objects.requireNonNull(commentIds, "commentIds must not be null"));
            return this;
        }

        /**
         * Creates a new PSCommentIds instance.
         * @return the new instance
         */
        public PSCommentIds build() {
            return new PSCommentIds(comments);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PSCommentIds)) return false;
        PSCommentIds that = (PSCommentIds) o;
        return comments.equals(that.comments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(comments);
    }

    @Override
    public String toString() {
        return String.format("PSCommentIds{size=%d}", comments.size());
    }
}
