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

package com.percussion.taxonomy.repository;

import com.percussion.taxonomy.domain.Attribute_lang;
import java.util.Collection;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class HibernateAttribute_langDAO implements Attribute_langDAO {

  @Autowired private SessionFactory sessionFactory;

  public Attribute_lang getAttribute_lang(int id) {
    Session session = sessionFactory.getCurrentSession();
    return session.find(Attribute_lang.class, id);
  }

  public Collection<Attribute_lang> getAllAttribute_langs() {
    Session session = sessionFactory.getCurrentSession();
    return session.createQuery("from Attribute_lang att", Attribute_lang.class).list();
  }

  public void saveAttribute_lang(Attribute_lang attribute_lang) {
    Session session = sessionFactory.getCurrentSession();
    session.merge(attribute_lang);
  }

  public void removeAttribute_lang(Attribute_lang attribute_lang) {
    Session session = sessionFactory.getCurrentSession();
    session.remove(attribute_lang);
  }
}
