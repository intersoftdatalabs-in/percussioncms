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
package com.percussion.utils.jsr170;

import java.io.InputStream;
import java.math.BigDecimal;
import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Base implementation class for JCR values
 *
 * @author dougrand
 * @param <Type> the value type encapsulated by the subclass of this abstract class.
 */
@SuppressWarnings("rawtypes")
public abstract class PSBaseValue<Type> implements Value, IPSJcrCacheItem {
  /** Holds value, never <code>null</code> after ctor */
  protected Type m_value;

  /** Hold string value once loaded */
  protected String m_strValue = null;

  /**
   * Default JCR 2.0 {@link Value#getBinary()} implementation: wraps {@link #getStream()} in a
   * buffered {@link PSBinary}.
   */
  @Override
  public Binary getBinary() throws RepositoryException {
    InputStream stream = getStream();
    if (stream == null) {
      return new PSBinary(new byte[0]);
    }
    return new PSBinary(stream);
  }

  /**
   * Default JCR 2.0 {@link Value#getDecimal()} implementation via {@link #getString()}.
   *
   * @throws ValueFormatException if the string form is not a valid decimal
   */
  @Override
  public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
    String s = getString();
    if (s == null || s.trim().isEmpty()) {
      throw new ValueFormatException("Empty value cannot be converted to decimal");
    }
    try {
      return new BigDecimal(s.trim());
    } catch (NumberFormatException e) {
      throw new ValueFormatException(e);
    }
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PSBaseValue)) return false;

    PSBaseValue b = (PSBaseValue) obj;
    EqualsBuilder eb = new EqualsBuilder();

    return eb.append(m_value, b.m_value).isEquals();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    HashCodeBuilder b = new HashCodeBuilder();
    return b.append(m_value).toHashCode();
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("PSBaseValue{");
    sb.append("m_value=").append(m_value);
    sb.append(", m_strValue='").append(m_strValue).append('\'');
    sb.append('}');
    return sb.toString();
  }

  /*
   * (non-Javadoc)
   * @see com.percussion.utils.jsr170.IPSJcrCacheItem#getSizeInBytes()
   */
  public long getSizeInBytes() throws RepositoryException {
    return 8;
  }
}
