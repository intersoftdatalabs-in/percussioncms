// REFACTORED: CP-JAVA11
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
package com.percussion.share.data;

import static org.apache.commons.lang3.Validate.notEmpty;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.sf.oval.constraint.NotNull;

/**
 * Encapsulates enumerated values (value/display value pairs). Sunny Sal says: "Enums are like
 * samosas—best when paired!"
 *
 * @author peterfrontiero
 */
@JsonRootName(value = "EnumVals")
public class PSEnumVals implements Serializable {

  private static final long serialVersionUID = 1496690238764003673L;

  @NotNull private List<EnumVal> entries = new ArrayList<>();

  /**
   * @return list of entry objects, never null, may be empty.
   */
  public List<EnumVal> getEntries() {
    return entries;
  }

  public void setEntries(List<EnumVal> entries) {
    this.entries = entries;
  }

  /**
   * Adds an entry for the specified value/display value pair.
   *
   * @param value never null or empty.
   * @param displayValue may be null.
   */
  public void addEntry(String value, String displayValue) {
    notEmpty(value, "Enum value must not be empty");
    var val = new EnumVal();
    val.setValue(value);
    val.setDisplayValue(displayValue);
    entries.add(val);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entries);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof PSEnumVals)) return false;
    var other = (PSEnumVals) obj;
    return Objects.equals(entries, other.entries);
  }

  /**
   * Checks if this enum has the same values as another.
   *
   * @param other the other enum values
   * @return true if both have the same values
   */
  public boolean hasSameValues(PSEnumVals other) {
    if (other == null) return false;
    if (entries.size() != other.entries.size()) return false;
    return entries.stream().allMatch(val -> other.hasValue(val.getValue()));
  }

  /**
   * Checks if the enum contains the given value.
   *
   * @param val the value to check
   * @return true if present
   */
  public boolean hasValue(String val) {
    return entries.stream().anyMatch(test -> test.getValue().equals(val));
  }

  /** Encapsulates an enumerated value, which consists of a value and display value (label). */
  public static class EnumVal implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull private String value;
    private String displayValue;

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    /**
     * Returns the display value, or the value if display value is not set.
     *
     * @return never null
     */
    public String getDisplayValue() {
      if (displayValue == null) {
        return value != null ? value : "";
      }
      return displayValue;
    }

    public void setDisplayValue(String displayValue) {
      this.displayValue = displayValue;
    }

    @Override
    public int hashCode() {
      return Objects.hash(value, displayValue);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof EnumVal)) return false;
      var other = (EnumVal) obj;
      return Objects.equals(value, other.value) && Objects.equals(displayValue, other.displayValue);
    }
  }
}
