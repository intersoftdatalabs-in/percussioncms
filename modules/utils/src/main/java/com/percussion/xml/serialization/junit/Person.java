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
package com.percussion.xml.serialization.junit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A sample person class used in unit test of the {@link
 * com.percussion.xml.serialization.PSObjectSerializer} class. As can be seen it is a simple java
 * bean with a default ctor (required) and setXxx() and getXxx() methods. It has some other java
 * objects and a collection of an object as fields.
 *
 * <p>This class is {@code final} and the two-arg constructor assigns the name field directly so no
 * subclass can observe a partially constructed instance via {@link #setName(Name)} ({@code
 * this-escape} under {@code -Xlint}).
 */
public final class Person {
  private Name name;

  private Address address;

  private List<Book> books = new ArrayList<Book>();

  /** Default ctor. Required by serializer. */
  public Person() {}

  public Person(String first, String last) {
    this.name = new Name(first, last);
  }

  public void setName(Name name) {
    this.name = name;
  }

  public Name getName() {
    return name;
  }

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public void addBook(Book book) {
    books.add(book);
  }

  public List<Book> getBooks() {
    return books;
  }

  public void setBooks(Collection<Book> books) {
    this.books = new ArrayList<Book>(books);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Person)) return false;
    Person person = (Person) o;
    return Objects.equals(getName(), person.getName())
        && Objects.equals(getAddress(), person.getAddress())
        && Objects.equals(getBooks(), person.getBooks());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), getAddress(), getBooks());
  }
}
