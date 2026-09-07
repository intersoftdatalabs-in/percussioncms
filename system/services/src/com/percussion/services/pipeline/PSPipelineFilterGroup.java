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

package com.percussion.services.pipeline;

import com.percussion.services.pipeline.model.FilterGroupIr;
import com.percussion.services.pipeline.model.WhereClauseIr;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validate and evaluate nested selector filter groups (AND/OR trees of predicates).
 *
 * <p>SQL execute compiles the same tree to parenthesized WHERE; HTTP execute filters mapped
 * document rows in memory so Test invoke against the bundled fixture honors the nested
 * predicate.
 */
public final class PSPipelineFilterGroup {

  private PSPipelineFilterGroup() {}

  public static boolean isPresent(FilterGroupIr group) {
    return group != null;
  }

  /**
   * Fail-closed structural validation for persist and execute.
   *
   * @param group never {@code null}
   */
  public static void validate(FilterGroupIr group) throws PSPipelineIrException {
    Objects.requireNonNull(group, "group");
    int[] nodes = {0};
    validateNode(group, 1, nodes, true);
  }

  private static void validateNode(FilterGroupIr node, int depth, int[] nodes, boolean root)
      throws PSPipelineIrException {
    if (node == null) {
      throw new PSPipelineIrException("Filter group contains a null node");
    }
    nodes[0]++;
    if (nodes[0] > FilterGroupIr.MAX_NODES) {
      throw new PSPipelineIrException(
          "Filter group exceeds " + FilterGroupIr.MAX_NODES + " nodes");
    }
    if (depth > FilterGroupIr.MAX_DEPTH) {
      throw new PSPipelineIrException(
          "Filter group exceeds max nesting depth " + FilterGroupIr.MAX_DEPTH);
    }
    if (root && !node.isGroup()) {
      throw new PSPipelineIrException("Filter group root must be type GROUP");
    }
    if (node.isGroup()) {
      String op = node.normalizedOp();
      if (!FilterGroupIr.OP_AND.equals(op) && !FilterGroupIr.OP_OR.equals(op)) {
        throw new PSPipelineIrException("Filter group op must be AND or OR (got " + op + ")");
      }
      List<FilterGroupIr> children = node.getChildren();
      if (children == null || children.isEmpty()) {
        throw new PSPipelineIrException("Filter group must have at least one child");
      }
      for (FilterGroupIr child : children) {
        validateNode(child, depth + 1, nodes, false);
      }
      return;
    }
    if (!node.isPredicate()) {
      throw new PSPipelineIrException(
          "Filter node type must be GROUP or PREDICATE (got " + node.getType() + ")");
    }
    if (!WhereClauseIr.KIND_COLUMN.equalsIgnoreCase(
        node.getLeftKind() != null ? node.getLeftKind().trim() : "")) {
      throw new PSPipelineIrException("Filter predicate leftKind must be COLUMN");
    }
    if (node.getLeft() == null || node.getLeft().isBlank()) {
      throw new PSPipelineIrException("Filter predicate column is required");
    }
    if (node.getOperator() == null || node.getOperator().isBlank()) {
      throw new PSPipelineIrException("Filter predicate operator is required");
    }
    String rightKind =
        node.getRightKind() != null ? node.getRightKind().trim().toUpperCase(Locale.ROOT) : "";
    if (!WhereClauseIr.KIND_LITERAL.equals(rightKind)
        && !WhereClauseIr.KIND_PARAM.equals(rightKind)
        && !WhereClauseIr.KIND_COLUMN.equals(rightKind)) {
      throw new PSPipelineIrException(
          "Filter predicate rightKind must be LITERAL, PARAM, or COLUMN");
    }
    String op = node.getOperator().trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    boolean unary = "IS NULL".equals(op) || "IS NOT NULL".equals(op);
    if (!unary && (node.getRight() == null || node.getRight().isBlank()) && !node.isOmitWhenNull()) {
      throw new PSPipelineIrException("Filter predicate value is required");
    }
  }

  /**
   * Keep rows that match the nested group. Empty result is a real filter outcome (not invented
   * rows).
   */
  public static List<Map<String, Object>> filterRows(
      FilterGroupIr group, List<Map<String, Object>> rows, Map<String, Object> params)
      throws PSPipelineIrException {
    if (group == null) {
      return rows != null ? rows : List.of();
    }
    validate(group);
    if (rows == null || rows.isEmpty()) {
      return List.of();
    }
    Map<String, Object> safeParams = params != null ? params : Map.of();
    List<Map<String, Object>> out = new ArrayList<>();
    for (Map<String, Object> row : rows) {
      if (matches(group, row, safeParams)) {
        out.add(row);
      }
    }
    return out;
  }

  static boolean matches(FilterGroupIr node, Map<String, Object> row, Map<String, Object> params)
      throws PSPipelineIrException {
    if (node == null) {
      return true;
    }
    if (node.isGroup()) {
      List<FilterGroupIr> children = node.getChildren();
      if (children == null || children.isEmpty()) {
        return true;
      }
      boolean and = FilterGroupIr.OP_AND.equals(node.normalizedOp());
      Boolean acc = null;
      for (FilterGroupIr child : children) {
        if (child == null) {
          continue;
        }
        Boolean childMatch = matchOrSkip(child, row, params);
        if (childMatch == null) {
          continue;
        }
        if (acc == null) {
          acc = childMatch;
        } else if (and) {
          acc = acc && childMatch;
        } else {
          acc = acc || childMatch;
        }
      }
      return acc == null || acc;
    }
    Boolean pred = matchPredicate(node, row, params);
    return pred == null || pred;
  }

  private static Boolean matchOrSkip(
      FilterGroupIr node, Map<String, Object> row, Map<String, Object> params)
      throws PSPipelineIrException {
    if (node.isGroup()) {
      return matches(node, row, params);
    }
    return matchPredicate(node, row, params);
  }

  /**
   * @return {@code null} when omitWhenNull skipped the predicate
   */
  private static Boolean matchPredicate(
      FilterGroupIr node, Map<String, Object> row, Map<String, Object> params)
      throws PSPipelineIrException {
    String opRaw = node.getOperator();
    String op = normalizeOperator(opRaw);
    Object leftVal = lookup(row, node.getLeft());
    if ("IS NULL".equals(op)) {
      return leftVal == null;
    }
    if ("IS NOT NULL".equals(op)) {
      return leftVal != null;
    }
    Object rightVal;
    String rightKind =
        node.getRightKind() != null
            ? node.getRightKind().trim().toUpperCase(Locale.ROOT)
            : WhereClauseIr.KIND_LITERAL;
    if (WhereClauseIr.KIND_COLUMN.equals(rightKind)) {
      rightVal = lookup(row, node.getRight());
    } else if (WhereClauseIr.KIND_PARAM.equals(rightKind)) {
      String name = node.getRight();
      boolean present = containsKeyIgnoreCase(params, name);
      rightVal = present ? findParamIgnoreCase(params, name) : null;
      if (node.isOmitWhenNull() && (!present || rightVal == null)) {
        return null;
      }
      if (!present) {
        throw new PSPipelineIrException(
            "Missing request param for filter group placeholder: " + name);
      }
    } else if (WhereClauseIr.KIND_LITERAL.equals(rightKind)) {
      rightVal = node.getRight();
      if (node.isOmitWhenNull() && (rightVal == null || String.valueOf(rightVal).isBlank())) {
        return null;
      }
    } else {
      throw new PSPipelineIrException("Filter predicate rightKind not executable: " + rightKind);
    }
    return compare(leftVal, op, rightVal);
  }

  private static boolean compare(Object left, String op, Object right) throws PSPipelineIrException {
    if ("LIKE".equals(op) || "NOT LIKE".equals(op)) {
      boolean like = like(stringify(left), stringify(right));
      return "LIKE".equals(op) == like;
    }
    int cmp = compareValues(left, right);
    return switch (op) {
      case "=" -> cmp == 0;
      case "<>", "!=" -> cmp != 0;
      case "<" -> cmp < 0;
      case "<=" -> cmp <= 0;
      case ">" -> cmp > 0;
      case ">=" -> cmp >= 0;
      default -> throw new PSPipelineIrException("Unsupported filter operator: " + op);
    };
  }

  private static int compareValues(Object left, Object right) {
    if (left == null && right == null) {
      return 0;
    }
    if (left == null) {
      return -1;
    }
    if (right == null) {
      return 1;
    }
    BigDecimal ln = toNumber(left);
    BigDecimal rn = toNumber(right);
    if (ln != null && rn != null) {
      return ln.compareTo(rn);
    }
    return stringify(left).compareTo(stringify(right));
  }

  private static BigDecimal toNumber(Object value) {
    if (value instanceof Number n) {
      return new BigDecimal(n.toString());
    }
    if (value == null) {
      return null;
    }
    String s = stringify(value);
    if (s.isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(s);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static boolean like(String value, String pattern) {
    if (pattern == null) {
      return false;
    }
    String v = value != null ? value : "";
    StringBuilder regex = new StringBuilder();
    regex.append('^');
    for (int i = 0; i < pattern.length(); i++) {
      char c = pattern.charAt(i);
      if (c == '%') {
        regex.append(".*");
      } else if (c == '_') {
        regex.append('.');
      } else {
        regex.append(Pattern.quote(String.valueOf(c)));
      }
    }
    regex.append('$');
    return Pattern.compile(regex.toString(), Pattern.DOTALL).matcher(v).matches();
  }

  private static String stringify(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  private static Object lookup(Map<String, Object> row, String field) {
    if (row == null || field == null || field.isBlank()) {
      return null;
    }
    String key = field.trim();
    if (row.containsKey(key)) {
      return row.get(key);
    }
    for (Map.Entry<String, Object> e : row.entrySet()) {
      if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
        return e.getValue();
      }
    }
    int dot = key.lastIndexOf('.');
    if (dot >= 0 && dot < key.length() - 1) {
      return lookup(row, key.substring(dot + 1));
    }
    return null;
  }

  private static String normalizeOperator(String opRaw) throws PSPipelineIrException {
    if (opRaw == null || opRaw.isBlank()) {
      throw new PSPipelineIrException("Filter predicate operator is required");
    }
    String t = opRaw.trim().replaceAll("\\s+", " ");
    if ("!=".equals(t) || "<>".equals(t) || "=".equals(t) || "<".equals(t) || ">".equals(t)
        || "<=".equals(t) || ">=".equals(t)) {
      return t;
    }
    return t.toUpperCase(Locale.ROOT);
  }

  private static boolean containsKeyIgnoreCase(Map<String, Object> params, String name) {
    if (params == null || name == null) {
      return false;
    }
    if (params.containsKey(name)) {
      return true;
    }
    for (String k : params.keySet()) {
      if (k != null && k.equalsIgnoreCase(name)) {
        return true;
      }
    }
    return false;
  }

  private static Object findParamIgnoreCase(Map<String, Object> params, String name) {
    if (params.containsKey(name)) {
      return params.get(name);
    }
    for (Map.Entry<String, Object> e : params.entrySet()) {
      if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
        return e.getValue();
      }
    }
    return null;
  }
}
