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

package com.percussion.soln.p13n.tracking;

import static org.junit.Assert.*;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.percussion.soln.p13n.tracking.impl.SegmentWeightUtil;


public class SegmentWeightUtilTest {

    HashMap<String, Integer> weights;
    HashMap<String, Integer> expected;
    HashMap<String, Integer> request;
    Map<String, Integer> actual;
    
    @Before
    public void setUp() throws Exception {
        weights = new HashMap<String, Integer>();
        request = new HashMap<String, Integer>();
        expected = new HashMap<String, Integer>();
        actual = new HashMap<String, Integer>();
    }

    @Test
    public void testMergeSegmentWeights() {
        weights.put("1", 1);
        weights.put("2", 2);
        
        request.put("1", 1);
        request.put("2", 1);
        request.put("3", 1);
        request.put("4", null);
        request.put(null, 2);
        
        expected.put("1", 2);
        expected.put("2", 3);
        expected.put("3", 1);
        expected.put("4", 0);
        
        actual = SegmentWeightUtil.mergeSegmentWeights(weights, request);
        
        assertEquals(expected,actual);
        
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void shouldFailOnNullInputOfMergeSegmentWeights() {
        SegmentWeightUtil.mergeSegmentWeights(null, null);
        
    }

    @Test
    public void testSetSegmentWeights() {

        weights.put("1", 1);
        weights.put("2", 2);
        
        request.put("1", 1);
        request.put("2", 1);
        request.put("3", 1);
        request.put("4", null);
        request.put(null, 2);
        
        expected.put("1", 1);
        expected.put("2", 1);
        expected.put("3", 1);
        expected.put("4", 0);
        
        actual = SegmentWeightUtil.setSegmentWeights(weights, request);
        
        assertEquals(expected,actual);
        
    }

    @Test(expected=IllegalArgumentException.class)
    public void shouldFailOnNullInputOfSetSegmentWeights() {
        SegmentWeightUtil.setSegmentWeights(null, null);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testCleanSegmentWeightsOfNull() {
        weights.put("1", 1);
        weights.put("2", 2);
        expected = (HashMap<String, Integer>)weights.clone();
        weights.put("3", null);
        weights.put("4", null);
        weights.put(null, 4);
        SegmentWeightUtil.cleanSegmentWeightsOfNull(weights);
        assertFalse(expected.containsKey("3") || expected.containsKey("4"));
        assertEquals(expected,weights);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void shouldFailOnNullInputOfCleanSegmentWeights() {
        SegmentWeightUtil.cleanSegmentWeightsOfNull(null);
    }
    

}
