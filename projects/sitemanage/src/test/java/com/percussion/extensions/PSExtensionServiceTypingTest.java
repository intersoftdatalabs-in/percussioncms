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
package com.percussion.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSExtensionListener;
import com.percussion.extension.PSExtensionRef;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

/** Ensures IPSExtensionService surface uses real generic Iterator types (issue #2032 batch 1). */
public class PSExtensionServiceTypingTest {

  @Test
  public void getExtensionFilesReturnsIteratorOfUrl() throws Exception {
    Method m = IPSExtensionService.class.getMethod("getExtensionFiles", PSExtensionRef.class);
    assertEquals(Iterator.class, m.getReturnType());
    var generic = m.getGenericReturnType();
    assertTrue(generic instanceof ParameterizedType);
    var args = ((ParameterizedType) generic).getActualTypeArguments();
    assertEquals(1, args.length);
    assertEquals(URL.class, args[0]);
  }

  @Test
  public void installAndUpdateAcceptWildcardIterator() throws Exception {
    Method install =
        IPSExtensionService.class.getMethod(
            "installExtension", IPSExtensionDef.class, Iterator.class);
    Method installListener =
        IPSExtensionService.class.getMethod(
            "installExtension", IPSExtensionDef.class, Iterator.class, IPSExtensionListener.class);
    Method update =
        IPSExtensionService.class.getMethod(
            "updateExtension", IPSExtensionDef.class, Iterator.class);

    for (Method m : new Method[] {install, installListener, update}) {
      var params = m.getGenericParameterTypes();
      // resources parameter is the second parameter
      assertTrue(params[1] instanceof ParameterizedType);
      var pt = (ParameterizedType) params[1];
      assertEquals(Iterator.class, pt.getRawType());
      assertEquals(1, pt.getActualTypeArguments().length);
      assertEquals("?", pt.getActualTypeArguments()[0].getTypeName());
    }
  }
}
