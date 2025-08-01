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

package com.percussion.HTTPClient;

import java.util.Objects;

/**
 * This class holds a Name/Value pair of strings. It's used for headers,
 * form-data, attribute-lists, etc. This class is immutable.
 *
 * @version	0.3-3  06/05/2001
 * @author	Ronald Tschalär
 */
@Deprecated
public final class NVPair
{
    /** the name */
    private final String name;

    /** the value */
    private final String value;

    // Constructors

    /**
     * Creates a new name/value pair and initializes it to the
     * specified name and value.
     *
     * @param name  the name, may be {@code null}
     * @param value the value, may be {@code null}
     */
    public NVPair(String name, String value)
    {
        this.name = name;
        this.value = value;
    }

    /**
     * Creates a copy of a given name/value pair.
     *
     * @param p the name/value pair to copy, may not be {@code null}
     * @throws IllegalArgumentException if p is {@code null}
     */
    public NVPair(NVPair p)
    {
        this(Objects.requireNonNull(p, "NVPair cannot be null").name, p.value);
    }

    // Methods

    /**
     * Get the name.
     *
     * @return the name, may be {@code null}
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get the value.
     *
     * @return the value, may be {@code null}
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Compares this object with another for equality.
     *
     * @param obj The object to compare with.
     *
     * @return {@code true} if the objects are equal, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        var other = (NVPair) obj;
        return Objects.equals(name, other.name) &&
               Objects.equals(value, other.value);
    }

    /**
     * Returns the hash code for this object.
     *
     * @return The hash code.
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(name, value);
    }

    /**
     * Produces a string containing the name and value of this instance.
     *
     * @return a string containing the class name and the name and value
     */
    @Override
    public String toString()
    {
        return getClass().getName() + "[name=" + name + ",value=" + value + "]";
    }
}
