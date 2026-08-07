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

import com.percussion.taxonomy.domain.Attribute;
import java.util.Collection;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class HibernateAttributeDAO implements AttributeDAO {

  @Autowired private SessionFactory sessionFactory;

  public Collection<Attribute> getAttribute(int id) {
    String queryString =
        "from Attribute a left join fetch a.taxonomy left join fetch a.attribute_langs where a.id ="
            + " "
            + id;
    Session session = sessionFactory.getCurrentSession();
    return session.createQuery(queryString, Attribute.class).getResultList();
  }

  /** Return all Attributes */
  public Collection<Attribute> getAllAttributes(int taxonomy_id, int langID) {
    String queryString =
        "from Attribute a left join fetch a.taxonomy left join fetch a.attribute_langs al join"
            + " fetch al.language where a.taxonomy.id = "
            + taxonomy_id
            + " and al.language.id = "
            + langID;
    Session session = sessionFactory.getCurrentSession();
    return session.createQuery(queryString, Attribute.class).getResultList();
  }

  /** Return all Attribute names and IDs */
  public Collection<Object[]> getAttributeNames(int taxonomy_id, int language_id) {
    String queryString =
        "select al.Name, a.id from Attribute a, Attribute_lang al where al.attribute.id = a.id and"
            + " a.taxonomy.id = "
            + taxonomy_id
            + " and al.language.id = "
            + language_id
            + " order by al.id";
    Session session = sessionFactory.getCurrentSession();
    return session.createQuery(queryString, Object[].class).getResultList();
  }

  public void saveAttribute(Attribute attribute) {
    Session session = sessionFactory.getCurrentSession();
    session.merge(attribute);
  }

  public void removeAttribute(Attribute attribute) {
    Session session = sessionFactory.getCurrentSession();
    session.remove(attribute);
  }
}
