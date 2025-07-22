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
package com.percussion.delivery.comments.services;

import com.percussion.delivery.comments.data.*;
import com.percussion.delivery.comments.service.rdbms.PSComment;
import com.percussion.delivery.listeners.IPSServiceDataChangeListener;
import com.percussion.error.PSExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Service implementation for managing comments in the CMS.
 * Thread-safe and uses Java 11 features for improved performance.
 */
@Service
public class PSCommentsService implements IPSCommentsService {
    private static final Logger log = LogManager.getLogger(PSCommentsService.class);

    private static final Duration COMMENT_VISIBILITY_DURATION = Duration.ofMinutes(1);
    private static final String[] PERC_COMMENTS_SERVICES = {"perc-comments-services"};

    private static final Map<PSCommentSort.SORTBY, String> SORTBY_FIELD_MAPPING =
        Collections.unmodifiableMap(new EnumMap<PSCommentSort.SORTBY, String>(PSCommentSort.SORTBY.class) {{
            put(PSCommentSort.SORTBY.CREATED_DATE, "createdDate");
            put(PSCommentSort.SORTBY.USERNAME, "username");
            put(PSCommentSort.SORTBY.TITLE, "title");
            put(PSCommentSort.SORTBY.STATE, "approvalState");
        }});

    private final IPSCommentsDao dao;
    private final List<IPSServiceDataChangeListener> listeners;
    private final PSProfanityFilter profanityFilter;

    @Autowired
    public PSCommentsService(IPSCommentsDao dao) {
        this.dao = Objects.requireNonNull(dao, "dao must not be null");
        this.listeners = new CopyOnWriteArrayList<>();
        this.profanityFilter = new PSProfanityFilter();
    }

    @Override
    public IPSComment addComment(IPSComment comment) {
        Objects.requireNonNull(comment, "comment must not be null");

        var siteSet = Set.of(comment.getSite());
        validateComment(comment);

        var savedComment = dao.addComment(comment);
        notifyListeners(siteSet);
        return savedComment;
    }

    private void validateComment(IPSComment comment) {
        if (StringUtils.isBlank(comment.getText())) {
            throw new IllegalArgumentException("Comment text cannot be empty");
        }

        if (profanityFilter.containsProfanity(comment.getText()) ||
            comment.getTitle().map(profanityFilter::containsProfanity).orElse(false)) {
            throw new IllegalArgumentException("Comment contains profanity");
        }
    }

    @Override
    public void addListener(IPSServiceDataChangeListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        listeners.add(listener);
    }

    @Override
    public void removeListener(IPSServiceDataChangeListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        listeners.remove(listener);
    }

    private void notifyListeners(Set<String> sites) {
        if (sites.isEmpty()) {
            return;
        }

        listeners.forEach(listener -> {
            try {
                listener.notifyContentChanged(PERC_COMMENTS_SERVICES, sites);
            } catch (Exception e) {
                log.error("Error notifying listener: " + listener, e);
            }
        });
    }

    @Override
    public List<IPSComment> findComments(PSCommentCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria must not be null");

        return dao.findComments(criteria).stream()
            .filter(this::isCommentVisible)
            .collect(Collectors.toUnmodifiableList());
    }

    private boolean isCommentVisible(IPSComment comment) {
        var commentAge = Duration.between(
            comment.getCreatedDate().toInstant(),
            Instant.now()
        );
        return commentAge.compareTo(COMMENT_VISIBILITY_DURATION) <= 0;
    }

    @Override
    public Optional<IPSComment> getComment(String id) {
        return Optional.ofNullable(id)
            .map(dao::getComment);
    }

    @Override
    public PSPageSummaries getPageSummaries(PSCommentCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria must not be null");

        var summaries = dao.getPageSummaries(criteria).stream()
            .map(this::enrichPageSummary)
            .collect(Collectors.toUnmodifiableList());

        return new PSPageSummaries(summaries);
    }

    private PSPageSummary enrichPageSummary(PSPageSummary summary) {
        return PSPageSummary.builder()
            .pagePath(summary.getPagePath())
            .commentCount(summary.getCommentCount())
            .approvedCount(summary.getApprovedCount())
            .newCommentCount(summary.getNewCommentCount())
            .build();
    }
    /**
     * Moderate the comments with the given IDs and approval state.
     * Notifies listeners of changes in comments so that cache regions can be flushed.
     * 
     * @param commentIds A list of comment IDs to moderate.
     * @param newApprovalState The new approval state for the given comments.
     */
    private void moderateComments(Collection<String> commentIds, APPROVAL_STATE newApprovalState)
    {
        Set<String> siteNames = new HashSet<>();
        try
        {
        	siteNames = dao.findSitesForCommentIds(commentIds);
            this.fireDataChangeRequestedEvent(siteNames);
        	dao.moderate(commentIds, newApprovalState);
        }
        catch (Exception ex)
        {
            log.error("Error in moderating comments: {}",PSExceptionUtils.getMessageForLog(ex));
            log.debug(ex);
            throw new RuntimeException(ex);
        }
        finally
        {
            this.fireDataChangedEvent(siteNames);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.percussion.comments.services.IPSCommentsService#deleteComments(java
     * .util.List)
     */
    public void deleteComments(Collection<String> commentIds)
    {
        Validate.notNull(commentIds);
        
        if (commentIds.size() == 0)
        {
            log.info("Comment IDs list is empty.");
            return;
        }
        
        

        log.info("Deleting comments with the following IDs: {}" , commentIds);

        Set<String> siteNames = new HashSet<>();
        try
        {
        	siteNames = dao.findSitesForCommentIds(commentIds);
            this.fireDataChangeRequestedEvent(siteNames);
        	dao.delete(commentIds);
        }
        catch (Exception ex)
        {
            log.error("Error in deleting comments: {}" , PSExceptionUtils.getMessageForLog(ex));
            log.debug(ex);
            throw new RuntimeException(ex);
        }
        finally
        {
            this.fireDataChangedEvent(siteNames);
        }
    }    

    /**
     * Gets all the comments according to the given criteria.
     * 
     * @param criteria The criteria object.
     * @return A PSComments object which has a comment list.
     */
    @SuppressWarnings("unchecked")
    public PSComments getComments(PSCommentCriteria criteria, boolean isModerator)
    {
        log.info("Getting all comments according to the given criteria object");

        List<IPSComment> comments = new ArrayList<>();

       
        try
        {
            List<IPSComment> result = dao.find(criteria);

            for (IPSComment com : result)
            {
                if (!isModerator && APPROVAL_STATE.REJECTED.equals(com.getApprovalState()))
                {
                    Calendar currentDate = Calendar.getInstance();
                    Calendar commentDate = Calendar.getInstance();
                    commentDate.setTime(com.getCreatedDate());

                    // Get the represented date in milliseconds
                    long milis1 = currentDate.getTimeInMillis();
                    long milis2 = commentDate.getTimeInMillis();

                    // Calculate difference in milliseconds
                    long diff = milis1 - milis2;

                    // Calculate difference in minutes
                    long diffMinutes = diff / (60 * 1000);

                    if (diffMinutes <= AMOUNT_MINUTES_COMMENT_VISIBLE)
                    {
                        comments.add(com);
                    }
                }
                else
                {
                    comments.add(com);
                }
            }

            // If 'isModerator', update the 'viewed' flag of fetched comments
            if (isModerator)
            {
                for (IPSComment com : result)
                {
                    markAsViewed(com);
                }
            }
        }
        catch (Exception ex)
        {
            log.error("Error in getting comments by criteria: {}" ,
                    PSExceptionUtils.getMessageForLog(ex));
            throw new RuntimeException(ex);
        }        

        return new PSComments(comments);
    }
    
    /**
     * Mark comment as viewed by moderator.
     * @param comment
     * @throws Exception
     */
    private void markAsViewed(IPSComment comment) throws Exception
    {
        if (!comment.isViewed())
        {
            comment.setViewed(true);
            dao.save(comment);
        }
    }

    

    /**
     * Get a page summary list (the ones with comments) according the site and
     * paging information (maxResult and startIndex).
     * 
     * @param site The site of the comments.
     * @param maxResults The maximum number of pages to return.
     * @param startIndex Specifies the offset of the first page to return.
     * @return A PSPageSummaries object with the list of pages with comments.
     */
    @SuppressWarnings("unchecked")    
    public PSPageSummaries getPagesWithComments(String site, int maxResults, int startIndex)
    {
        Validate.notEmpty(site);

        log.info("Getting all pages with comments");

        List<PSPageSummary> pageSummaries = new ArrayList<>();

        
        try
        {
        	List<PSPageInfo> result = dao.findPagesWithComments(site);

            pageSummaries = createPageSummaries(result);

            int startIndexProcessed = startIndex > 0 ? startIndex : 0;
            int maxResultProcessed = maxResults > 0 ? maxResults : pageSummaries.size();

            int fromIndex = startIndexProcessed * maxResultProcessed;
            int toIndex = (startIndexProcessed * maxResultProcessed) + maxResultProcessed;
            if (toIndex > pageSummaries.size())
                toIndex = pageSummaries.size();

            if (pageSummaries.size() > 0)
                pageSummaries = pageSummaries.subList(fromIndex, toIndex);
        }
        catch (Exception ex)
        {
            log.error("Error in getting pages with comments: {}" , 
                    PSExceptionUtils.getMessageForLog(ex));
            log.debug(ex);
            throw new RuntimeException(ex);
        }        

        return new PSPageSummaries(pageSummaries);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.percussion.comments.services.IPSCommentsService#getDefaultModerationState
     * (java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public APPROVAL_STATE getDefaultModerationState(String site)
    {
        try
        {
            APPROVAL_STATE state = dao.findDefaultModerationState(site);
            return state;

        }
        catch (Exception ex)
        {
            log.error("Error getting default moderation state: {}" , PSExceptionUtils.getMessageForLog(ex));
            throw new RuntimeException(ex);
        }
        
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.percussion.comments.services.IPSCommentsService#setDefaultModerationState
     * (java.lang.String,
     * com.percussion.comments.data.IPSComment.APPROVAL_STATE)
     */
    public void setDefaultModerationState(String sitename, APPROVAL_STATE dflt)
    {
        
        try
        {
            dao.saveDefaultModerationState(sitename, dflt);
        }
        catch (Exception ex)
        {
            log.error("Error setting default moderation state: {}" , PSExceptionUtils.getMessageForLog(ex));
            log.debug(ex);
            throw new RuntimeException(ex);
        }       

    }

    /**
     * Given a list of Object arrays with information of a custom HQL query,
     * creates a list of PSPageSummary objects.
     * 
     * @param pagePathSummaryQuery page info object.
     * @return A List of PSPageSummary objects.
     */
    private List<PSPageSummary> createPageSummaries(List<PSPageInfo> pagePathSummaryQuery)
    {
        List<PSPageSummary> pageSummaries = new ArrayList<>();

        // Create a Map with the pagepath as key, and a Long array with the
        // approved
        // comments count in the first position, and unapproved ones in the
        // second
        // position.
        Map<String, CommentCount> pagepathAndCommentsCountMap = new HashMap<>();
        CommentCount pagepathWithCommentCount;

        for (PSPageInfo tmp : pagePathSummaryQuery)
        {
            String realPagepath = tmp.getPagePath();
            String lowerCasedPagepath = realPagepath.toLowerCase();
            String approvalState = tmp.getApprovalState();
            Long count = (Long) tmp.getCommentCount();
            boolean viewed = tmp.isViewed();
            if (!pagepathAndCommentsCountMap.containsKey(lowerCasedPagepath))
            {
                pagepathWithCommentCount = new CommentCount(realPagepath);
                pagepathAndCommentsCountMap.put(lowerCasedPagepath, pagepathWithCommentCount);
            }
            else
            {
                pagepathWithCommentCount = pagepathAndCommentsCountMap.get(lowerCasedPagepath);
            }

            if (approvalState.equals(APPROVAL_STATE.APPROVED.toString()))
                pagepathWithCommentCount.approvedCount += count;
            else
                pagepathWithCommentCount.unapprovedCount += count;
            if (!viewed)
                pagepathWithCommentCount.newComments += count;
        }

        // Create the PSPageSummary objects list using the Map object generated
        // above.
        long commentCount;
        long approvedCount;
        long newCommentsCount;
        for (String pagePath : pagepathAndCommentsCountMap.keySet())
        {
            pagepathWithCommentCount = pagepathAndCommentsCountMap.get(pagePath);
            commentCount = 0;
            approvedCount = 0;
            newCommentsCount = 0;

            // Check if we have information about approved comments for this
            // pagepath
            if (pagepathWithCommentCount.approvedCount != null)
            {
                commentCount += pagepathWithCommentCount.approvedCount;
                approvedCount = pagepathWithCommentCount.approvedCount;
            }

            // Check if we have information about unapproved comments for this
            // pagepath
            if (pagepathWithCommentCount.unapprovedCount != null)
            {
                commentCount += pagepathWithCommentCount.unapprovedCount;
            }

            if (pagepathWithCommentCount.newComments != null)
            {
                newCommentsCount = pagepathWithCommentCount.newComments;
            }

            pageSummaries.add(new PSPageSummary(pagepathWithCommentCount.pagepath, commentCount, approvedCount,
                    newCommentsCount));
        }

        // Sort the list by pagepath in ascending order
        Collections.sort(pageSummaries, new Comparator<PSPageSummary>()
        {
            public int compare(PSPageSummary o1, PSPageSummary o2)
            {
                return o1.getPagePath().compareTo(o2.getPagePath());
            }
        });

        return pageSummaries;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.percussion.comments.services.IPSCommentsService#getTags(int,
     * int)
     */
    public List<String> getTags(int maxResults, int startIndex)
    {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see com.percussion.metadata.IPSMetadataIndexerService#addMetadataListener(com.percussion.metadata.event.IPSMetadataListener)
     */
    public void addMetadataListener(IPSServiceDataChangeListener listener)
    {
        Validate.notNull(listener, "listener cannot be null.");
        if(!listeners.contains(listener))
            listeners.add(listener);
        
    }

    /* (non-Javadoc)
     * @see com.percussion.metadata.IPSMetadataIndexerService#removeMetadataListener(com.percussion.metadata.event.IPSMetadataListener)
     */
    public void removeMetadataListener(IPSServiceDataChangeListener listener)
    {
        Validate.notNull(listener, "listener cannot be null.");
        if(listeners.contains(listener))
            listeners.remove(listener);
    }

    /**
     * Fire a data change event for all registered listeners.
     */
    private void fireDataChangedEvent(Set<String> sites)
    {
        if(sites == null || sites.size() == 0)
        {
            return;
        }

        for(IPSServiceDataChangeListener listener : listeners)
        {
            listener.dataChanged(sites, this.PERC_COMMENTS_SERVICES);
        }
    }

    /**
     * Fire a data change event for all registered listeners.
     */
    private void fireDataChangeRequestedEvent(Set<String> sites)
    {
        if(sites == null || sites.size() == 0)
        {
            return;
        }

        for(IPSServiceDataChangeListener listener : listeners)
        {
            listener.dataChangeRequested(sites, this.PERC_COMMENTS_SERVICES);
        }
    }

    @Override
    public boolean updateCommentsForRenameSite(String prevSiteName,
                                               String newSiteName) {
        PSCommentCriteria criteria = new PSCommentCriteria();
        criteria.setSite(prevSiteName);
        try {
            List<IPSComment> comments = dao.find(criteria);
            for (IPSComment comment : comments) {
                comment.setSite(newSiteName);
                try {
                    dao.save(comment);
                } catch (Exception e) {
                    log.error("Error updating comment with id: {} An administrator should attempt to update the comment manually. Error: {}",
                            comment.getId(),
                            PSExceptionUtils.getMessageForLog(e));
                    log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                }
            }
        } catch (Exception e) {
            log.error("Error finding comments for site: {} Error: {}",
                    prevSiteName,
                    PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            return false;
        }
        return true;
    }
}

/**
 * Small class to represent a real pagepath (not the lowercased one) and the
 * amount of approved and unapproved comments posted there.
 * 
 * @author miltonpividori
 * 
 */
class CommentCount
{
    String pagepath;

    Long approvedCount;

    Long unapprovedCount;

    Long newComments;

    CommentCount(String pagepath)
    {
        this.pagepath = pagepath;
        this.approvedCount = 0L;
        this.unapprovedCount = 0L;
        this.newComments = 0L;
    }
}
