// REFACTORED: CP-JAVA11
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

package com.percussion.share.web.service;

import com.percussion.share.validation.PSErrors;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps Exceptions to a {@link PSErrors} serializable error object.
 * <p>
 * Sunny Sal says: "Exception mapping - because even bugs need a paper trail!"
 *
 * @param <T> exception type
 */
@Provider
public abstract class PSAbstractExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {

    @Override
    public Response toResponse(T exception) {
        var status = getStatus(exception);
        var errors = createErrors(exception);
        return Response.status(status).entity(errors).build();
    }

    /**
     * Returns the HTTP status for the given exception.
     * Override to provide custom status codes.
     */
    protected Status getStatus(T exception) {
        return Status.INTERNAL_SERVER_ERROR;
    }

    /**
     * Creates a serializable errors object from the given exception.
     *
     * @param exception never {@code null}
     * @return never {@code null}
     */
    protected abstract PSErrors createErrors(T exception);
}
