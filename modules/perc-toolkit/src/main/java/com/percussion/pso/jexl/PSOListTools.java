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
/*
 * @author Adam Gent
 *
 */
package com.percussion.pso.jexl;

import com.percussion.extension.IPSJexlExpression;
import com.percussion.extension.IPSJexlMethod;
import com.percussion.extension.IPSJexlParam;
import com.percussion.extension.PSJexlUtilBase;
import com.percussion.utils.types.PSPair;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is a utility class to help create and manipulate lists with JEXL.
 *
 * @author agent
 * @since 6.0
 */
public class PSOListTools extends PSJexlUtilBase implements IPSJexlExpression {

  /** Logger for this class */
  private static final Logger log = LogManager.getLogger(PSOListTools.class);

  public PSOListTools() {
    super();
  }

  /**
   * Works just like the java.util.List.subList but with a <code>Collection</code> object.
   *
   * <p>If the arguments are invalid an exception will be thrown.
   *
   * @see List#subList(int, int)
   * @param c the collection.
   * @param start start index (inclusive) for the slice
   * @param end end index (exclusive)
   * @return a subsection of the collection as a list.
   * @throws IllegalArgumentException if arguments are invalid
   * @throws IndexOutOfBoundsException if indices are out of bounds
   */
  public List<Object> subListUnSafe(Collection<?> c, int start, int end)
      throws IllegalArgumentException, IndexOutOfBoundsException {
    if (c == null) {
      throw new IllegalArgumentException("The collection cannot be null");
    }
    if (start > end) {
      throw new IllegalArgumentException("Start index is greater then end.");
    }
    if (start > c.size() || end > c.size() || start < 0 || end < 0) {
      throw new IndexOutOfBoundsException("Index out of bounds");
    }
    List<Object> rvalue = new ArrayList<>(c);
    rvalue = rvalue.subList(start, end);

    return rvalue;
  }

  /**
   * Gets a sublist from the specified Collection avoiding exceptions.
   *
   * <p>A negative start position can be used to start/end <code>n</code> objects from the end of
   * the Collection.
   *
   * <p>The returned sublist starts with the character in the <code>start</code> position and ends
   * before the <code>end</code> position. All position counting is zero-based -- i.e., to start at
   * the beginning of the collection use <code>start = 0</code>. Negative start and end positions
   * can be used to specify offsets relative to the end of the String.
   *
   * <p>If <code>start</code> is not strictly to the left of <code>end</code>, "" is returned.
   *
   * <pre>
   *  sublist(null, *, *)    = [];
   *  sublist([], * ,  *)    = [];
   *  sublist(["a","b","c"], 0, 2)   = ["a","b"]
   *  sublist(["a","b","c"], 2, 0)   = []
   *  sublist(["a","b","c"], 2, 4)   = ["c"]
   *  sublist(["a","b","c"], 4, 6)   = []
   *  sublist(["a","b","c"], 2, 2)   = []
   *  sublist(["a","b","c"], -2, -1) = ["b"]
   *  sublist(["a","b","c"], -4, 2)  = ["a","b"]
   * </pre>
   *
   * @param c the Collection to get the sublist from, may be null
   * @param start the position to start from, negative means count back from the end of the
   *     Collection by this many characters
   * @param end the position to end at (exclusive), negative means count back from the end of the
   *     Collection by this many characters
   * @return subset of the collection from start position to end positon, never <code>null</code>.
   */
  @IPSJexlMethod(
      description = "sublist sections a part of a collection into a list",
      params = {
        @IPSJexlParam(name = "collection", description = "the collection to be used"),
        @IPSJexlParam(
            name = "start",
            description =
                "the start index (negative means count back from the end of the collection)"),
        @IPSJexlParam(name = "end", description = "the end index (exclusive)")
      },
      returns = "a subset of collection as a list")
  public <T> List<T> sublist(Collection<T> c, int start, int end) {
    log.debug("processing sublist(Collection c, int start, int end)");
    List<T> rvalue = new ArrayList<>();
    if (c == null) {
      return rvalue;
    }

    final int size = c.size();
    // handle negatives
    if (end < 0) {
      end = size + end; // remember end is negative
    }
    if (start < 0) {
      start = size + start; // remember start is negative
    }

    // check length next
    if (end > size) {
      end = size;
    }

    // if start is greater than end, return ""
    if (start > end) {
      return rvalue;
    }

    if (start < 0) {
      start = 0;
    }
    if (end < 0) {
      end = 0;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    List tmp = subListUnSafe(c, start, end);
    rvalue = tmp;
    return rvalue;
  }

  /**
   * @see #sublist(Collection, int, int)
   * @param c
   * @param start
   * @param end
   * @return a subset of the collection never <code>null</code>
   */
  public <T> List<T> sublist(Collection<T> c, String start, String end) {
    log.debug("processing sublist(Collection c, String start, String end)");
    int[] i = convertIndexs(start, end);
    return sublist(c, i[0], i[1]);
  }

  /**
   * @see #sublist(Collection, int, int)
   * @param c
   * @param start
   * @param end
   * @return a subset of the collection never <code>null</code>.
   */
  public <T> List<T> sublist(Collection<T> c, Number start, Number end) {
    log.debug("processing sublist(Collection c, Number start, Number end)");
    int[] i = convertIndexs(start, end);
    return sublist(c, i[0], i[1]);
  }

  /**
   * @see #sublist(Collection, int, int)
   * @param c
   * @param start
   * @param end
   * @return a subset of the collection never <code>null</code>.
   */
  public <T> List<T> sublist(T[] c, int start, int end) {
    log.debug("processing sublist(Object[] c, int start, int end)");
    List<T> rvalue;
    if (c == null) {
      rvalue = new ArrayList<>();
    } else {
      List<T> tmpList = Arrays.asList(c);
      rvalue = sublist(tmpList, start, end);
    }
    return rvalue;
  }

  /**
   * @param c
   * @param start
   * @param end
   * @return a subset of the collection never <code>null</code>.
   * @see #sublist(Collection, int, int)
   */
  public <T> List<T> sublist(T[] c, String start, String end) {
    log.debug("processing sublist(Object[] c, String start, String end)");
    int[] i = convertIndexs(start, end);
    return sublist(c, i[0], i[1]);
  }

  /**
   * Creates a list from an array or collection.
   *
   * <p>If its neither the list will contain just the item (<code>list.size()==1</code>).
   *
   * @param single if <code>null</code> returns an empty list.
   * @return a list representing the array,collection, or single item, never <code>null</code>
   */
  @IPSJexlMethod(
      description =
          "Creates a list from an array or collection. If its neither the list will contain just"
              + " the item.",
      params = {@IPSJexlParam(name = "value", description = "value ")},
      returns = "a list")
  @SuppressWarnings({"unchecked", "rawtypes"})
  public List<Object> asList(Object single) {
    if (single == null) {
      return new ArrayList<>();
    } else if (single instanceof Collection) {
      Collection tmpCollection = (Collection) single;
      List<Object> rvalue = new ArrayList<>();
      rvalue.addAll(tmpCollection);
      return rvalue;
    } else if (single instanceof Object[]) {
      return Arrays.asList((Object[]) single);
    } else {
      List<Object> rvalue = new ArrayList<>();
      rvalue.add(single);
      return rvalue;
    }
  }

  /**
   * Creates a list of size with the given value for each index.
   *
   * @param <T>
   * @param value the value should be repeated on each index.
   * @param size size of the array to return. Invalid sizes return an empty list.
   * @return an array with the given size, Never <code>null</code>.
   */
  @IPSJexlMethod(
      description = "Creates a list of size with given value for each index",
      params = {
        @IPSJexlParam(name = "value", description = "value "),
        @IPSJexlParam(name = "size", description = "size of the array to return")
      },
      returns = "T[]")
  public <T> List<T> asList(T value, int size) {
    List<T> rvalue = new ArrayList<T>();
    for (int i = 0; i < size; i++) {
      rvalue.add(value);
    }
    return rvalue;
  }

  /**
   * Creates a PSPair Object this simply has two properties First and Second. This can be used with
   * $tools.sorter.sort to sort items
   *
   * @param <A> the type of first object
   * @param <B> the type of second object
   * @param first the first object.
   * @param second the second object.
   * @return a PSPair Object, Never null.
   */
  @IPSJexlMethod(
      description = "Creates a PSPair has properties First and Second ",
      params = {
        @IPSJexlParam(name = "first", description = "first object"),
        @IPSJexlParam(name = "second", description = "second object")
      },
      returns = "a list")
  public <A, B> PSPair<A, B> asPair(A first, B second) {
    PSPair<A, B> pair = new PSPair<A, B>(first, second);
    return pair;
  }

  /**
   * Validates and converts start and end to integers.
   *
   * @param start
   * @param end
   * @return an two index (length = 2) integer array with start first and end last.
   */
  private int[] convertIndexs(String start, String end) {
    if (start == null || StringUtils.isBlank(start)) {
      throw new IllegalArgumentException("Start Index cannot be null or empty");
    }
    if (end == null || StringUtils.isBlank(end)) {
      throw new IllegalArgumentException("End Index cannot be null or empty");
    }
    if (NumberUtils.isNumber(start) && NumberUtils.isNumber(end)) {
      return new int[] {NumberUtils.toInt(start), NumberUtils.toInt(end)};
    }

    throw new IllegalArgumentException("Either Start or End index is not a number");
  }

  /**
   * Validates and converts start and end to integers.
   *
   * @param start
   * @param end
   * @return an two index (length = 2) integer array with start first and end last.
   */
  private int[] convertIndexs(Number start, Number end) {
    return new int[] {start.intValue(), end.intValue()};
  }

  /**
   * Reverses the order of the elements in the specified list. Proxies {@link
   * java.util.Collections#reverse(java.util.List)}
   *
   * @param list the list whose elements are to be reversed. this list must support the <code>set
   *     </code> method.
   * @see java.util.Collections#reverse(java.util.List)
   */
  @IPSJexlMethod(
      description = "Reverses the order of the elements in the specified list.",
      params = {
        @IPSJexlParam(name = "list", description = "the list whose elements are to be reversed.")
      })
  public void reverse(List list) {
    Collections.reverse(list);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    boolean first = true;
    StringBuilder s = new StringBuilder();
    Method[] methods = this.getClass().getMethods();
    for (int i = 0; i < methods.length; i++) {
      Method m = methods[i];
      if (m.isAnnotationPresent(IPSJexlMethod.class)) {
        if (first) {
          first = false;
        } else {
          s.append(",");
        }
        s.append(m.getName() + "(");
        Class[] params = m.getParameterTypes();
        for (int j = 0; j < params.length; j++) {
          s.append(params[j].getName());
          if (j < (params.length - 1)) {
            s.append(",");
          }
        }
        s.append(")");
      }
    }
    return s.toString();
  }
}
