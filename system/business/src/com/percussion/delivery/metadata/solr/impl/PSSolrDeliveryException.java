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

package com.percussion.delivery.metadata.solr.impl;

/**
 * Exception class for handling errors related to Solr delivery in Percussion CMS.
 */
// REFACTORED: CP-JAVA11
public class PSSolrDeliveryException extends RuntimeException {
    /**
     * Constructs a new PSSolrDeliveryException with no detail message.
     */
    public PSSolrDeliveryException() {
        super();
    }

    /**
     * Constructs a new PSSolrDeliveryException with the specified detail message.
     *
     * @param message the detail message
     */
    public PSSolrDeliveryException(String message) {
        super(message);
    }

    /**
     * Constructs a new PSSolrDeliveryException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public PSSolrDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new PSSolrDeliveryException with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public PSSolrDeliveryException(Throwable cause) {
        super(cause);
    }
}
