/*
 *     Percussion CMS
 *     Copyright (C) 1999-2021 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package com.percussion.taxonomy.repository;

import com.percussion.taxonomy.domain.Language;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import java.util.Collection;

public class HibernateLanguageDAO extends HibernateDaoSupport implements LanguageDAO {

    public Language getLanguage(int id) {
        return (Language) getHibernateTemplate().get(Language.class, new Integer(id));
    }

    public Collection getAllLanguages() {
        //Optional: Add order by to query
        return getHibernateTemplate().find("from Language lan");
    }

    public void saveLanguage(Language language) {
        getHibernateTemplate().saveOrUpdate(language);
    }

    public void removeLanguage(Language language) {
        getHibernateTemplate().delete(language);
    }
}
