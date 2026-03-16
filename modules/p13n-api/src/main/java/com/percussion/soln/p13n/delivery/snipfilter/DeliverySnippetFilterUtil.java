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

package com.percussion.soln.p13n.delivery.snipfilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.percussion.soln.p13n.delivery.IDeliveryResponseSnippetItem;
import com.percussion.soln.segment.Segment;

/**
 * Utility class for filtering and sorting delivery response snippet items.
 * Provides methods to match segments and snippets, sort by index, and perform
 * set operations on segment collections.
 * 
 * @author adamgent
 * @since 8.0.0
 */
public class DeliverySnippetFilterUtil {
    private static final DeliveryResponseSnippetItemSorter snippetSorter = new DeliveryResponseSnippetItemSorter();

    /**
     * Finds segments that exist in both the given segments collection and snippet items.
     * 
     * @param segments the segments to match against
     * @param snipItems the snippet items to check
     * @return list of matching segments
     * @throws IllegalArgumentException if either parameter is null
     */
    public static List<? extends Segment> matchingSegments(
            Collection<? extends Segment> segments, 
            List<IDeliveryResponseSnippetItem> snipItems) {
        if(segments == null || snipItems == null) 
            throw new IllegalArgumentException("Arguments cannot be null");
        ArrayList<Segment> matching = new ArrayList<Segment>();
        ArrayList<Segment> snipSegments = new ArrayList<Segment>();
        for(IDeliveryResponseSnippetItem snip : snipItems) { snipSegments.addAll(snip.getSegments()); }
        for(Segment seg: segments) { if (containsSegment(seg, segments)) matching.add(seg); }
        return matching;
    }
    
    /**
     * Finds snippet items that contain at least one segment from the given segments collection.
     * 
     * @param segments the segments to match
     * @param snipItems the snippet items to check
     * @return list of matching snippet items
     */
    public static List<IDeliveryResponseSnippetItem> matchingSnippets(
            Collection<? extends Segment> segments,
            List<IDeliveryResponseSnippetItem> snipItems) {
        List<IDeliveryResponseSnippetItem> matching = new ArrayList<IDeliveryResponseSnippetItem>();
        for(IDeliveryResponseSnippetItem snip : snipItems) { 
            if (containsAnySegment(segments, snip.getSegments())) {
                matching.add(snip);
            }
        }
        return matching;
    }
    
    /**
     * Sorts snippet items by their sort index in ascending order.
     * Items with no or invalid sort index are placed at the beginning.
     * 
     * @param unSorted the unsorted list of snippet items
     * @return sorted list of snippet items
     */
    public static List<IDeliveryResponseSnippetItem> sortSnippets(List<IDeliveryResponseSnippetItem> unSorted) {
        List<IDeliveryResponseSnippetItem> sort = new ArrayList<IDeliveryResponseSnippetItem>(unSorted);
        Collections.sort(sort, snippetSorter);
        return sort;
    }
    
    private static class DeliveryResponseSnippetItemSorter implements Comparator<IDeliveryResponseSnippetItem> {
        
        public int compare(IDeliveryResponseSnippetItem lh, IDeliveryResponseSnippetItem rh) {
            Integer lhI = getIndex(lh);
            Integer rhI = getIndex(rh);
            return lhI.compareTo(rhI);
        }
        
        private int getIndex(IDeliveryResponseSnippetItem item) {
            return item.getSortIndex() <= 0 ? Integer.MIN_VALUE : item.getSortIndex();
        }
    }
    
    /**
     * Checks if a snippet exists in the given list of snippets.
     * 
     * @param snippetToMatch the snippet to find
     * @param snippets the list to search in
     * @return true if the snippet is found, false otherwise
     */
    public static boolean containsSnippet(IDeliveryResponseSnippetItem snippetToMatch, List<IDeliveryResponseSnippetItem> snippets) {
        if (snippetToMatch == null) return false;
        if (snippets == null) return false;
        for(IDeliveryResponseSnippetItem snippet : snippets) {
            if (snippet != null && 
                    (snippetToMatch == snippet || snippetToMatch.getId().equals(snippet.getId())))
                return true;
        }
        return false;
    }
    
    /**
     * Checks if any segment from collection A exists in collection B.
     * 
     * @param as first collection of segments
     * @param bs second collection of segments
     * @return true if any segment from A is found in B
     */
    public static boolean containsAnySegment(
            Collection<? extends Segment> as,
            Collection<? extends Segment> bs ) {
        for(Segment a : as) { if(containsSegment(a, bs)) { return true; } }
        return false;
    }
    
    /**
     * Returns the intersection of two segment collections.
     * 
     * @param as first collection of segments
     * @param bs second collection of segments
     * @return collection of segments that exist in both A and B
     */
    public static Collection<? extends Segment> intersectSegments(
            Collection<? extends Segment> as,
            Collection<? extends Segment> bs ) {
        ArrayList<Segment> segs = new ArrayList<Segment>();
        for(Segment a : as) { if(containsSegment(a, bs)) { segs.add(a); } }
        return segs;
    }
    
    /**
     * Checks if a segment exists in the given collection by comparing segment objects.
     * 
     * @param segment the segment to find
     * @param segments the collection to search
     * @return true if found, false otherwise
     */
    public static boolean containsSegment(Segment segment, 
            Collection<? extends Segment> segments) {
        if (segment == null) return false;
        return containsSegment(segment.getId(), segments);
    }
    
    /**
     * Checks if a segment with the given ID exists in the collection.
     * 
     * @param id the segment ID to find
     * @param segments the collection to search
     * @return true if found, false otherwise
     */
    public static boolean containsSegment(String id, Collection<? extends Segment> segments) {
        for(Segment s : segments) { if (id.equals(s.getId())) return true; }
        return false;
    }
}
