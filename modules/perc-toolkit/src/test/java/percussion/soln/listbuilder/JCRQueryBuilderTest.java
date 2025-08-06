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

package percussion.soln.listbuilder;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.percussion.soln.listbuilder.JCRQueryBuilder;


public class JCRQueryBuilderTest {
    
    private JCRQueryBuilder builder;
    
    @BeforeEach
    public void setUp() {
        builder = new JCRQueryBuilder();
    }
    
    
    @Test
    public void testGetQuery() throws Exception {
        String query = builder.getQuery();
        assertNotNull(query);
    }
    
    @Test
    public void testGetQueryWithDateRange() throws Exception {
        
        builder.setTitleContains("News");
        builder.setStartDate("2010");
        
        String query = builder.getQuery();
        
        assertNotNull(query);
        assertEquals("select rx:sys_contentid, rx:sys_folderid, jcr:path from nt:base where " +
        		"(('2010' < rx:sys_contentstartdate ) and rx:displaytitle like '%News%' )", query.replaceAll("  ", " "));
    }
    
    @Test
    public void testGetQueryWithPaths() throws Exception {
        builder.setFolderPaths(asList("//Sites/a", "//Sites/b"));
        String query = builder.getQuery();
        assertNotNull(query);
        assertEquals("select rx:sys_contentid, rx:sys_folderid, jcr:path from nt:base where (( jcr:path like '//Sites/a/%'  or  jcr:path like '//Sites/b/%' ))", query);
    }
    
    

}
