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
package com.percussion.error;

import java.util.Objects;

/**
 * Used to return error information about an invalid entry in the rule
 * table model. A string describing the problem and the row/col# of the cell
 * containing the error. This info can be used to highlight/activate the offending
 * cell to aid the user. It is an immutable wrapper around error data.
 */
public final class PSTableCellValidationError {

   /**
    * Constructor
    *
    * @param errorText the text string to be used as the displayed error message
    * @param row the table cell's row index
    * @param col the table cell's column index
    * @throws IllegalArgumentException if row or col is negative
    */
   public PSTableCellValidationError(String errorText, int row, int col) {
      if (row < 0) {
         throw new IllegalArgumentException("Row index cannot be negative: " + row);
      }
      if (col < 0) {
         throw new IllegalArgumentException("Column index cannot be negative: " + col);
      }

      this.errorText = errorText;
      this.errorRow = row;
      this.errorCol = col;
   }

   /**
    * Returns the row index for this error.
    *
    * @return the row index, always >= 0
    */
   public int getErrorRow() {
      return errorRow;
   }

   /**
    * Returns the column index for this error.
    *
    * @return the column index, always >= 0
    */
   public int getErrorCol() {
      return errorCol;
   }

   /**
    * Returns the error text string for this error.
    *
    * @return the error text, may be {@code null} or empty
    */
   public String getErrorText() {
      return errorText;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
         return false;
      }

      var other = (PSTableCellValidationError) obj;
      return errorRow == other.errorRow &&
             errorCol == other.errorCol &&
             Objects.equals(errorText, other.errorText);
   }

   @Override
   public int hashCode() {
      return Objects.hash(errorRow, errorCol, errorText);
   }

   @Override
   public String toString() {
      return String.format("PSTableCellValidationError{row=%d, col=%d, text='%s'}",
                          errorRow, errorCol, errorText);
   }

   /**
    * The table cell's row index for this error.
    * Always >= 0.
    */
   private final int errorRow;

   /**
    * The table cell's column index for this error.
    * Always >= 0.
    */
   private final int errorCol;

   /**
    * The table cell's text message for this error.
    * May be {@code null} or empty.
    */
   private final String errorText;
}
