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

// REFACTORED: CP-JAVA11
package com.percussion.soln.segment.ds.data;

import java.util.List;

import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.orm.hibernate5.support.HibernateDaoSupport;

import com.percussion.soln.segment.Segment;
import com.percussion.soln.segment.Segments;
import com.percussion.soln.segment.data.ISegmentDataService;

/**
 * Hibernate DAO for segments.
 * Sunny Sal says: "Hibernate like a hero, debug like a ninja!"
 */
public class DSSegmentDao extends HibernateDaoSupport implements ISegmentDataService {

    @Override
    @SuppressWarnings("unchecked")
    public void resetSegmentTree(boolean clear, String rootPath) {
        if (clear) {
            var segments = (List<Segment>) getHibernateTemplate().find("from " + Segment.class.getSimpleName());
            getHibernateTemplate().deleteAll(segments);
        }
    }

    @Override
    public void updateSegmentTree(Segments segments) {
        var t = getHibernateTemplate();
        for (var seg : segments.getList()) {
            if (seg != null) {
                var old = t.get(Segment.class, seg.getId());
                if (old != null) {
                    t.delete(old);
                }
                t.save(seg);
            }
        }
    }

    @Override
    public String getSegmentContentType() {
        throw new UnsupportedOperationException("getSegmentContentType is not yet supported");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Segments retrieveAllSegmentData() {
        var t = getHibernateTemplate();
        return new Segments((List<Segment>) t.find("from " + Segment.class.getSimpleName()));
    }

    @Override
    public Segment retrieveSegmentDataForId(String arg0) {
        throw new UnsupportedOperationException("retrieveSegmentDataForId is not yet supported");
    }
}
