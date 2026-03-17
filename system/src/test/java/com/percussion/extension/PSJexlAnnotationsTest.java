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
package com.percussion.extension;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import org.junit.jupiter.api.Test;

public class PSJexlAnnotationsTest {

  @IPSJexlMethod(
      description = "A test method",
      params = {
        @IPSJexlParam(name = "first", type = "int", description = "first parameter"),
        @IPSJexlParam(name = "second", description = "second parameter")
      })
  public void tryit(int param1, int param2) {}

  @Test
  public void test1() throws Exception {
    Class<?> noargs[] = new Class<?>[] {int.class, int.class};
    Method m = this.getClass().getMethod("tryit", noargs);
    Annotation a[] = m.getAnnotations();
    assertEquals(1, a.length);
    IPSJexlMethod method = (IPSJexlMethod) a[0];
    System.out.println(method.description());
    IPSJexlParam parameters[] = method.params();
    for (IPSJexlParam p : parameters) {
      System.out.println("Param " + p);
    }
  }
}
