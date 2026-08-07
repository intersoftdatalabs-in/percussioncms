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

import com.percussion.taxonomy.domain.Relationship_type;
import java.util.Collection;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class HibernateRelationship_typeDAO implements Relationship_typeDAO {

  @Autowired private SessionFactory sessionFactory;

  public Relationship_type getRelationship_type(int id) {
    Session session = sessionFactory.getCurrentSession();
    return session.find(Relationship_type.class, id);
  }

  public Collection<Relationship_type> getAllRelationship_types() {
    Session session = sessionFactory.getCurrentSession();
    return session.createQuery("from Relationship_type rel", Relationship_type.class).list();
  }

  public void saveRelationship_type(Relationship_type relationship_type) {
    Session session = sessionFactory.getCurrentSession();
    session.merge(relationship_type);
  }

  public void removeRelationship_type(Relationship_type relationship_type) {
    Session session = sessionFactory.getCurrentSession();
    session.remove(relationship_type);
  }
}
