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
// REFACTORED: CP-JAVA11
package com.percussion.utils.security;

import java.security.acl.Permission;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Java 11 refactored: Simple basic permission object for use with integers or enum ordinal values.
 * <p>
 * Implements {@link Permission} and uses Google Java Style.
 * <p>
 * All fields are immutable and class is thread-safe.
 *
 * @author dougrand
 */
public class PSBasicPermission implements Permission {
    /**
     * The permission value. Immutable.
     */
    private final int m_perm;

    /**
     * Constructs a basic permission with the given value.
     * @param val the permission value
     */
    public PSBasicPermission(int val) {
        m_perm = val;
    }

    /**
     * Checks equality based on permission value.
     * @param obj the object to compare
     * @return true if equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PSBasicPermission)) return false;
        var objb = (PSBasicPermission) obj;
        return new EqualsBuilder().append(m_perm, objb.m_perm).isEquals();
    }

    /**
     * Returns hash code based on permission value.
     * @return hash code
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(m_perm).toHashCode();
    }

    /**
     * Returns the permission value.
     * @return the permission value
     */
    public int getPermission() {
        return m_perm;
    }
}
