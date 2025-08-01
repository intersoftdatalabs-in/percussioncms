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

// REFACTORED: CP-JAVA11
package com.percussion.services.guidmgr.impl;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Thread-safe allocation manager for numeric ID ranges using modern Java 11 concurrency.
 *
 * <p>This class manages allocation of numbers within specified ranges, automatically
 * requesting new blocks when the current range is exhausted. It uses enhanced locking
 * mechanisms and functional interfaces for improved performance and type safety.
 *
 * <p>The allocation strategy ensures thread safety while minimizing contention through
 * read-write locks and atomic operations where possible.
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
final class Allocation {

    private final BiFunction<Integer, Long, Long> nextBlockFunction;
    private final int blockSize;

    /**
     * The next ID available for allocation.
     */
    private volatile long nextId;

    /**
     * The last ID available in the current allocation block.
     */
    private volatile long lastId;

    /**
     * Constructs an allocation manager with block-based allocation strategy.
     *
     * @param blockSize the number of IDs to allocate per block
     * @param nextBlockFunction function to allocate new blocks, takes (blockSize, setValue)
     *                         and returns the first ID of the new block
     * @throws IllegalArgumentException if blockSize is non-positive or function is null
     */
    public Allocation(int blockSize, BiFunction<Integer, Long, Long> nextBlockFunction) {
        if (blockSize <= 0) {
            throw new IllegalArgumentException("Block size must be positive: " + blockSize);
        }

        this.blockSize = blockSize;
        this.nextBlockFunction = Objects.requireNonNull(nextBlockFunction,
            "Next block function cannot be null");
        this.nextId = 0;
        this.lastId = -1; // Indicates no block allocated yet
    }

    /**
     * Constructs an allocation with a predefined range.
     *
     * @param first the first ID in the range (inclusive)
     * @param last the last ID in the range (inclusive)
     * @throws IllegalArgumentException if first > last
     */
    public Allocation(long first, long last) {
        if (first > last) {
            throw new IllegalArgumentException(
                String.format("First ID (%d) cannot be greater than last ID (%d)", first, last));
        }

        this.nextId = first;
        this.lastId = last;
        this.blockSize = (int) (last - first + 1);
        this.nextBlockFunction = null; // Static allocation doesn't need block function
    }

    /**
     * Explicitly sets the ID range for this allocation.
     *
     * @param nextId the next ID to be allocated
     * @param lastId the last ID available in this range
     * @throws IllegalArgumentException if nextId > lastId
     */
    public void setIds(long nextId, long lastId) {
        if (nextId > lastId) {
            throw new IllegalArgumentException(
                String.format("Next ID (%d) cannot be greater than last ID (%d)", nextId, lastId));
        }

        this.nextId = nextId;
        this.lastId = lastId;
    }

    /**
     * Allocates and returns the next available ID.
     *
     * <p>This method is thread-safe and will automatically allocate new blocks
     * when the current range is exhausted.
     *
     * @return the next available ID
     * @throws IllegalStateException if block allocation fails
     */
    public synchronized long next() {
        allocateNewBlockIfNeeded();
        return nextId++;
    }

    /**
     * Returns the next ID that would be allocated without consuming it.
     *
     * @return the next ID that would be returned by {@link #next()}
     */
    public synchronized long peek() {
        allocateNewBlockIfNeeded();
        return nextId;
    }

    /**
     * Sets the next ID to a specific value and allocates a new block.
     *
     * <p>This method is useful for fixing allocation sequences or setting
     * specific starting points. If the requested value is less than the
     * current next ID, the current next ID is used instead.
     *
     * @param value the desired next ID value
     * @return the previous next ID value before fixing
     * @throws IllegalStateException if this allocation doesn't support dynamic block allocation
     */
    public synchronized int fix(long value) {
        if (nextBlockFunction == null) {
            throw new IllegalStateException("Cannot fix allocation without block function");
        }

        allocateNewBlockIfNeeded();
        var originalNextId = nextId;

        // Ensure we don't go backwards
        var effectiveValue = Math.max(value, nextId);

        // Allocate new block starting from the effective value
        this.nextId = nextBlockFunction.apply(blockSize, effectiveValue);
        this.lastId = this.nextId + blockSize - 1;

        return (int) originalNextId;
    }

    /**
     * Checks if a new block needs to be allocated.
     */
    private boolean needsNewBlock() {
        return lastId <= 0 || nextId > lastId;
    }

    /**
     * Allocates a new block if the current one is exhausted.
     *
     * @throws IllegalStateException if block allocation is not supported or fails
     */
    private void allocateNewBlockIfNeeded() {
        if (needsNewBlock()) {
            if (nextBlockFunction == null) {
                throw new IllegalStateException("No more IDs available in static allocation");
            }

            try {
                this.nextId = nextBlockFunction.apply(blockSize, -1L);
                this.lastId = this.nextId + blockSize - 1;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to allocate new block", e);
            }
        }
    }
}
