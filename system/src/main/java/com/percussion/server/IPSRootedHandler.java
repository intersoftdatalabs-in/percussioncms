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
package com.percussion.server;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An interface to be implemented by all handlers which have to go into
 * the list of rooted handlers. A rooted handler is one that may handle an
 * HTTP request made against the Rhythmyx server, and is identified as the
 * appropriate handler for those requests based on the request root. The
 * request root is the portion of the URI immediately following the Rhythmyx
 * server root.
 * <p>
 * Thus if a Rhythmyx server is handling any HTTP requests made to
 * "http://myServer:9992/Rhythmyx/...", then in the request
 * "http://myserver:9992/Rhythmyx/foo/bar.html", "foo" is the request root.
 * Request roots may not contain multiple levels, so in the request
 * "http://myserver:9992/Rhythmyx/foo/images/bar.gif", "foo" is the request
 * root.
 */
public interface IPSRootedHandler extends IPSRequestHandler {
  /**
   * Get the name of the rooted handler. Used by the server to identify the
   * handler during initialization and when reporting information about all
   * rooted handlers. All rooted handlers will be served by rhythmyx at
   * runtime.
   *
   * @return the handler name, should be unique across all rooted
   *    handlers, never {@code null} or empty. If {@code null}
   *    or empty the server will ignore this handler. If not unique, the
   *    results will be unpredictable as to which handler will receive the
   *    request for processing.
   */
  String getName();

  /**
   * Get all request roots of the rooted handler. Called by the server when
   * it is initializing the handler.
   *
   * @return an iterator over one or more request roots as Strings. The
   *    iterator must contain at least one entry, and should not contain
   *    duplicates. Never {@code null} or empty. If {@code null} or
   *    empty the server will ignore this handler.
   */
  Iterator<String> getRequestRoots();

  /**
   * Default method to get request roots as a Collection for more convenient access.
   * This provides a more modern alternative to the Iterator-based method.
   *
   * @return a Collection of request roots as Strings, never {@code null} or empty.
   * Default implementation collects from the iterator.
   */
  default Collection<String> getRequestRootsCollection() {
    return Stream.generate(() -> null)
        .takeWhile(x -> getRequestRoots().hasNext())
        .map(x -> getRequestRoots().next())
        .toList();
  }

  /**
   * Default method to get the handler name as an Optional for null-safe access.
   *
   * @return Optional containing the handler name if present and non-empty,
   * empty Optional otherwise.
   */
  default Optional<String> getNameOptional() {
    var name = getName();
    return Optional.ofNullable(name).filter(n -> !n.trim().isEmpty());
  }

  /**
   * Default method to check if this handler can handle a specific request root.
   *
   * @param requestRoot the request root to check, may be {@code null}
   * @return {@code true} if this handler can handle the specified request root,
   * {@code false} otherwise
   */
  default boolean canHandle(String requestRoot) {
    if (requestRoot == null || requestRoot.trim().isEmpty()) {
      return false;
    }

    var iterator = getRequestRoots();
    while (iterator.hasNext()) {
      if (requestRoot.equals(iterator.next())) {
        return true;
      }
    }
    return false;
  }
}
