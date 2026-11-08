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

import java.lang.invoke.MethodHandles;
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
   * <p>Uses {@link ClassLoadingStrategy.UsingLookup MethodHandles Lookup}
   * injection so the class is defined in the target {@link ClassLoader} itself
   * (visible by name to Hibernate mappings). The legacy
   * {@code ClassLoadingStrategy.Default.INJECTION} strategy relies on Unsafe
   * reflection and is disabled by default as of Java 26, so it must not be
   * used here.
   *
   * <p>{@code Lookup.defineClass} can only define classes in the same package
   * as the lookup class, so the requested {@code className} contributes only
   * its simple name: the class is always defined in the superclass's package
   * (or this generator's own package when the superclass has no suitable
   * package, e.g. {@code Object} or a {@code java.*} type).
   *
   * @return the generated {@link Class}
   */
  Class<?> createClass() {
    Class<?> lookupClass = lookupClassFor(superclass);
    String targetPackage = packageOf(lookupClass);
    String simpleName = simpleNameOf(className);
    // Append a unique suffix to guarantee a unique class name for each call,
    // since multiple content types may resolve to the same base name and the
    // same ClassLoader cannot define the same class name twice.
    String uniqueName = targetPackage + "." + simpleName + "$" + CLASS_COUNTER.incrementAndGet();

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

    try {
      MethodHandles.Lookup lookup =
          MethodHandles.privateLookupIn(lookupClass, MethodHandles.lookup());
      return builder.make().load(loader, ClassLoadingStrategy.UsingLookup.of(lookup)).getLoaded();
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(
          "Cannot obtain MethodHandles lookup for bean superclass " + lookupClass.getName(), e);
    }
  }

  private static Class<?> lookupClassFor(Class<?> superclass) {
    if (superclass == null
        || superclass.isPrimitive()
        || superclass.isArray()
        || superclass == Object.class
        || superclass.getPackageName().startsWith("java.")) {
      return PSBeanGenerator.class;
    }
    return superclass;
  }

  private static String packageOf(Class<?> lookupClass) {
    String pkg = lookupClass.getPackageName();
    if (pkg == null || pkg.isEmpty()) {
      return PSBeanGenerator.class.getPackageName();
    }
    return pkg;
  }

  private static String simpleNameOf(String className) {
    if (className == null || className.isBlank()) {
      return "GeneratedBean";
    }
    int dot = className.lastIndexOf('.');
    String simple = dot == -1 ? className : className.substring(dot + 1);
    return simple.isEmpty() ? "GeneratedBean" : simple;
  }

  private static String getterName(String property) {
    return "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
  }

  private static String setterName(String property) {
    return "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
  }
}
