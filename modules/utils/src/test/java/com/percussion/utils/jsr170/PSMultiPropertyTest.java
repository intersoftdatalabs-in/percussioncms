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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.utils.beans.PSPropertyAccessException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javax.jcr.Node;
import javax.jcr.Value;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PSMultiProperty}. The non-Collection property value case (review thread
 * PRRT_kwDOKZBp3M6XPfAm) is the primary coverage target: a non-Collection property value must
 * surface as {@link ClassCastException}, not be silently replaced with an empty list.
 */
@Tag("UnitTest")
public class PSMultiPropertyTest {

  /** Test bean whose {@code getValues()} returns the configured value (raw Object). */
  public static class TestMappingClass {
    private final Object m_values;

    public TestMappingClass(Object values) {
      m_values = values;
    }

    @SuppressWarnings("unused")
    public Object getValues() {
      return m_values;
    }
  }

  /**
   * Build a stub {@link Node} via a {@link Proxy}. The PSMultiProperty constructor only invokes
   * {@link Node#getDepth()} on the parent and stores the reference; everything else can be a no-op.
   */
  private static Node stubNode() {
    InvocationHandler handler =
        new InvocationHandler() {
          @Override
          public Object invoke(Object proxy, Method method, Object[] args) {
            if ("getDepth".equals(method.getName())) {
              return 0;
            }
            if ("toString".equals(method.getName())) {
              return "StubNode";
            }
            if ("hashCode".equals(method.getName())) {
              return System.identityHashCode(proxy);
            }
            if ("equals".equals(method.getName())) {
              return proxy == args[0];
            }
            Class<?> rt = method.getReturnType();
            if (rt == boolean.class) return Boolean.FALSE;
            if (rt == int.class) return 0;
            if (rt == long.class) return 0L;
            if (rt == double.class) return 0.0;
            return null;
          }
        };
    return (Node)
        Proxy.newProxyInstance(Node.class.getClassLoader(), new Class<?>[] {Node.class}, handler);
  }

  /** Happy path: a Collection of values produces the corresponding JCR Value[]. */
  @Test
  public void collectionValuesAreExposed() throws Exception {
    Collection<String> source = Arrays.asList("alpha", "beta");
    PSMultiProperty prop = new PSMultiProperty("values", stubNode(), new TestMappingClass(source));

    Value[] values = prop.getValues();
    assertNotNull(values);
    assertEquals(2, values.length);
    assertEquals("alpha", values[0].getString());
    assertEquals("beta", values[1].getString());
  }

  /**
   * A null property value has always surfaced as NPE on the iterator (matches pre-PR behavior since
   * the raw cast yields null and {@code values.iterator()} then NPEs). Document that behavior here
   * so it does not silently flip if someone later reintroduces a fallback.
   */
  @Test
  public void nullValueThrowsNullPointerExceptionOnIteration() {
    TestMappingClass rep = new TestMappingClass(null);
    assertThrows(NullPointerException.class, () -> new PSMultiProperty("values", stubNode(), rep));
  }

  /**
   * Non-Collection property value (the misconfiguration case flagged by review thread
   * PRRT_kwDOKZBp3M6XPfAm) must surface as ClassCastException so corruption / misconfiguration is
   * detected immediately instead of silently producing an empty list.
   */
  @Test
  public void nonCollectionValueThrowsClassCastException() {
    TestMappingClass rep = new TestMappingClass("not-a-collection");
    assertThrows(ClassCastException.class, () -> new PSMultiProperty("values", stubNode(), rep));
  }

  /** An ArrayList of strings is the most common production shape and must round-trip. */
  @Test
  public void arrayListOfStringsRoundTrips() throws Exception {
    ArrayList<String> source = new ArrayList<>(Arrays.asList("x", "y", "z"));
    PSMultiProperty prop = new PSMultiProperty("values", stubNode(), new TestMappingClass(source));

    assertEquals(3, prop.getValues().length);
    assertEquals("y", prop.getValues()[1].getString());
  }

  /** A bean whose getValues throws is propagated. */
  @Test
  public void accessExceptionIsPropagated() {
    Object rep =
        new Object() {
          @SuppressWarnings("unused")
          public Object getValues() throws PSPropertyAccessException {
            throw new PSPropertyAccessException("boom");
          }
        };
    assertThrows(
        PSPropertyAccessException.class, () -> new PSMultiProperty("values", stubNode(), rep));
  }
}
