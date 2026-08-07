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

import com.percussion.taxonomy.domain.Taxonomy;
import java.util.Collection;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class HibernateTaxonomyDAO implements TaxonomyDAO {

  @Autowired private SessionFactory sessionFactory;

  public Taxonomy getTaxonomy(int id) {
    Session session = sessionFactory.getCurrentSession();
    return session.find(Taxonomy.class, id);
  }

  public List<Taxonomy> getTaxonomy(String name) {
    Session session = sessionFactory.getCurrentSession();
    Query<Taxonomy> q =
        session
            .createQuery(
                "select distinct t from Taxonomy t where lower(t.name) like :name", Taxonomy.class)
            .setParameter("name", name.toLowerCase());
    return q.list();
  }

  public List<Integer> getTaxonomyIdForName(String name) {
    Session session = sessionFactory.getCurrentSession();
    Query<Integer> query =
        session.createQuery("select id from Taxonomy where name like :name", Integer.class);
    query.setParameter("name", name);
    return query.list();
  }

  public Collection<Taxonomy> getAllTaxonomys() {
    Session session = sessionFactory.getCurrentSession();
    Query<Taxonomy> query =
        session.createQuery("from Taxonomy tax order by lower(name) asc", Taxonomy.class);
    return query.list();
  }

  public void saveTaxonomy(Taxonomy taxonomy) {
    Session session = sessionFactory.getCurrentSession();
    session.merge(taxonomy);
  }

  public void removeTaxonomy(Taxonomy taxonomy) {
    Session session = sessionFactory.getCurrentSession();
    session.remove(taxonomy);
  }
}
