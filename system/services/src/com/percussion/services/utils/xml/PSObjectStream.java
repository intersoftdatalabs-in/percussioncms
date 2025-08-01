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
package com.percussion.services.utils.xml;

import com.google.common.collect.AbstractIterator;
import com.google.common.io.Closeables;
import com.percussion.util.PSPurgableTempFile;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Modern Java 11 implementation of object streaming with enhanced performance and type safety.
 * This class saves a stream of objects to temporary storage for later iteration, ideal for
 * large query results where returning a list would be problematic and other options would
 * break transaction boundaries.
 * <p>
 * This class implements {@link Iterable}, allowing the {@link #iterator()} method to be
 * called multiple times, making it a drop-in replacement for collection iteration.
 * <p>
 * Key features include:
 * <ul>
 *   <li>Thread-safe operations with read-write locks</li>
 *   <li>Modern Java 11 features like var, Optional, and enhanced exception handling</li>
 *   <li>Automatic resource management with try-with-resources</li>
 *   <li>Stream API integration for functional programming</li>
 * </ul>
 *
 * @author adamgent
 * @param <T> the type of objects in the stream
 */
public abstract class PSObjectStream<T> implements Iterable<T>, Closeable, AutoCloseable {

    private static final Logger ms_log = LogManager.getLogger(PSObjectStream.class);

    /**
     * States of the object stream lifecycle
     */
    public enum State {
        INIT("Initialized"),
        WRITING("Writing"),
        WRITTEN("Written"),
        READING("Reading"),
        CLOSED("Closed"),
        ERROR("Error");

        private final String description;

        State(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private PSPurgableTempFile tempFile;
    private volatile State state = State.INIT;
    private final AtomicLong size = new AtomicLong(0);
    private final ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();
    private final Object streamLock = new Object();

    /**
     * Default constructor initializes the object stream
     */
    protected PSObjectStream() {
        ms_log.debug("Initializing PSObjectStream instance");
    }

    /**
     * Write objects to the stream with enhanced error handling
     *
     * @param objects the collection of objects to write, not {@code null}
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if stream is in invalid state for writing
     */
    public void writeAll(Collection<? extends T> objects) throws IOException {
        Objects.requireNonNull(objects, "Objects collection cannot be null");

        validateStateForWriting();

        synchronized (streamLock) {
            try {
                setState(State.WRITING);

                if (tempFile == null) {
                    tempFile = createTempFile();
                }

                try (var outputStream = createOutputStream()) {
                    var count = 0L;
                    for (var obj : objects) {
                        if (obj != null) {
                            writeObject(obj, outputStream);
                            count++;
                        }
                    }
                    size.set(count);

                    ms_log.debug("Successfully wrote {} objects to stream", count);
                }

                setState(State.WRITTEN);
            } catch (Exception e) {
                setState(State.ERROR);
                ms_log.error("Failed to write objects to stream: {}", e.getMessage(), e);
                throw new IOException("Failed to write objects to stream", e);
            }
        }
    }

    /**
     * Write a single object to the stream
     *
     * @param object the object to write, may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    public void write(T object) throws IOException {
        if (object == null) {
            ms_log.trace("Skipping null object write");
            return;
        }

        writeAll(List.of(object));
    }

    /**
     * Get the current state of the stream
     *
     * @return the current state
     */
    public State getState() {
        stateLock.readLock().lock();
        try {
            return state;
        } finally {
            stateLock.readLock().unlock();
        }
    }

    /**
     * Get the number of objects in the stream
     *
     * @return the count of objects
     */
    public long size() {
        return size.get();
    }

    /**
     * Check if the stream is empty
     *
     * @return {@code true} if stream has no objects, {@code false} otherwise
     */
    public boolean isEmpty() {
        return size.get() == 0;
    }

    /**
     * Create a Stream from this object stream for functional programming
     *
     * @return a Stream of objects
     * @throws IllegalStateException if stream is not ready for reading
     */
    public Stream<T> stream() {
        validateStateForReading();

        var spliterator = Spliterators.spliteratorUnknownSize(
            iterator(),
            Spliterator.ORDERED | Spliterator.NONNULL
        );

        return StreamSupport.stream(spliterator, false);
    }

    /**
     * Process each object in the stream with a consumer function
     *
     * @param consumer the function to apply to each object, not {@code null}
     * @throws IllegalStateException if stream is not ready for reading
     */
    public void forEach(Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "Consumer cannot be null");
        stream().forEach(consumer);
    }

    /**
     * Transform objects in the stream using a mapping function
     *
     * @param <R> the type of the result stream
     * @param mapper the mapping function, not {@code null}
     * @return a new stream with transformed objects
     */
    public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "Mapper function cannot be null");
        return stream().map(mapper);
    }

    /**
     * Filter objects in the stream using a predicate
     *
     * @param predicate the filtering predicate, not {@code null}
     * @return a filtered stream
     */
    public Stream<T> filter(java.util.function.Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "Predicate cannot be null");
        return stream().filter(predicate);
    }

    @Override
    public Iterator<T> iterator() {
        validateStateForReading();

        try {
            setState(State.READING);
            return new ObjectStreamIterator();
        } catch (Exception e) {
            setState(State.ERROR);
            ms_log.error("Failed to create iterator: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create iterator", e);
        }
    }

    @Override
    public void close() throws IOException {
        stateLock.writeLock().lock();
        try {
            if (state == State.CLOSED) {
                return;
            }

            cleanupResources();
            state = State.CLOSED;
            ms_log.debug("PSObjectStream closed successfully");
        } catch (Exception e) {
            state = State.ERROR;
            ms_log.error("Error closing PSObjectStream: {}", e.getMessage(), e);
            throw new IOException("Error closing stream", e);
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    /**
     * Create a temporary file for object storage
     *
     * @return a new temporary file
     * @throws IOException if file creation fails
     */
    protected PSPurgableTempFile createTempFile() throws IOException {
        try {
            var tempFile = new PSPurgableTempFile("obj_stream", ".dat", null);
            ms_log.debug("Created temporary file: {}", tempFile.getFile().getAbsolutePath());
            return tempFile;
        } catch (Exception e) {
            ms_log.error("Failed to create temporary file: {}", e.getMessage(), e);
            throw new IOException("Failed to create temporary file", e);
        }
    }

    /**
     * Create an output stream for writing objects
     *
     * @return a new output stream
     * @throws IOException if stream creation fails
     */
    protected abstract OutputStream createOutputStream() throws IOException;

    /**
     * Create an input stream for reading objects
     *
     * @return a new input stream
     * @throws IOException if stream creation fails
     */
    protected abstract InputStream createInputStream() throws IOException;

    /**
     * Write a single object to the output stream
     *
     * @param object the object to write
     * @param outputStream the output stream
     * @throws IOException if writing fails
     */
    protected abstract void writeObject(T object, OutputStream outputStream) throws IOException;

    /**
     * Read a single object from the input stream
     *
     * @param inputStream the input stream
     * @return the read object, or {@code null} if end of stream
     * @throws IOException if reading fails
     */
    protected abstract T readObject(InputStream inputStream) throws IOException;

    /**
     * Validate that the stream is in a valid state for writing
     *
     * @throws IllegalStateException if state is invalid for writing
     */
    private void validateStateForWriting() {
        var currentState = getState();
        if (currentState != State.INIT && currentState != State.WRITTEN) {
            throw new IllegalStateException(
                String.format("Cannot write in state %s (%s)",
                    currentState, currentState.getDescription()));
        }
    }

    /**
     * Validate that the stream is in a valid state for reading
     *
     * @throws IllegalStateException if state is invalid for reading
     */
    private void validateStateForReading() {
        var currentState = getState();
        if (currentState != State.WRITTEN && currentState != State.READING) {
            throw new IllegalStateException(
                String.format("Cannot read in state %s (%s)",
                    currentState, currentState.getDescription()));
        }
    }

    /**
     * Set the stream state with proper locking
     *
     * @param newState the new state to set
     */
    private void setState(State newState) {
        stateLock.writeLock().lock();
        try {
            var oldState = this.state;
            this.state = newState;
            ms_log.trace("State changed from {} to {}", oldState, newState);
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    /**
     * Clean up all resources including temporary files
     */
    private void cleanupResources() {
        if (tempFile != null) {
            try {
                tempFile.release();
                ms_log.debug("Released temporary file resources");
            } catch (Exception e) {
                ms_log.warn("Failed to release temporary file: {}", e.getMessage());
            }
        }
    }

    /**
     * Get statistics about the object stream
     *
     * @return a map containing stream statistics
     */
    public Map<String, Object> getStatistics() {
        var stats = new HashMap<String, Object>();
        stats.put("state", getState().name());
        stats.put("stateDescription", getState().getDescription());
        stats.put("size", size());
        stats.put("isEmpty", isEmpty());
        stats.put("hasTempFile", tempFile != null);

        if (tempFile != null) {
            try {
                var file = tempFile.getFile();
                stats.put("tempFileSize", Files.size(file.toPath()));
                stats.put("tempFilePath", file.getAbsolutePath());
            } catch (Exception e) {
                stats.put("tempFileError", e.getMessage());
            }
        }

        return Collections.unmodifiableMap(stats);
    }

    /**
     * Iterator implementation for the object stream
     */
    private class ObjectStreamIterator extends AbstractIterator<T> {
        private InputStream inputStream;
        private boolean streamClosed = false;

        public ObjectStreamIterator() {
            try {
                inputStream = createInputStream();
                ms_log.trace("Created object stream iterator");
            } catch (IOException e) {
                ms_log.error("Failed to create input stream for iterator: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to create input stream", e);
            }
        }

        @Override
        protected T computeNext() {
            if (streamClosed) {
                return endOfData();
            }

            try {
                var object = readObject(inputStream);
                if (object == null) {
                    closeInputStream();
                    return endOfData();
                }
                return object;
            } catch (IOException e) {
                closeInputStream();
                ms_log.error("Error reading object from stream: {}", e.getMessage(), e);
                throw new RuntimeException("Error reading object from stream", e);
            }
        }

        private void closeInputStream() {
            if (!streamClosed && inputStream != null) {
                try {
                    inputStream.close();
                    streamClosed = true;
                    ms_log.trace("Closed input stream for iterator");
                } catch (IOException e) {
                    ms_log.warn("Failed to close input stream: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public String toString() {
        return String.format("PSObjectStream[state=%s, size=%d, hasTemp=%s]",
            getState(), size(), tempFile != null);
    }
}
