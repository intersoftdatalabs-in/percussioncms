/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.services.pipeline.model;

import java.util.Objects;

/**
 * One selector WHERE predicate in pipeline IR (classic {@code PSWhereClause} / {@code
 * PSConditional}).
 *
 * <p>Executable planner path supports column left-hand side with relational operators and
 * PARAM/LITERAL/COLUMN right-hand side. Unsupported right kinds or operators are stored for
 * inventory but rejected when planning SQL unless the native-statement escape hatch is used.
 */
public class WhereClauseIr {

  /** Left/right operand is a backend column ({@code alias.col} or {@code col}). */
  public static final String KIND_COLUMN = "COLUMN";

  /** Right (or left) operand is a request/HTML parameter name. */
  public static final String KIND_PARAM = "PARAM";

  /** Literal text/number bound as a JDBC parameter (never concatenated). */
  public static final String KIND_LITERAL = "LITERAL";

  /** Unmapped classic replacement type retained for inventory only. */
  public static final String KIND_OTHER = "OTHER";

  public static final String BOOL_AND = "AND";
  public static final String BOOL_OR = "OR";

  private String leftKind = KIND_OTHER;
  private String left;
  private String operator;
  private String rightKind = KIND_OTHER;
  private String right;
  private String booleanOp = BOOL_AND;
  private boolean omitWhenNull;

  public String getLeftKind() {
    return leftKind;
  }

  public void setLeftKind(String leftKind) {
    this.leftKind = leftKind != null ? leftKind : KIND_OTHER;
  }

  public String getLeft() {
    return left;
  }

  public void setLeft(String left) {
    this.left = left;
  }

  public String getOperator() {
    return operator;
  }

  public void setOperator(String operator) {
    this.operator = operator;
  }

  public String getRightKind() {
    return rightKind;
  }

  public void setRightKind(String rightKind) {
    this.rightKind = rightKind != null ? rightKind : KIND_OTHER;
  }

  public String getRight() {
    return right;
  }

  public void setRight(String right) {
    this.right = right;
  }

  public String getBooleanOp() {
    return booleanOp;
  }

  public void setBooleanOp(String booleanOp) {
    this.booleanOp = booleanOp;
  }

  public boolean isOmitWhenNull() {
    return omitWhenNull;
  }

  public void setOmitWhenNull(boolean omitWhenNull) {
    this.omitWhenNull = omitWhenNull;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof WhereClauseIr that)) {
      return false;
    }
    return omitWhenNull == that.omitWhenNull
        && Objects.equals(leftKind, that.leftKind)
        && Objects.equals(left, that.left)
        && Objects.equals(operator, that.operator)
        && Objects.equals(rightKind, that.rightKind)
        && Objects.equals(right, that.right)
        && Objects.equals(booleanOp, that.booleanOp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(leftKind, left, operator, rightKind, right, booleanOp, omitWhenNull);
  }
}
