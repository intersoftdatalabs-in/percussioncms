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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

class PSBeanGeneratorTest {

  @Test
  void createClass_generatesClassWithProperties() throws Exception {
    PSBeanGenerator gen = new PSBeanGenerator();
    gen.setSuperclass(PSTypeConfiguration.GeneratedClassBase.class);
    gen.setClassLoader(getClass().getClassLoader());
    gen.setClassName("com.percussion.services.generated.TestBean");
    gen.addProperty("sys_contentid", Integer.class);
    gen.addProperty("title", String.class);

    Class<?> beanClass = gen.createClass();

    assertNotNull(beanClass);
    assertTrue(PSTypeConfiguration.GeneratedClassBase.class.isAssignableFrom(beanClass));

    // Verify getter/setter for sys_contentid
    Object instance = beanClass.getDeclaredConstructor().newInstance();
    Method setter = beanClass.getMethod("setSys_contentid", Integer.class);
    Method getter = beanClass.getMethod("getSys_contentid");
    setter.invoke(instance, 42);
    assertEquals(42, getter.invoke(instance));

    // Verify getter/setter for title
    Method titleSetter = beanClass.getMethod("setTitle", String.class);
    Method titleGetter = beanClass.getMethod("getTitle");
    titleSetter.invoke(instance, "hello");
    assertEquals("hello", titleGetter.invoke(instance));
  }

  @Test
  void createClass_supportsListProperty() throws Exception {
    PSBeanGenerator gen = new PSBeanGenerator();
    gen.setSuperclass(PSTypeConfiguration.GeneratedClassBase.class);
    gen.setClassLoader(getClass().getClassLoader());
    gen.setClassName("com.percussion.services.generated.TestBeanWithList");
    gen.addProperty("children", List.class);

    Class<?> beanClass = gen.createClass();

    assertNotNull(beanClass);
    assertTrue(beanClass.getMethod("getChildren") != null);
    assertTrue(beanClass.getMethod("setChildren", List.class) != null);
  }

  @Test
  void createClass_uniqueNamesPerInvocation() {
    PSBeanGenerator gen1 = new PSBeanGenerator();
    gen1.setSuperclass(PSTypeConfiguration.GeneratedClassBase.class);
    gen1.setClassLoader(getClass().getClassLoader());
    gen1.setClassName("com.percussion.services.generated.Duplicate");
    gen1.addProperty("field1", String.class);

    PSBeanGenerator gen2 = new PSBeanGenerator();
    gen2.setSuperclass(PSTypeConfiguration.GeneratedClassBase.class);
    gen2.setClassLoader(getClass().getClassLoader());
    gen2.setClassName("com.percussion.services.generated.Duplicate");
    gen2.addProperty("field2", Integer.class);

    Class<?> class1 = gen1.createClass();
    Class<?> class2 = gen2.createClass();

    assertNotNull(class1);
    assertNotNull(class2);
    // Each call should produce a distinct class even with the same base name
    assertTrue(!class1.getName().equals(class2.getName()));
  }

  @Test
  void createClass_instanceIsSerializable() throws Exception {
    PSBeanGenerator gen = new PSBeanGenerator();
    gen.setSuperclass(PSTypeConfiguration.GeneratedClassBase.class);
    gen.setClassLoader(getClass().getClassLoader());
    gen.setClassName("com.percussion.services.generated.SerializableBean");
    gen.addProperty("value", String.class);

    Class<?> beanClass = gen.createClass();
    Object instance = beanClass.getDeclaredConstructor().newInstance();

    assertTrue(instance instanceof Serializable);
  }

  @Test
  void createClass_noPropertiesStillWorks() {
    PSBeanGenerator gen = new PSBeanGenerator();
    gen.setSuperclass(PSTypeConfiguration.GeneratedClassBase.class);
    gen.setClassLoader(getClass().getClassLoader());
    gen.setClassName("com.percussion.services.generated.EmptyBean");

    assertDoesNotThrow(gen::createClass);
  }
}
