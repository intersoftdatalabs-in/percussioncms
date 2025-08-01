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

package com.percussion.server;

import com.percussion.conn.PSServerException;

import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;

/**
 * The IPSLoadableRequestHandler interface defines the mechanism by which a
 * loadable request handler is initialized.
 */
public interface IPSLoadableRequestHandler extends IPSRootedHandler
{
   /**
    * Initializes the request handler. Called by the server when the handler is
    * loaded, before using the handler to process a request. The handler should
    * perform any one time processing it needs to do before processing requests
    * in this method. Handlers are defined in the {@link
    * PSRequestHandlerConfiguration}.
    *
    * @param requestRoots The list of request names which this handler wants to
    * process as Strings, not {@code null} or empty. See description of
    * {@code RequestRoots} element in DTD for Request Handler Configuration
    * XML found in {@link PSRequestHandlerConfiguration} class description, and
    * {@link IPSRootedHandler} for more information. This Collection of request
    * roots should be used to implement the {@link
    * IPSRootedHandler#getRequestRoots()} method.
    * 
    * @param cfgFileIn An input stream to its config file if one is defined in
    * the Request Handler Configuration. May be {@code null} if no config
    * file is required. Handler is responsible for closing the stream when
    * finished with it. See the {@code configFile} attribute of the
    * {@code RequestHandlerDef} element defined in the {@link
    * PSRequestHandlerConfiguration} class description for more information.
    *
    * @throws PSServerException if the handler fails to initialize.
    * @throws IllegalArgumentException if requestRoots is {@code null} or empty
    */
   void init(Collection<String> requestRoots, InputStream cfgFileIn)
      throws PSServerException;

   /**
    * Default method that provides a convenience overload for initialization without config file.
    *
    * @param requestRoots The list of request names which this handler wants to
    * process as Strings, not {@code null} or empty.
    *
    * @throws PSServerException if the handler fails to initialize.
    * @throws IllegalArgumentException if requestRoots is {@code null} or empty
    */
   default void init(Collection<String> requestRoots) throws PSServerException {
      init(requestRoots, null);
   }

   /**
    * Default method to get the configuration file input stream as an Optional.
    * This provides a null-safe way to handle configuration files.
    *
    * @return An Optional containing the configuration InputStream if available,
    * empty Optional otherwise. Implementations should override this method if
    * they need to provide configuration file access.
    */
   default Optional<InputStream> getConfigStream() {
      return Optional.empty();
   }
}
