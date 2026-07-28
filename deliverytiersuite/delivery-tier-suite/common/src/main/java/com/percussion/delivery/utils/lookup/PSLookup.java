/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

package com.percussion.delivery.utils.lookup;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Provides a generic sys_Lookup style list that can be used by Rhythmyx controls.
 *
 * @author natechadwick
 */
@XmlRootElement(name = "sys_Lookup")
@XmlAccessorType(XmlAccessType.FIELD)
public class PSLookup implements List<PSXEntry> {

  @XmlElement(name = "PSXEntry")
  /** The list of {@link PSXEntry} values backing this lookup. */
  private List<PSXEntry> list = new ArrayList<>();

  /** Default constructor. */
  public PSLookup() {}

  /**
   * Returns the number of entries in this lookup.
   *
   * @return the number of entries.
   */
  @Override
  public int size() {
    return list.size();
  }

  @Override
  public boolean isEmpty() {
    return list.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return list.contains(o);
  }

  @Override
  public Iterator<PSXEntry> iterator() {
    return list.iterator();
  }

  @Override
  public Object[] toArray() {
    return list.toArray();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] toArray(T[] a) {
    return list.toArray(a);
  }

  @Override
  public boolean remove(Object o) {
    return list.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return list.containsAll(c);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean addAll(Collection<? extends PSXEntry> c) {
    return list.addAll((Collection<PSXEntry>) c);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean addAll(int index, Collection<? extends PSXEntry> c) {
    return list.addAll(index, (Collection<PSXEntry>) c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return list.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return list.retainAll(c);
  }

  @Override
  public void clear() {
    list.clear();
  }

  @Override
  public int indexOf(Object o) {
    return list.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return list.lastIndexOf(o);
  }

  @Override
  public ListIterator<PSXEntry> listIterator() {
    return list.listIterator();
  }

  @Override
  public ListIterator<PSXEntry> listIterator(int index) {
    return list.listIterator(index);
  }

  @Override
  public List<PSXEntry> subList(int fromIndex, int toIndex) {
    return list.subList(fromIndex, toIndex);
  }

  @Override
  public boolean add(PSXEntry e) {
    return list.add(e);
  }

  @Override
  public PSXEntry get(int index) {
    return list.get(index);
  }

  @Override
  public PSXEntry set(int index, PSXEntry element) {
    return list.set(index, element);
  }

  @Override
  public void add(int index, PSXEntry element) {
    list.add(index, element);
  }

  @Override
  public PSXEntry remove(int index) {
    return list.remove(index);
  }
}
