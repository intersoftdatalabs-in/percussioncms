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
package com.percussion.tablefactory;

import java.util.Vector;

/** This class is used to define a column */
public class RxColumns {
  RxColumns() {}

  // mutators
  void setColName(String pcolName) {
    colName = pcolName;
  }

  void setJdbcDataType(String pdataType) {
    dataType = pdataType;
  }

  void setColNo(int pcolNo) {
    colNo = pcolNo;
  }

  void setNativeType(String pnativeType) {
    nativeType = pnativeType;
  }

  void setColLength(int pnativeColLength) {
    nativeColLength = pnativeColLength;
  }

  void setKey(boolean pbKey) {
    bKey = pbKey;
  }

  void setAllowNull(boolean pallowNull) {
    allowNull = pallowNull;
  }

  void setOrdinalPosition(int pOrdinal) {
    ordPosition = pOrdinal;
  }

  void setDecimal(int pDecimal) {
    decimal = pDecimal;
  }

  void setColValue(String pColValue) {
    colValue = pColValue;
  }

  void setColAction(int pColAction) {
    colAction = pColAction;
  }

  // accessors
  String getColName() {
    return colName;
  }

  String getJdbcDataType() {
    return dataType;
  }

  int getColNo() {
    return colNo;
  }

  String getNativeType() {
    return nativeType;
  }

  int getColLength() {
    return nativeColLength;
  }

  boolean getKey() {
    return bKey;
  }

  boolean getAllowNull() {
    return allowNull;
  }

  int getOrdinalPosition() {
    return ordPosition;
  }

  int getDecimal() {
    return decimal;
  }

  String getColValue() {
    return colValue;
  }

  int getColAction() {
    return colAction;
  }

  /**
   * Builds the native column definition fragment for this column.
   *
   * @return the column definition string, never <code>null</code>.
   */
  public String getColumnDef() {
    StringBuilder buf = new StringBuilder();

    buf.append(colName);
    buf.append(" ");
    buf.append(nativeType);
    if (nativeColLength > 0) {
      buf.append("(");
      buf.append(nativeColLength);
      buf.append(")");
    }

    String nullClause = "";
    if (allowNull) nullClause = " NULL";
    else nullClause = " NOT NULL";

    buf.append(nullClause);

    return buf.toString();
  }

  // member variables
  private String colName = new String();
  private String dataType = new String();
  private int colNo = 0;
  private String nativeType = new String();
  private int nativeColLength = 0;
  private boolean bKey = false;
  private boolean allowNull = false;
  private int ordPosition = 0;
  private int decimal = 0;
  private int colAction = 0;
  private String colValue = new String();
  /** The previously existing values of this column. */
  public Vector<String> vtOldValues = new Vector<>();

  /** Column action indicating the column should be added. */
  public static final int ADD_COLUMN = 1;
  /** Column action indicating the column should be modified. */
  public static final int MODIFY_COLUMN = 2;
  /** Column action indicating the column should be deleted. */
  public static final int DELETE_COLUMN = 3;
  /** Column action indicating a user-defined column. */
  public static final int USER_COLUMN = 4;
  /** Column action indicating no action should be taken. */
  public static final int NO_ACTION = 0;
}
