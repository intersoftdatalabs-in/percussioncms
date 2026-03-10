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

package com.percussion.data;

import com.percussion.design.objectstore.IPSReplacementValue;
import java.util.Objects;

/**
 * The PSDataExtractor abstract class can be extended by classes wanting to extend the
 * IPSDataExtractor interface. This is not required. At this time, only getSource is implemented in
 * this class.
 *
 * @author Tas Giakouminakis
 * @version 1.0
 * @since 1.0
 */
public abstract class PSDataExtractor implements IPSDataExtractor {
  /**
   * Construct the extractor for the specified source object.
   *
   * @param source the source object for this extractor, may not be {@code null}
   * @throws IllegalArgumentException if source is {@code null}
   */
  protected PSDataExtractor(IPSReplacementValue source) {
    this(new IPSReplacementValue[] {Objects.requireNonNull(source, "source cannot be null")});
  }

  /**
   * Construct the extractor for the specified source objects.
   *
   * @param source the source object(s) for this extractor, may be {@code null}
   */
  protected PSDataExtractor(IPSReplacementValue[] source) {
    super();
    m_sourceReplacementValues = (source == null) ? new IPSReplacementValue[0] : source;
  }

  /**
   * Get the source IPSReplacementValue objects used to create this extractor.
   *
   * @return the source objects, never {@code null}
   */
  public IPSReplacementValue[] getSource() {
    return m_sourceReplacementValues.clone();
  }

  /**
   * Gets the first IPSReplacementValue object used to create this extractor.
   *
   * @return the first IPSReplacementValue object, may be {@code null} if the source array is empty
   */
  public IPSReplacementValue getSingleSource() {
    if (m_sourceReplacementValues.length == 0) {
      return null;
    }

    return m_sourceReplacementValues[0];
  }

  /** The source replacement values, never {@code null} after construction. */
  protected final IPSReplacementValue[] m_sourceReplacementValues;
}
