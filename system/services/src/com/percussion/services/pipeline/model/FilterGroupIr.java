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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Nested boolean filter node for pipeline selectors.
 *
 * <p>{@code type=GROUP} combines {@code children} with {@code op} AND/OR. {@code type=PREDICATE}
 * is a leaf (same column/operator/value shape as {@link WhereClauseIr}).
 */
public class FilterGroupIr {

  public static final String TYPE_GROUP = "GROUP";
  public static final String TYPE_PREDICATE = "PREDICATE";
  public static final String OP_AND = "AND";
  public static final String OP_OR = "OR";

  /** Maximum nesting depth from the root group (root is depth 1). */
  public static final int MAX_DEPTH = 8;

  /** Maximum nodes (groups + predicates) in one tree. */
  public static final int MAX_NODES = 64;

  private String type = TYPE_GROUP;
  private String op = OP_AND;
  private List<FilterGroupIr> children = new ArrayList<>();

  private String leftKind = WhereClauseIr.KIND_COLUMN;
  private String left;
  private String operator;
  private String rightKind = WhereClauseIr.KIND_LITERAL;
  private String right;
  private boolean omitWhenNull;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type != null ? type.trim() : TYPE_GROUP;
  }

  public String getOp() {
    return op;
  }

  public void setOp(String op) {
    this.op = op;
  }

  public List<FilterGroupIr> getChildren() {
    return children;
  }

  public void setChildren(List<FilterGroupIr> children) {
    this.children = children != null ? children : new ArrayList<>();
  }

  public String getLeftKind() {
    return leftKind;
  }

  public void setLeftKind(String leftKind) {
    this.leftKind = leftKind != null ? leftKind : WhereClauseIr.KIND_COLUMN;
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
    this.rightKind = rightKind != null ? rightKind : WhereClauseIr.KIND_LITERAL;
  }

  public String getRight() {
    return right;
  }

  public void setRight(String right) {
    this.right = right;
  }

  public boolean isOmitWhenNull() {
    return omitWhenNull;
  }

  public void setOmitWhenNull(boolean omitWhenNull) {
    this.omitWhenNull = omitWhenNull;
  }

  public boolean isGroup() {
    return TYPE_GROUP.equalsIgnoreCase(normalizedType());
  }

  public boolean isPredicate() {
    return TYPE_PREDICATE.equalsIgnoreCase(normalizedType());
  }

  public String normalizedType() {
    return type == null || type.isBlank() ? TYPE_GROUP : type.trim().toUpperCase(Locale.ROOT);
  }

  public String normalizedOp() {
    return op == null || op.isBlank() ? OP_AND : op.trim().toUpperCase(Locale.ROOT);
  }

  /** Convert a PREDICATE node to a classic where-clause for the SQL planner. */
  public WhereClauseIr toWhereClause() {
    WhereClauseIr clause = new WhereClauseIr();
    clause.setLeftKind(leftKind);
    clause.setLeft(left);
    clause.setOperator(operator);
    clause.setRightKind(rightKind);
    clause.setRight(right);
    clause.setOmitWhenNull(omitWhenNull);
    clause.setBooleanOp(WhereClauseIr.BOOL_AND);
    return clause;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FilterGroupIr that)) {
      return false;
    }
    return omitWhenNull == that.omitWhenNull
        && Objects.equals(normalizedType(), that.normalizedType())
        && Objects.equals(normalizedOp(), that.normalizedOp())
        && Objects.equals(children, that.children)
        && Objects.equals(leftKind, that.leftKind)
        && Objects.equals(left, that.left)
        && Objects.equals(operator, that.operator)
        && Objects.equals(rightKind, that.rightKind)
        && Objects.equals(right, that.right);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        normalizedType(),
        normalizedOp(),
        children,
        leftKind,
        left,
        operator,
        rightKind,
        right,
        omitWhenNull);
  }
}
