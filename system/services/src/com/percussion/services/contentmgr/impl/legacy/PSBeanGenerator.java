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
package com.percussion.services.contentmgr.impl.legacy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates JavaBean classes dynamically using ByteBuddy. Replaces the legacy
 * CGLib {@code BeanGenerator} which is incompatible with JDK 21's module system
 * (CGLib requires reflective access to {@code ClassLoader.defineClass} which is
 * blocked by the module system).
 *
 * <p>Each generated class extends a configurable superclass and contains private
 * fields with public getter/setter pairs for each declared property.</p>
 */
class PSBeanGenerator {

  private static final AtomicInteger CLASS_COUNTER = new AtomicInteger(0);

  private Class<?> superclass = Object.class;
  private ClassLoader classLoader;
  private String className;
  private final Map<String, Class<?>> properties = new LinkedHashMap<>();

  void setSuperclass(Class<?> superclass) {
    this.superclass = superclass;
  }

  void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  /**
   * Sets the fully qualified class name for the generated bean.
   *
   * @param className the desired class name, e.g. {@code
   *     "com.percussion.services.generated.MyType"}
   */
  void setClassName(String className) {
    this.className = className;
  }

  /**
   * Adds a bean property (field + getter + setter) to the generated class.
   *
   * @param name the property name
   * @param type the property type
   */
  void addProperty(String name, Class<?> type) {
    properties.put(name, type);
  }

  /**
   * Generates and loads the bean class with all declared properties.
   *
   * @return the generated {@link Class}
   */
  Class<?> createClass() {
    // Append a unique suffix to guarantee a unique class name for each call,
    // since multiple content types may resolve to the same base name and the
    // same ClassLoader cannot define the same class name twice.
    String uniqueName = className + "$" + CLASS_COUNTER.incrementAndGet();

    DynamicType.Builder<?> builder =
        new ByteBuddy().subclass(superclass).name(uniqueName);

    for (Map.Entry<String, Class<?>> entry : properties.entrySet()) {
      String propName = entry.getKey();
      Class<?> propType = entry.getValue();

      builder =
          builder
              .defineField(propName, propType, Visibility.PRIVATE)
              .defineMethod(getterName(propName), propType, Visibility.PUBLIC)
              .intercept(FieldAccessor.ofField(propName))
              .defineMethod(setterName(propName), void.class, Visibility.PUBLIC)
              .withParameter(propType)
              .intercept(FieldAccessor.ofField(propName));
    }

    ClassLoader loader =
        classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();

    return builder.make().load(loader, ClassLoadingStrategy.Default.INJECTION).getLoaded();
  }

  private static String getterName(String property) {
    return "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
  }

  private static String setterName(String property) {
    return "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
  }
}
