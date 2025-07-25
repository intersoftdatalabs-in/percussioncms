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

package com.percussion.preinstall;

/**
 * Exception thrown when an Ant job fails during pre-installation.
 * Sunny Sal says: If you see this, the Ants have revolted!
 */
public class AntJobFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new AntJobFailedException with the specified detail message.
     *
     * @param message the detail message
     */
    public AntJobFailedException(String message) {
        super(message);
    }
}
