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

package com.percussion.widgets.image.web.impl;

/**
 * Exception thrown when binary upload operations fail.
 * This is a runtime exception that should be used for recoverable conditions
 * related to binary upload processing in the image widget service.
 *
 * @since Java 11
 */
public class PSBinaryUploadException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new binary upload exception with {@code null} as its detail message.
     */
    public PSBinaryUploadException() {
        super();
    }

    /**
     * Constructs a new binary upload exception with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method)
     */
    public PSBinaryUploadException(String message) {
        super(message);
    }

    /**
     * Constructs a new binary upload exception with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method)
     * @param cause the cause (which is saved for later retrieval by the
     *              {@link #getCause()} method). A {@code null} value is
     *              permitted, and indicates that the cause is nonexistent or unknown
     */
    public PSBinaryUploadException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new binary upload exception with the specified cause and a detail
     * message of {@code (cause==null ? null : cause.toString())} (which
     * typically contains the class and detail message of {@code cause}).
     *
     * @param cause the cause (which is saved for later retrieval by the
     *              {@link #getCause()} method). A {@code null} value is
     *              permitted, and indicates that the cause is nonexistent or unknown
     */
    public PSBinaryUploadException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new binary upload exception with the specified detail message,
     * cause, suppression enabled or disabled, and writable stack trace enabled
     * or disabled.
     *
     * @param message the detail message
     * @param cause the cause
     * @param enableSuppression whether or not suppression is enabled or disabled
     * @param writableStackTrace whether or not the stack trace should be writable
     */
    protected PSBinaryUploadException(String message, Throwable cause,
                                    boolean enableSuppression,
                                    boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
